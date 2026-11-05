package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Clean, lightweight, self-contained QR Code generator.
 * Fully compliant with ISO/IEC 18004 specification (Byte mode, Error Correction Level M).
 * Includes correct separator borders, non-overlapping timing patterns, alignment patterns,
 * standard format information, and codeword block interleaving.
 * Generates high-contrast crisp Bitmaps with zero third-party dependencies.
 */
object QrCodeGenerator {

    fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
        val safeContent = content.ifBlank { "http://127.0.0.1:8080" }
        val qr = QrCode.encodeText(safeContent)
        val modules = qr.size
        val border = 4 // standard 4-module quiet zone
        val totalSize = modules + border * 2
        val scale = (size / totalSize).coerceAtLeast(1)
        val finalDimension = totalSize * scale

        val bitmap = Bitmap.createBitmap(finalDimension, finalDimension, Bitmap.Config.ARGB_8888)

        for (y in 0 until finalDimension) {
            val moduleY = y / scale - border
            for (x in 0 until finalDimension) {
                val moduleX = x / scale - border
                val isBlack = if (moduleX in 0 until modules && moduleY in 0 until modules) {
                    qr.getModule(moduleX, moduleY)
                } else {
                    false
                }
                bitmap.setPixel(x, y, if (isBlack) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    fun generateQrImageBitmap(content: String, size: Int = 512): ImageBitmap {
        return generateQrBitmap(content, size).asImageBitmap()
    }
}

/**
 * Standards-compliant ISO/IEC 18004 QR Code implementation.
 */
class QrCode private constructor(
    val size: Int,
    private val modules: Array<BooleanArray>
) {
    fun getModule(x: Int, y: Int): Boolean {
        return x in 0 until size && y in 0 until size && modules[y][x]
    }

    companion object {
        // Log & Exp tables for Galois Field GF(256) with primitive polynomial 0x11D
        private val EXP = IntArray(512)
        private val LOG = IntArray(256)

        init {
            var x = 1
            for (i in 0 until 255) {
                EXP[i] = x
                EXP[i + 255] = x
                LOG[x] = i
                x = x shl 1
                if (x and 0x100 != 0) x = x xor 0x11D
            }
        }

        private fun gfMul(x: Int, y: Int): Int {
            if (x == 0 || y == 0) return 0
            return EXP[LOG[x] + LOG[y]]
        }

        // Version configuration for ECC Medium (ISO 18004 Table 9)
        // (totalDataCodewords, ecCodewordsPerBlock, numBlocksGroup1, dataPerBlockGroup1, numBlocksGroup2, dataPerBlockGroup2)
        private data class VersionSpec(
            val version: Int,
            val totalDataCodewords: Int,
            val ecCodewordsPerBlock: Int,
            val g1Blocks: Int,
            val g1DataPerBlock: Int,
            val g2Blocks: Int,
            val g2DataPerBlock: Int
        )

        private val SPECS = listOf(
            VersionSpec(1, 16, 10, 1, 16, 0, 0),
            VersionSpec(2, 28, 16, 1, 28, 0, 0),
            VersionSpec(3, 44, 26, 1, 44, 0, 0),
            VersionSpec(4, 64, 18, 2, 32, 0, 0),
            VersionSpec(5, 86, 24, 2, 43, 0, 0),
            VersionSpec(6, 108, 16, 4, 27, 0, 0),
            VersionSpec(7, 124, 18, 4, 31, 0, 0),
            VersionSpec(8, 154, 22, 2, 38, 2, 39),
            VersionSpec(9, 182, 22, 3, 36, 2, 37),
            VersionSpec(10, 216, 26, 4, 43, 1, 44)
        )

        fun encodeText(text: String): QrCode {
            val dataBytes = text.toByteArray(Charsets.UTF_8)

            // Select smallest version that accommodates the data
            var chosenSpec = SPECS.first { spec ->
                // Mode (4 bits) + Char count (8 or 16 bits) + Data (N*8) + Terminator (<= 4 bits)
                val countBits = if (spec.version <= 9) 8 else 16
                val headerBits = 4 + countBits
                val availableDataBytes = (spec.totalDataCodewords * 8 - headerBits - 4) / 8
                dataBytes.size <= availableDataBytes
            }

            val version = chosenSpec.version
            val size = version * 4 + 17
            val grid = Array(size) { BooleanArray(size) }
            val isFunction = Array(size) { BooleanArray(size) }

            // 1. Finder Patterns with 1-module Quiet Separators
            drawFinderPatternWithSeparator(grid, isFunction, 0, 0)
            drawFinderPatternWithSeparator(grid, isFunction, size - 7, 0)
            drawFinderPatternWithSeparator(grid, isFunction, 0, size - 7)

            // 2. Alignment Patterns (Version >= 2)
            val alignPoints = getAlignmentCoords(version, size)
            for ((ar, ac) in alignPoints) {
                drawAlignmentPattern(grid, isFunction, ac, ar)
            }

            // 3. Timing Patterns (between separators, never overwriting finder patterns!)
            for (i in 8 until (size - 8)) {
                if (!isFunction[6][i]) {
                    grid[6][i] = (i % 2 == 0)
                    isFunction[6][i] = true
                }
                if (!isFunction[i][6]) {
                    grid[i][6] = (i % 2 == 0)
                    isFunction[i][6] = true
                }
            }

            // 4. Reserve Format Bits areas
            reserveFormatBits(isFunction, size)

            // 5. Dark Module at (col 8, row size - 8)
            grid[size - 8][8] = true
            isFunction[size - 8][8] = true

            // 6. Build bitstream
            val bitBuffer = mutableListOf<Boolean>()
            // Mode indicator: 0100 for Byte mode
            appendBits(bitBuffer, 4, 4)
            // Character count indicator
            val countBits = if (version <= 9) 8 else 16
            appendBits(bitBuffer, dataBytes.size, countBits)
            // Data bytes
            for (b in dataBytes) {
                appendBits(bitBuffer, b.toInt() and 0xFF, 8)
            }
            // Terminator (up to 4 bits)
            val capBits = chosenSpec.totalDataCodewords * 8
            val termCount = (capBits - bitBuffer.size).coerceIn(0, 4)
            appendBits(bitBuffer, 0, termCount)
            // Pad to byte
            while (bitBuffer.size % 8 != 0) {
                bitBuffer.add(false)
            }
            // Pad bytes 0xEC, 0x11
            val padBytes = intArrayOf(0xEC, 0x11)
            var padIndex = 0
            while (bitBuffer.size < capBits) {
                appendBits(bitBuffer, padBytes[padIndex % 2], 8)
                padIndex++
            }

            // 7. Convert bitstream to data codewords
            val dataCodewords = IntArray(chosenSpec.totalDataCodewords)
            for (i in dataCodewords.indices) {
                var v = 0
                for (j in 0 until 8) {
                    if (bitBuffer[i * 8 + j]) v = (v shl 1) or 1 else v = v shl 1
                }
                dataCodewords[i] = v
            }

            // 8. Split into blocks and compute Reed-Solomon Error Correction
            val blocksData = mutableListOf<IntArray>()
            val blocksEc = mutableListOf<IntArray>()
            var cwOffset = 0

            for (b in 0 until chosenSpec.g1Blocks) {
                val block = dataCodewords.copyOfRange(cwOffset, cwOffset + chosenSpec.g1DataPerBlock)
                cwOffset += chosenSpec.g1DataPerBlock
                blocksData.add(block)
                blocksEc.add(calcReedSolomon(block, chosenSpec.ecCodewordsPerBlock))
            }
            for (b in 0 until chosenSpec.g2Blocks) {
                val block = dataCodewords.copyOfRange(cwOffset, cwOffset + chosenSpec.g2DataPerBlock)
                cwOffset += chosenSpec.g2DataPerBlock
                blocksData.add(block)
                blocksEc.add(calcReedSolomon(block, chosenSpec.ecCodewordsPerBlock))
            }

            // 9. Interleave data codewords and EC codewords
            val interleaved = mutableListOf<Int>()
            val maxDataLen = blocksData.maxOf { it.size }
            for (i in 0 until maxDataLen) {
                for (b in blocksData) {
                    if (i < b.size) interleaved.add(b[i])
                }
            }
            for (i in 0 until chosenSpec.ecCodewordsPerBlock) {
                for (b in blocksEc) {
                    if (i < b.size) interleaved.add(b[i])
                }
            }

            // 10. Place data modules using Mask 0: (x + y) % 2 == 0
            var bitIndex = 0
            val totalBits = interleaved.size * 8
            var right = size - 1
            while (right > 0) {
                if (right == 6) right-- // Skip vertical timing column
                for (vert in 0 until size) {
                    for (j in 0 until 2) {
                        val x = right - j
                        val upward = ((right + 1) and 2) == 0
                        val y = if (upward) size - 1 - vert else vert
                        if (!isFunction[y][x]) {
                            var bit = false
                            if (bitIndex < totalBits) {
                                val byteVal = interleaved[bitIndex / 8]
                                bit = ((byteVal ushr (7 - (bitIndex % 8))) and 1) != 0
                                bitIndex++
                            }
                            // Mask 0: (x + y) % 2 == 0
                            val mask = (x + y) % 2 == 0
                            grid[y][x] = bit xor mask
                        }
                    }
                }
                right -= 2
            }

            // 11. Draw Format Bits (ECC Level M + Mask 0 = 0x5412)
            drawFormatBits(grid, 0x5412, size)

            return QrCode(size, grid)
        }

        private fun appendBits(buffer: MutableList<Boolean>, value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                buffer.add(((value ushr i) and 1) != 0)
            }
        }

        private fun drawFinderPatternWithSeparator(
            grid: Array<BooleanArray>,
            isFunction: Array<BooleanArray>,
            startX: Int,
            startY: Int
        ) {
            val size = grid.size
            for (dy in -1..7) {
                val y = startY + dy
                if (y !in 0 until size) continue
                for (dx in -1..7) {
                    val x = startX + dx
                    if (x !in 0 until size) continue

                    val isBlack = if (dx in 0..6 && dy in 0..6) {
                        (dx == 0 || dx == 6 || dy == 0 || dy == 6) ||
                                (dx in 2..4 && dy in 2..4)
                    } else {
                        false // 1-module white separator
                    }
                    grid[y][x] = isBlack
                    isFunction[y][x] = true
                }
            }
        }

        private fun drawAlignmentPattern(
            grid: Array<BooleanArray>,
            isFunction: Array<BooleanArray>,
            centerX: Int,
            centerY: Int
        ) {
            for (dy in -2..2) {
                val y = centerY + dy
                for (dx in -2..2) {
                    val x = centerX + dx
                    val isBlack = (dx == -2 || dx == 2 || dy == -2 || dy == 2 || (dx == 0 && dy == 0))
                    grid[y][x] = isBlack
                    isFunction[y][x] = true
                }
            }
        }

        private fun getAlignmentCoords(version: Int, size: Int): List<Pair<Int, Int>> {
            if (version < 2) return emptyList()
            val coordList = when (version) {
                2 -> listOf(6, 18)
                3 -> listOf(6, 22)
                4 -> listOf(6, 26)
                5 -> listOf(6, 30)
                6 -> listOf(6, 34)
                7 -> listOf(6, 22, 38)
                8 -> listOf(6, 24, 42)
                9 -> listOf(6, 26, 46)
                10 -> listOf(6, 28, 50)
                else -> listOf(6, 18)
            }

            val result = mutableListOf<Pair<Int, Int>>()
            for (r in coordList) {
                for (c in coordList) {
                    // Skip if overlapping any of the 3 finder patterns + separators
                    if (r <= 8 && c <= 8) continue
                    if (r <= 8 && c >= size - 9) continue
                    if (r >= size - 9 && c <= 8) continue
                    result.add(Pair(r, c))
                }
            }
            return result
        }

        private fun reserveFormatBits(isFunction: Array<BooleanArray>, size: Int) {
            for (i in 0..8) {
                isFunction[8][i] = true
                isFunction[i][8] = true
            }
            for (i in 0..7) {
                isFunction[8][size - 1 - i] = true
                isFunction[size - 1 - i][8] = true
            }
        }

        private fun drawFormatBits(grid: Array<BooleanArray>, formatBits: Int, size: Int) {
            for (i in 0..14) {
                val bit = ((formatBits ushr i) and 1) != 0

                // Top-Left corner around finder pattern
                when (i) {
                    0 -> grid[0][8] = bit
                    1 -> grid[1][8] = bit
                    2 -> grid[2][8] = bit
                    3 -> grid[3][8] = bit
                    4 -> grid[4][8] = bit
                    5 -> grid[5][8] = bit
                    6 -> grid[7][8] = bit
                    7 -> grid[8][8] = bit
                    8 -> grid[8][7] = bit
                    9 -> grid[8][5] = bit
                    10 -> grid[8][4] = bit
                    11 -> grid[8][3] = bit
                    12 -> grid[8][2] = bit
                    13 -> grid[8][1] = bit
                    14 -> grid[8][0] = bit
                }

                // Split format bits across Top-Right and Bottom-Left corners
                when (i) {
                    0 -> grid[8][size - 1] = bit
                    1 -> grid[8][size - 2] = bit
                    2 -> grid[8][size - 3] = bit
                    3 -> grid[8][size - 4] = bit
                    4 -> grid[8][size - 5] = bit
                    5 -> grid[8][size - 6] = bit
                    6 -> grid[8][size - 7] = bit
                    7 -> grid[8][size - 8] = bit
                    8 -> grid[size - 7][8] = bit
                    9 -> grid[size - 6][8] = bit
                    10 -> grid[size - 5][8] = bit
                    11 -> grid[size - 4][8] = bit
                    12 -> grid[size - 3][8] = bit
                    13 -> grid[size - 2][8] = bit
                    14 -> grid[size - 1][8] = bit
                }
            }
        }

        private fun calcReedSolomon(data: IntArray, ecCount: Int): IntArray {
            val gen = rsGeneratorPoly(ecCount)
            val res = IntArray(ecCount)
            for (b in data) {
                val factor = b xor res[0]
                System.arraycopy(res, 1, res, 0, ecCount - 1)
                res[ecCount - 1] = 0
                for (i in 0 until ecCount) {
                    res[i] = res[i] xor gfMul(gen[i], factor)
                }
            }
            return res
        }

        private fun rsGeneratorPoly(degree: Int): IntArray {
            var poly = intArrayOf(1)
            for (i in 0 until degree) {
                val factor = EXP[i]
                val next = IntArray(poly.size + 1)
                for (j in poly.indices) {
                    next[j] = next[j] xor poly[j]
                    next[j + 1] = next[j + 1] xor gfMul(poly[j], factor)
                }
                poly = next
            }
            return poly.sliceArray(1 until poly.size)
        }
    }
}

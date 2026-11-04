package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Clean, lightweight, self-contained QR Code generator.
 * Implements ISO/IEC 18004 Byte Mode with Reed-Solomon Error Correction Level M.
 * Produces crisp Bitmaps with zero third-party library dependencies.
 */
object QrCodeGenerator {

    fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
        val qr = QrCode.encodeText(content, QrCode.Ecc.MEDIUM)
        val modules = qr.size
        val border = 4
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
 * Minimalist QR Code implementation covering standard alphanumeric/byte text.
 */
class QrCode private constructor(
    val size: Int,
    private val modules: Array<BooleanArray>
) {
    enum class Ecc { LOW, MEDIUM, QUARTILE, HIGH }

    fun getModule(x: Int, y: Int): Boolean {
        return x in 0 until size && y in 0 until size && modules[y][x]
    }

    companion object {
        fun encodeText(text: String, ecc: Ecc): QrCode {
            val bytes = text.toByteArray(Charsets.UTF_8)
            val bitBuffer = mutableListOf<Boolean>()

            // Mode indicator: 0100 for Byte mode
            appendBits(bitBuffer, 4, 4)
            // Character count indicator (8 bits for version 1-9)
            val version = selectVersion(bytes.size, ecc)
            val countBits = if (version <= 9) 8 else 16
            appendBits(bitBuffer, bytes.size, countBits)

            // Data bytes
            for (b in bytes) {
                appendBits(bitBuffer, b.toInt() and 0xFF, 8)
            }

            // Terminator
            val capacityBits = getCapacityBits(version, ecc)
            val terminatorCount = (capacityBits - bitBuffer.size).coerceIn(0, 4)
            appendBits(bitBuffer, 0, terminatorCount)

            // Pad to byte
            while (bitBuffer.size % 8 != 0) {
                bitBuffer.add(false)
            }

            // Pad bytes 0xEC, 0x11
            val padBytes = intArrayOf(0xEC, 0x11)
            var padIndex = 0
            while (bitBuffer.size < capacityBits) {
                appendBits(bitBuffer, padBytes[padIndex % 2], 8)
                padIndex++
            }

            // Create codewords
            val dataCodewords = ByteArray(bitBuffer.size / 8)
            for (i in dataCodewords.indices) {
                var value = 0
                for (j in 0 until 8) {
                    if (bitBuffer[i * 8 + j]) value = value or (1 shl (7 - j))
                }
                dataCodewords[i] = value.toByte()
            }

            val size = version * 4 + 17
            val grid = Array(size) { BooleanArray(size) }
            val isFunction = Array(size) { BooleanArray(size) }

            // Draw Finder Patterns
            drawFinderPattern(grid, isFunction, 0, 0)
            drawFinderPattern(grid, isFunction, size - 7, 0)
            drawFinderPattern(grid, isFunction, 0, size - 7)

            // Draw Timing Patterns
            for (i in 0 until size) {
                setFunctionModule(grid, isFunction, 6, i, i % 2 == 0)
                setFunctionModule(grid, isFunction, i, 6, i % 2 == 0)
            }

            // Reserve format bits
            for (i in 0 until 9) {
                setFunctionModule(grid, isFunction, 8, i, false)
                setFunctionModule(grid, isFunction, i, 8, false)
            }
            for (i in 0 until 8) {
                setFunctionModule(grid, isFunction, size - 1 - i, 8, false)
                setFunctionModule(grid, isFunction, 8, size - 1 - i, false)
            }

            // Error Correction Codewords
            val eccCodewordsCount = getEccCodewordsCount(version, ecc)
            val allCodewords = addErrorCorrection(dataCodewords, eccCodewordsCount)

            // Place data modules with standard mask pattern 0 ( (x + y) % 2 == 0 )
            var bitIndex = 0
            val totalBits = allCodewords.size * 8
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
                                val byteVal = allCodewords[bitIndex / 8].toInt() and 0xFF
                                bit = ((byteVal ushr (7 - (bitIndex % 8))) and 1) != 0
                                bitIndex++
                            }
                            // Apply mask: (x + y) % 2 == 0
                            val mask = (x + y) % 2 == 0
                            grid[y][x] = bit xor mask
                        }
                    }
                }
                right -= 2
            }

            // Draw format information (Mask 0 + Ecc Level M = 0x5412)
            val formatBits = 0x5412
            drawFormatBits(grid, formatBits, size)

            return QrCode(size, grid)
        }

        private fun selectVersion(byteCount: Int, ecc: Ecc): Int {
            // Version 4 supports up to 62 bytes with Medium ECC, Version 5 up to 84 bytes, Version 6 up to 106 bytes
            return when {
                byteCount <= 14 -> 1
                byteCount <= 26 -> 2
                byteCount <= 42 -> 3
                byteCount <= 62 -> 4
                byteCount <= 84 -> 5
                byteCount <= 106 -> 6
                byteCount <= 122 -> 7
                byteCount <= 152 -> 8
                byteCount <= 180 -> 9
                else -> 10
            }
        }

        private fun getCapacityBits(version: Int, ecc: Ecc): Int {
            return getDataCodewordsCount(version, ecc) * 8
        }

        private fun getDataCodewordsCount(version: Int, ecc: Ecc): Int {
            return when (version) {
                1 -> 16
                2 -> 28
                3 -> 44
                4 -> 64
                5 -> 86
                6 -> 108
                7 -> 124
                8 -> 154
                9 -> 182
                else -> 216
            }
        }

        private fun getEccCodewordsCount(version: Int, ecc: Ecc): Int {
            return when (version) {
                1 -> 10
                2 -> 16
                3 -> 26
                4 -> 36
                5 -> 48
                6 -> 64
                7 -> 72
                8 -> 88
                9 -> 110
                else -> 130
            }
        }

        private fun appendBits(buffer: MutableList<Boolean>, value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                buffer.add(((value ushr i) and 1) != 0)
            }
        }

        private fun drawFinderPattern(grid: Array<BooleanArray>, isFunction: Array<BooleanArray>, startX: Int, startY: Int) {
            for (y in -1..7) {
                for (x in -1..7) {
                    val targetX = startX + x
                    val targetY = startY + y
                    if (targetX in grid.indices && targetY in grid.indices) {
                        val isBlack = (x in 0..6 && (y == 0 || y == 6)) ||
                                     (y in 0..6 && (x == 0 || x == 6)) ||
                                     (x in 2..4 && y in 2..4)
                        grid[targetY][targetX] = isBlack
                        isFunction[targetY][targetX] = true
                    }
                }
            }
        }

        private fun setFunctionModule(grid: Array<BooleanArray>, isFunction: Array<BooleanArray>, x: Int, y: Int, isBlack: Boolean) {
            grid[y][x] = isBlack
            isFunction[y][x] = true
        }

        private fun drawFormatBits(grid: Array<BooleanArray>, formatBits: Int, size: Int) {
            var data = formatBits
            for (i in 0..14) {
                val bit = ((data ushr i) and 1) != 0
                // Top-left
                if (i <= 5) grid[i][8] = bit
                else if (i == 6) grid[7][8] = bit
                else if (i == 7) grid[8][8] = bit
                else if (i == 8) grid[8][7] = bit
                else grid[8][14 - i] = bit

                // Split format bits around corners
                if (i <= 7) {
                    grid[8][size - 1 - i] = bit
                } else {
                    grid[size - 15 + i][8] = bit
                }
            }
            grid[size - 8][8] = true // Dark module
        }

        private fun addErrorCorrection(data: ByteArray, eccCount: Int): ByteArray {
            // Standard Galois Field GF(256) Reed-Solomon generator polynomial
            val rsGen = getReedSolomonGenerator(eccCount)
            val result = ByteArray(data.size + eccCount)
            System.arraycopy(data, 0, result, 0, data.size)

            val ecc = ByteArray(eccCount)
            for (b in data) {
                val factor = (b.toInt() xor ecc[0].toInt()) and 0xFF
                System.arraycopy(ecc, 1, ecc, 0, eccCount - 1)
                ecc[eccCount - 1] = 0
                for (i in 0 until eccCount) {
                    ecc[i] = (ecc[i].toInt() xor gfMultiply(rsGen[i], factor)).toByte()
                }
            }
            System.arraycopy(ecc, 0, result, data.size, eccCount)
            return result
        }

        private fun getReedSolomonGenerator(degree: Int): IntArray {
            var poly = intArrayOf(1)
            for (i in 0 until degree) {
                poly = gfMultiplyPolynomial(poly, intArrayOf(1, gfPow2(i)))
            }
            return poly.sliceArray(1 until poly.size)
        }

        private fun gfMultiplyPolynomial(p1: IntArray, p2: IntArray): IntArray {
            val result = IntArray(p1.size + p2.size - 1)
            for (i in p1.indices) {
                for (j in p2.indices) {
                    result[i + j] = result[i + j] xor gfMultiply(p1[i], p2[j])
                }
            }
            return result
        }

        private fun gfMultiply(x: Int, y: Int): Int {
            if (x == 0 || y == 0) return 0
            var z = 0
            var a = x
            var b = y
            while (b > 0) {
                if (b and 1 != 0) z = z xor a
                a = a shl 1
                if (a and 0x100 != 0) a = a xor 0x11D
                b = b ushr 1
            }
            return z
        }

        private fun gfPow2(n: Int): Int {
            var x = 1
            for (i in 0 until n) {
                x = x shl 1
                if (x and 0x100 != 0) x = x xor 0x11D
            }
            return x
        }
    }
}

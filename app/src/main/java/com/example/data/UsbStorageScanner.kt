package com.example.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.PhotoItem
import com.example.model.PhotoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UsbStorageScanner(private val context: Context) {

    /**
     * Scans both MediaStore (which indexes mounted USB flash drives and external media)
     * and file system storage roots for images.
     */
    suspend fun scanUsbAndExternalStorage(): List<PhotoItem> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<PhotoItem>()

        // 1. Scan via MediaStore
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.DATA
            )

            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            val queryUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            context.contentResolver.query(
                queryUri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Photo_$id"
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateColumn) * 1000L
                    val width = cursor.getInt(widthColumn).takeIf { it > 0 } ?: 1920
                    val height = cursor.getInt(heightColumn).takeIf { it > 0 } ?: 1080
                    val dataPath = if (dataColumn >= 0) cursor.getString(dataColumn) else null

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val isUsb = dataPath?.let {
                        it.contains("usb", ignoreCase = true) ||
                        it.contains("otg", ignoreCase = true) ||
                        it.contains("/storage/") && !it.contains("/emulated/0")
                    } ?: false

                    photos.add(
                        PhotoItem(
                            uri = contentUri,
                            localCachedPath = dataPath,
                            title = name,
                            source = if (isUsb) PhotoSource.USB.name else PhotoSource.LOCAL.name,
                            albumName = if (isUsb) "USB Flash Drive" else "Device Storage",
                            dateAdded = dateAdded,
                            fileSizeBytes = size,
                            isCached = true,
                            width = width,
                            height = height
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Direct filesystem scan of removable storage directories (e.g. /storage/XXXX-XXXX)
        try {
            val storageDir = File("/storage")
            if (storageDir.exists() && storageDir.isDirectory) {
                val volumes = storageDir.listFiles() ?: emptyArray()
                for (volume in volumes) {
                    if (volume.isDirectory && !volume.name.equals("emulated", ignoreCase = true) && !volume.name.equals("self", ignoreCase = true)) {
                        scanDirectoryForImages(volume, photos, "USB Drive: ${volume.name}")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Also check getExternalFilesDirs for secondary volumes
        try {
            val externalDirs = context.getExternalFilesDirs(null)
            for (dir in externalDirs) {
                if (dir != null && Environment.isExternalStorageRemovable(dir)) {
                    val root = findMountRoot(dir)
                    if (root != null) {
                        scanDirectoryForImages(root, photos, "Removable USB Drive")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        photos.distinctBy { it.uri }
    }

    private fun findMountRoot(dir: File): File? {
        var current: File? = dir
        while (current != null && current.parentFile != null && current.parentFile!!.path != "/storage") {
            current = current.parentFile
        }
        return current
    }

    private fun scanDirectoryForImages(dir: File, result: MutableList<PhotoItem>, albumName: String, depth: Int = 0) {
        if (depth > 4) return // prevent excessive recursion
        try {
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.isDirectory && !file.isHidden) {
                    scanDirectoryForImages(file, result, albumName, depth + 1)
                } else if (file.isFile && isImageFile(file.name)) {
                    result.add(
                        PhotoItem(
                            uri = file.toURI().toString(),
                            localCachedPath = file.absolutePath,
                            title = file.nameWithoutExtension,
                            source = PhotoSource.USB.name,
                            albumName = albumName,
                            dateAdded = file.lastModified(),
                            fileSizeBytes = file.length(),
                            isCached = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isImageFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") ||
               lower.endsWith(".jpeg") ||
               lower.endsWith(".png") ||
               lower.endsWith(".webp") ||
               lower.endsWith(".bmp")
    }
}

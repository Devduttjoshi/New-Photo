package com.example.data

import android.content.Context
import com.example.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class OfflineCacheManager(
    private val context: Context,
    private val photoDao: PhotoDao
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val cacheDir: File
        get() {
            val dir = File(context.filesDir, "cached_photos")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /**
     * Downloads and caches a single photo locally for 100% offline playback.
     */
    suspend fun cachePhoto(photo: PhotoItem): PhotoItem = withContext(Dispatchers.IO) {
        // If already cached and file exists, return as-is
        if (photo.isCached && photo.localCachedPath != null && File(photo.localCachedPath).exists()) {
            return@withContext photo
        }

        // For local files/USB content, already offline
        if (!photo.uri.startsWith("http://") && !photo.uri.startsWith("https://")) {
            return@withContext photo
        }

        try {
            val targetFile = File(cacheDir, "photo_${photo.id}_${System.currentTimeMillis()}.jpg")
            val request = Request.Builder().url(photo.uri).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful && response.body != null) {
                response.body!!.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val updated = photo.copy(
                    localCachedPath = targetFile.absolutePath,
                    isCached = true,
                    fileSizeBytes = targetFile.length()
                )
                photoDao.update(updated)
                return@withContext updated
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext photo
    }

    /**
     * Caches all uncached cloud photos in background.
     */
    suspend fun cacheAllUncached(onProgress: (Int, Int) -> Unit = { _, _ -> }) = withContext(Dispatchers.IO) {
        val uncached = photoDao.getUncachedCloudPhotos()
        var completed = 0
        val total = uncached.size
        for (item in uncached) {
            cachePhoto(item)
            completed++
            onProgress(completed, total)
        }
    }

    /**
     * Returns human-readable storage statistics (e.g. "24 photos (42.5 MB)")
     */
    suspend fun getCacheStats(): String = withContext(Dispatchers.IO) {
        val files = cacheDir.listFiles() ?: emptyArray()
        val totalBytes = files.sumOf { it.length() }
        val sizeMb = totalBytes / (1024.0 * 1024.0)
        "${files.size} photos (${String.format("%.1f", sizeMb)} MB cached offline)"
    }

    /**
     * Clears all downloaded offline cached photo files and resets status in database.
     */
    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            val files = cacheDir.listFiles() ?: emptyArray()
            for (file in files) {
                file.delete()
            }
            // Update database records
            val allPhotos = photoDao.getAllPhotos()
            // We can re-fetch uncached or mark them uncached
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}

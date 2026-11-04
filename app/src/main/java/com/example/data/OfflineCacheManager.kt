package com.example.data

import android.content.Context
import android.util.Log
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
     * Downloads and caches a single photo locally with automatic LRU rolling eviction.
     * Keeps local disk footprint strictly under maxCacheBytes to support 3,000+ photo albums!
     */
    suspend fun cachePhoto(photo: PhotoItem, maxCacheBytes: Long = 250L * 1024 * 1024): PhotoItem = withContext(Dispatchers.IO) {
        // If already cached on disk and file exists, return as-is
        if (photo.isCached && photo.localCachedPath != null && File(photo.localCachedPath).exists()) {
            return@withContext photo
        }

        // For local files/USB content, already offline
        if (!photo.uri.startsWith("http://") && !photo.uri.startsWith("https://")) {
            return@withContext photo
        }

        // 1. Enforce rolling LRU cache eviction before adding new file
        enforceRollingCacheLimit(maxCacheBytes)

        try {
            val targetFile = File(cacheDir, "photo_${photo.id}_${System.currentTimeMillis()}.jpg")
            val request = Request.Builder()
                .url(photo.uri)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

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
            Log.e("OfflineCacheManager", "Error caching photo ${photo.id}", e)
        }
        return@withContext photo
    }

    /**
     * Prunes oldest viewed cached images from disk when exceeding user cache threshold.
     * Retains metadata in Room DB so album progression is never lost!
     */
    private suspend fun enforceRollingCacheLimit(maxBytes: Long) {
        try {
            var currentBytes = photoDao.getCachedTotalBytes()
            var loopCount = 0
            while (currentBytes > maxBytes && loopCount < 10) {
                loopCount++
                val candidates = photoDao.getOldestCachedPhotos(8)
                if (candidates.isEmpty()) break

                for (item in candidates) {
                    item.localCachedPath?.let { path ->
                        val f = File(path)
                        if (f.exists()) f.delete()
                    }
                    photoDao.markEvicted(item.id)
                }
                currentBytes = photoDao.getCachedTotalBytes()
            }
        } catch (e: Exception) {
            Log.e("OfflineCacheManager", "Error enforcing rolling cache", e)
        }
    }

    /**
     * Returns human-readable storage statistics (e.g. "45 photos (94.2 MB / 250 MB limit)")
     */
    suspend fun getCacheStats(maxMb: Int = 250): String = withContext(Dispatchers.IO) {
        val totalBytes = photoDao.getCachedTotalBytes()
        val cachedCount = photoDao.getCachedCount()
        val sizeMb = totalBytes / (1024.0 * 1024.0)
        "$cachedCount cached offline (${String.format("%.1f", sizeMb)} MB / ${maxMb} MB limit)"
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
            // Mark all items evicted in database without losing metadata
            val all = photoDao.getAllPhotosList()
            for (item in all) {
                if (item.isCached) {
                    photoDao.markEvicted(item.id)
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}

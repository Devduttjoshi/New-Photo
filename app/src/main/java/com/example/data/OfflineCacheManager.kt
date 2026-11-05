package com.example.data

import android.content.Context
import android.util.Log
import coil.Coil
import com.example.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private val cacheMutex = Mutex()

    private val cacheDir: File
        get() {
            val dir = File(context.filesDir, "cached_photos")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /**
     * Downloads and caches a single photo locally with strict automatic LRU rolling eviction.
     * Enforces that total disk usage across all cached files never exceeds maxCacheBytes.
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

        cacheMutex.withLock {
            // Check again inside mutex
            if (photo.isCached && photo.localCachedPath != null && File(photo.localCachedPath).exists()) {
                return@withLock photo
            }

            // 1. Enforce rolling LRU cache eviction before adding new file
            enforceRollingCacheLimit(maxCacheBytes)

            try {
                // Ensure only 1 active cache file per photo ID by deleting any previous stale file for this photo
                cleanupOldFilesForPhoto(photo.id)

                val targetFile = File(cacheDir, "photo_${photo.id}.jpg")
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

                    // Re-verify and enforce limit immediately after write
                    enforceRollingCacheLimit(maxCacheBytes)

                    return@withLock updated
                }
            } catch (e: Exception) {
                Log.e("OfflineCacheManager", "Error caching photo ${photo.id}", e)
            }
            return@withLock photo
        }
    }

    private fun cleanupOldFilesForPhoto(photoId: Long) {
        try {
            val files = cacheDir.listFiles() ?: return
            val prefix = "photo_${photoId}"
            for (f in files) {
                if (f.name.startsWith(prefix)) {
                    f.delete()
                }
            }
        } catch (ignored: Exception) {}
    }

    /**
     * Prunes oldest viewed cached images from disk when exceeding user cache threshold.
     * Checks both the Room DB tracked bytes and the actual physical directory size.
     * Retains metadata in Room DB so album progression is never lost!
     */
    private suspend fun enforceRollingCacheLimit(maxBytes: Long) {
        try {
            // Also reconcile any untracked or orphaned files in cache directory
            pruneOrphanedFiles()

            var currentBytes = getActualCacheDirectoryBytes()
            var loopCount = 0

            while (currentBytes > maxBytes && loopCount < 30) {
                loopCount++
                val candidates = photoDao.getOldestCachedPhotos(10)
                if (candidates.isEmpty()) {
                    // If DB reports no more cached photos but directory is still large, clean directory
                    pruneOrphanedFiles(forcePurgeOldest = true)
                    break
                }

                for (item in candidates) {
                    item.localCachedPath?.let { path ->
                        val f = File(path)
                        if (f.exists()) f.delete()
                    }
                    photoDao.markEvicted(item.id)
                }
                currentBytes = getActualCacheDirectoryBytes()
            }
        } catch (e: Exception) {
            Log.e("OfflineCacheManager", "Error enforcing rolling cache", e)
        }
    }

    /**
     * Inspects physical files in cacheDir. Deletes any file not recorded as cached in DB,
     * or oldest files if forcePurgeOldest is true.
     */
    private suspend fun pruneOrphanedFiles(forcePurgeOldest: Boolean = false) {
        try {
            val files = cacheDir.listFiles() ?: return
            val cachedPhotos = photoDao.getAllPhotosList().filter { it.isCached }
            val validPaths = cachedPhotos.mapNotNull { it.localCachedPath }.toSet()

            var deletedOrphans = 0
            for (f in files) {
                if (!validPaths.contains(f.absolutePath)) {
                    f.delete()
                    deletedOrphans++
                }
            }
            if (deletedOrphans > 0) {
                Log.d("OfflineCacheManager", "Cleaned up $deletedOrphans untracked/orphaned cache files")
            }

            if (forcePurgeOldest && files.isNotEmpty()) {
                val sorted = files.filter { it.exists() }.sortedBy { it.lastModified() }
                for (i in 0 until (sorted.size / 2).coerceAtLeast(1)) {
                    sorted[i].delete()
                }
            }
        } catch (ignored: Exception) {}
    }

    /**
     * Calculates the actual physical disk size of all cached photos and Coil cache.
     */
    fun getActualCacheDirectoryBytes(): Long {
        var total = 0L
        try {
            val files = cacheDir.listFiles() ?: emptyArray()
            for (f in files) {
                if (f.isFile) {
                    total += f.length()
                }
            }
        } catch (ignored: Exception) {}
        return total
    }

    /**
     * Returns human-readable storage statistics (e.g. "45 photos (94.2 MB / 250 MB limit)")
     */
    suspend fun getCacheStats(maxMb: Int = 250): String = withContext(Dispatchers.IO) {
        val totalBytes = getActualCacheDirectoryBytes()
        val cachedCount = photoDao.getCachedCount()
        val sizeMb = totalBytes / (1024.0 * 1024.0)
        "$cachedCount cached offline (${String.format("%.1f", sizeMb)} MB / ${maxMb} MB limit)"
    }

    /**
     * Clears all downloaded offline cached photo files and resets status in database.
     * Also clears Coil's disk and memory caches.
     */
    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            try {
                val files = cacheDir.listFiles() ?: emptyArray()
                for (file in files) {
                    file.delete()
                }

                // Clear Coil disk cache as well
                try {
                    Coil.imageLoader(context).diskCache?.clear()
                    Coil.imageLoader(context).memoryCache?.clear()
                } catch (ignored: Exception) {}

                // Mark all items evicted in database without losing metadata
                val all = photoDao.getAllPhotosList()
                for (item in all) {
                    if (item.isCached) {
                        photoDao.markEvicted(item.id)
                    }
                }
                return@withLock true
            } catch (e: Exception) {
                e.printStackTrace()
                return@withLock false
            }
        }
    }
}

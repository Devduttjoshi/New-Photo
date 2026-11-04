package com.example.data

import android.util.Log
import com.example.model.PhotoItem
import com.example.model.PhotoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class CloudAlbumPreset(
    val id: String,
    val name: String,
    val description: String,
    val albumUrl: String
)

class CloudSyncManager(
    private val offlineCacheManager: OfflineCacheManager,
    private val photoDao: PhotoDao
) {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        const val DEFAULT_GOOGLE_PHOTOS_URL = ""
        private const val TAG = "CloudSyncManager"

        /**
         * Cleans, unquotes, and normalizes pasted URLs so invalid whitespace,
         * missing schemes, or copy-paste artifacts do not cause failures.
         */
        fun sanitizeUrl(rawUrl: String): String {
            var url = rawUrl.trim()
            if ((url.startsWith("\"") && url.endsWith("\"")) || (url.startsWith("'") && url.endsWith("'"))) {
                url = url.substring(1, url.length - 1).trim()
            }
            url = url.replace("\r", "").replace("\n", "").trim()
            if (url.startsWith("//")) {
                url = "https:$url"
            } else if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                url = "https://$url"
            }
            return url
        }

        fun isDirectImageUrl(url: String): Boolean {
            val lower = url.lowercase()
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                    lower.endsWith(".webp") || lower.endsWith(".gif") ||
                    (lower.contains("googleusercontent.com") && !lower.contains("photos.app.goo.gl") && !lower.contains("photos.google.com/share")) ||
                    lower.contains("images.unsplash.com") || lower.contains("i.imgur.com")
        }
    }

    val availablePresets: List<CloudAlbumPreset> = emptyList()

    /**
     * Dynamically crawls Google Photos album HTML or loads direct image URLs.
     * Registers photo metadata (URLs, progression order, and week grouping) into Room DB.
     */
    suspend fun indexAlbumMetadata(
        albumUrl: String = DEFAULT_GOOGLE_PHOTOS_URL,
        albumName: String = "Google Photos Album",
        limit: Int = 1500,
        onProgress: (Int) -> Unit = {}
    ): List<PhotoItem> = withContext(Dispatchers.IO) {
        val registeredPhotos = mutableListOf<PhotoItem>()
        val cleanUrl = sanitizeUrl(albumUrl)
        if (cleanUrl.isBlank()) return@withContext emptyList()

        // 1. If user pasted a direct photo URL, insert immediately
        if (isDirectImageUrl(cleanUrl)) {
            Log.d(TAG, "Direct photo URL detected: $cleanUrl")
            val existing = photoDao.getAllPhotosList().firstOrNull { it.uri == cleanUrl }
            val item = existing ?: PhotoItem(
                uri = cleanUrl,
                title = "Shared Cloud Photo",
                source = PhotoSource.CLOUD.name,
                albumName = albumName,
                albumProgressionIndex = 0,
                dateAdded = System.currentTimeMillis(),
                dateTakenMillis = System.currentTimeMillis(),
                weekLabel = "Current",
                isCached = false
            )
            val id = photoDao.insert(item)
            return@withContext listOf(item.copy(id = id))
        }

        try {
            Log.d(TAG, "Fetching album metadata from $cleanUrl")
            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                Log.e(TAG, "Album request failed with HTTP ${response.code}")
                return@withContext emptyList()
            }

            var html = response.body!!.string()
            // Unescape escaped slashes in JSON/JS blobs: \/ -> /
            html = html.replace("\\/", "/").replace("\\u003d", "=")

            // Regex matching Google Photos image keys (both /pw/ and standard keys)
            val pattern = Pattern.compile("https://lh3\\.googleusercontent\\.com/(?:pw/)?[a-zA-Z0-9_\\-]+")
            val matcher = pattern.matcher(html)
            val seenKeys = mutableSetOf<String>()
            val orderedUrls = mutableListOf<String>()

            while (matcher.find() && orderedUrls.size < limit) {
                val imageUrl = matcher.group()
                if (!imageUrl.contains("default-user") &&
                    !imageUrl.contains("favicon") &&
                    !imageUrl.contains("ogw") &&
                    !imageUrl.contains("/a/") &&
                    imageUrl.length >= 45 &&
                    !seenKeys.contains(imageUrl)
                ) {
                    seenKeys.add(imageUrl)
                    orderedUrls.add(imageUrl)
                }
            }

            Log.d(TAG, "Extracted ${orderedUrls.size} unique photos in album progression from $cleanUrl")

            if (orderedUrls.isEmpty()) {
                Log.w(TAG, "No photos matched in HTML from $cleanUrl")
                return@withContext emptyList()
            }

            // Distribute photos into progressive temporal buckets for weekly timeline
            val calendar = Calendar.getInstance()
            val weekFormat = SimpleDateFormat("MMM yyyy • 'Week' w", Locale.getDefault())
            val baseTime = System.currentTimeMillis() - (orderedUrls.size * 12L * 3600L * 1000L)

            val existingPhotos = photoDao.getAllPhotosList()
            val existingUriMap = existingPhotos.associateBy { it.uri }

            for ((idx, baseKey) in orderedUrls.withIndex()) {
                val fullResUri = "$baseKey=w2048"
                val existing = existingUriMap[fullResUri]

                val estimatedTime = baseTime + (idx * 12L * 3600L * 1000L)
                calendar.timeInMillis = estimatedTime
                val weekLabel = weekFormat.format(Date(estimatedTime))

                val item = existing?.copy(
                    albumProgressionIndex = idx,
                    weekLabel = weekLabel
                ) ?: PhotoItem(
                    uri = fullResUri,
                    title = "Photo #${idx + 1}",
                    source = PhotoSource.CLOUD.name,
                    albumName = albumName,
                    albumProgressionIndex = idx,
                    dateAdded = System.currentTimeMillis(),
                    dateTakenMillis = estimatedTime,
                    weekLabel = weekLabel,
                    isCached = false
                )

                val id = photoDao.insert(item)
                registeredPhotos.add(item.copy(id = id))
                if (idx % 25 == 0) onProgress(idx + 1)
            }
            onProgress(registeredPhotos.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error indexing album metadata from $cleanUrl", e)
        }

        registeredPhotos
    }

    /**
     * Loads/caches a single photo on-demand as it appears in the slideshow,
     * maintaining the rolling LRU storage cap.
     */
    suspend fun preparePhotoForPlayback(photo: PhotoItem, maxCacheMb: Int = 250): PhotoItem {
        return offlineCacheManager.cachePhoto(photo, maxCacheMb.toLong() * 1024 * 1024)
    }
}

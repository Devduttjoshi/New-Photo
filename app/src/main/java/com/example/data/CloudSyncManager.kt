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
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    companion object {
        const val DEFAULT_GOOGLE_PHOTOS_URL = "https://photos.app.goo.gl/4J68BKAjwN3pMTrY9"
        const val USER_TEST_ALBUM_URL = DEFAULT_GOOGLE_PHOTOS_URL
        private const val TAG = "CloudSyncManager"
    }

    val availablePresets: List<CloudAlbumPreset> = listOf(
        CloudAlbumPreset(
            id = "GOOGLE_PHOTOS_DYNAMIC",
            name = "Google Photos Album (Progression)",
            description = "Dynamically streams 3000+ photos in album progression with weekly timeline",
            albumUrl = DEFAULT_GOOGLE_PHOTOS_URL
        )
    )

    /**
     * Dynamically crawls the Google Photos album HTML to register photo metadata
     * (URLs, progression order, and week grouping) WITHOUT downloading full-res images.
     * Takes minimal bandwidth (~200KB of text) to index hundreds of photos!
     */
    suspend fun indexAlbumMetadata(
        albumUrl: String = DEFAULT_GOOGLE_PHOTOS_URL,
        albumName: String = "Google Photos Album",
        limit: Int = 1000,
        onProgress: (Int) -> Unit = {}
    ): List<PhotoItem> = withContext(Dispatchers.IO) {
        val registeredPhotos = mutableListOf<PhotoItem>()
        if (albumUrl.isBlank()) return@withContext emptyList()

        try {
            Log.d(TAG, "Fetching album metadata from $albumUrl")
            val request = Request.Builder()
                .url(albumUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                Log.e(TAG, "Album request failed: ${response.code}")
                return@withContext emptyList()
            }

            val html = response.body!!.string()

            // 1. Regex to match photos in album progression sequence
            val pattern = Pattern.compile("https://lh3\\.googleusercontent\\.com/(?:pw/)?[a-zA-Z0-9_\\-]+")
            val matcher = pattern.matcher(html)
            val seenKeys = mutableSetOf<String>()
            val orderedUrls = mutableListOf<String>()

            while (matcher.find() && orderedUrls.size < limit) {
                val imageUrl = matcher.group()
                if (!imageUrl.contains("default-user") &&
                    !imageUrl.contains("favicon") &&
                    imageUrl.length > 55 &&
                    !seenKeys.contains(imageUrl)
                ) {
                    seenKeys.add(imageUrl)
                    orderedUrls.add(imageUrl)
                }
            }

            Log.d(TAG, "Extracted ${orderedUrls.size} unique photos in album progression")

            // 2. Compute calendar week grouping for album timeline
            val calendar = Calendar.getInstance()
            val weekFormat = SimpleDateFormat("MMM yyyy • 'Week' w", Locale.getDefault())

            // Distribute photos into progressive temporal buckets for the timeline
            val baseTime = System.currentTimeMillis() - (orderedUrls.size * 12L * 3600L * 1000L) // smooth timeline distribution

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
            Log.e(TAG, "Error indexing album metadata", e)
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

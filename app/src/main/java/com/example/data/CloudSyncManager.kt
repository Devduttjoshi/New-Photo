package com.example.data

import com.example.model.PhotoItem
import com.example.model.PhotoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

data class CloudAlbumPreset(
    val id: String,
    val name: String,
    val description: String,
    val photos: List<PhotoItem>
)

class CloudSyncManager(
    private val offlineCacheManager: OfflineCacheManager,
    private val photoDao: PhotoDao
) {
    private val client = OkHttpClient()

    /**
     * Curated high-resolution free cloud photo albums (Unsplash / Wikimedia Commons direct HD image URLs)
     * which are 100% free, require no API keys, and can be cached locally.
     */
    val availablePresets: List<CloudAlbumPreset> = listOf(
        CloudAlbumPreset(
            id = "LANDSCAPES",
            name = "Majestic Landscapes",
            description = "High-resolution mountains, alpine lakes, and sunsets",
            photos = listOf(
                PhotoItem(
                    uri = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1920&q=80",
                    title = "Yosemite Valley Reflections",
                    source = PhotoSource.CLOUD.name,
                    albumName = "Majestic Landscapes"
                ),
                PhotoItem(
                    uri = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=1920&q=80",
                    title = "Misty Mountain Pines",
                    source = PhotoSource.CLOUD.name,
                    albumName = "Majestic Landscapes"
                ),
                PhotoItem(
                    uri = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1920&q=80",
                    title = "Snowy Mountain Peaks",
                    source = PhotoSource.CLOUD.name,
                    albumName = "Majestic Landscapes"
                ),
                PhotoItem(
                    uri = "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1920&q=80",
                    title = "Emerald Lake Sunset",
                    source = PhotoSource.CLOUD.name,
                    albumName = "Majestic Landscapes"
                ),
                PhotoItem(
                    uri = "https://images.unsplash.com/photo-1511497584788-87676104235f?w=1920&q=80",
                    title = "Sunlight Through Forest Mist",
                    source = PhotoSource.CLOUD.name,
                    albumName = "Majestic Landscapes"
                )
            )
        ),
        CloudAlbumPreset(
            id = "ARCHITECTURE",
            name = "Architectural Marvels",
            description = "Stunning cityscapes, ancient monuments, and modern design",
            photos = listOf(
                PhotoItem(
                    uri = "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=1920&q=80",
                    title = "Minimalist Architectural Geometry",
                    source = PhotoSource.CLOUD.name,
                    albumName = "Architectural Marvels"
                ),
                PhotoItem(
                    uri = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=1920&q=80",
                    title = "Modern Glass Skyscrapers",
                    source = PhotoSource.CLOUD.name,
                    albumName = "Architectural Marvels"
                ),
                PhotoItem(
                    uri = "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=1920&q=80",
                    title = "Parisian Skyline at Dusk",
                    source = PhotoSource.CLOUD.name,
                    albumName = "Architectural Marvels"
                ),
                PhotoItem(
                    uri = "https://images.unsplash.com/photo-1518684079-3c830dcef090?w=1920&q=80",
                    title = "Dubai Marina Reflections",
                    source = PhotoSource.CLOUD.name,
                    albumName = "Architectural Marvels"
                )
            )
        ),
        CloudAlbumPreset(
            id = "COSMOS",
            name = "Night Sky & Aurora",
            description = "Milky Way astrophotography, Northern Lights, and starry nights",
            photos = listOf(
                PhotoItem(
                    uri = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1920&q=80",
                    title = "Aurora Borealis Dancing",
                    source = PhotoSource.CLOUD.name,
                    albumName = "Night Sky & Aurora"
                ),
                PhotoItem(
                    uri = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=1920&q=80",
                    title = "Milky Way Galaxy Panorama",
                    source = PhotoSource.CLOUD.name,
                    albumName = "Night Sky & Aurora"
                ),
                PhotoItem(
                    uri = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1920&q=80",
                    title = "Star Trails Over Mountain Lake",
                    source = PhotoSource.CLOUD.name,
                    albumName = "Night Sky & Aurora"
                ),
                PhotoItem(
                    uri = "https://images.unsplash.com/photo-1419242902214-272b3f66ee7a?w=1920&q=80",
                    title = "Deep Space Nebula & Stars",
                    source = PhotoSource.CLOUD.name,
                    albumName = "Night Sky & Aurora"
                )
            )
        )
    )

    /**
     * Initial preloaded ambient photos bundled for immediate offline launch.
     */
    val initialPreloadedPhotos: List<PhotoItem> = listOf(
        PhotoItem(
            uri = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1920&q=80",
            title = "Tropical Shoreline at Dawn",
            source = PhotoSource.PRELOADED.name,
            albumName = "Ambient TV Gallery"
        ),
        PhotoItem(
            uri = "https://images.unsplash.com/photo-1472214103451-9374bd1c798e?w=1920&q=80",
            title = "Rolling Green Hills & Horizon",
            source = PhotoSource.PRELOADED.name,
            albumName = "Ambient TV Gallery"
        ),
        PhotoItem(
            uri = "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=1920&q=80",
            title = "Sunlit Redwood Forest",
            source = PhotoSource.PRELOADED.name,
            albumName = "Ambient TV Gallery"
        ),
        PhotoItem(
            uri = "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=1920&q=80",
            title = "Starry Night Over Snowy Alps",
            source = PhotoSource.PRELOADED.name,
            albumName = "Ambient TV Gallery"
        ),
        PhotoItem(
            uri = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1920&q=80",
            title = "Yosemite Waterfall Basin",
            source = PhotoSource.PRELOADED.name,
            albumName = "Ambient TV Gallery"
        )
    )

    /**
     * Syncs cloud photos from a selected preset or URL and automatically caches them offline.
     */
    suspend fun syncPreset(presetId: String, autoCache: Boolean = true, onProgress: (Int, Int) -> Unit = { _, _ -> }): Int = withContext(Dispatchers.IO) {
        val preset = availablePresets.find { it.id == presetId } ?: availablePresets.first()
        val insertedItems = mutableListOf<PhotoItem>()

        for (photo in preset.photos) {
            val id = photoDao.insert(photo)
            insertedItems.add(photo.copy(id = id))
        }

        if (autoCache) {
            var count = 0
            val total = insertedItems.size
            for (item in insertedItems) {
                offlineCacheManager.cachePhoto(item)
                count++
                onProgress(count, total)
            }
        }

        insertedItems.size
    }

    /**
     * Syncs custom Cloud / Google Photos album link (Free tier without API quota limits).
     * Extracts direct image URLs from public album pages or RSS/JSON feeds.
     */
    suspend fun syncCustomUrl(url: String, albumName: String = "Custom Cloud Album"): List<PhotoItem> = withContext(Dispatchers.IO) {
        val extractedPhotos = mutableListOf<PhotoItem>()
        if (url.isBlank()) return@withContext emptyList()

        try {
            // Direct image URL check
            if (url.endsWith(".jpg") || url.endsWith(".png") || url.endsWith(".webp") || url.contains("unsplash.com")) {
                val item = PhotoItem(
                    uri = url,
                    title = "Cloud Photo ${System.currentTimeMillis() % 1000}",
                    source = PhotoSource.CLOUD.name,
                    albumName = albumName
                )
                val id = photoDao.insert(item)
                offlineCacheManager.cachePhoto(item.copy(id = id))
                return@withContext listOf(item)
            }

            // Google Photos shared album or web page parsing
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
                val html = response.body!!.string()
                // Regex to find Google Photos image URLs (lh3.googleusercontent.com)
                val pattern = Pattern.compile("https://lh3\\.googleusercontent\\.com/[a-zA-Z0-9_\\-]+")
                val matcher = pattern.matcher(html)
                val urls = mutableSetOf<String>()
                while (matcher.find() && urls.size < 25) {
                    val imageUrl = matcher.group()
                    if (!imageUrl.contains("favicon") && imageUrl.length > 50) {
                        urls.add("$imageUrl=w1920-h1080-no")
                    }
                }

                for ((idx, imgUri) in urls.withIndex()) {
                    val item = PhotoItem(
                        uri = imgUri,
                        title = "Google Photos #$idx",
                        source = PhotoSource.CLOUD.name,
                        albumName = "Google Photos Album"
                    )
                    val id = photoDao.insert(item)
                    extractedPhotos.add(item.copy(id = id))
                }

                // Cache offline
                for (item in extractedPhotos) {
                    offlineCacheManager.cachePhoto(item)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        extractedPhotos
    }
}

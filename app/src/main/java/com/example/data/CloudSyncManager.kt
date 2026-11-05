package com.example.data

import android.util.Log
import com.example.model.PhotoItem
import com.example.model.PhotoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
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
    private val photoDao: PhotoDao,
    private val albumLinkDao: AlbumLinkDao? = null
) {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
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

    private data class BatchResult(
        val photos: List<PhotoItem>,
        val nextContinuationToken: String?
    )

    private data class InitialPageData(
        val photos: List<PhotoItem>,
        val continuationToken: String?,
        val albumKey: String?,
        val albumTitle: String?
    )

    /**
     * Dynamically crawls Google Photos album HTML or loads direct image URLs.
     * Sequentially extracts EVERY photo in the album using continuation tokens (handling 3,000+ photos).
     * Deduplicates by URI and registers photo metadata into Room DB with album progression.
     */
    suspend fun indexAlbumMetadata(
        albumUrl: String = DEFAULT_GOOGLE_PHOTOS_URL,
        albumName: String = "Google Photos Album",
        limit: Int = 10000,
        onProgress: (Int) -> Unit = {}
    ): List<PhotoItem> = withContext(Dispatchers.IO) {
        val cleanUrl = sanitizeUrl(albumUrl)
        if (cleanUrl.isBlank()) return@withContext emptyList()

        // 1. Direct photo URL fast path
        if (isDirectImageUrl(cleanUrl)) {
            Log.d(TAG, "Direct photo URL detected: $cleanUrl")
            val item = PhotoItem(
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
            val saved = photoDao.upsertPhotos(listOf(item))
            albumLinkDao?.let { dao ->
                val existing = dao.getAlbumByUrl(cleanUrl)
                dao.insertOrUpdate(
                    com.example.model.AlbumLinkItem(
                        id = existing?.id ?: 0L,
                        url = cleanUrl,
                        albumName = albumName,
                        photoCount = saved.size,
                        dateAdded = existing?.dateAdded ?: System.currentTimeMillis(),
                        isIncludedInSlideshow = existing?.isIncludedInSlideshow ?: true
                    )
                )
            }
            return@withContext saved
        }

        val accumulatedPhotos = mutableListOf<PhotoItem>()
        var detectedAlbumName = albumName

        try {
            Log.d(TAG, "Fetching album metadata from $cleanUrl")
            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                Log.e(TAG, "Album request failed with HTTP ${response.code}")
                return@withContext emptyList()
            }

            val finalUrl = response.request.url.toString()
            var albumKey = finalUrl.substringAfter("/share/").substringBefore("?").substringBefore("/").trim()
            val authKey = response.request.url.queryParameter("key") ?: ""

            var html = response.body!!.string()
            html = html.replace("\\/", "/").replace("\\u003d", "=")

            // Extract album title
            val ogTitle = extractAlbumTitle(html)
            if (!ogTitle.isNullOrBlank()) {
                detectedAlbumName = ogTitle
            }

            // Strategy A: Parse initial batch from AF_initDataCallback (ds:0 or ds:1)
            val initialData = extractInitData(html, detectedAlbumName)
            if (initialData.albumTitle?.isNotBlank() == true && detectedAlbumName == albumName) {
                detectedAlbumName = initialData.albumTitle
            }
            if (albumKey.isBlank() && !initialData.albumKey.isNullOrBlank()) {
                albumKey = initialData.albumKey
            }

            val seenUris = mutableSetOf<String>()

            if (initialData.photos.isNotEmpty()) {
                Log.d(TAG, "Extracted initial batch of ${initialData.photos.size} photos from HTML")
                for (p in initialData.photos) {
                    if (seenUris.add(p.uri)) {
                        accumulatedPhotos.add(p)
                    }
                }
                onProgress(accumulatedPhotos.size)

                // Pagination Loop: If album has more photos (e.g. up to 3,000+ photos), fetch all remaining pages!
                var currentToken = initialData.continuationToken
                var pageNumber = 1
                val maxPages = 40 // Up to 15,000+ photos

                while (!currentToken.isNullOrBlank() && accumulatedPhotos.size < limit && pageNumber < maxPages && albumKey.isNotBlank()) {
                    pageNumber++
                    Log.d(TAG, "Fetching subsequent page $pageNumber using continuation token...")
                    val batchResult = fetchBatchexecutePage(
                        albumKey = albumKey,
                        continuationToken = currentToken,
                        authKey = authKey,
                        albumName = detectedAlbumName,
                        startIndex = accumulatedPhotos.size
                    )

                    if (batchResult.photos.isEmpty()) {
                        Log.d(TAG, "No more photos returned in page $pageNumber")
                        break
                    }

                    var addedThisPage = 0
                    for (p in batchResult.photos) {
                        if (seenUris.add(p.uri) && accumulatedPhotos.size < limit) {
                            accumulatedPhotos.add(p)
                            addedThisPage++
                        }
                    }

                    Log.d(TAG, "Page $pageNumber added $addedThisPage photos (total so far: ${accumulatedPhotos.size})")
                    onProgress(accumulatedPhotos.size)

                    // If same token returned or no new token, terminate pagination
                    if (batchResult.nextContinuationToken == currentToken || batchResult.nextContinuationToken.isNullOrBlank()) {
                        break
                    }
                    currentToken = batchResult.nextContinuationToken
                }
            }

            // Strategy B: Fallback Regex extraction if structured initData returned nothing
            if (accumulatedPhotos.isEmpty()) {
                val orderedKeys = extractPhotoKeysWithRegex(html, limit)
                Log.d(TAG, "Fallback extracted ${orderedKeys.size} photos via regex matching from $cleanUrl")
                val calendar = Calendar.getInstance()
                val weekFormat = SimpleDateFormat("MMM yyyy • 'Week' w", Locale.getDefault())
                val baseTime = System.currentTimeMillis() - (orderedKeys.size * 12L * 3600L * 1000L)

                for ((idx, baseKey) in orderedKeys.withIndex()) {
                    val fullResUri = "$baseKey=w2048"
                    if (seenUris.add(fullResUri)) {
                        val estimatedTime = baseTime + (idx * 12L * 3600L * 1000L)
                        calendar.timeInMillis = estimatedTime
                        val weekLabel = weekFormat.format(Date(estimatedTime))

                        accumulatedPhotos.add(
                            PhotoItem(
                                uri = fullResUri,
                                title = "Photo #${idx + 1}",
                                source = PhotoSource.CLOUD.name,
                                albumName = detectedAlbumName,
                                albumProgressionIndex = idx,
                                dateAdded = System.currentTimeMillis(),
                                dateTakenMillis = estimatedTime,
                                weekLabel = weekLabel,
                                isCached = false
                            )
                        )
                    }
                }
            }

            if (accumulatedPhotos.isEmpty()) {
                Log.w(TAG, "No photos found for URL $cleanUrl")
                return@withContext emptyList()
            }

            // Assign strict 0-based sequential progression indices
            val sequentialPhotos = accumulatedPhotos.distinctBy { it.uri }.mapIndexed { idx, item ->
                item.copy(
                    albumProgressionIndex = idx,
                    albumName = detectedAlbumName,
                    source = PhotoSource.CLOUD.name
                )
            }

            Log.d(TAG, "Persisting ${sequentialPhotos.size} distinct sequential photos for album '$detectedAlbumName'...")
            val savedPhotos = photoDao.upsertPhotos(sequentialPhotos)

            // Persist album in link library with exact photo count
            albumLinkDao?.let { dao ->
                val existing = dao.getAlbumByUrl(cleanUrl)
                val linkItem = com.example.model.AlbumLinkItem(
                    id = existing?.id ?: 0L,
                    url = cleanUrl,
                    albumName = detectedAlbumName,
                    photoCount = savedPhotos.size,
                    dateAdded = existing?.dateAdded ?: System.currentTimeMillis(),
                    isIncludedInSlideshow = existing?.isIncludedInSlideshow ?: true
                )
                dao.insertOrUpdate(linkItem)
            }

            onProgress(savedPhotos.size)
            return@withContext savedPhotos
        } catch (e: Exception) {
            Log.e(TAG, "Error indexing album metadata from $cleanUrl", e)
        }

        emptyList()
    }

    private suspend fun fetchBatchexecutePage(
        albumKey: String,
        continuationToken: String,
        authKey: String,
        albumName: String,
        startIndex: Int
    ): BatchResult = withContext(Dispatchers.IO) {
        try {
            val rpcId = "snAcKc"
            val innerJson = JSONArray().apply {
                put(albumKey)
                put(continuationToken)
                put(JSONObject.NULL)
                if (authKey.isNotBlank()) put(authKey)
            }.toString()

            val envelope = JSONArray().apply {
                put(JSONArray().apply {
                    put(rpcId)
                    put(innerJson)
                    put(JSONObject.NULL)
                    put("generic")
                })
            }
            val fReq = JSONArray().apply { put(envelope) }.toString()

            val formBody = FormBody.Builder()
                .add("f.req", fReq)
                .build()

            val postRequest = Request.Builder()
                .url("https://photos.google.com/_/PhotosUi/data/batchexecute?rpcids=$rpcId")
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .post(formBody)
                .build()

            val response = client.newCall(postRequest).execute()
            if (!response.isSuccessful || response.body == null) {
                Log.w(TAG, "batchexecute HTTP failure: ${response.code}")
                return@withContext BatchResult(emptyList(), null)
            }

            val bodyStr = response.body!!.string()
            parseBatchexecuteResponse(bodyStr, albumName, startIndex)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing batchexecute page", e)
            BatchResult(emptyList(), null)
        }
    }

    private fun parseBatchexecuteResponse(
        bodyStr: String,
        albumName: String,
        startIndex: Int
    ): BatchResult {
        val photos = mutableListOf<PhotoItem>()
        var nextToken: String? = null

        try {
            val lines = bodyStr.split('\n')
            for (line in lines) {
                val trimmed = line.trim()
                if (!trimmed.startsWith("[[[") && !trimmed.startsWith("[[")) continue
                if (!trimmed.contains("wrb.fr")) continue

                val outerArray = JSONArray(trimmed)
                for (i in 0 until outerArray.length()) {
                    val rpcEnvelope = outerArray.optJSONArray(i) ?: continue
                    val payloadStr = rpcEnvelope.optString(2, "")
                    if (payloadStr.isBlank() || payloadStr == "null") continue

                    if (!payloadStr.startsWith("[")) continue
                    val innerArray = try {
                        JSONArray(payloadStr)
                    } catch (e: Exception) {
                        continue
                    }
                    val itemsArray = innerArray.optJSONArray(0)
                    val rawToken = innerArray.optString(1, "")
                    if (rawToken.isNotBlank() && rawToken != "null") {
                        nextToken = rawToken
                    }

                    if (itemsArray != null) {
                        val weekFormat = SimpleDateFormat("MMM yyyy • 'Week' w", Locale.getDefault())
                        for (j in 0 until itemsArray.length()) {
                            val item = itemsArray.optJSONArray(j) ?: continue
                            val subArr = item.optJSONArray(1) ?: continue
                            val rawUrl = subArr.optString(0, "")
                            if (rawUrl.isBlank() || !rawUrl.startsWith("http")) continue

                            val baseKey = rawUrl.substringBefore("=")
                            val fullResUri = "$baseKey=w2048"

                            var timeMillis = item.optLong(2, 0L)
                            if (timeMillis <= 0L) timeMillis = item.optLong(5, System.currentTimeMillis())
                            if (timeMillis <= 0L) timeMillis = System.currentTimeMillis()

                            val weekLabel = weekFormat.format(Date(timeMillis))
                            val currentProgression = startIndex + photos.size

                            photos.add(
                                PhotoItem(
                                    uri = fullResUri,
                                    title = "Photo #${currentProgression + 1}",
                                    source = PhotoSource.CLOUD.name,
                                    albumName = albumName,
                                    albumProgressionIndex = currentProgression,
                                    dateAdded = System.currentTimeMillis(),
                                    dateTakenMillis = timeMillis,
                                    weekLabel = weekLabel,
                                    isCached = false
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing batchexecute response", e)
        }

        return BatchResult(photos, nextToken)
    }

    private fun extractInitData(html: String, albumName: String): InitialPageData {
        val results = mutableListOf<PhotoItem>()
        var continuationToken: String? = null
        var detectedKey: String? = null
        var detectedTitle: String? = null

        val callbackKeywords = listOf("key: 'ds:0'", "key: 'ds:1'", "key:\"ds:0\"", "key:\"ds:1\"")
        for (keyword in callbackKeywords) {
            var searchPos = 0
            while (true) {
                val idx = html.indexOf(keyword, searchPos)
                if (idx == -1) break
                searchPos = idx + keyword.length

                val dataIdx = html.indexOf("data:", idx)
                if (dataIdx != -1 && dataIdx - idx < 500) {
                    val arrayStr = extractJsonArrayWithBrackets(html, dataIdx)
                    if (!arrayStr.isNullOrBlank()) {
                        try {
                            val rootArray = JSONArray(arrayStr)
                            if (rootArray.length() > 1) {
                                val itemsArray = rootArray.optJSONArray(1)
                                val rawToken = rootArray.optString(2, "")
                                if (rawToken.isNotBlank() && rawToken != "null") {
                                    continuationToken = rawToken
                                }

                                val metaArray = rootArray.optJSONArray(3)
                                if (metaArray != null) {
                                    val keyCandidate = metaArray.optString(0, "")
                                    if (keyCandidate.isNotBlank()) detectedKey = keyCandidate
                                    val titleCandidate = metaArray.optString(1, "")
                                    if (titleCandidate.isNotBlank()) detectedTitle = titleCandidate
                                }

                                if (itemsArray != null && itemsArray.length() > 0) {
                                    val weekFormat = SimpleDateFormat("MMM yyyy • 'Week' w", Locale.getDefault())
                                    for (i in 0 until itemsArray.length()) {
                                        val item = itemsArray.optJSONArray(i) ?: continue
                                        val subArr = item.optJSONArray(1) ?: continue
                                        val rawUrl = subArr.optString(0, "")
                                        if (rawUrl.isBlank() || !rawUrl.startsWith("http")) continue

                                        val baseKey = rawUrl.substringBefore("=")
                                        val fullResUri = "$baseKey=w2048"

                                        var timeMillis = item.optLong(2, 0L)
                                        if (timeMillis <= 0L) timeMillis = item.optLong(5, System.currentTimeMillis())
                                        if (timeMillis <= 0L) timeMillis = System.currentTimeMillis()

                                        val weekLabel = weekFormat.format(Date(timeMillis))

                                        results.add(
                                            PhotoItem(
                                                uri = fullResUri,
                                                title = "Photo #${i + 1}",
                                                source = PhotoSource.CLOUD.name,
                                                albumName = albumName,
                                                albumProgressionIndex = i,
                                                dateAdded = System.currentTimeMillis(),
                                                dateTakenMillis = timeMillis,
                                                weekLabel = weekLabel,
                                                isCached = false
                                            )
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "Could not parse JSON array for $keyword: ${e.message}")
                        }
                    }
                }

                if (results.isNotEmpty()) break
            }
            if (results.isNotEmpty()) break
        }

        return InitialPageData(results, continuationToken, detectedKey, detectedTitle)
    }

    private fun extractJsonArrayWithBrackets(text: String, startPos: Int): String? {
        val bracketPos = text.indexOf('[', startPos)
        if (bracketPos == -1) return null
        var depth = 0
        var inString = false
        var escape = false

        for (i in bracketPos until text.length) {
            val c = text[i]
            if (escape) {
                escape = false
                continue
            }
            if (c == '\\') {
                escape = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == '[') depth++
                else if (c == ']') {
                    depth--
                    if (depth == 0) {
                        return text.substring(bracketPos, i + 1)
                    }
                }
            }
        }
        return null
    }

    private fun extractAlbumTitle(html: String): String? {
        val pattern = Pattern.compile("<meta\\s+property=[\"']og:title[\"']\\s+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            val title = matcher.group(1)?.trim()
            if (!title.isNullOrBlank()) {
                return title.substringBefore(" · ").substringBefore(" - ")
            }
        }
        return null
    }

    private fun extractPhotoKeysWithRegex(html: String, limit: Int): List<String> {
        val seen = mutableSetOf<String>()
        val ordered = mutableListOf<String>()

        // 1. First priority: Google Photos /pw/ image keys
        val pwPattern = Pattern.compile("https://lh3\\.googleusercontent\\.com/pw/([a-zA-Z0-9_\\-]+)")
        val pwMatcher = pwPattern.matcher(html)
        while (pwMatcher.find() && ordered.size < limit) {
            val fullKey = "https://lh3.googleusercontent.com/pw/" + pwMatcher.group(1)
            if (pwMatcher.group(1)!!.length >= 35 && !seen.contains(fullKey)) {
                seen.add(fullKey)
                ordered.add(fullKey)
            }
        }

        // 2. Secondary fallback: other googleusercontent photo keys if /pw/ was not present
        if (ordered.isEmpty()) {
            val generalPattern = Pattern.compile("https://lh3\\.googleusercontent\\.com/([a-zA-Z0-9_\\-]+)")
            val generalMatcher = generalPattern.matcher(html)
            while (generalMatcher.find() && ordered.size < limit) {
                val base = "https://lh3.googleusercontent.com/" + generalMatcher.group(1)
                val key = generalMatcher.group(1)!!
                if (!key.contains("default-user") &&
                    !key.contains("favicon") &&
                    !key.contains("ogw") &&
                    key.length >= 45 &&
                    !seen.contains(base)
                ) {
                    seen.add(base)
                    ordered.add(base)
                }
            }
        }

        return ordered
    }

    /**
     * Loads/caches a single photo on-demand as it appears in the slideshow,
     * maintaining the rolling LRU storage cap.
     */
    suspend fun preparePhotoForPlayback(photo: PhotoItem, maxCacheMb: Int = 250): PhotoItem {
        return offlineCacheManager.cachePhoto(photo, maxCacheMb.toLong() * 1024 * 1024)
    }
}

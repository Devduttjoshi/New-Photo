package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CloudSyncManager
import com.example.data.OfflineCacheManager
import com.example.data.SettingsRepository
import com.example.data.SlideshowSettings
import com.example.data.UsbStorageScanner
import com.example.model.PhotoItem
import com.example.model.PhotoSource
import com.example.server.TvLocalHttpServer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class SlideshowUiState(
    val currentPhoto: PhotoItem? = null,
    val previousPhoto: PhotoItem? = null,
    val displayList: List<PhotoItem> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = true,
    val progress: Float = 0f,
    val isHudVisible: Boolean = true,
    val isSettingsOpen: Boolean = false,
    val isLibraryOpen: Boolean = false,
    val isQrModalOpen: Boolean = false,
    val isSyncing: Boolean = false,
    val syncStatusMessage: String = "",
    val cacheStatsText: String = "Calculating cache...",
    val totalPhotosCount: Int = 0,
    val usbPhotosCount: Int = 0,
    val cloudPhotosCount: Int = 0,
    val phonePhotosCount: Int = 0,
    val crossfadeSequence: Long = 0L,
    val localServerUrl: String = "",
    val localIpAddress: String = "",
    val isPhoneConnected: Boolean = false,
    val availableWeeks: List<String> = emptyList(),
    val activeWeek: String = "ALL",
    val activeSourceFilter: String = "ALL",
    val sessionResumedNotice: String = "",
    val isLibraryRefreshing: Boolean = false,
    val isBrowserOpen: Boolean = false,
    val browserUrl: String = "https://photos.google.com",
    val albumLinks: List<com.example.model.AlbumLinkItem> = emptyList()
)

class SlideshowViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val photoDao = db.photoDao()
    private val albumLinkDao = db.albumLinkDao()
    val settingsRepo = SettingsRepository.getInstance(application)
    val settings: StateFlow<SlideshowSettings> = settingsRepo.settings

    private val offlineCacheManager = OfflineCacheManager(application, photoDao)
    val cloudSyncManager = CloudSyncManager(offlineCacheManager, photoDao, albumLinkDao)
    private val usbScanner = UsbStorageScanner(application)

    private val _uiState = MutableStateFlow(SlideshowUiState())
    val uiState: StateFlow<SlideshowUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var hudHideJob: Job? = null
    private var hasResumedSession = false

    private var localHttpServer: TvLocalHttpServer? = null

    init {
        initLocalHttpServer(application)

        // Observe distinct weeks for the timeline selector
        viewModelScope.launch {
            photoDao.getDistinctWeeks().collect { weeks ->
                _uiState.value = _uiState.value.copy(availableWeeks = weeks)
            }
        }

        // Observe stored album links for the Link Library
        viewModelScope.launch {
            albumLinkDao.getAllAlbumLinks().collect { links ->
                _uiState.value = _uiState.value.copy(albumLinks = links)
            }
        }

        // Collect photos, settings, and album link selection
        viewModelScope.launch {
            combine(photoDao.getAllPhotos(), settings, albumLinkDao.getAllAlbumLinks()) { photos, currentSettings, albumLinks ->
                Triple(photos, currentSettings, albumLinks)
            }.collect { (photos, currentSettings, albumLinks) ->
                val hasUserPhotos = photos.any { it.source != PhotoSource.DEMO.name }
                val activePhotos = if (hasUserPhotos) {
                    photos.filter { it.source != PhotoSource.DEMO.name }
                } else {
                    photos
                }

                // Determine which cloud album names are excluded by the user
                val excludedAlbumNames = albumLinks
                    .filter { !it.isIncludedInSlideshow }
                    .map { it.albumName }
                    .toSet()

                // Filter out photos belonging to unselected albums
                val albumFilteredPhotos = if (excludedAlbumNames.isNotEmpty()) {
                    activePhotos.filter { it.source != PhotoSource.CLOUD.name || !excludedAlbumNames.contains(it.albumName) }
                } else {
                    activePhotos
                }

                // Filter by source
                var filtered = when (currentSettings.activeSourceFilter) {
                    "USB" -> albumFilteredPhotos.filter { it.source == PhotoSource.USB.name }
                    "CLOUD" -> albumFilteredPhotos.filter { it.source == PhotoSource.CLOUD.name }
                    "PHONE" -> albumFilteredPhotos.filter { it.source == PhotoSource.LOCAL.name }
                    else -> albumFilteredPhotos
                }

                // Filter by week timeline if set
                if (currentSettings.activeWeekFilter != "ALL" && currentSettings.activeWeekFilter.isNotBlank()) {
                    filtered = filtered.filter { it.weekLabel == currentSettings.activeWeekFilter }
                }

                val finalDisplay = if (currentSettings.shuffleOrder) {
                    filtered.shuffled()
                } else {
                    // Album progression order
                    filtered.sortedBy { it.albumProgressionIndex }
                }

                val usbCount = photos.count { it.source == PhotoSource.USB.name }
                val cloudCount = photos.count { it.source == PhotoSource.CLOUD.name }
                val phoneCount = photos.count { it.source == PhotoSource.LOCAL.name }

                // Resume session progression on first load
                var initialIndex = _uiState.value.currentIndex.coerceIn(0, (finalDisplay.size - 1).coerceAtLeast(0))
                var resumeNotice = ""

                if (!hasResumedSession && currentSettings.autoResumeSession && finalDisplay.isNotEmpty()) {
                    hasResumedSession = true
                    if (currentSettings.lastSessionPhotoId != 0L) {
                        val resumeIdx = finalDisplay.indexOfFirst { it.id == currentSettings.lastSessionPhotoId }
                        if (resumeIdx >= 0) {
                            initialIndex = resumeIdx
                            resumeNotice = "Resumed where you left off (Photo ${resumeIdx + 1})"
                        }
                    } else if (currentSettings.lastSessionIndex in finalDisplay.indices) {
                        initialIndex = currentSettings.lastSessionIndex
                        resumeNotice = "Resumed from previous session (Photo ${initialIndex + 1})"
                    }
                }

                val currentPhoto = if (finalDisplay.isNotEmpty()) finalDisplay[initialIndex] else null

                _uiState.value = _uiState.value.copy(
                    displayList = finalDisplay,
                    currentIndex = initialIndex,
                    currentPhoto = currentPhoto,
                    totalPhotosCount = photos.size,
                    usbPhotosCount = usbCount,
                    cloudPhotosCount = cloudCount,
                    phonePhotosCount = phoneCount,
                    activeWeek = currentSettings.activeWeekFilter,
                    activeSourceFilter = currentSettings.activeSourceFilter,
                    sessionResumedNotice = resumeNotice
                )

                // If DB is empty, seed 5 generic high-quality test photos to immediately display
                if (photos.isEmpty()) {
                    seedGenericTestPhotos()
                } else if (currentPhoto != null) {
                    prepareCurrentPhotoForOffline(currentPhoto)
                }
            }
        }

        refreshCacheStats()
        startSlideshowTimer()
        scheduleHudAutoHide()

        // Automatically scan USB drives and verify saved Google Photos link on startup/restart
        viewModelScope.launch {
            refreshLibraryOnStartup()
        }
    }

    private fun seedGenericTestPhotos() {
        viewModelScope.launch {
            val count = photoDao.getCount()
            if (count > 0) return@launch

            val genericTestPhotos = listOf(
                PhotoItem(
                    uri = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1920&q=80",
                    title = "Alpine Mountain Reflections",
                    albumName = "Scenic Landscapes",
                    source = PhotoSource.CLOUD.name,
                    albumProgressionIndex = 0,
                    weekLabel = "Autumn • Week 41",
                    dateAdded = System.currentTimeMillis() - 400000000L,
                    dateTakenMillis = System.currentTimeMillis() - 400000000L,
                    isCached = false
                ),
                PhotoItem(
                    uri = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=1920&q=80",
                    title = "Morning Fog Over Pine Valley",
                    albumName = "Scenic Landscapes",
                    source = PhotoSource.CLOUD.name,
                    albumProgressionIndex = 1,
                    weekLabel = "Autumn • Week 41",
                    dateAdded = System.currentTimeMillis() - 350000000L,
                    dateTakenMillis = System.currentTimeMillis() - 350000000L,
                    isCached = false
                ),
                PhotoItem(
                    uri = "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=1920&q=80",
                    title = "Autumn Forest Canopy Trail",
                    albumName = "Scenic Landscapes",
                    source = PhotoSource.CLOUD.name,
                    albumProgressionIndex = 2,
                    weekLabel = "Autumn • Week 42",
                    dateAdded = System.currentTimeMillis() - 300000000L,
                    dateTakenMillis = System.currentTimeMillis() - 300000000L,
                    isCached = false
                ),
                PhotoItem(
                    uri = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1920&q=80",
                    title = "Pacific Coastal Horizon Sunset",
                    albumName = "Scenic Landscapes",
                    source = PhotoSource.CLOUD.name,
                    albumProgressionIndex = 3,
                    weekLabel = "Autumn • Week 42",
                    dateAdded = System.currentTimeMillis() - 250000000L,
                    dateTakenMillis = System.currentTimeMillis() - 250000000L,
                    isCached = false
                ),
                PhotoItem(
                    uri = "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=1920&q=80",
                    title = "Starry Night Over Snow Peaks",
                    albumName = "Scenic Landscapes",
                    source = PhotoSource.CLOUD.name,
                    albumProgressionIndex = 4,
                    weekLabel = "Autumn • Week 43",
                    dateAdded = System.currentTimeMillis() - 200000000L,
                    dateTakenMillis = System.currentTimeMillis() - 200000000L,
                    isCached = false
                )
            )

            photoDao.insertAll(genericTestPhotos)
            _uiState.value = _uiState.value.copy(
                syncStatusMessage = "Ready with 5 test photos"
            )
        }
    }

    private suspend fun refreshLibraryOnStartup() {
        _uiState.value = _uiState.value.copy(isLibraryRefreshing = true)
        try {
            // 1. Silent scan for attached USB media and external storage
            val usbPhotos = usbScanner.scanUsbAndExternalStorage()
            if (usbPhotos.isNotEmpty()) {
                photoDao.insertAll(usbPhotos)
            }

            // 2. Silent background crawl/refresh of saved Google Photos link if configured
            val savedUrl = settings.value.savedGooglePhotosUrl.trim()
            if (savedUrl.isNotBlank()) {
                cloudSyncManager.indexAlbumMetadata(
                    albumUrl = savedUrl,
                    limit = 2000
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("SlideshowViewModel", "Error refreshing library on startup", e)
        } finally {
            _uiState.value = _uiState.value.copy(isLibraryRefreshing = false)
            refreshCacheStats()
        }
    }

    private fun startDynamicAlbumSync() {
        val savedUrl = settings.value.savedGooglePhotosUrl.trim()
        if (savedUrl.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                syncStatusMessage = "Connecting to Google Photos album..."
            )
            val indexed = cloudSyncManager.indexAlbumMetadata(
                albumUrl = savedUrl,
                onProgress = { count ->
                    _uiState.value = _uiState.value.copy(
                        syncStatusMessage = "Registered $count photos in album progression..."
                    )
                }
            )
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncStatusMessage = "Ready (${indexed.size} photos found)"
            )
            refreshCacheStats()
        }
    }

    private fun prepareCurrentPhotoForOffline(photo: PhotoItem) {
        viewModelScope.launch {
            // Cache current photo in background with rolling LRU eviction
            val updated = cloudSyncManager.preparePhotoForPlayback(photo, settings.value.maxCacheSizeMb)
            if (updated.isCached && _uiState.value.currentPhoto?.id == photo.id) {
                _uiState.value = _uiState.value.copy(currentPhoto = updated)
            }
            // Pre-cache next photo for buttery smooth playback
            val list = _uiState.value.displayList
            if (list.size > 1) {
                val nextIdx = (_uiState.value.currentIndex + 1) % list.size
                cloudSyncManager.preparePhotoForPlayback(list[nextIdx], settings.value.maxCacheSizeMb)
            }
            refreshCacheStats()
        }
    }

    private fun initLocalHttpServer(application: Application) {
        try {
            localHttpServer = TvLocalHttpServer(
                context = application,
                onAlbumLinkReceived = { albumUrl ->
                    onAlbumLinkReceivedFromPhone(albumUrl)
                },
                onPhotoUploaded = { file, title ->
                    onPhotoUploadedFromPhone(file, title)
                },
                onPhoneConnected = {
                    _uiState.value = _uiState.value.copy(isPhoneConnected = true)
                }
            ).apply {
                start()
            }

            _uiState.value = _uiState.value.copy(
                localServerUrl = localHttpServer?.serverUrl ?: "",
                localIpAddress = localHttpServer?.localIpAddress ?: ""
            )
        } catch (t: Throwable) {
            android.util.Log.e("SlideshowViewModel", "Error initializing local HTTP server", t)
        }
    }

    private fun onAlbumLinkReceivedFromPhone(url: String) {
        val clean = CloudSyncManager.sanitizeUrl(url)
        settingsRepo.updateSavedGooglePhotosUrl(clean)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                syncStatusMessage = "Syncing album progression from phone..."
            )
            val imported = cloudSyncManager.indexAlbumMetadata(albumUrl = clean, limit = 2000)
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncStatusMessage = if (imported.isNotEmpty()) "Imported ${imported.size} photos!" else "Connected to album"
            )
            refreshCacheStats()

            delay(1500)
            _uiState.value = _uiState.value.copy(isQrModalOpen = false)
            if (imported.isNotEmpty()) {
                settingsRepo.updateSourceFilter("ALL")
                settingsRepo.updateWeekFilter("ALL")
                delay(300)
                jumpToPhoto(0)
            }
        }
    }

    private fun onPhotoUploadedFromPhone(file: File, title: String) {
        viewModelScope.launch {
            val item = PhotoItem(
                uri = file.toURI().toString(),
                localCachedPath = file.absolutePath,
                title = title,
                source = PhotoSource.LOCAL.name,
                albumName = "Phone Uploads",
                dateAdded = System.currentTimeMillis(),
                fileSizeBytes = file.length(),
                isCached = true
            )
            photoDao.insert(item)
            _uiState.value = _uiState.value.copy(
                syncStatusMessage = "Received '$title' from phone!"
            )
            refreshCacheStats()
        }
    }

    fun startSlideshowTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val intervalMs = (settings.value.slideIntervalSeconds * 1000L).coerceAtLeast(2000L)
            val stepMs = 50L
            var elapsedMs = 0L

            while (isActive) {
                delay(stepMs)
                if (_uiState.value.isPlaying &&
                    !_uiState.value.isSettingsOpen &&
                    !_uiState.value.isLibraryOpen &&
                    !_uiState.value.isQrModalOpen
                ) {
                    elapsedMs += stepMs
                    val currentProgress = (elapsedMs.toFloat() / intervalMs).coerceIn(0f, 1f)
                    _uiState.value = _uiState.value.copy(progress = currentProgress)

                    if (elapsedMs >= intervalMs) {
                        elapsedMs = 0L
                        advanceToNext()
                    }
                }
            }
        }
    }

    private fun advanceToNext() {
        val list = _uiState.value.displayList
        if (list.size <= 1) return

        val prev = _uiState.value.currentPhoto
        val nextIdx = (_uiState.value.currentIndex + 1) % list.size
        val nextPhoto = list[nextIdx]

        _uiState.value = _uiState.value.copy(
            previousPhoto = prev,
            currentPhoto = nextPhoto,
            currentIndex = nextIdx,
            progress = 0f,
            crossfadeSequence = _uiState.value.crossfadeSequence + 1
        )

        // Save session progression
        settingsRepo.saveSessionProgression(nextPhoto.id, nextIdx)
        viewModelScope.launch {
            photoDao.recordPhotoShown(nextPhoto.id, System.currentTimeMillis())
        }

        prepareCurrentPhotoForOffline(nextPhoto)
    }

    fun nextPhoto() {
        showHudTemporarily()
        val list = _uiState.value.displayList
        if (list.isEmpty()) return

        val prev = _uiState.value.currentPhoto
        val nextIdx = (_uiState.value.currentIndex + 1) % list.size
        val nextPhoto = list[nextIdx]

        _uiState.value = _uiState.value.copy(
            previousPhoto = prev,
            currentPhoto = nextPhoto,
            currentIndex = nextIdx,
            progress = 0f,
            crossfadeSequence = _uiState.value.crossfadeSequence + 1
        )

        settingsRepo.saveSessionProgression(nextPhoto.id, nextIdx)
        viewModelScope.launch {
            photoDao.recordPhotoShown(nextPhoto.id, System.currentTimeMillis())
        }

        prepareCurrentPhotoForOffline(nextPhoto)
        startSlideshowTimer()
    }

    fun previousPhoto() {
        showHudTemporarily()
        val list = _uiState.value.displayList
        if (list.isEmpty()) return

        val prev = _uiState.value.currentPhoto
        val prevIdx = if (_uiState.value.currentIndex - 1 < 0) list.size - 1 else _uiState.value.currentIndex - 1
        val nextPhoto = list[prevIdx]

        _uiState.value = _uiState.value.copy(
            previousPhoto = prev,
            currentPhoto = nextPhoto,
            currentIndex = prevIdx,
            progress = 0f,
            crossfadeSequence = _uiState.value.crossfadeSequence + 1
        )

        settingsRepo.saveSessionProgression(nextPhoto.id, prevIdx)
        prepareCurrentPhotoForOffline(nextPhoto)
        startSlideshowTimer()
    }

    fun jumpToPhoto(index: Int) {
        val list = _uiState.value.displayList
        if (index in list.indices) {
            val prev = _uiState.value.currentPhoto
            val nextPhoto = list[index]
            _uiState.value = _uiState.value.copy(
                previousPhoto = prev,
                currentPhoto = nextPhoto,
                currentIndex = index,
                progress = 0f,
                crossfadeSequence = _uiState.value.crossfadeSequence + 1,
                isLibraryOpen = false
            )
            settingsRepo.saveSessionProgression(nextPhoto.id, index)
            prepareCurrentPhotoForOffline(nextPhoto)
            startSlideshowTimer()
        }
    }

    fun selectWeek(week: String) {
        settingsRepo.updateWeekFilter(week)
        showHudTemporarily()
    }

    fun indexFullAlbum() {
        startDynamicAlbumSync()
    }

    fun syncCloudPreset(preset: String) {
        settingsRepo.updateCloudPreset(preset)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                syncStatusMessage = "Syncing preset '$preset'..."
            )
            val albumUrl = settings.value.savedGooglePhotosUrl.ifBlank { "" }
            if (albumUrl.isNotBlank()) {
                cloudSyncManager.indexAlbumMetadata(albumUrl)
            }
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncStatusMessage = "Preset synced"
            )
            refreshCacheStats()
        }
    }

    fun syncCustomCloudUrl(url: String) {
        val targetUrl = if (url.trim() == "1234") "https://photos.app.goo.gl/mwFXXYhT5P5mMD9x6" else url
        val clean = CloudSyncManager.sanitizeUrl(targetUrl)
        if (clean.isBlank()) return
        settingsRepo.updateSavedGooglePhotosUrl(clean)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                syncStatusMessage = "Fetching album metadata..."
            )
            val results = cloudSyncManager.indexAlbumMetadata(clean, limit = 2000)
            val message = if (results.isNotEmpty()) {
                "Imported ${results.size} photos into album!"
            } else {
                "No photos found. Check link or album sharing permission."
            }
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncStatusMessage = message
            )
            refreshCacheStats()
            if (results.isNotEmpty()) {
                settingsRepo.updateSourceFilter("ALL")
                settingsRepo.updateWeekFilter("ALL")
                delay(300)
                jumpToPhoto(0)
            }
        }
    }

    fun selectSourceFilter(filter: String) {
        settingsRepo.updateSourceFilter(filter)
    }

    fun toggleAlbumIncluded(albumId: Long, isIncluded: Boolean) {
        viewModelScope.launch {
            albumLinkDao.setIncludedInSlideshow(albumId, isIncluded)
        }
    }

    fun removeAlbumLink(album: com.example.model.AlbumLinkItem) {
        viewModelScope.launch {
            // 1. Remove album entry from link library
            albumLinkDao.deleteById(album.id)
            // 2. Remove associated cloud photos from database
            photoDao.deleteByAlbumName(album.albumName)
            _uiState.value = _uiState.value.copy(
                syncStatusMessage = "Removed album '${album.albumName}'"
            )
            refreshCacheStats()
        }
    }

    fun refreshLibrarySubtly() {
        if (_uiState.value.isLibraryRefreshing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLibraryRefreshing = true)
            try {
                // Subtle scan of USB storage
                val usbPhotos = usbScanner.scanUsbAndExternalStorage()
                if (usbPhotos.isNotEmpty()) {
                    photoDao.insertAll(usbPhotos)
                }
                // Subtle scan of saved Google Photos link
                val savedUrl = settings.value.savedGooglePhotosUrl.trim()
                if (savedUrl.isNotBlank()) {
                    cloudSyncManager.indexAlbumMetadata(albumUrl = savedUrl, limit = 2000)
                }
            } catch (e: Exception) {
                android.util.Log.e("SlideshowViewModel", "Error refreshing library subtly", e)
            } finally {
                _uiState.value = _uiState.value.copy(isLibraryRefreshing = false)
                refreshCacheStats()
            }
        }
    }

    fun openBrowser(url: String = "https://photos.google.com") {
        _uiState.value = _uiState.value.copy(
            isBrowserOpen = true,
            browserUrl = url,
            isSettingsOpen = false,
            isHudVisible = false
        )
    }

    fun closeBrowser() {
        _uiState.value = _uiState.value.copy(isBrowserOpen = false)
        showHudTemporarily()
    }

    fun togglePlayPause() {
        val newPlayState = !_uiState.value.isPlaying
        _uiState.value = _uiState.value.copy(isPlaying = newPlayState)
        showHudTemporarily()
    }

    fun showHudTemporarily() {
        _uiState.value = _uiState.value.copy(isHudVisible = true)
        scheduleHudAutoHide()
    }

    fun hideHud() {
        hudHideJob?.cancel()
        _uiState.value = _uiState.value.copy(isHudVisible = false)
    }

    private fun scheduleHudAutoHide() {
        hudHideJob?.cancel()
        hudHideJob = viewModelScope.launch {
            delay(6000)
            if (!_uiState.value.isSettingsOpen &&
                !_uiState.value.isLibraryOpen &&
                !_uiState.value.isQrModalOpen
            ) {
                _uiState.value = _uiState.value.copy(isHudVisible = false)
            }
        }
    }

    fun openQrModal() {
        val server = localHttpServer
        if (server != null) {
            _uiState.value = _uiState.value.copy(
                isQrModalOpen = true,
                isSettingsOpen = false,
                isHudVisible = true,
                localServerUrl = server.serverUrl,
                localIpAddress = server.localIpAddress
            )
        }
    }

    fun closeQrModal() {
        _uiState.value = _uiState.value.copy(isQrModalOpen = false)
        scheduleHudAutoHide()
        startSlideshowTimer()
    }

    fun openSettings() {
        _uiState.value = _uiState.value.copy(isSettingsOpen = true, isHudVisible = true)
        refreshCacheStats()
    }

    fun closeSettings() {
        _uiState.value = _uiState.value.copy(isSettingsOpen = false)
        scheduleHudAutoHide()
        startSlideshowTimer()
    }

    fun openLibrary() {
        _uiState.value = _uiState.value.copy(isLibraryOpen = true, isHudVisible = true)
        refreshLibrarySubtly()
    }

    fun closeLibrary() {
        _uiState.value = _uiState.value.copy(isLibraryOpen = false)
        scheduleHudAutoHide()
    }

    fun scanUsbStorage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                syncStatusMessage = "Scanning USB and external media..."
            )
            val usbPhotos = usbScanner.scanUsbAndExternalStorage()
            if (usbPhotos.isNotEmpty()) {
                photoDao.insertAll(usbPhotos)
            }
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncStatusMessage = "Found ${usbPhotos.size} local & USB photos"
            )
            refreshCacheStats()
        }
    }

    fun refreshCacheStats() {
        viewModelScope.launch {
            val stats = offlineCacheManager.getCacheStats(settings.value.maxCacheSizeMb)
            _uiState.value = _uiState.value.copy(cacheStatsText = stats)
        }
    }

    fun clearOfflineCache() {
        viewModelScope.launch {
            offlineCacheManager.clearCache()
            refreshCacheStats()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        hudHideJob?.cancel()
        localHttpServer?.stop()
    }
}

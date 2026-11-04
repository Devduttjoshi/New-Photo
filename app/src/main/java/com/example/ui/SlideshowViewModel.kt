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
    val crossfadeSequence: Long = 0L,
    val localServerUrl: String = "",
    val localIpAddress: String = "",
    val isPhoneConnected: Boolean = false,
    val availableWeeks: List<String> = emptyList(),
    val activeWeek: String = "ALL",
    val sessionResumedNotice: String = ""
)

class SlideshowViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val photoDao = db.photoDao()
    val settingsRepo = SettingsRepository.getInstance(application)
    val settings: StateFlow<SlideshowSettings> = settingsRepo.settings

    private val offlineCacheManager = OfflineCacheManager(application, photoDao)
    val cloudSyncManager = CloudSyncManager(offlineCacheManager, photoDao)
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

        // Collect photos and settings
        viewModelScope.launch {
            combine(photoDao.getAllPhotos(), settings) { photos, currentSettings ->
                Pair(photos, currentSettings)
            }.collect { (photos, currentSettings) ->
                // Filter by source
                var filtered = when (currentSettings.activeSourceFilter) {
                    "USB" -> photos.filter { it.source == PhotoSource.USB.name }
                    "CLOUD" -> photos.filter { it.source == PhotoSource.CLOUD.name }
                    else -> photos
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
                    activeWeek = currentSettings.activeWeekFilter,
                    sessionResumedNotice = resumeNotice
                )

                // If DB is empty, dynamically crawl the album progression
                if (photos.isEmpty()) {
                    startDynamicAlbumSync()
                } else if (currentPhoto != null) {
                    prepareCurrentPhotoForOffline(currentPhoto)
                }
            }
        }

        refreshCacheStats()
        startSlideshowTimer()
        scheduleHudAutoHide()
    }

    private fun startDynamicAlbumSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                syncStatusMessage = "Connecting to Google Photos album..."
            )
            val indexed = cloudSyncManager.indexAlbumMetadata(
                albumUrl = CloudSyncManager.DEFAULT_GOOGLE_PHOTOS_URL,
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                syncStatusMessage = "Syncing album progression from phone..."
            )
            val imported = cloudSyncManager.indexAlbumMetadata(albumUrl = url, limit = 1500)
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncStatusMessage = if (imported.isNotEmpty()) "Imported ${imported.size} photos!" else "Connected to album"
            )
            refreshCacheStats()

            delay(1500)
            _uiState.value = _uiState.value.copy(isQrModalOpen = false)
            if (imported.isNotEmpty()) {
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
            val albumUrl = when (preset) {
                "GOOGLE_PHOTOS_TEST" -> CloudSyncManager.USER_TEST_ALBUM_URL
                else -> CloudSyncManager.DEFAULT_GOOGLE_PHOTOS_URL
            }
            cloudSyncManager.indexAlbumMetadata(albumUrl)
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncStatusMessage = "Preset synced"
            )
            refreshCacheStats()
        }
    }

    fun syncCustomCloudUrl(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                syncStatusMessage = "Fetching album metadata..."
            )
            val results = cloudSyncManager.indexAlbumMetadata(url)
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncStatusMessage = "Found ${results.size} photos in album"
            )
            refreshCacheStats()
            if (results.isNotEmpty()) {
                jumpToPhoto(0)
            }
        }
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

    private fun scheduleHudAutoHide() {
        hudHideJob?.cancel()
        hudHideJob = viewModelScope.launch {
            delay(4500)
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

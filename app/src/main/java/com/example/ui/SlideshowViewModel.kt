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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    val isSyncing: Boolean = false,
    val syncStatusMessage: String = "",
    val cacheStatsText: String = "Calculating cache...",
    val totalPhotosCount: Int = 0,
    val usbPhotosCount: Int = 0,
    val cloudPhotosCount: Int = 0,
    val crossfadeSequence: Long = 0L // incremented on each transition to trigger fresh animations
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

    init {
        // Collect photos and settings
        viewModelScope.launch {
            combine(photoDao.getAllPhotos(), settings) { photos, currentSettings ->
                Pair(photos, currentSettings)
            }.collect { (photos, currentSettings) ->
                val filtered = when (currentSettings.activeSourceFilter) {
                    "USB" -> photos.filter { it.source == PhotoSource.USB.name }
                    "CLOUD" -> photos.filter { it.source == PhotoSource.CLOUD.name }
                    else -> photos
                }

                val finalDisplay = if (currentSettings.shuffleOrder) {
                    filtered.shuffled()
                } else {
                    filtered
                }

                val usbCount = photos.count { it.source == PhotoSource.USB.name }
                val cloudCount = photos.count { it.source == PhotoSource.CLOUD.name }

                val currentIdx = _uiState.value.currentIndex.coerceIn(0, (finalDisplay.size - 1).coerceAtLeast(0))
                val currentPhoto = if (finalDisplay.isNotEmpty()) finalDisplay[currentIdx] else null

                _uiState.value = _uiState.value.copy(
                    displayList = finalDisplay,
                    currentIndex = currentIdx,
                    currentPhoto = currentPhoto,
                    totalPhotosCount = photos.size,
                    usbPhotosCount = usbCount,
                    cloudPhotosCount = cloudCount
                )

                // If DB is empty, initialize with preloaded ambient photos and scan USB
                if (photos.isEmpty()) {
                    initializeInitialLibrary()
                }
            }
        }

        refreshCacheStats()
        startSlideshowTimer()
        scheduleHudAutoHide()
    }

    private fun initializeInitialLibrary() {
        viewModelScope.launch {
            // Insert preloaded ambient photos
            photoDao.insertAll(cloudSyncManager.initialPreloadedPhotos)
            // Automatically scan for attached USB drives or local photos
            scanUsbStorage()
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
                if (_uiState.value.isPlaying && !_uiState.value.isSettingsOpen && !_uiState.value.isLibraryOpen) {
                    elapsedMs += stepMs
                    val currentProgress = (elapsedMs.toFloat() / intervalMs).coerceIn(0f, 1f)
                    _uiState.value = _uiState.value.copy(progress = currentProgress)

                    if (elapsedMs >= intervalMs) {
                        elapsedMs = 0L
                        advanceToNext()
                    }
                } else {
                    // Paused
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
        // Reset timer
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
            startSlideshowTimer()
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
            if (!_uiState.value.isSettingsOpen && !_uiState.value.isLibraryOpen) {
                _uiState.value = _uiState.value.copy(isHudVisible = false)
            }
        }
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

    fun syncCloudPreset(presetId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                syncStatusMessage = "Syncing cloud album & caching offline..."
            )
            settingsRepo.updateCloudPreset(presetId)
            val count = cloudSyncManager.syncPreset(presetId, autoCache = settings.value.cacheCloudPhotosOffline) { done, total ->
                _uiState.value = _uiState.value.copy(
                    syncStatusMessage = "Caching offline: $done / $total photos"
                )
            }
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncStatusMessage = "Synced $count photos (cached locally)"
            )
            refreshCacheStats()
        }
    }

    fun syncCustomCloudUrl(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                syncStatusMessage = "Connecting to cloud photo link..."
            )
            val result = cloudSyncManager.syncCustomUrl(url)
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncStatusMessage = if (result.isNotEmpty()) "Imported and cached ${result.size} photos" else "No photos found at link"
            )
            refreshCacheStats()
        }
    }

    fun refreshCacheStats() {
        viewModelScope.launch {
            val stats = offlineCacheManager.getCacheStats()
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
    }
}

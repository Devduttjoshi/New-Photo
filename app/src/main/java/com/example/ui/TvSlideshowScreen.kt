package com.example.ui

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.tween
import com.example.ui.components.AmbientHudOverlay
import com.example.ui.components.CoverFlowPlaybackView
import com.example.ui.components.CrossfadeImageLayer
import com.example.ui.components.TvBrowserScreen
import com.example.ui.components.TvPhotoLibrarySheet
import com.example.ui.components.TvQrPairingDialog
import com.example.ui.components.TvSettingsDrawer

@Composable
fun TvSlideshowScreen(
    viewModel: SlideshowViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val slideshowFocusRequester = remember { FocusRequester() }
    val hudFocusRequester = remember { FocusRequester() }

    val hasActiveOverlay = uiState.isSettingsOpen || uiState.isLibraryOpen || uiState.isQrModalOpen || uiState.isHudVisible

    LaunchedEffect(Unit) {
        slideshowFocusRequester.requestFocus()
    }

    LaunchedEffect(hasActiveOverlay) {
        if (!hasActiveOverlay) {
            try {
                slideshowFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Back button handling
    BackHandler(enabled = hasActiveOverlay) {
        if (uiState.isQrModalOpen) viewModel.closeQrModal()
        else if (uiState.isSettingsOpen) viewModel.closeSettings()
        else if (uiState.isLibraryOpen) viewModel.closeLibrary()
        else if (uiState.isHudVisible) viewModel.hideHud()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(slideshowFocusRequester)
            .focusable(enabled = !hasActiveOverlay)
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    viewModel.showHudTemporarily()

                    // If a drawer or dialog is open, let its focus system handle D-pad navigation
                    if (uiState.isSettingsOpen || uiState.isLibraryOpen || uiState.isQrModalOpen) {
                        return@onKeyEvent false
                    }

                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            viewModel.nextPhoto()
                            return@onKeyEvent true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_DPAD_LEFT -> {
                            viewModel.previousPhoto()
                            return@onKeyEvent true
                        }
                        KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_DPAD_UP -> {
                            viewModel.openSettings()
                            return@onKeyEvent true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            viewModel.openLibrary()
                            return@onKeyEvent true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            viewModel.togglePlayPause()
                            return@onKeyEvent true
                        }
                    }
                }
                false
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (uiState.isQrModalOpen) {
                            viewModel.closeQrModal()
                        } else if (uiState.isSettingsOpen) {
                            viewModel.closeSettings()
                        } else if (uiState.isLibraryOpen) {
                            viewModel.closeLibrary()
                        } else {
                            viewModel.showHudTemporarily()
                        }
                    },
                    onDoubleTap = {
                        if (!uiState.isQrModalOpen) {
                            viewModel.togglePlayPause()
                        }
                    }
                )
            }
    ) {
        // 1. Dual-Layer Smooth Crossfade Image Canvas
        CrossfadeImageLayer(
            currentPhoto = uiState.currentPhoto,
            previousPhoto = uiState.previousPhoto,
            crossfadeSequence = uiState.crossfadeSequence,
            crossfadeDurationMs = settings.crossfadeDurationMs,
            kenBurnsEnabled = settings.kenBurnsEnabled,
            slideIntervalSeconds = settings.slideIntervalSeconds,
            framingMode = settings.framingMode
        )

        // 2. Ambient HUD (Digital Clock, Date, Metadata, Progress Bar, Remote hints)
        AmbientHudOverlay(
            photo = uiState.currentPhoto,
            isPlaying = uiState.isPlaying,
            progress = uiState.progress,
            isHudVisible = uiState.isHudVisible,
            showClock = settings.showClockAndDate,
            showMetadata = settings.showMetadataBadge,
            alwaysShowClock = settings.alwaysShowClock,
            alwaysShowPhotoTakenDate = settings.alwaysShowPhotoTakenDate,
            totalPhotos = uiState.displayList.size,
            currentIndex = uiState.currentIndex,
            displayList = uiState.displayList,
            activeWeek = uiState.activeWeek,
            sessionResumedNotice = uiState.sessionResumedNotice,
            onPlayPauseToggle = { viewModel.togglePlayPause() },
            onNext = { viewModel.nextPhoto() },
            onPrevious = { viewModel.previousPhoto() },
            onOpenSettings = { viewModel.openSettings() },
            onOpenLibrary = { viewModel.openLibrary() },
            onOpenQrModal = { viewModel.openQrModal() },
            onResetHudTimer = { viewModel.showHudTemporarily() },
            playPauseFocusRequester = hudFocusRequester
        )

        // 3. Thumbnails Library Drawer (Slides from bottom)
        AnimatedVisibility(
            visible = uiState.isLibraryOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            TvPhotoLibrarySheet(
                photos = uiState.displayList,
                currentIndex = uiState.currentIndex,
                availableWeeks = uiState.availableWeeks,
                activeWeek = uiState.activeWeek,
                activeSourceFilter = uiState.activeSourceFilter,
                totalPhotosCount = uiState.totalPhotosCount,
                usbPhotosCount = uiState.usbPhotosCount,
                cloudPhotosCount = uiState.cloudPhotosCount,
                phonePhotosCount = uiState.phonePhotosCount,
                isRefreshing = uiState.isLibraryRefreshing,
                onSelectSourceFilter = { viewModel.selectSourceFilter(it) },
                onSelectWeek = { viewModel.selectWeek(it) },
                onSelectPhoto = { viewModel.jumpToPhoto(it) },
                onTriggerSubtleRefresh = { viewModel.refreshLibrarySubtly() },
                onClose = { viewModel.closeLibrary() }
            )
        }

        // 4. Settings Drawer (Slides from right)
        AnimatedVisibility(
            visible = uiState.isSettingsOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            TvSettingsDrawer(
                settings = settings,
                cacheStatsText = uiState.cacheStatsText,
                isSyncing = uiState.isSyncing,
                syncStatusMessage = uiState.syncStatusMessage,
                totalPhotos = uiState.totalPhotosCount,
                usbPhotosCount = uiState.usbPhotosCount,
                cloudPhotosCount = uiState.cloudPhotosCount,
                availableWeeks = uiState.availableWeeks,
                activeWeek = uiState.activeWeek,
                onSelectWeek = { viewModel.selectWeek(it) },
                onClose = { viewModel.closeSettings() },
                onUpdateAutoStart = { viewModel.settingsRepo.updateAutoStartOnBoot(it) },
                onUpdateAutoResumeSession = { viewModel.settingsRepo.updateAutoResumeSession(it) },
                onUpdateInterval = { viewModel.settingsRepo.updateSlideInterval(it) },
                onUpdateCrossfade = { viewModel.settingsRepo.updateCrossfadeDuration(it) },
                onUpdateKenBurns = { viewModel.settingsRepo.updateKenBurns(it) },
                onUpdateShowClock = { viewModel.settingsRepo.updateShowClock(it) },
                onUpdateShowMetadata = { viewModel.settingsRepo.updateShowMetadata(it) },
                onUpdateAlwaysShowClock = { viewModel.settingsRepo.updateAlwaysShowClock(it) },
                onUpdateAlwaysShowPhotoTakenDate = { viewModel.settingsRepo.updateAlwaysShowPhotoTakenDate(it) },
                onUpdateShuffle = { viewModel.settingsRepo.updateShuffle(it) },
                onUpdateSourceFilter = { viewModel.settingsRepo.updateSourceFilter(it) },
                onScanUsb = { viewModel.scanUsbStorage() },
                onSyncCloudPreset = { viewModel.syncCloudPreset(it) },
                onSyncCustomUrl = { viewModel.syncCustomCloudUrl(it) },
                onClearCache = { viewModel.clearOfflineCache() },
                onUpdateMaxCacheLimit = { viewModel.settingsRepo.updateMaxCacheSizeMb(it) },
                onOpenQrModal = { viewModel.openQrModal() },
                onUpdateFramingMode = { viewModel.settingsRepo.updateFramingMode(it) },
                onOpenBrowser = { viewModel.openBrowser(it) },
                albumLinks = uiState.albumLinks,
                onToggleAlbumIncluded = { albumId, isIncluded -> viewModel.toggleAlbumIncluded(albumId, isIncluded) },
                onRemoveAlbumLink = { album -> viewModel.removeAlbumLink(album) },
                onUpdateCoverFlowEnabled = { viewModel.settingsRepo.updateCoverFlowEnabled(it) },
                onUpdateCoverFlowAngle = { viewModel.settingsRepo.updateCoverFlowAngle(it) },
                onUpdateCoverFlowSpacing = { viewModel.settingsRepo.updateCoverFlowSpacing(it) }
            )
        }

        // 5. QR Code Phone Pairing Dialog
        TvQrPairingDialog(
            isOpen = uiState.isQrModalOpen,
            serverUrl = uiState.localServerUrl,
            localIpAddress = uiState.localIpAddress,
            isPhoneConnected = uiState.isPhoneConnected,
            syncStatusMessage = uiState.syncStatusMessage,
            onClose = { viewModel.closeQrModal() }
        )

        // 6. In-App TV Web Browser (Debug / Beta)
        if (uiState.isBrowserOpen) {
            TvBrowserScreen(
                initialUrl = uiState.browserUrl,
                onClose = { viewModel.closeBrowser() }
            )
        }
    }
}

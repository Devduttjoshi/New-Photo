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
import com.example.ui.components.AmbientHudOverlay
import com.example.ui.components.CrossfadeImageLayer
import com.example.ui.components.TvPhotoLibrarySheet
import com.example.ui.components.TvSettingsDrawer

@Composable
fun TvSlideshowScreen(
    viewModel: SlideshowViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Back button handling
    BackHandler(enabled = uiState.isSettingsOpen || uiState.isLibraryOpen) {
        if (uiState.isSettingsOpen) viewModel.closeSettings()
        if (uiState.isLibraryOpen) viewModel.closeLibrary()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    viewModel.showHudTemporarily()
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            if (!uiState.isSettingsOpen && !uiState.isLibraryOpen) {
                                viewModel.togglePlayPause()
                                return@onKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT,
                        KeyEvent.KEYCODE_MEDIA_NEXT,
                        KeyEvent.KEYCODE_PAGE_DOWN -> {
                            if (!uiState.isSettingsOpen && !uiState.isLibraryOpen) {
                                viewModel.nextPhoto()
                                return@onKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                        KeyEvent.KEYCODE_PAGE_UP -> {
                            if (!uiState.isSettingsOpen && !uiState.isLibraryOpen) {
                                viewModel.previousPhoto()
                                return@onKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_MENU -> {
                            if (!uiState.isSettingsOpen) {
                                viewModel.openSettings()
                                return@onKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (!uiState.isLibraryOpen && !uiState.isSettingsOpen) {
                                viewModel.openLibrary()
                                return@onKeyEvent true
                            }
                        }
                    }
                }
                false
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (uiState.isSettingsOpen) {
                            viewModel.closeSettings()
                        } else if (uiState.isLibraryOpen) {
                            viewModel.closeLibrary()
                        } else {
                            viewModel.showHudTemporarily()
                        }
                    },
                    onDoubleTap = {
                        viewModel.togglePlayPause()
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
            slideIntervalSeconds = settings.slideIntervalSeconds
        )

        // 2. Ambient HUD (Digital Clock, Date, Metadata, Progress Bar, Remote hints)
        AmbientHudOverlay(
            photo = uiState.currentPhoto,
            isPlaying = uiState.isPlaying,
            progress = uiState.progress,
            isHudVisible = uiState.isHudVisible,
            showClock = settings.showClockAndDate,
            showMetadata = settings.showMetadataBadge,
            totalPhotos = uiState.displayList.size,
            currentIndex = uiState.currentIndex,
            onPlayPauseToggle = { viewModel.togglePlayPause() },
            onNext = { viewModel.nextPhoto() },
            onPrevious = { viewModel.previousPhoto() },
            onOpenSettings = { viewModel.openSettings() },
            onOpenLibrary = { viewModel.openLibrary() }
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
                onSelectPhoto = { viewModel.jumpToPhoto(it) },
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
                onClose = { viewModel.closeSettings() },
                onUpdateAutoStart = { viewModel.settingsRepo.updateAutoStartOnBoot(it) },
                onUpdateInterval = { viewModel.settingsRepo.updateSlideInterval(it) },
                onUpdateCrossfade = { viewModel.settingsRepo.updateCrossfadeDuration(it) },
                onUpdateKenBurns = { viewModel.settingsRepo.updateKenBurns(it) },
                onUpdateShowClock = { viewModel.settingsRepo.updateShowClock(it) },
                onUpdateShowMetadata = { viewModel.settingsRepo.updateShowMetadata(it) },
                onUpdateShuffle = { viewModel.settingsRepo.updateShuffle(it) },
                onUpdateSourceFilter = { viewModel.settingsRepo.updateSourceFilter(it) },
                onScanUsb = { viewModel.scanUsbStorage() },
                onSyncCloudPreset = { viewModel.syncCloudPreset(it) },
                onSyncCustomUrl = { viewModel.syncCustomCloudUrl(it) },
                onClearCache = { viewModel.clearOfflineCache() }
            )
        }
    }
}

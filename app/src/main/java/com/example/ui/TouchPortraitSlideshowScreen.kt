package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CoverFlowPlaybackView
import com.example.ui.components.CrossfadeImageLayer
import com.example.ui.components.TvBrowserScreen
import com.example.ui.components.TvPhotoLibrarySheet
import com.example.ui.components.TvQrPairingDialog
import com.example.ui.components.TvSettingsDrawer
import com.example.ui.theme.TvAccentAmber
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TouchPortraitSlideshowScreen(
    viewModel: SlideshowViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val hasActiveOverlay = uiState.isSettingsOpen || uiState.isLibraryOpen || uiState.isQrModalOpen || uiState.isHudVisible || uiState.isCoverFlowViewOpen

    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        while (true) {
            currentTimeString = timeFormat.format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    BackHandler(enabled = hasActiveOverlay) {
        if (uiState.isCoverFlowViewOpen) viewModel.closeCoverFlowView()
        else if (uiState.isQrModalOpen) viewModel.closeQrModal()
        else if (uiState.isSettingsOpen) viewModel.closeSettings()
        else if (uiState.isLibraryOpen) viewModel.closeLibrary()
        else if (uiState.isHudVisible) viewModel.hideHud()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
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
        // 1. Crossfade Image Layer (Standard clean transitions)
        CrossfadeImageLayer(
            currentPhoto = uiState.currentPhoto,
            previousPhoto = uiState.previousPhoto,
            crossfadeSequence = uiState.crossfadeSequence,
            crossfadeDurationMs = settings.crossfadeDurationMs,
            kenBurnsEnabled = settings.kenBurnsEnabled,
            slideIntervalSeconds = settings.slideIntervalSeconds,
            framingMode = settings.framingMode
        )

        // 2. Portrait Touch Ambient Overlay & Controls
        AnimatedVisibility(
            visible = uiState.isHudVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                // Top Bar: Clock & Quick Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (settings.alwaysShowClock || settings.showClockAndDate) {
                            Text(
                                text = currentTimeString,
                                color = TvTextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        uiState.currentPhoto?.let { photo ->
                            Text(
                                text = "Photo ${uiState.currentIndex + 1} of ${uiState.displayList.size}",
                                color = TvTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            IconButton(onClick = { viewModel.openLibrary() }) {
                                Icon(Icons.Default.Folder, contentDescription = "Library", tint = TvTextPrimary)
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            IconButton(onClick = { viewModel.openSettings() }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TvTextPrimary)
                            }
                        }
                    }
                }

                // Bottom Thumb-Friendly Touch Controls for Portrait
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .fillMaxWidth(0.92f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.previousPhoto() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = TvTextPrimary, modifier = Modifier.size(32.dp))
                        }

                        Surface(
                            shape = CircleShape,
                            color = TvAccentAmber,
                            modifier = Modifier.size(60.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.nextPhoto() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = TvTextPrimary, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }

        // 3. Thumbnails Library Sheet
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

        // 4. Settings Drawer
        AnimatedVisibility(
            visible = uiState.isSettingsOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
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

        // 5. QR Code Pairing Dialog
        TvQrPairingDialog(
            isOpen = uiState.isQrModalOpen,
            serverUrl = uiState.localServerUrl,
            localIpAddress = uiState.localIpAddress,
            isPhoneConnected = uiState.isPhoneConnected,
            syncStatusMessage = uiState.syncStatusMessage,
            onClose = { viewModel.closeQrModal() }
        )

        // 6. In-App Web Browser
        if (uiState.isBrowserOpen) {
            TvBrowserScreen(
                initialUrl = uiState.browserUrl,
                onClose = { viewModel.closeBrowser() }
            )
        }
    }
}

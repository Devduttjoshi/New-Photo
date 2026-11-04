package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PhotoItem
import com.example.model.PhotoSource
import com.example.ui.theme.TvAccentAmber
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentEmerald
import com.example.ui.theme.TvSurfaceElevated
import com.example.ui.theme.TvTextMuted
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AmbientHudOverlay(
    photo: PhotoItem?,
    isPlaying: Boolean,
    progress: Float,
    isHudVisible: Boolean,
    showClock: Boolean,
    showMetadata: Boolean,
    totalPhotos: Int,
    currentIndex: Int,
    activeWeek: String,
    sessionResumedNotice: String,
    onPlayPauseToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenQrModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTimeString = timeFormat.format(now)
            currentDateString = dateFormat.format(now)
            delay(1000)
        }
    }

    AnimatedVisibility(
        visible = isHudVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.82f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        ) {
            val isPortrait = maxHeight > maxWidth || maxWidth < 700.dp
            val horizontalPadding = if (isPortrait) 16.dp else 40.dp
            val topVerticalPadding = if (isPortrait) 16.dp else 28.dp

            // TOP BAR: Clock, Status Badges, QR pairing, and Settings
            if (isPortrait) {
                // Portrait Layout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = topVerticalPadding)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showClock) {
                            Column {
                                Text(
                                    text = currentTimeString,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Light,
                                    color = TvTextPrimary
                                )
                                Text(
                                    text = currentDateString,
                                    fontSize = 11.sp,
                                    color = TvTextSecondary
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        // Right Action Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TvPillButton(
                                icon = Icons.Default.QrCode2,
                                label = "Phone (QR)",
                                onClick = onOpenQrModal,
                                testTag = "pair_phone_qr_button"
                            )

                            TvIconButton(
                                icon = Icons.Default.Settings,
                                contentDescription = "Settings",
                                onClick = onOpenSettings,
                                size = 44.dp,
                                iconSize = 22.dp,
                                testTag = "settings_button"
                            )
                        }
                    }

                    // Secondary info row: Source Badge, Week Badge & Session Notice
                    if (photo != null && showMetadata) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SourceBadge(source = photo.source)

                            if (photo.weekLabel.isNotBlank()) {
                                WeekBadge(week = photo.weekLabel)
                            }

                            if (photo.isCached) {
                                OfflineCachedChip()
                            }
                        }
                    }
                }
            } else {
                // Landscape TV Layout (Wide, Spacious)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = topVerticalPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (showClock) {
                        Column {
                            Text(
                                text = currentTimeString,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Light,
                                color = TvTextPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = currentDateString,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = TvTextSecondary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Right Badges & Actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (photo != null && showMetadata) {
                            SourceBadge(source = photo.source)

                            if (photo.weekLabel.isNotBlank()) {
                                WeekBadge(week = photo.weekLabel)
                            }

                            if (photo.isCached) {
                                OfflineCachedChip()
                            }
                        }

                        TvPillButton(
                            icon = Icons.Default.QrCode2,
                            label = "Connect Phone (QR)",
                            onClick = onOpenQrModal,
                            testTag = "pair_phone_qr_button"
                        )

                        TvIconButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Settings",
                            onClick = onOpenSettings,
                            size = 48.dp,
                            iconSize = 24.dp,
                            testTag = "settings_button"
                        )
                    }
                }
            }

            // BOTTOM BAR: Photo metadata, Playback progress, and Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = horizontalPadding, vertical = if (isPortrait) 16.dp else 24.dp)
            ) {
                // Slideshow slide interval progress bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = TvAccentCyan,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (isPortrait) {
                    // Portrait Layout: Stacked vertically
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Title & Info
                        Column {
                            if (photo != null) {
                                Text(
                                    text = photo.title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TvTextPrimary,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = photo.albumName,
                                        fontSize = 12.sp,
                                        color = TvTextSecondary
                                    )
                                    if (totalPhotos > 0) {
                                        Text(
                                            text = " • Photo ${currentIndex + 1} of $totalPhotos",
                                            fontSize = 12.sp,
                                            color = TvTextMuted
                                        )
                                    }
                                }
                            }
                            if (sessionResumedNotice.isNotBlank()) {
                                Text(
                                    text = sessionResumedNotice,
                                    fontSize = 10.sp,
                                    color = TvAccentAmber,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Playback Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TvIconButton(
                                icon = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                onClick = onPrevious,
                                size = 46.dp,
                                iconSize = 22.dp,
                                testTag = "previous_button"
                            )

                            TvIconButton(
                                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                onClick = onPlayPauseToggle,
                                size = 52.dp,
                                iconSize = 28.dp,
                                containerColor = TvAccentCyan,
                                tint = Color.Black,
                                focusedBorderColor = Color.White,
                                testTag = "play_pause_button"
                            )

                            TvIconButton(
                                icon = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                onClick = onNext,
                                size = 46.dp,
                                iconSize = 22.dp,
                                testTag = "next_button"
                            )

                            TvPillButton(
                                icon = Icons.Default.PhotoAlbum,
                                label = "Timeline",
                                onClick = onOpenLibrary,
                                testTag = "library_button"
                            )
                        }
                    }
                } else {
                    // Landscape TV Layout (Side-by-side title and controls)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            if (photo != null) {
                                Text(
                                    text = photo.title,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TvTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = photo.albumName,
                                        fontSize = 14.sp,
                                        color = TvTextSecondary
                                    )
                                    if (totalPhotos > 0) {
                                        Text(
                                            text = " • Photo ${currentIndex + 1} of $totalPhotos",
                                            fontSize = 14.sp,
                                            color = TvTextMuted
                                        )
                                    }
                                }
                            }
                            if (sessionResumedNotice.isNotBlank()) {
                                Text(
                                    text = sessionResumedNotice,
                                    fontSize = 12.sp,
                                    color = TvAccentAmber,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Right: Controls with clear TV focus borders
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TvIconButton(
                                icon = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                onClick = onPrevious,
                                size = 48.dp,
                                iconSize = 24.dp,
                                testTag = "previous_button"
                            )

                            TvIconButton(
                                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                onClick = onPlayPauseToggle,
                                size = 56.dp,
                                iconSize = 30.dp,
                                containerColor = TvAccentCyan,
                                tint = Color.Black,
                                focusedBorderColor = Color.White,
                                testTag = "play_pause_button"
                            )

                            TvIconButton(
                                icon = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                onClick = onNext,
                                size = 48.dp,
                                iconSize = 24.dp,
                                testTag = "next_button"
                            )

                            TvPillButton(
                                icon = Icons.Default.PhotoAlbum,
                                label = "Timeline (▼)",
                                onClick = onOpenLibrary,
                                testTag = "library_button"
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * TV Focus-Aware Icon Button with high-contrast background, solid border, and elevation on D-pad focus.
 */
@Composable
fun TvIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    tint: Color = Color.White,
    containerColor: Color = TvSurfaceElevated,
    focusedBorderColor: Color = TvAccentCyan,
    testTag: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = CircleShape,
        color = if (isFocused) TvAccentCyan else containerColor,
        border = BorderStroke(
            width = if (isFocused) 3.5.dp else 1.5.dp,
            color = if (isFocused) Color.White else Color.White.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .size(size)
            .scale(if (isFocused) 1.14f else 1.0f)
            .clip(CircleShape)
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isFocused) Color.Black else tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * TV Focus-Aware Pill Button with high-contrast solid background, bold label, and elevation on D-pad focus.
 */
@Composable
fun TvPillButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = TvAccentCyan,
    testTag: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isFocused) accentColor else TvSurfaceElevated,
        border = BorderStroke(
            width = if (isFocused) 3.5.dp else 1.5.dp,
            color = if (isFocused) Color.White else accentColor.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .scale(if (isFocused) 1.08f else 1.0f)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Color.Black else accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = if (isFocused) Color.Black else Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SourceBadge(source: String) {
    val (icon, label, color) = when (source) {
        PhotoSource.USB.name -> Triple(Icons.Default.Usb, "USB Drive", TvAccentCyan)
        PhotoSource.CLOUD.name -> Triple(Icons.Default.Cloud, "Cloud Album", TvAccentAmber)
        PhotoSource.LOCAL.name -> Triple(Icons.Default.Folder, "Local Media", TvAccentEmerald)
        else -> Triple(Icons.Default.PhotoAlbum, "Ambient Gallery", TvAccentCyan)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun WeekBadge(week: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = TvAccentCyan.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, TvAccentCyan.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = TvAccentCyan, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = week, fontSize = 11.sp, color = TvAccentCyan, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun OfflineCachedChip() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = TvAccentEmerald.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, TvAccentEmerald.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Cached", tint = TvAccentEmerald, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = "Offline Cached", fontSize = 11.sp, color = TvAccentEmerald, fontWeight = FontWeight.Medium)
        }
    }
}

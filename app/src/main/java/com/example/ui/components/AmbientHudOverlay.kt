package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import coil.compose.AsyncImage
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import android.view.KeyEvent
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
    alwaysShowClock: Boolean = true,
    alwaysShowPhotoTakenDate: Boolean = true,
    totalPhotos: Int,
    currentIndex: Int,
    displayList: List<PhotoItem> = emptyList(),
    activeWeek: String,
    sessionResumedNotice: String,
    onPlayPauseToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenQrModal: () -> Unit,
    onResetHudTimer: () -> Unit = {},
    playPauseFocusRequester: FocusRequester? = null,
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

    LaunchedEffect(isHudVisible) {
        if (isHudVisible) {
            delay(60)
            try {
                playPauseFocusRequester?.requestFocus()
            } catch (_: Exception) {}
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
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        onResetHudTimer()
                    }
                    false
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        ) {
            val isPortrait = maxHeight > maxWidth || maxWidth < 700.dp
            val horizontalPadding = if (isPortrait) 16.dp else 40.dp
            val topVerticalPadding = if (isPortrait) 16.dp else 28.dp

            val prevPhoto = if (displayList.isNotEmpty()) displayList[(currentIndex - 1 + displayList.size) % displayList.size] else null
            val nextPhoto = if (displayList.isNotEmpty()) displayList[(currentIndex + 1) % displayList.size] else null

            // 1. LEFT EDGE PREVIEW (Previous Photo with AnimatedContent transition)
            if (prevPhoto != null) {
                AnimatedContent(
                    targetState = prevPhoto,
                    transitionSpec = {
                        fadeIn(animationSpec = androidx.compose.animation.core.tween(400)) togetherWith
                                fadeOut(animationSpec = androidx.compose.animation.core.tween(400))
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 24.dp)
                ) { targetPrev ->
                    Box(
                        modifier = Modifier
                            .size(width = 140.dp, height = 95.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = targetPrev.uri,
                            contentDescription = "Previous Photo Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(topEnd = 6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("◀ Prev", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. RIGHT EDGE PREVIEW (Next Photo with AnimatedContent transition)
            if (nextPhoto != null) {
                AnimatedContent(
                    targetState = nextPhoto,
                    transitionSpec = {
                        fadeIn(animationSpec = androidx.compose.animation.core.tween(400)) togetherWith
                                fadeOut(animationSpec = androidx.compose.animation.core.tween(400))
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                ) { targetNext ->
                    Box(
                        modifier = Modifier
                            .size(width = 140.dp, height = 95.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = targetNext.uri,
                            contentDescription = "Next Photo Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(topStart = 6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Next ▶", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. TOP BAR: Clock, Status Badges, QR pairing, and Settings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = horizontalPadding, vertical = topVerticalPadding)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showClock || alwaysShowClock) {
                        Column {
                            Text(
                                text = currentTimeString,
                                fontSize = if (isPortrait) 24.sp else 36.sp,
                                fontWeight = FontWeight.Light,
                                color = TvTextPrimary
                            )
                            Text(
                                text = currentDateString,
                                fontSize = if (isPortrait) 11.sp else 14.sp,
                                color = TvTextSecondary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                            label = "Phone (QR)",
                            onClick = onOpenQrModal,
                            testTag = "pair_phone_qr_button"
                        )

                        TvIconButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Settings",
                            onClick = onOpenSettings,
                            size = 46.dp,
                            iconSize = 22.dp,
                            testTag = "settings_button"
                        )
                    }
                }
            }

            // 4. CENTERED FROSTED PLAYBACK BUTTONS
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvIconButton(
                    icon = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    onClick = onPrevious,
                    size = 56.dp,
                    iconSize = 28.dp,
                    testTag = "previous_button"
                )

                TvIconButton(
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    onClick = onPlayPauseToggle,
                    size = 68.dp,
                    iconSize = 36.dp,
                    modifier = if (playPauseFocusRequester != null) Modifier.focusRequester(playPauseFocusRequester) else Modifier,
                    testTag = "play_pause_button"
                )

                TvIconButton(
                    icon = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    onClick = onNext,
                    size = 56.dp,
                    iconSize = 28.dp,
                    testTag = "next_button"
                )
            }

            // 5. BOTTOM METADATA BAR (Order, Album, Taken Date & Time)
            if (photo != null) {
                val takenDateStr = remember(photo.dateTakenMillis) {
                    if (photo.dateTakenMillis > 0) {
                        SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(photo.dateTakenMillis))
                    } else ""
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = photo.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TvTextPrimary,
                            maxLines = 1
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Photo ${currentIndex + 1} of $totalPhotos",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TvAccentCyan
                            )
                            Text(
                                text = "•",
                                fontSize = 12.sp,
                                color = TvTextMuted
                            )
                            Text(
                                text = photo.albumName,
                                fontSize = 12.sp,
                                color = TvTextSecondary
                            )
                            if (alwaysShowPhotoTakenDate && takenDateStr.isNotBlank()) {
                                Text(
                                    text = "•",
                                    fontSize = 12.sp,
                                    color = TvTextMuted
                                )
                                Text(
                                    text = "Taken: $takenDateStr",
                                    fontSize = 12.sp,
                                    color = TvTextSecondary
                                )
                            }
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
    containerColor: Color = Color.Black.copy(alpha = 0.55f),
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
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
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
            width = if (isFocused) 3.5.dp else 2.dp,
            color = if (isFocused) Color.White else accentColor.copy(alpha = 0.75f)
        ),
        modifier = modifier
            .scale(if (isFocused) 1.08f else 1.0f)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
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

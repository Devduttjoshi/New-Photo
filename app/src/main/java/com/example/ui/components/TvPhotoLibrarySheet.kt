package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.PhotoItem
import com.example.ui.theme.TvAccentAmber
import com.example.ui.theme.TvAccentBlue
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentEmerald
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceElevated
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary

@Composable
fun TvPhotoLibrarySheet(
    photos: List<PhotoItem>,
    currentIndex: Int,
    availableWeeks: List<String> = emptyList(),
    activeWeek: String = "ALL",
    activeSourceFilter: String = "ALL",
    totalPhotosCount: Int = 0,
    usbPhotosCount: Int = 0,
    cloudPhotosCount: Int = 0,
    phonePhotosCount: Int = 0,
    isRefreshing: Boolean = false,
    onSelectSourceFilter: (String) -> Unit = {},
    onSelectWeek: (String) -> Unit = {},
    onSelectPhoto: (Int) -> Unit,
    onTriggerSubtleRefresh: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val closeFocusRequester = remember { FocusRequester() }

    // Subtly and automatically trigger metadata & storage verification whenever library is opened
    LaunchedEffect(Unit) {
        onTriggerSubtleRefresh()
        kotlinx.coroutines.delay(100)
        try {
            closeFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Auto-scroll to currently playing photo
    LaunchedEffect(currentIndex, photos.size) {
        if (photos.isNotEmpty() && currentIndex in photos.indices) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = TvSurfaceDark.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        modifier = modifier
            .fillMaxWidth()
            .height(385.dp)
            .testTag("tv_photo_library_sheet")
    ) {
        Column(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 12.dp)
        ) {
            // 1. HEADER ROW: Title, Subtle Sync Badge, and Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Title & Subtle Sync Status Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = TvAccentCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Photo Library",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TvTextPrimary
                    )

                    // Subtle background sync pill (implements automatic library refresh without intrusive popups)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isRefreshing) TvAccentCyan.copy(alpha = 0.15f) else TvAccentEmerald.copy(alpha = 0.15f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isRefreshing) TvAccentCyan.copy(alpha = 0.5f) else TvAccentEmerald.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    color = TvAccentCyan,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Checking for new photos...",
                                    fontSize = 11.sp,
                                    color = TvAccentCyan,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = "●",
                                    fontSize = 9.sp,
                                    color = TvAccentEmerald
                                )
                                Text(
                                    text = "Library Synced • ${photos.size} Available",
                                    fontSize = 11.sp,
                                    color = TvAccentEmerald,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Right: Refresh & Close buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Manual refresh button (subtle quick action)
                    var isRefreshFocused by remember { mutableStateOf(false) }
                    Surface(
                        shape = CircleShape,
                        color = if (isRefreshFocused) TvSurfaceElevated else Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(
                            width = if (isRefreshFocused) 3.5.dp else 1.dp,
                            color = if (isRefreshFocused) TvAccentCyan else Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier
                            .size(38.dp)
                            .scale(if (isRefreshFocused) 1.1f else 1.0f)
                            .clip(CircleShape)
                            .clickable { onTriggerSubtleRefresh() }
                            .onFocusChanged { isRefreshFocused = it.isFocused }
                            .focusable()
                            .testTag("refresh_library_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh library",
                                tint = if (isRefreshFocused) TvAccentCyan else TvTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Close Button with TV D-Pad Focus Border
                    var isCloseFocused by remember { mutableStateOf(false) }
                    Surface(
                        shape = CircleShape,
                        color = if (isCloseFocused) TvAccentCyan else TvSurfaceElevated,
                        border = BorderStroke(
                            width = if (isCloseFocused) 3.5.dp else 2.dp,
                            color = if (isCloseFocused) Color.White else Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .size(38.dp)
                            .scale(if (isCloseFocused) 1.12f else 1.0f)
                            .focusRequester(closeFocusRequester)
                            .onFocusChanged { isCloseFocused = it.isFocused }
                            .focusable()
                            .clickable { onClose() }
                            .testTag("close_library_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close library",
                                tint = if (isCloseFocused) Color.Black else TvTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. SOURCE FILTER TABS: All, Google Photos, USB Flash, Phone Uploads
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibrarySourcePill(
                    label = "All Photos",
                    count = totalPhotosCount,
                    icon = Icons.Default.PhotoLibrary,
                    isSelected = activeSourceFilter == "ALL",
                    onClick = { onSelectSourceFilter("ALL") }
                )
                LibrarySourcePill(
                    label = "Google Photos",
                    count = cloudPhotosCount,
                    icon = Icons.Default.CloudDone,
                    isSelected = activeSourceFilter == "CLOUD",
                    onClick = { onSelectSourceFilter("CLOUD") }
                )
                LibrarySourcePill(
                    label = "USB Storage",
                    count = usbPhotosCount,
                    icon = Icons.Default.Usb,
                    isSelected = activeSourceFilter == "USB",
                    onClick = { onSelectSourceFilter("USB") }
                )
                if (phonePhotosCount > 0) {
                    LibrarySourcePill(
                        label = "Phone Uploads",
                        count = phonePhotosCount,
                        icon = Icons.Default.Smartphone,
                        isSelected = activeSourceFilter == "PHONE",
                        onClick = { onSelectSourceFilter("PHONE") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. TIMELINE WEEKS ROW (Optional horizontal scroll)
            if (availableWeeks.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        WeekFilterChip(
                            label = "All Timeline Weeks",
                            isSelected = activeWeek == "ALL",
                            onClick = { onSelectWeek("ALL") }
                        )
                    }
                    items(availableWeeks) { week ->
                        WeekFilterChip(
                            label = week,
                            isSelected = activeWeek == week,
                            onClick = { onSelectWeek(week) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. PHOTO THUMBNAILS CARDS ROW
            if (photos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No photos found in selected filter",
                            fontSize = 14.sp,
                            color = TvTextSecondary
                        )
                        if (activeSourceFilter != "ALL" || activeWeek != "ALL") {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = TvAccentCyan,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSelectSourceFilter("ALL")
                                        onSelectWeek("ALL")
                                    }
                                    .focusable()
                            ) {
                                Text(
                                    text = "Show All Photos",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(photos) { index, item ->
                        val isPlaying = index == currentIndex
                        var isFocused by remember { mutableStateOf(false) }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Black,
                            border = BorderStroke(
                                width = if (isFocused) 3.5.dp else if (isPlaying) 2.5.dp else 1.dp,
                                color = when {
                                    isFocused -> TvAccentCyan
                                    isPlaying -> TvAccentAmber
                                    else -> Color.White.copy(alpha = 0.15f)
                                }
                            ),
                            modifier = Modifier
                                .width(200.dp)
                                .height(180.dp)
                                .scale(if (isFocused) 1.08f else 1.0f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectPhoto(index) }
                                .onFocusChanged { isFocused = it.isFocused }
                                .focusable()
                                .testTag("thumbnail_card_$index")
                        ) {
                            Box {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.thumbnailUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )

                                // Top Badges Row: Source + Cache status
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopCenter)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Source Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.Black.copy(alpha = 0.75f)
                                    ) {
                                        Text(
                                            text = when (item.source) {
                                                "USB" -> "USB"
                                                "CLOUD" -> "Cloud"
                                                else -> "Local"
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (item.source) {
                                                "USB" -> TvAccentCyan
                                                "CLOUD" -> TvAccentBlue
                                                else -> TvAccentEmerald
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Cached Offline or Streaming Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (item.isCached) TvAccentEmerald.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.65f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            if (item.isCached) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    text = "Cached",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.CloudQueue,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    text = "Stream",
                                                    fontSize = 9.sp,
                                                    color = Color.White.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Bottom metadata bar: Title, Week, and Progression Index
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.7f),
                                                    Color.Black.copy(alpha = 0.95f)
                                                )
                                            )
                                        )
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = item.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TvTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (item.weekLabel.isNotBlank()) item.weekLabel else item.albumName,
                                                fontSize = 9.sp,
                                                color = if (isFocused || isPlaying) TvAccentCyan else TvTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Text(
                                                text = "#${item.albumProgressionIndex + 1}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPlaying) TvAccentAmber else TvTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySourcePill(
    label: String,
    count: Int,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = when {
            isFocused -> TvAccentCyan
            isSelected -> TvSurfaceElevated
            else -> Color.White.copy(alpha = 0.08f)
        },
        border = BorderStroke(
            width = if (isFocused) 3.5.dp else 1.5.dp,
            color = when {
                isFocused -> Color.White
                isSelected -> TvAccentCyan
                else -> Color.White.copy(alpha = 0.45f)
            }
        ),
        modifier = Modifier
            .scale(if (isFocused) 1.08f else 1.0f)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Color.Black else if (isSelected) TvAccentCyan else TvTextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "$label ($count)",
                fontSize = 11.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                color = if (isFocused) Color.Black else if (isSelected) TvAccentCyan else TvTextPrimary
            )
        }
    }
}

@Composable
private fun WeekFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = when {
            isFocused -> TvAccentCyan
            isSelected -> TvSurfaceElevated
            else -> Color.White.copy(alpha = 0.08f)
        },
        border = BorderStroke(
            width = if (isFocused) 3.5.dp else 1.5.dp,
            color = when {
                isFocused -> Color.White
                isSelected -> TvAccentCyan
                else -> Color.White.copy(alpha = 0.45f)
            }
        ),
        modifier = Modifier
            .scale(if (isFocused) 1.08f else 1.0f)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = if (isFocused) Color.Black else if (isSelected) TvAccentCyan else Color.White,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                color = if (isFocused) Color.Black else if (isSelected) TvAccentCyan else TvTextPrimary
            )
        }
    }
}

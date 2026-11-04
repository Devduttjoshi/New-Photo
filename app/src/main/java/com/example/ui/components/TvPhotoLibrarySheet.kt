package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.PhotoItem
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvFocusBorder
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
    onSelectWeek: (String) -> Unit = {},
    onSelectPhoto: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        if (photos.isNotEmpty() && currentIndex in photos.indices) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = TvSurfaceDark.copy(alpha = 0.98f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = modifier
            .fillMaxWidth()
            .height(310.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 12.dp)
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhotoAlbum,
                        contentDescription = null,
                        tint = TvAccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Album Progression (${photos.size} Photos)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TvTextPrimary
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_library_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close library",
                        tint = TvTextPrimary
                    )
                }
            }

            // Timeline Weeks Chips Row
            if (availableWeeks.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    item {
                        WeekFilterChip(
                            label = "All Weeks",
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
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Photo Thumbnails Row
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(photos) { index, item ->
                    val isSelected = index == currentIndex
                    var isFocused by remember { mutableStateOf(false) }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected || isFocused) 2.dp else 1.dp,
                            color = when {
                                isSelected -> TvAccentCyan
                                isFocused -> TvFocusBorder
                                else -> Color.White.copy(alpha = 0.15f)
                            }
                        ),
                        modifier = Modifier
                            .width(180.dp)
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectPhoto(index) }
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
                            .testTag("thumbnail_card_$index")
                    ) {
                        Box {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.displayUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )

                            // Title & Week metadata badge at bottom
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.82f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Column {
                                    Text(
                                        text = item.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TvTextPrimary,
                                        maxLines = 1
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (item.weekLabel.isNotBlank()) item.weekLabel else item.source,
                                            fontSize = 9.sp,
                                            color = if (isSelected) TvAccentCyan else TvTextSecondary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "#${item.albumProgressionIndex + 1}",
                                            fontSize = 9.sp,
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
            isSelected -> TvAccentCyan
            isFocused -> TvSurfaceElevated
            else -> Color.White.copy(alpha = 0.08f)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isFocused && !isSelected) TvFocusBorder else Color.Transparent
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = if (isSelected) Color.Black else TvAccentCyan,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.Black else TvTextPrimary
            )
        }
    }
}

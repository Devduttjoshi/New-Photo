package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SlideshowSettings
import com.example.ui.theme.TvAccentAmber
import com.example.ui.theme.TvAccentBlue
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentEmerald
import com.example.ui.theme.TvFocusBorder
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceElevated
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary

@Composable
fun TvSettingsDrawer(
    settings: SlideshowSettings,
    cacheStatsText: String,
    isSyncing: Boolean,
    syncStatusMessage: String,
    totalPhotos: Int,
    usbPhotosCount: Int,
    cloudPhotosCount: Int,
    availableWeeks: List<String>,
    activeWeek: String,
    onSelectWeek: (String) -> Unit,
    onIndexFullAlbum: () -> Unit,
    onClose: () -> Unit,
    onUpdateAutoStart: (Boolean) -> Unit,
    onUpdateAutoResumeSession: (Boolean) -> Unit,
    onUpdateInterval: (Int) -> Unit,
    onUpdateCrossfade: (Int) -> Unit,
    onUpdateKenBurns: (Boolean) -> Unit,
    onUpdateShowClock: (Boolean) -> Unit,
    onUpdateShowMetadata: (Boolean) -> Unit,
    onUpdateShuffle: (Boolean) -> Unit,
    onUpdateSourceFilter: (String) -> Unit,
    onScanUsb: () -> Unit,
    onSyncCloudPreset: (String) -> Unit,
    onSyncCustomUrl: (String) -> Unit,
    onClearCache: () -> Unit,
    onUpdateMaxCacheLimit: (Int) -> Unit,
    onOpenQrModal: () -> Unit,
    onUpdateFramingMode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var customCloudUrl by remember { mutableStateOf("") }

    BoxWithConstraints(modifier = modifier.fillMaxHeight()) {
        val drawerWidth = if (maxWidth < 560.dp) maxWidth else 520.dp

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(drawerWidth)
                .background(TvSurfaceDark.copy(alpha = 0.98f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxHeight()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TV Slideshow Settings",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvTextPrimary
                        )
                        Text(
                            text = "Album Progression, Timeline & Storage",
                            fontSize = 12.sp,
                            color = TvTextSecondary
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close settings",
                            tint = TvTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable options list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    // SECTION 1: TIMELINE & WEEKLY GROUPING
                    item {
                        SettingsSectionTitle("Timeline & Weekly Grouping")
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TvSurfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = TvAccentCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Filter or Start From Specific Week",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TvTextPrimary
                                    )
                                }
                                Text(
                                    text = "Photos are grouped by week of capture. Whichever photo is loaded registers into metadata.",
                                    fontSize = 11.sp,
                                    color = TvTextSecondary,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    item {
                                        val isAll = activeWeek == "ALL"
                                        TvOptionPill(
                                            label = "All Weeks",
                                            isSelected = isAll,
                                            onClick = { onSelectWeek("ALL") }
                                        )
                                    }
                                    items(availableWeeks) { week ->
                                        val isSelected = activeWeek == week
                                        TvOptionPill(
                                            label = week,
                                            isSelected = isSelected,
                                            onClick = { onSelectWeek(week) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 2: SESSION RESUME & BOOT
                    item {
                        SettingsSectionTitle("Session Persistence & Startup")
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            TvToggleCard(
                                icon = Icons.Default.History,
                                title = "Resume Where Left Off",
                                subtitle = "Registers shown photos and continues progression across app restarts",
                                checked = settings.autoResumeSession,
                                onCheckedChange = onUpdateAutoResumeSession
                            )
                            TvToggleCard(
                                icon = Icons.Default.PowerSettingsNew,
                                title = "Auto-Start on TV Boot",
                                subtitle = "Automatically launches photo slideshow when TV powers on",
                                checked = settings.autoStartOnBoot,
                                onCheckedChange = onUpdateAutoStart
                            )
                        }
                    }

                    // SECTION 3: FULL METADATA INDEXING & DYNAMIC SYNC
                    item {
                        SettingsSectionTitle("Dynamic Album Progression (${totalPhotos} Registered)")
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TvSurfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Lightweight Metadata Directory",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TvTextPrimary
                                )
                                Text(
                                    text = "By default, photos register metadata progressively as they display. For large albums (3,000+ photos), you can index all photo positions and weeks immediately without downloading raw images.",
                                    fontSize = 12.sp,
                                    color = TvTextSecondary
                                )

                                Button(
                                    onClick = onIndexFullAlbum,
                                    colors = ButtonDefaults.buttonColors(containerColor = TvAccentCyan),
                                    enabled = !isSyncing,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("index_full_album_button")
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(
                                            color = Color.Black,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = syncStatusMessage.ifBlank { "Indexing Album..." },
                                            color = Color.Black,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.CloudSync,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Index All 3,000+ Album Photos Now",
                                            color = Color.Black,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (isSyncing && syncStatusMessage.isNotBlank()) {
                                    Text(
                                        text = syncStatusMessage,
                                        fontSize = 11.sp,
                                        color = TvAccentAmber,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 4: ROLLING LRU CACHE & STORAGE CAP
                    item {
                        SettingsSectionTitle("Storage & Rolling LRU Cache")
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TvSurfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Rolling Cache Limit",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TvTextPrimary
                                        )
                                        Text(
                                            text = "Prevents 3,000+ photo albums from exhausting TV disk storage",
                                            fontSize = 11.sp,
                                            color = TvTextSecondary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = TvAccentEmerald,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        Pair(100, "100 MB"),
                                        Pair(250, "250 MB"),
                                        Pair(500, "500 MB"),
                                        Pair(1000, "1 GB")
                                    ).forEach { (sizeMb, label) ->
                                        val isSelected = settings.maxCacheSizeMb == sizeMb
                                        TvOptionPill(
                                            label = label,
                                            isSelected = isSelected,
                                            onClick = { onUpdateMaxCacheLimit(sizeMb) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cacheStatsText,
                                        fontSize = 11.sp,
                                        color = TvAccentEmerald,
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedButton(
                                        onClick = onClearCache,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clear Cache", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 5: PHOTO SOURCES & PHONE PAIRING
                    item {
                        SettingsSectionTitle("Photo Sources")
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Filter pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Pair("ALL", "All Photos ($totalPhotos)"),
                                    Pair("USB", "USB ($usbPhotosCount)"),
                                    Pair("CLOUD", "Cloud ($cloudPhotosCount)")
                                ).forEach { (filterKey, label) ->
                                    val isSelected = settings.activeSourceFilter == filterKey
                                    TvOptionPill(
                                        label = label,
                                        isSelected = isSelected,
                                        onClick = { onUpdateSourceFilter(filterKey) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // QR Phone Pairing button
                            Button(
                                onClick = onOpenQrModal,
                                colors = ButtonDefaults.buttonColors(containerColor = TvAccentCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("open_qr_pairing_from_settings")
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Pair Phone via QR (Send Links & Photos)",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            // USB Scan button
                            Button(
                                onClick = onScanUsb,
                                colors = ButtonDefaults.buttonColors(containerColor = TvAccentBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("scan_usb_button")
                            ) {
                                Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan USB Drive & Local Media", fontSize = 12.sp)
                            }

                            // Custom link field
                            OutlinedTextField(
                                value = customCloudUrl,
                                onValueChange = { customCloudUrl = it },
                                placeholder = { Text("Enter Google Photos album / image link", fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TvAccentCyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = TvTextPrimary,
                                    unfocusedTextColor = TvTextPrimary
                                ),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (customCloudUrl.isNotBlank()) {
                                                onSyncCustomUrl(customCloudUrl)
                                                customCloudUrl = ""
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.CloudSync, contentDescription = "Sync", tint = TvAccentCyan)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // SECTION 6: TRANSITIONS & MOTION
                    item {
                        SettingsSectionTitle("Transitions & Motion")
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Slide Interval
                            Text(
                                text = "Slide Display Interval",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TvTextPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(4, 8, 15, 30, 60).forEach { seconds ->
                                    val isSelected = settings.slideIntervalSeconds == seconds
                                    TvOptionPill(
                                        label = "${seconds}s",
                                        isSelected = isSelected,
                                        onClick = { onUpdateInterval(seconds) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Ken Burns motion toggle
                            TvToggleCard(
                                icon = Icons.Default.Speed,
                                title = "Ken Burns Ambient Motion",
                                subtitle = "Slow pan and subtle zoom during playback",
                                checked = settings.kenBurnsEnabled,
                                onCheckedChange = onUpdateKenBurns
                            )

                            // Crossfade transition duration
                            Text(
                                text = "Cross-fade Speed",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TvTextPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Triple(800, "Fast", "800ms"),
                                    Triple(1500, "Smooth", "1.5s"),
                                    Triple(3000, "Dreamy", "3.0s")
                                ).forEach { (ms, label, desc) ->
                                    val isSelected = settings.crossfadeDurationMs == ms
                                    TvOptionPill(
                                        label = "$label ($desc)",
                                        isSelected = isSelected,
                                        onClick = { onUpdateCrossfade(ms) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Framing Mode (Portrait & Landscape handling)
                            Text(
                                text = "Photo Framing & Orientation",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TvTextPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Pair("AMBIENT_FIT", "Ambient Blur"),
                                    Pair("FILL_CROP", "Fill Screen"),
                                    Pair("BLACK_BARS", "Clean Black")
                                ).forEach { (mode, title) ->
                                    val isSelected = settings.framingMode == mode
                                    TvOptionPill(
                                        label = title,
                                        isSelected = isSelected,
                                        onClick = { onUpdateFramingMode(mode) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 7: DISPLAY OVERLAYS
                    item {
                        SettingsSectionTitle("Display Overlays")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvToggleCard(
                                icon = Icons.Default.Timer,
                                title = "Show Clock & Date",
                                subtitle = "Ambient digital time in top corner",
                                checked = settings.showClockAndDate,
                                onCheckedChange = onUpdateShowClock
                            )
                            TvToggleCard(
                                icon = Icons.Default.Refresh,
                                title = "Shuffle Slideshow",
                                subtitle = "Randomize photo order (off = album progression)",
                                checked = settings.shuffleOrder,
                                onCheckedChange = onUpdateShuffle
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = TvAccentCyan,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun TvToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) TvSurfaceElevated.copy(alpha = 0.9f) else TvSurfaceElevated.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isFocused) TvFocusBorder else Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) TvAccentCyan else TvTextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TvTextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TvTextSecondary
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TvAccentCyan,
                    checkedTrackColor = TvAccentBlue
                )
            )
        }
    }
}

@Composable
private fun TvOptionPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = when {
            isSelected -> TvAccentCyan
            isFocused -> TvSurfaceElevated
            else -> Color.White.copy(alpha = 0.08f)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isFocused && !isSelected) TvFocusBorder else Color.Transparent
        ),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.Black else TvTextPrimary
            )
        }
    }
}

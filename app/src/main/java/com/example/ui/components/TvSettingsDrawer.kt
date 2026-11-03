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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.ui.theme.TvTextMuted
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
    onClose: () -> Unit,
    onUpdateAutoStart: (Boolean) -> Unit,
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
    modifier: Modifier = Modifier
) {
    var customCloudUrl by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(520.dp)
            .background(TvSurfaceDark.copy(alpha = 0.96f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
            )
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TV Slideshow Settings",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TvTextPrimary
                    )
                    Text(
                        text = "Android TV Startup, USB & Cloud Sync",
                        fontSize = 13.sp,
                        color = TvTextSecondary
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close settings",
                        tint = TvTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable list of TV options
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // SECTION 1: TV BOOT STARTUP
                item {
                    SettingsSectionTitle("Android TV Startup")
                    TvToggleCard(
                        icon = Icons.Default.PowerSettingsNew,
                        title = "Auto-Start on Boot",
                        subtitle = "Automatically launches photo slideshow when your TV turns on",
                        checked = settings.autoStartOnBoot,
                        onCheckedChange = onUpdateAutoStart
                    )
                }

                // SECTION 2: TRANSITIONS & MOTION
                item {
                    SettingsSectionTitle("Transitions & Motion")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Cross-fade animation speeds
                        Text(
                            text = "Cross-fade Transition Speed",
                            fontSize = 14.sp,
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

                        Spacer(modifier = Modifier.height(4.dp))

                        // Ken Burns motion toggle
                        TvToggleCard(
                            icon = Icons.Default.Speed,
                            title = "Ken Burns Ambient Motion",
                            subtitle = "Slow pan and subtle zoom during playback",
                            checked = settings.kenBurnsEnabled,
                            onCheckedChange = onUpdateKenBurns
                        )

                        // Slide Interval duration
                        Text(
                            text = "Slide Display Interval",
                            fontSize = 14.sp,
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
                    }
                }

                // SECTION 3: PHOTO SOURCES & SYNC
                item {
                    SettingsSectionTitle("Photo Sources (Total: $totalPhotos)")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Filter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Pair("ALL", "All Photos ($totalPhotos)"),
                                Pair("USB", "USB Only ($usbPhotosCount)"),
                                Pair("CLOUD", "Cloud Only ($cloudPhotosCount)")
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

                        // USB Drive Scan button
                        Button(
                            onClick = onScanUsb,
                            colors = ButtonDefaults.buttonColors(containerColor = TvAccentBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("scan_usb_button")
                        ) {
                            Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan USB Flash Drive & Local Media")
                        }

                        // Free Cloud Presets
                        Text(
                            text = "Cloud Sync Album (Free Tier)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TvTextPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Pair("LANDSCAPES", "Landscapes"),
                                Pair("ARCHITECTURE", "Architecture"),
                                Pair("COSMOS", "Cosmos & Sky")
                            ).forEach { (presetKey, title) ->
                                val isSelected = settings.cloudAlbumPreset == presetKey
                                TvOptionPill(
                                    label = title,
                                    isSelected = isSelected,
                                    onClick = { onSyncCloudPreset(presetKey) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Custom Google Photos / Cloud Album URL
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

                        if (isSyncing) {
                            Text(
                                text = syncStatusMessage,
                                fontSize = 12.sp,
                                color = TvAccentAmber,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // SECTION 4: OFFLINE CACHE & DATA SAVER
                item {
                    SettingsSectionTitle("Offline Cache & Data Saver")
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
                                        text = "Offline Storage Status",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TvTextPrimary
                                    )
                                    Text(
                                        text = cacheStatsText,
                                        fontSize = 12.sp,
                                        color = TvAccentEmerald
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = TvAccentEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onClearCache,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Clear Cache", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // SECTION 5: DISPLAY OVERLAYS
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
                            subtitle = "Randomize photo order",
                            checked = settings.shuffleOrder,
                            onCheckedChange = onUpdateShuffle
                        )
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
        fontSize = 15.sp,
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
                    fontSize = 14.sp,
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
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
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

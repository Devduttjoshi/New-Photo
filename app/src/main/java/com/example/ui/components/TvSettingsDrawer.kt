package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CloudSyncManager
import com.example.data.SlideshowSettings
import com.example.ui.theme.TvAccentAmber
import com.example.ui.theme.TvAccentBlue
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentEmerald
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
    onClose: () -> Unit,
    onUpdateAutoStart: (Boolean) -> Unit,
    onUpdateAutoResumeSession: (Boolean) -> Unit,
    onUpdateInterval: (Int) -> Unit,
    onUpdateCrossfade: (Int) -> Unit,
    onUpdateKenBurns: (Boolean) -> Unit,
    onUpdateShowClock: (Boolean) -> Unit,
    onUpdateShowMetadata: (Boolean) -> Unit,
    onUpdateAlwaysShowClock: (Boolean) -> Unit = {},
    onUpdateAlwaysShowPhotoTakenDate: (Boolean) -> Unit = {},
    onUpdateShuffle: (Boolean) -> Unit,
    onUpdateSourceFilter: (String) -> Unit,
    onScanUsb: () -> Unit,
    onSyncCloudPreset: (String) -> Unit,
    onSyncCustomUrl: (String) -> Unit,
    onClearCache: () -> Unit,
    onUpdateMaxCacheLimit: (Int) -> Unit,
    onOpenQrModal: () -> Unit,
    onUpdateFramingMode: (String) -> Unit,
    onOpenBrowser: (String) -> Unit = {},
    albumLinks: List<com.example.model.AlbumLinkItem> = emptyList(),
    onToggleAlbumIncluded: (Long, Boolean) -> Unit = { _, _ -> },
    onRemoveAlbumLink: (com.example.model.AlbumLinkItem) -> Unit = {},
    onUpdateCoverFlowEnabled: (Boolean) -> Unit = {},
    onUpdateCoverFlowAngle: (Float) -> Unit = {},
    onUpdateCoverFlowSpacing: (Float) -> Unit = {},
    onOpenCoverFlow: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var customCloudUrl by remember { mutableStateOf("") }
    var isTextFieldFocused by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    BoxWithConstraints(modifier = modifier.fillMaxHeight()) {
        val drawerWidth = if (maxWidth < 560.dp) maxWidth else 520.dp

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(drawerWidth)
                .background(TvSurfaceDark.copy(alpha = 0.98f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
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
                            text = "D-Pad Navigation • Album Progression • Cloud Sync",
                            fontSize = 12.sp,
                            color = TvTextSecondary
                        )
                    }

                    val closeFocusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(100)
                        try {
                            closeFocusRequester.requestFocus()
                        } catch (_: Exception) {}
                    }

                    var isCloseFocused by remember { mutableStateOf(false) }
                    Surface(
                        shape = CircleShape,
                        color = if (isCloseFocused) TvAccentCyan else TvSurfaceElevated,
                        border = BorderStroke(
                            width = if (isCloseFocused) 3.5.dp else 2.dp,
                            color = if (isCloseFocused) Color.White else Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .size(44.dp)
                            .scale(if (isCloseFocused) 1.12f else 1.0f)
                            .focusRequester(closeFocusRequester)
                            .onFocusChanged { isCloseFocused = it.isFocused }
                            .focusable()
                            .clickable { onClose() }
                            .testTag("close_settings_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close settings",
                                tint = if (isCloseFocused) Color.Black else TvTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable options list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    // SECTION 1: PHOTO SOURCES & LINK INPUT (Placed at top for easy TV access)
                    item {
                        SettingsSectionTitle("Paste / Add Google Photos Album")
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
                                    text = "Album Link or Image URL",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TvTextPrimary
                                )
                                Text(
                                    text = "Paste any Google Photos album link or image URL. You can also use TV clipboard or phone QR pairing.",
                                    fontSize = 11.sp,
                                    color = TvTextSecondary
                                )

                                // Text input field with clear TV focus border
                                OutlinedTextField(
                                    value = customCloudUrl,
                                    onValueChange = { customCloudUrl = it },
                                    placeholder = { Text("https://photos.app.goo.gl/... or image URL", fontSize = 12.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                    keyboardActions = KeyboardActions(
                                        onGo = {
                                            if (customCloudUrl.isNotBlank()) {
                                                onSyncCustomUrl(customCloudUrl)
                                            }
                                        }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TvAccentCyan,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                        focusedTextColor = TvTextPrimary,
                                        unfocusedTextColor = TvTextPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isTextFieldFocused = it.isFocused }
                                        .border(
                                            width = if (isTextFieldFocused) 3.dp else 0.dp,
                                            color = if (isTextFieldFocused) TvAccentCyan else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )

                                // Action Buttons Row: Paste from Clipboard & Load Album
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TvActionButton(
                                        text = "Paste Clipboard",
                                        icon = Icons.Default.ContentPaste,
                                        onClick = {
                                            val clip = clipboardManager.getText()?.text?.trim()
                                            if (!clip.isNullOrBlank()) {
                                                customCloudUrl = clip
                                                onSyncCustomUrl(clip)
                                            }
                                        },
                                        containerColor = TvSurfaceElevated,
                                        contentColor = Color.White,
                                        modifier = Modifier.weight(1f),
                                        testTag = "paste_clipboard_button"
                                    )

                                    TvActionButton(
                                        text = if (isSyncing) "Loading..." else "Load & Sync Album",
                                        icon = Icons.Default.CloudSync,
                                        onClick = {
                                            if (customCloudUrl.isNotBlank()) {
                                                onSyncCustomUrl(customCloudUrl)
                                            }
                                        },
                                        containerColor = TvAccentCyan,
                                        contentColor = Color.Black,
                                        enabled = !isSyncing && customCloudUrl.isNotBlank(),
                                        modifier = Modifier.weight(1.3f),
                                        testTag = "load_album_button"
                                    )
                                }

                                if (isSyncing || syncStatusMessage.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(
                                                color = TvAccentCyan,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = syncStatusMessage,
                                            fontSize = 12.sp,
                                            color = if (syncStatusMessage.startsWith("No photos")) Color(0xFFEF4444) else TvAccentEmerald,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // QR Pairing Button (No typing needed)
                                TvActionButton(
                                    text = "Pair Phone via QR Code",
                                    icon = Icons.Default.QrCode2,
                                    onClick = onOpenQrModal,
                                    containerColor = TvSurfaceElevated,
                                    contentColor = TvAccentCyan,
                                    focusedBorderColor = TvAccentCyan,
                                    testTag = "open_qr_pairing_from_settings"
                                )
                            }
                        }
                    }

                    // SECTION 2: LINK LIBRARY (Stored Album Links, Photo Counts, Slideshow Inclusions, Remove Button)
                    item {
                        SettingsSectionTitle("Link Library • Stored Albums")
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TvSurfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Albums in Library (${albumLinks.size})",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TvTextPrimary
                                        )
                                        Text(
                                            text = "Select which albums to include in the slideshow. Remove unwanted links anytime.",
                                            fontSize = 11.sp,
                                            color = TvTextSecondary
                                        )
                                    }
                                }

                                if (albumLinks.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                            .padding(14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No saved album links yet. Paste an album link above or scan phone QR to store albums.",
                                            fontSize = 12.sp,
                                            color = TvTextSecondary
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        albumLinks.forEach { album ->
                                            AlbumLinkCard(
                                                album = album,
                                                onToggleIncluded = { isInc -> onToggleAlbumIncluded(album.id, isInc) },
                                                onRemove = { onRemoveAlbumLink(album) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 2: TIMELINE & WEEKLY GROUPING
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

                    // SECTION 3: SESSION PERSISTENCE & TV STARTUP
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
                                            text = "Prevents large albums from exhausting TV disk storage",
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

                                    var isClearFocused by remember { mutableStateOf(false) }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isClearFocused) Color(0xFFEF4444).copy(alpha = 0.25f) else Color.Transparent,
                                        border = BorderStroke(
                                            width = if (isClearFocused) 3.dp else 1.dp,
                                            color = if (isClearFocused) Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier
                                            .scale(if (isClearFocused) 1.05f else 1.0f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onClearCache() }
                                            .onFocusChanged { isClearFocused = it.isFocused }
                                            .focusable()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Clear Cache", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 6: SOURCE FILTER & USB SCAN
                    item {
                        SettingsSectionTitle("Filter Sources & Local USB")
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Filter pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Pair("ALL", "All ($totalPhotos)"),
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

                            // USB Scan button
                            TvActionButton(
                                text = "Scan USB Drive & Local Storage",
                                icon = Icons.Default.Usb,
                                onClick = onScanUsb,
                                containerColor = TvAccentBlue,
                                contentColor = Color.White,
                                testTag = "scan_usb_button"
                            )
                        }
                    }

                    // SECTION 7: TRANSITIONS & MOTION
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

                    // SECTION 8: DISPLAY OVERLAYS
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
                                icon = Icons.Default.Timer,
                                title = "Always Display Clock",
                                subtitle = "Keep digital clock visible on screen",
                                checked = settings.alwaysShowClock,
                                onCheckedChange = onUpdateAlwaysShowClock
                            )
                            TvToggleCard(
                                icon = Icons.Default.DateRange,
                                title = "Always Display Photo Taken Date",
                                subtitle = "Show date and time when photo was taken",
                                checked = settings.alwaysShowPhotoTakenDate,
                                onCheckedChange = onUpdateAlwaysShowPhotoTakenDate
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

                    // SECTION 9: DEBUG / BETA
                    item {
                        SettingsSectionTitle("Debug / Beta")
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TvSurfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = TvAccentCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Web Browser (Beta)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TvTextPrimary
                                    )
                                }
                                Text(
                                    text = "Open the built-in TV browser to test web pages, explore online galleries, or inspect photo album links.",
                                    fontSize = 11.sp,
                                    color = TvTextSecondary
                                )

                                TvActionButton(
                                    text = "Launch Web Browser",
                                    icon = Icons.Default.Language,
                                    onClick = { onOpenBrowser("https://photos.google.com") },
                                    containerColor = TvSurfaceElevated,
                                    contentColor = TvAccentCyan,
                                    focusedBorderColor = TvAccentCyan,
                                    testTag = "open_web_browser_btn"
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
 * TV Focus-Aware Full Action Button with high-contrast 3.5dp border and solid surface.
 * Guarantees visible, high-contrast text and icons on all TV display profiles.
 */
@Composable
fun TvActionButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = TvSurfaceElevated,
    contentColor: Color = Color.White,
    focusedBorderColor: Color = TvAccentCyan,
    enabled: Boolean = true,
    testTag: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }

    // When focused: bright cyan background with black text.
    // When unfocused: solid elevated container with crisp white text.
    val effectiveBg = when {
        !enabled -> containerColor.copy(alpha = 0.4f)
        isFocused -> TvAccentCyan
        else -> containerColor
    }

    val effectiveText = when {
        !enabled -> Color.White.copy(alpha = 0.5f)
        isFocused -> Color.Black
        else -> contentColor
    }

    val effectiveBorder = when {
        isFocused -> Color.White
        else -> Color.White.copy(alpha = 0.5f)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = effectiveBg,
        border = BorderStroke(
            width = if (isFocused) 3.5.dp else 2.dp,
            color = effectiveBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .scale(if (isFocused) 1.05f else 1.0f)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(enabled = enabled)
            .clickable(enabled = enabled) { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = effectiveText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = effectiveText,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
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
        color = if (isFocused) TvSurfaceElevated else TvSurfaceElevated.copy(alpha = 0.5f),
        border = BorderStroke(
            width = if (isFocused) 3.5.dp else 1.5.dp,
            color = if (isFocused) Color.White else Color.White.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isFocused) 1.03f else 1.0f)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused || checked) TvAccentCyan else TvTextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
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
        modifier = modifier
            .scale(if (isFocused) 1.08f else 1.0f)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isFocused -> Color.Black
                    isSelected -> TvAccentCyan
                    else -> Color.White
                }
            )
        }
    }
}

/**
 * Focus-aware Card displaying an album in the Link Library:
 * Shows album title, photo count, inclusion toggle, and a separate remove button.
 * Fully navigable via TV remote D-pad with prominent 3.5dp borders.
 */
@Composable
private fun AlbumLinkCard(
    album: com.example.model.AlbumLinkItem,
    onToggleIncluded: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    var isToggleFocused by remember { mutableStateOf(false) }
    var isRemoveFocused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = TvSurfaceDark.copy(alpha = 0.85f),
        border = BorderStroke(
            width = 1.5.dp,
            color = Color.White.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Album Button: Toggles inclusion in slideshow
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    isToggleFocused -> TvAccentCyan
                    album.isIncludedInSlideshow -> TvAccentCyan.copy(alpha = 0.15f)
                    else -> Color.White.copy(alpha = 0.06f)
                },
                border = BorderStroke(
                    width = if (isToggleFocused) 3.5.dp else 1.5.dp,
                    color = if (isToggleFocused) Color.White else if (album.isIncludedInSlideshow) TvAccentCyan else Color.White.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .scale(if (isToggleFocused) 1.03f else 1.0f)
                    .onFocusChanged { isToggleFocused = it.isFocused }
                    .focusable()
                    .clickable { onToggleIncluded(!album.isIncludedInSlideshow) }
                    .testTag("toggle_album_${album.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = album.albumName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isToggleFocused) Color.Black else TvTextPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${album.photoCount} photos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isToggleFocused) Color.Black else TvAccentCyan
                            )
                            Text(
                                text = if (album.isIncludedInSlideshow) "• In Slideshow" else "• Excluded",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isToggleFocused) Color.Black else if (album.isIncludedInSlideshow) TvAccentEmerald else TvTextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = album.isIncludedInSlideshow,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (isToggleFocused) Color.Black else TvAccentCyan,
                            checkedTrackColor = if (isToggleFocused) Color.White else TvAccentBlue
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Dedicated Remove Album Button
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isRemoveFocused) Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.15f),
                border = BorderStroke(
                    width = if (isRemoveFocused) 3.5.dp else 1.5.dp,
                    color = if (isRemoveFocused) Color.White else Color(0xFFEF4444)
                ),
                modifier = Modifier
                    .scale(if (isRemoveFocused) 1.08f else 1.0f)
                    .onFocusChanged { isRemoveFocused = it.isFocused }
                    .focusable()
                    .clickable { onRemove() }
                    .testTag("remove_album_${album.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove album",
                        tint = if (isRemoveFocused) Color.White else Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Remove",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRemoveFocused) Color.White else Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

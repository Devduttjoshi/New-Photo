package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentEmerald
import com.example.ui.theme.TvFocusBorder
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceElevated
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.util.QrCodeGenerator

@Composable
fun TvQrPairingDialog(
    isOpen: Boolean,
    serverUrl: String,
    localIpAddress: String,
    isPhoneConnected: Boolean,
    syncStatusMessage: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val qrImageBitmap = remember(serverUrl) {
        QrCodeGenerator.generateQrImageBitmap(serverUrl, 600)
    }

    var isCloseButtonFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = TvSurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
                .width(820.dp)
                .clickable(enabled = false) {} // consume clicks inside
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = TvAccentCyan.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = TvAccentCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Connect Phone to TV",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TvTextPrimary
                            )
                            Text(
                                text = "Scan with your phone camera to send Google Photos without typing",
                                fontSize = 13.sp,
                                color = TvTextSecondary
                            )
                        }
                    }

                    var isTopCloseFocused by remember { mutableStateOf(false) }
                    Surface(
                        shape = CircleShape,
                        color = if (isTopCloseFocused) TvSurfaceElevated else Color.White.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isTopCloseFocused) 3.5.dp else 1.dp,
                            color = if (isTopCloseFocused) TvAccentCyan else Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { onClose() }
                            .onFocusChanged { isTopCloseFocused = it.isFocused }
                            .focusable()
                            .testTag("close_qr_dialog_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (isTopCloseFocused) TvAccentCyan else TvTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Body: QR Left, Instructions Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Crisp QR Code
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(280.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .size(260.dp)
                                .padding(8.dp)
                        ) {
                            Image(
                                bitmap = qrImageBitmap,
                                contentDescription = "QR Code to pair phone",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = serverUrl,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvAccentCyan
                        )
                        Text(
                            text = "Or type this in your phone's browser",
                            fontSize = 11.sp,
                            color = TvTextSecondary
                        )
                    }

                    // Right: Step-by-Step Instructions & Live Status
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PairingStep(
                            number = "1",
                            title = "Scan QR with Phone Camera",
                            desc = "Open your smartphone camera or QR scanner to launch the web companion."
                        )

                        PairingStep(
                            number = "2",
                            title = "Paste Google Photos Link",
                            desc = "Copy any public or shared Google Photos album link and tap Send to TV."
                        )

                        PairingStep(
                            number = "3",
                            title = "Or Upload Photos Directly",
                            desc = "Select photos from your phone gallery to beam directly to the TV."
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPhoneConnected) TvAccentEmerald.copy(alpha = 0.15f) else TvSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPhoneConnected) TvAccentEmerald.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPhoneConnected) Icons.Default.CheckCircle else Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = if (isPhoneConnected) TvAccentEmerald else TvAccentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isPhoneConnected) "Phone Connected!" else "TV Ready on Local Wi-Fi",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isPhoneConnected) TvAccentEmerald else TvTextPrimary
                                    )
                                    Text(
                                        text = if (syncStatusMessage.isNotBlank()) syncStatusMessage else "Keep phone on the same Wi-Fi network",
                                        fontSize = 11.sp,
                                        color = TvTextSecondary
                                    )
                                }
                            }
                        }

                        // Close button for TV remote
                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCloseButtonFocused) TvAccentCyan else TvSurfaceElevated,
                                contentColor = if (isCloseButtonFocused) Color.Black else TvTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .scale(if (isCloseButtonFocused) 1.04f else 1.0f)
                                .onFocusChanged { isCloseButtonFocused = it.isFocused }
                                .border(
                                    width = if (isCloseButtonFocused) 3.5.dp else 1.dp,
                                    color = if (isCloseButtonFocused) TvAccentCyan else Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .focusable()
                                .testTag("dismiss_qr_modal_button")
                        ) {
                            Text(
                                text = "Return to Slideshow",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PairingStep(
    number: String,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = TvAccentCyan.copy(alpha = 0.2f),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TvAccentCyan
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TvTextPrimary
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = TvTextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

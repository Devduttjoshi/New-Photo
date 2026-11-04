package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentEmerald
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceElevated
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary

/**
 * TV Web Browser (Debug / Beta)
 * Built for remote / D-pad interaction with address bar, quick bookmarks,
 * back/forward navigation, reload, progress indicator, and smooth web viewing.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TvBrowserScreen(
    initialUrl: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentUrl by remember { mutableStateOf(initialUrl.ifBlank { "https://photos.google.com" }) }
    var inputUrl by remember { mutableStateOf(currentUrl) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf("Web Browser") }
    var isLoading by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // Intercept back button to navigate web history first, or close browser
    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onClose()
        }
    }

    val quickBookmarks = listOf(
        Pair("Google Photos", "https://photos.google.com"),
        Pair("Unsplash Nature", "https://unsplash.com/t/nature"),
        Pair("Wikimedia Featured", "https://commons.wikimedia.org/wiki/Main_Page"),
        Pair("Google", "https://www.google.com")
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvSurfaceDark)
            .testTag("tv_web_browser_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP BAR: Navigation Controls & URL Input
            Surface(
                color = TvSurfaceDark.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Badge / Beta tag
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TvAccentEmerald.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, TvAccentEmerald)
                        ) {
                            Text(
                                text = "BETA",
                                color = TvAccentEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Navigation Buttons: Back, Forward, Reload
                        TvBrowserIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            enabled = canGoBack,
                            onClick = { webViewInstance?.goBack() },
                            testTag = "browser_back_btn"
                        )

                        TvBrowserIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            enabled = canGoForward,
                            onClick = { webViewInstance?.goForward() },
                            testTag = "browser_forward_btn"
                        )

                        TvBrowserIconButton(
                            icon = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            enabled = true,
                            onClick = { webViewInstance?.reload() },
                            testTag = "browser_reload_btn"
                        )

                        // Address / URL Bar
                        var isUrlFocused by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            placeholder = { Text("Enter URL or search...", fontSize = 13.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = if (inputUrl.startsWith("https://")) Icons.Default.Lock else Icons.Default.Language,
                                    contentDescription = null,
                                    tint = if (isUrlFocused) TvAccentCyan else TvTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        color = TvAccentCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    val formatted = formatUrl(inputUrl)
                                    currentUrl = formatted
                                    webViewInstance?.loadUrl(formatted)
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TvAccentCyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = TvTextPrimary,
                                unfocusedTextColor = TvTextPrimary,
                                focusedContainerColor = TvSurfaceElevated,
                                unfocusedContainerColor = TvSurfaceElevated.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .onFocusChanged { isUrlFocused = it.isFocused }
                                .testTag("browser_url_input")
                        )

                        // GO Button
                        TvBrowserTextButton(
                            text = "Go",
                            onClick = {
                                val formatted = formatUrl(inputUrl)
                                currentUrl = formatted
                                webViewInstance?.loadUrl(formatted)
                            },
                            testTag = "browser_go_btn"
                        )

                        // Close Button (Back to Slideshow)
                        TvBrowserIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = "Close Browser",
                            enabled = true,
                            onClick = onClose,
                            containerColor = Color.White.copy(alpha = 0.1f),
                            tint = Color.White,
                            testTag = "browser_close_btn"
                        )
                    }

                    // Bookmarks Row
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Text(
                                text = "Quick Bookmarks:",
                                fontSize = 11.sp,
                                color = TvTextSecondary,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        items(quickBookmarks) { (label, url) ->
                            val isSelected = currentUrl == url
                            TvBrowserChip(
                                label = label,
                                isSelected = isSelected,
                                onClick = {
                                    inputUrl = url
                                    currentUrl = url
                                    webViewInstance?.loadUrl(url)
                                }
                            )
                        }
                    }
                }
            }

            // Progress bar
            AnimatedVisibility(
                visible = isLoading && loadProgress in 1..99,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    progress = { loadProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = TvAccentCyan,
                    trackColor = Color.Transparent
                )
            }

            // WEBVIEW CONTAINER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White)
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                mediaPlaybackRequiresUserGesture = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                userAgentString = "Mozilla/5.0 (Linux; Android 14; TV Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            }

                            isFocusable = true
                            isFocusableInTouchMode = true

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    url?.let {
                                        currentUrl = it
                                        inputUrl = it
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                    pageTitle = view?.title ?: "Web Browser"
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                    }
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    loadProgress = newProgress
                                    if (newProgress >= 100) {
                                        isLoading = false
                                    }
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    super.onReceivedTitle(view, title)
                                    if (!title.isNullOrBlank()) {
                                        pageTitle = title
                                    }
                                }
                            }

                            loadUrl(currentUrl)
                            webViewInstance = this
                        }
                    },
                    update = { view ->
                        // Keep instance updated
                        webViewInstance = view
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.stopLoading()
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }
}

private fun formatUrl(raw: String): String {
    val trimmed = raw.trim()
    return when {
        trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
        else -> "https://www.google.com/search?q=" + java.net.URLEncoder.encode(trimmed, "UTF-8")
    }
}

@Composable
private fun TvBrowserIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    containerColor: Color = TvSurfaceElevated,
    tint: Color = TvTextPrimary,
    testTag: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = CircleShape,
        color = if (isFocused) TvSurfaceDark else containerColor.copy(alpha = if (enabled) 1f else 0.4f),
        border = BorderStroke(
            width = if (isFocused) 3.dp else 1.dp,
            color = if (isFocused) TvAccentCyan else Color.White.copy(alpha = 0.15f)
        ),
        modifier = Modifier
            .size(44.dp)
            .scale(if (isFocused) 1.1f else 1.0f)
            .clip(CircleShape)
            .clickable(enabled = enabled) { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(enabled = enabled)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isFocused) TvAccentCyan else if (enabled) tint else tint.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TvBrowserTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isFocused) TvSurfaceElevated else TvAccentCyan,
        border = BorderStroke(
            width = if (isFocused) 3.dp else 0.dp,
            color = if (isFocused) TvAccentCyan else Color.Transparent
        ),
        modifier = modifier
            .height(44.dp)
            .scale(if (isFocused) 1.05f else 1.0f)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = text,
                color = if (isFocused) Color.White else Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun TvBrowserChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = when {
            isFocused -> TvAccentCyan.copy(alpha = 0.35f)
            isSelected -> TvAccentCyan.copy(alpha = 0.2f)
            else -> TvSurfaceElevated
        },
        border = BorderStroke(
            width = if (isFocused) 3.dp else 1.dp,
            color = when {
                isFocused -> TvAccentCyan
                isSelected -> TvAccentCyan.copy(alpha = 0.8f)
                else -> Color.White.copy(alpha = 0.15f)
            }
        ),
        modifier = Modifier
            .scale(if (isFocused) 1.08f else 1.0f)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
            color = if (isFocused || isSelected) Color.White else TvTextPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

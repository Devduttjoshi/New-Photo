package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.PhotoItem
import kotlinx.coroutines.launch

@Composable
fun CrossfadeImageLayer(
    currentPhoto: PhotoItem?,
    previousPhoto: PhotoItem?,
    crossfadeSequence: Long,
    crossfadeDurationMs: Int,
    kenBurnsEnabled: Boolean,
    slideIntervalSeconds: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Alpha for incoming foreground photo
    val alphaAnim = remember { Animatable(1f) }

    // Ken Burns slow pan and zoom
    val zoomAnim = remember { Animatable(1.0f) }
    val panAnim = remember { Animatable(0f) }

    var displayedForeground by remember { mutableStateOf<PhotoItem?>(currentPhoto) }
    var displayedBackground by remember { mutableStateOf<PhotoItem?>(previousPhoto) }

    LaunchedEffect(crossfadeSequence) {
        if (currentPhoto != null) {
            displayedBackground = previousPhoto ?: displayedForeground
            displayedForeground = currentPhoto

            // Reset animations
            alphaAnim.snapTo(0f)
            zoomAnim.snapTo(1.0f)
            panAnim.snapTo(-12f)

            val totalIntervalMs = (slideIntervalSeconds * 1000).coerceAtLeast(2000)

            // 1. Ken Burns Zoom
            launch {
                if (kenBurnsEnabled) {
                    zoomAnim.animateTo(
                        targetValue = 1.07f,
                        animationSpec = tween(durationMillis = totalIntervalMs, easing = LinearEasing)
                    )
                }
            }

            // 2. Ken Burns Horizontal Drift
            launch {
                if (kenBurnsEnabled) {
                    panAnim.animateTo(
                        targetValue = 12f,
                        animationSpec = tween(durationMillis = totalIntervalMs, easing = LinearEasing)
                    )
                }
            }

            // 3. Smooth Crossfade Transition
            launch {
                alphaAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = crossfadeDurationMs.coerceIn(400, 5000),
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Outgoing background layer
        displayedBackground?.let { bgPhoto ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(bgPhoto.displayUri)
                    .crossfade(false)
                    .build(),
                contentDescription = bgPhoto.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Incoming foreground layer with animated crossfade alpha & motion
        displayedForeground?.let { fgPhoto ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(fgPhoto.displayUri)
                    .crossfade(false)
                    .build(),
                contentDescription = fgPhoto.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alphaAnim.value)
                    .graphicsLayer {
                        if (kenBurnsEnabled) {
                            scaleX = zoomAnim.value
                            scaleY = zoomAnim.value
                            translationX = panAnim.value
                        }
                    }
            )
        }
    }
}

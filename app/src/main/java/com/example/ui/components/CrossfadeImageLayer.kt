package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    framingMode: String = "AMBIENT_FIT", // "AMBIENT_FIT", "FILL_CROP", "BLACK_BARS"
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
            val oldFg = displayedForeground
            displayedBackground = previousPhoto ?: oldFg
            displayedForeground = currentPhoto

            // Reset animations
            alphaAnim.snapTo(0f)
            zoomAnim.snapTo(1.0f)
            panAnim.snapTo(-10f)

            val totalIntervalMs = (slideIntervalSeconds * 1000).coerceAtLeast(2000)

            // 1. Ken Burns Zoom
            launch {
                if (kenBurnsEnabled) {
                    zoomAnim.animateTo(
                        targetValue = 1.05f,
                        animationSpec = tween(durationMillis = totalIntervalMs, easing = LinearEasing)
                    )
                }
            }

            // 2. Ken Burns Horizontal Drift
            launch {
                if (kenBurnsEnabled) {
                    panAnim.animateTo(
                        targetValue = 10f,
                        animationSpec = tween(durationMillis = totalIntervalMs, easing = LinearEasing)
                    )
                }
            }

            // 3. Smooth Crossfade Transition
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = crossfadeDurationMs.coerceIn(400, 5000),
                    easing = FastOutSlowInEasing
                )
            )

            // At the end of transition, sync background to current foreground so no old photo gets stuck underneath
            displayedBackground = currentPhoto
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
            RenderPhotoComposite(
                photo = bgPhoto,
                framingMode = framingMode,
                kenBurnsEnabled = false,
                scale = 1.0f,
                translationX = 0f,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Incoming foreground layer with crossfade alpha & Ken Burns motion
        displayedForeground?.let { fgPhoto ->
            RenderPhotoComposite(
                photo = fgPhoto,
                framingMode = framingMode,
                kenBurnsEnabled = kenBurnsEnabled,
                scale = zoomAnim.value,
                translationX = panAnim.value,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alphaAnim.value)
            )
        }
    }
}

@Composable
private fun RenderPhotoComposite(
    photo: PhotoItem,
    framingMode: String,
    kenBurnsEnabled: Boolean,
    scale: Float,
    translationX: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (framingMode) {
            "AMBIENT_FIT" -> {
                // 1. Underneath: Ambient zoomed & darkened backdrop filling letterbox/pillarbox
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photo.displayUri)
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.25f
                            scaleY = 1.25f
                            alpha = 0.35f
                        }
                )

                // Soft dark gradient overlay over ambient backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )

                // 2. Foreground: 100% full uncropped image centered with subtle Ken Burns drift
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photo.displayUri)
                        .crossfade(false)
                        .build(),
                    contentDescription = photo.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (kenBurnsEnabled) {
                                scaleX = scale
                                scaleY = scale
                                this.translationX = translationX
                            }
                        }
                        .shadow(16.dp, RoundedCornerShape(4.dp))
                )
            }

            "FILL_CROP" -> {
                // Edge-to-edge crop
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photo.displayUri)
                        .crossfade(false)
                        .build(),
                    contentDescription = photo.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (kenBurnsEnabled) {
                                scaleX = scale
                                scaleY = scale
                                this.translationX = translationX
                            }
                        }
                )
            }

            else -> {
                // "BLACK_BARS" (Clean Fit with pure black border)
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photo.displayUri)
                        .crossfade(false)
                        .build(),
                    contentDescription = photo.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (kenBurnsEnabled) {
                                scaleX = scale
                                scaleY = scale
                                this.translationX = translationX
                            }
                        }
                )
            }
        }
    }
}

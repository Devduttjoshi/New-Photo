package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.PhotoItem
import com.example.ui.theme.TvAccentCyan
import kotlin.math.absoluteValue

@Composable
fun CoverFlowPlaybackView(
    photos: List<PhotoItem>,
    currentIndex: Int,
    coverFlowAngle: Float = 45f,
    coverFlowSpacing: Float = 0.5f,
    onSelectPhoto: (Int) -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTimeline: () -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeIndex by remember(currentIndex) { mutableStateOf(currentIndex) }
    val context = LocalContext.current

    val morphAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        morphAnim.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black,
                        Color(0xFF111118),
                        Color.Black
                    )
                )
            )
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (keyEvent.key) {
                        Key.DirectionRight, Key.PageDown, Key.MediaNext -> {
                            if (activeIndex < photos.size - 1) {
                                activeIndex++
                                onSelectPhoto(activeIndex)
                            }
                            true
                        }
                        Key.DirectionLeft, Key.PageUp, Key.MediaPrevious -> {
                            if (activeIndex > 0) {
                                activeIndex--
                                onSelectPhoto(activeIndex)
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            onOpenSettings()
                            true
                        }
                        Key.DirectionDown -> {
                            onOpenTimeline()
                            true
                        }
                        Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                            onTogglePlayPause()
                            true
                        }
                        Key.Escape, Key.Back -> {
                            onClose()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .pointerInput(photos) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        if (dragAmount < -30 && activeIndex < photos.size - 1) {
                            activeIndex++
                            onSelectPhoto(activeIndex)
                        } else if (dragAmount > 30 && activeIndex > 0) {
                            activeIndex--
                            onSelectPhoto(activeIndex)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .scale(0.7f + 0.3f * morphAnim.value)
                .graphicsLayer {
                    alpha = morphAnim.value
                },
            contentAlignment = Alignment.Center
        ) {
            val itemWidth = 320.dp
            val itemHeight = 440.dp

            photos.forEachIndexed { index, photo ->
                val distance = index - activeIndex
                val absDistance = distance.absoluteValue

                if (absDistance <= 3) {
                    val rotation = when {
                        distance < 0 -> coverFlowAngle
                        distance > 0 -> -coverFlowAngle
                        else -> 0f
                    }
                    val xOffset = distance * (itemWidth.value * (0.35f + coverFlowSpacing * 0.2f))
                    val scale = 1.0f - (absDistance * 0.15f)

                    Box(
                        modifier = Modifier
                            .size(itemWidth, itemHeight)
                            .graphicsLayer {
                                translationX = xOffset * density
                                rotationY = rotation
                                cameraDistance = 14f * density
                                scaleX = scale
                                scaleY = scale
                                alpha = if (absDistance == 0) 1.0f else (0.8f / (absDistance + 1))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.DarkGray,
                            shadowElevation = if (absDistance == 0) 24.dp else 4.dp,
                            border = if (absDistance == 0) BorderStroke(2.dp, TvAccentCyan) else BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(photo.uri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = photo.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = if (absDistance == 0) 0.1f else 0.6f)
                                                )
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

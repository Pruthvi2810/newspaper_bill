package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    // 0-400 ms: Sunrise backdrop fade-in + logo subtle scale-up
    val backdropAlpha = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.92f) }

    // 400-900 ms: Brand title, tagline fade-in + orange underline draw left-to-right
    val brandAlpha = remember { Animatable(0f) }
    val brandOffsetY = remember { Animatable(14f) }
    val underlineProgress = remember { Animatable(0f) }

    // 900-1400 ms: Delivery person, city, service highlights & footer reveal
    val lowerAlpha = remember { Animatable(0f) }
    val lowerOffsetY = remember { Animatable(24f) }

    LaunchedEffect(Unit) {
        // Stage 1: 0 - 400 ms
        launch {
            backdropAlpha.animateTo(1f, animationSpec = tween(400, easing = LinearOutSlowInEasing))
        }
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(380, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
        }

        // Stage 2: 400 - 900 ms
        delay(400L)
        launch {
            brandAlpha.animateTo(1f, animationSpec = tween(420, easing = FastOutSlowInEasing))
        }
        launch {
            brandOffsetY.animateTo(0f, animationSpec = tween(420, easing = FastOutSlowInEasing))
        }
        launch {
            delay(100L)
            underlineProgress.animateTo(1f, animationSpec = tween(380, easing = FastOutSlowInEasing))
        }

        // Stage 3: 900 - 1400 ms
        delay(500L)
        launch {
            lowerAlpha.animateTo(1f, animationSpec = tween(480, easing = FastOutSlowInEasing))
        }
        launch {
            lowerOffsetY.animateTo(0f, animationSpec = tween(480, easing = FastOutSlowInEasing))
        }

        // Stage 4: 1400 - 1800 ms hold, then transition smoothly to Login
        delay(900L)
        onFinished()
    }

    val splashPainter = painterResource(id = R.drawable.splash_launch_bg)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Soft sunrise gradient base that fades in during 0-400 ms
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = backdropAlpha.value }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFD8E6F8),
                            Color(0xFFFDFBF7),
                            Color(0xFFFFF1D6),
                            Color(0xFF0D2B6B)
                        )
                    )
                )
        )

        // Band 1 (0%..29%): Top sky + Globe & Newspaper logo with subtle scale-up (0-400 ms)
        Image(
            painter = splashPainter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = logoAlpha.value
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                }
                .drawWithContent {
                    clipRect(
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = size.height * 0.295f
                    ) {
                        this@drawWithContent.drawContent()
                    }
                }
        )

        // Band 2 (29%..49%): "Pruthvi News Agency" + "News at Your Doorstep..." (400-900 ms)
        Image(
            painter = splashPainter,
            contentDescription = "Pruthvi News Agency - News at Your Doorstep",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = brandAlpha.value
                    translationY = brandOffsetY.value
                }
                .drawWithContent {
                    clipRect(
                        left = 0f,
                        top = size.height * 0.29f,
                        right = size.width,
                        bottom = size.height * 0.495f
                    ) {
                        this@drawWithContent.drawContent()
                    }
                }
        )

        // Quick left-to-right orange underline accent during 400-900 ms
        if (underlineProgress.value > 0f && lowerAlpha.value < 0.99f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val startX = size.width * 0.41f
                val endX = size.width * 0.78f
                val currentEndX = startX + (endX - startX) * underlineProgress.value
                val baseY = size.height * 0.485f
                val path = Path().apply {
                    moveTo(startX, baseY)
                    quadraticTo(
                        (startX + currentEndX) * 0.5f,
                        baseY - 12f * underlineProgress.value,
                        currentEndX,
                        baseY - 16f * underlineProgress.value
                    )
                }
                drawPath(
                    path = path,
                    color = Color(0xFFF38218).copy(alpha = (1f - lowerAlpha.value * 0.6f).coerceIn(0f, 1f)),
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
            }
        }

        // Band 3 (49%..100%): Delivery person, sunrise city, service highlights & footer (900-1400 ms)
        Image(
            painter = splashPainter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = lowerAlpha.value
                    translationY = lowerOffsetY.value
                }
                .drawWithContent {
                    clipRect(
                        left = 0f,
                        top = size.height * 0.49f,
                        right = size.width,
                        bottom = size.height
                    ) {
                        this@drawWithContent.drawContent()
                    }
                }
        )
    }
}

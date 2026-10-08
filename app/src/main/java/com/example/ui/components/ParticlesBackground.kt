package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PureBlack
import kotlin.math.sin
import kotlin.random.Random

private enum class ParticleShape {
    CIRCLE,
    GLOW_ORB,
    DIAMOND,
    CROSS,
    STAR_POINT
}

private data class AliveParticle(
    var x: Float,
    var y: Float,
    val speedX: Float,
    val speedY: Float,
    val size: Float,
    val baseAlpha: Float,
    val shape: ParticleShape,
    val rotationSpeed: Float,
    val wobbleSpeed: Float,
    val wobbleOffset: Float,
    var rotation: Float = 0f
)

@Composable
fun ParticlesBackground(
    modifier: Modifier = Modifier,
    particleCount: Int = 50,
    content: @Composable () -> Unit = {}
) {
    val particles = remember {
        val rand = Random(1337)
        List(particleCount) { index ->
            val shape = when (index % 5) {
                0 -> ParticleShape.DIAMOND
                1 -> ParticleShape.CROSS
                2 -> ParticleShape.GLOW_ORB
                3 -> ParticleShape.STAR_POINT
                else -> ParticleShape.CIRCLE
            }
            AliveParticle(
                x = rand.nextFloat(),
                y = rand.nextFloat(),
                speedX = (rand.nextFloat() - 0.5f) * 0.04f,
                speedY = -(rand.nextFloat() * 0.05f + 0.025f), // continuous gentle upward drift
                size = rand.nextFloat() * 3.5f + 2f,
                baseAlpha = rand.nextFloat() * 0.35f + 0.12f,
                shape = shape,
                rotationSpeed = (rand.nextFloat() - 0.5f) * 35f,
                wobbleSpeed = rand.nextFloat() * 2f + 1f,
                wobbleOffset = rand.nextFloat() * 6.28f,
                rotation = rand.nextFloat() * 360f
            )
        }
    }

    // Frame animation driver
    val timeState = remember { mutableFloatStateOf(0f) }
    val touchPoint = remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(Unit) {
        var lastNano = 0L
        while (true) {
            withFrameNanos { nano ->
                if (lastNano != 0L) {
                    val dt = ((nano - lastNano) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
                    timeState.floatValue += dt

                    for (p in particles) {
                        // Apply movement
                        p.x += p.speedX * dt
                        p.y += p.speedY * dt
                        p.rotation += p.rotationSpeed * dt

                        // Subtle horizontal sine drift
                        val wobble = sin(timeState.floatValue * p.wobbleSpeed + p.wobbleOffset) * 0.015f * dt
                        p.x += wobble

                        // Wrap edges smoothly
                        if (p.y < -0.05f) {
                            p.y = 1.05f
                            p.x = Random.nextFloat()
                        }
                        if (p.y > 1.05f) {
                            p.y = -0.05f
                        }
                        if (p.x < -0.05f) {
                            p.x = 1.05f
                        }
                        if (p.x > 1.05f) {
                            p.x = -0.05f
                        }
                    }
                }
                lastNano = nano
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PureBlack,
                        DarkBackground,
                        Color(0xFF0D0D0D),
                        PureBlack
                    )
                )
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> touchPoint.value = offset },
                    onDragEnd = { touchPoint.value = null },
                    onDragCancel = { touchPoint.value = null },
                    onDrag = { change, _ -> touchPoint.value = change.position }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        touchPoint.value = offset
                        tryAwaitRelease()
                        touchPoint.value = null
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // CRITICAL: Read state inside draw scope to trigger continuous 60fps/120fps recomposition/draw!
            val currentTime = timeState.floatValue
            val currentTouch = touchPoint.value

            val width = size.width
            val height = size.height

            // Interactive reaction to touch
            if (currentTouch != null && width > 0 && height > 0) {
                val touchNormX = currentTouch.x / width
                val touchNormY = currentTouch.y / height
                for (p in particles) {
                    val dx = p.x - touchNormX
                    val dy = p.y - touchNormY
                    val distSq = dx * dx + dy * dy
                    if (distSq < 0.04f && distSq > 0.0001f) {
                        val force = 0.0008f / distSq
                        p.x += (dx * force).coerceIn(-0.02f, 0.02f)
                        p.y += (dy * force).coerceIn(-0.02f, 0.02f)
                    }
                }
            }

            // Draw connecting constellation lines
            val maxDistancePx = 140f
            val maxDistSq = maxDistancePx * maxDistancePx
            for (i in particles.indices) {
                val p1 = particles[i]
                val pos1 = Offset(p1.x * width, p1.y * height)

                for (j in (i + 1) until particles.size) {
                    val p2 = particles[j]
                    val pos2 = Offset(p2.x * width, p2.y * height)
                    val dx = pos1.x - pos2.x
                    val dy = pos1.y - pos2.y
                    val distSq = dx * dx + dy * dy

                    if (distSq < maxDistSq) {
                        val dist = kotlin.math.sqrt(distSq)
                        val lineAlpha = (1f - dist / maxDistancePx) * 0.12f
                        drawLine(
                            color = Color.White.copy(alpha = lineAlpha),
                            start = pos1,
                            end = pos2,
                            strokeWidth = 1f
                        )
                    }
                }
            }

            // Draw individual particles
            for (p in particles) {
                val center = Offset(p.x * width, p.y * height)
                // Gentle pulsing alpha
                val pulse = (sin(currentTime * p.wobbleSpeed + p.wobbleOffset) * 0.15f)
                val alpha = (p.baseAlpha + pulse).coerceIn(0.05f, 0.85f)

                when (p.shape) {
                    ParticleShape.CIRCLE -> {
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = p.size,
                            center = center
                        )
                    }
                    ParticleShape.GLOW_ORB -> {
                        // Outer soft glow
                        drawCircle(
                            color = Color.White.copy(alpha = alpha * 0.28f),
                            radius = p.size * 2.8f,
                            center = center
                        )
                        // Inner solid core
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = p.size * 0.9f,
                            center = center
                        )
                    }
                    ParticleShape.DIAMOND -> {
                        rotate(degrees = p.rotation, pivot = center) {
                            val half = p.size * 1.1f
                            drawRect(
                                color = Color.White.copy(alpha = alpha),
                                topLeft = Offset(center.x - half, center.y - half),
                                size = Size(half * 2f, half * 2f)
                            )
                        }
                    }
                    ParticleShape.CROSS -> {
                        rotate(degrees = p.rotation, pivot = center) {
                            val arm = p.size * 1.5f
                            drawLine(
                                color = Color.White.copy(alpha = alpha),
                                start = Offset(center.x - arm, center.y),
                                end = Offset(center.x + arm, center.y),
                                strokeWidth = 1.3f
                            )
                            drawLine(
                                color = Color.White.copy(alpha = alpha),
                                start = Offset(center.x, center.y - arm),
                                end = Offset(center.x, center.y + arm),
                                strokeWidth = 1.3f
                            )
                        }
                    }
                    ParticleShape.STAR_POINT -> {
                        rotate(degrees = p.rotation * 0.5f, pivot = center) {
                            val r = p.size * 1.3f
                            drawLine(
                                color = Color(0xFFDDDDDD).copy(alpha = alpha),
                                start = Offset(center.x - r, center.y),
                                end = Offset(center.x + r, center.y),
                                strokeWidth = 1.1f
                            )
                            drawLine(
                                color = Color(0xFFDDDDDD).copy(alpha = alpha),
                                start = Offset(center.x, center.y - r),
                                end = Offset(center.x, center.y + r),
                                strokeWidth = 1.1f
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = alpha),
                                radius = p.size * 0.5f,
                                center = center
                            )
                        }
                    }
                }
            }
        }

        content()
    }
}

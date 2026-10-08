package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PureBlack
import kotlin.random.Random

private enum class ParticleShape {
    CIRCLE,
    DIAMOND,
    CROSS,
    GLOW_POINT
}

private data class Particle(
    var x: Float,
    var y: Float,
    val speedX: Float,
    val speedY: Float,
    val size: Float,
    val baseAlpha: Float,
    val shape: ParticleShape,
    val rotationSpeed: Float,
    var rotation: Float = 0f
)

@Composable
fun ParticlesBackground(
    modifier: Modifier = Modifier,
    particleCount: Int = 38,
    content: @Composable () -> Unit = {}
) {
    val particles = remember {
        val rand = Random(42)
        List(particleCount) {
            val shape = when (it % 5) {
                0 -> ParticleShape.DIAMOND
                1 -> ParticleShape.CROSS
                2 -> ParticleShape.GLOW_POINT
                else -> ParticleShape.CIRCLE
            }
            Particle(
                x = rand.nextFloat(),
                y = rand.nextFloat(),
                speedX = (rand.nextFloat() - 0.5f) * 0.015f,
                speedY = -(rand.nextFloat() * 0.035f + 0.01f),
                size = rand.nextFloat() * 4f + 2f,
                baseAlpha = rand.nextFloat() * 0.28f + 0.08f,
                shape = shape,
                rotationSpeed = (rand.nextFloat() - 0.5f) * 1.5f
            )
        }
    }

    val tickState = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastTime = 0L
        while (true) {
            withFrameNanos { time ->
                if (lastTime != 0L) {
                    val dt = ((time - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
                    for (p in particles) {
                        p.x += p.speedX * dt
                        p.y += p.speedY * dt
                        p.rotation += p.rotationSpeed * dt * 45f

                        if (p.y < -0.05f) {
                            p.y = 1.05f
                            p.x = Random.nextFloat()
                        }
                        if (p.x < -0.05f) p.x = 1.05f
                        if (p.x > 1.05f) p.x = -0.05f
                    }
                    tickState.floatValue += dt
                }
                lastTime = time
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
                        Color(0xFF0F0F0F),
                        PureBlack
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Connect nearby particles with subtle faint lines (depth mesh)
            for (i in particles.indices) {
                val p1 = particles[i]
                val pos1 = Offset(p1.x * width, p1.y * height)

                for (j in (i + 1) until particles.size) {
                    val p2 = particles[j]
                    val pos2 = Offset(p2.x * width, p2.y * height)
                    val dx = pos1.x - pos2.x
                    val dy = pos1.y - pos2.y
                    val distSq = dx * dx + dy * dy
                    val maxDist = 180f
                    if (distSq < maxDist * maxDist) {
                        val dist = kotlin.math.sqrt(distSq)
                        val lineAlpha = (1f - dist / maxDist) * 0.07f
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
                val alpha = p.baseAlpha

                when (p.shape) {
                    ParticleShape.CIRCLE -> {
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = p.size,
                            center = center
                        )
                    }
                    ParticleShape.GLOW_POINT -> {
                        drawCircle(
                            color = Color.White.copy(alpha = alpha * 0.4f),
                            radius = p.size * 2.5f,
                            center = center
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = p.size * 0.9f,
                            center = center
                        )
                    }
                    ParticleShape.DIAMOND -> {
                        rotate(degrees = 45f + p.rotation, pivot = center) {
                            drawRect(
                                color = Color.White.copy(alpha = alpha),
                                topLeft = Offset(center.x - p.size, center.y - p.size),
                                size = Size(p.size * 2f, p.size * 2f)
                            )
                        }
                    }
                    ParticleShape.CROSS -> {
                        rotate(degrees = p.rotation, pivot = center) {
                            val arm = p.size * 1.6f
                            drawLine(
                                color = Color.White.copy(alpha = alpha),
                                start = Offset(center.x - arm, center.y),
                                end = Offset(center.x + arm, center.y),
                                strokeWidth = 1.2f
                            )
                            drawLine(
                                color = Color.White.copy(alpha = alpha),
                                start = Offset(center.x, center.y - arm),
                                end = Offset(center.x, center.y + arm),
                                strokeWidth = 1.2f
                            )
                        }
                    }
                }
            }
        }

        content()
    }
}

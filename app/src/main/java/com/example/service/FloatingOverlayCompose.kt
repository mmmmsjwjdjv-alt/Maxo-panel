package com.example.service

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Luxury3DSwitch
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import kotlin.math.sin
import kotlin.random.Random

private data class PanelParticle(
    var x: Float,
    var y: Float,
    val speedX: Float,
    val speedY: Float,
    val size: Float,
    val alpha: Float,
    val wobbleSpeed: Float
)

@Composable
fun FloatingOverlayUi(
    userKey: String,
    fps: Int,
    temperature: String,
    isPanelExpanded: Boolean,
    onExpandPanel: () -> Unit,
    onCollapsePanel: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
    onCloseService: () -> Unit
) {
    if (!isPanelExpanded) {
        // COMPACT SLEEK FLOATING DOLLAR (42dp x 42dp)
        CompactFloatingDollar(
            onTap = onExpandPanel,
            onDragDelta = onDragDelta
        )
    } else {
        // EXPANDED 3D GLASS PANEL WITH ATTACHED EMBLEM
        ExpandedFloatingPanel(
            userKey = userKey,
            fps = fps,
            temperature = temperature,
            onClose = onCollapsePanel,
            onDragDelta = onDragDelta
        )
    }
}

@Composable
fun CompactFloatingDollar(
    onTap: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit
) {
    var totalDragDist by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .size(42.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { totalDragDist = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragDist += kotlin.math.abs(dragAmount.x) + kotlin.math.abs(dragAmount.y)
                        onDragDelta(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = {
                        if (totalDragDist < 8f) {
                            onTap()
                        }
                    }
                )
            }
            .clickable { onTap() }
            .shadow(10.dp, CircleShape, spotColor = Color.White)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF222222),
                        Color(0xFF0F0F0F),
                        PureBlack
                    )
                )
            )
            .border(
                width = 1.6.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFF666666),
                        Color(0xFFFFFFFF)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_dollar_button),
            contentDescription = "Open MAXO Panel",
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
fun ExpandedFloatingPanel(
    userKey: String,
    fps: Int,
    temperature: String,
    onClose: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit
) {
    // Switch states
    var aimBot by remember { mutableStateOf(false) }
    var aimLock by remember { mutableStateOf(false) }
    var boostAim by remember { mutableStateOf(false) }
    var speedMobile by remember { mutableStateOf(false) }

    // Particles simulation inside the panel
    val panelParticles = remember {
        val rand = Random(99)
        List(28) {
            PanelParticle(
                x = rand.nextFloat(),
                y = rand.nextFloat(),
                speedX = (rand.nextFloat() - 0.5f) * 0.05f,
                speedY = -(rand.nextFloat() * 0.06f + 0.02f),
                size = rand.nextFloat() * 2.8f + 1.2f,
                alpha = rand.nextFloat() * 0.4f + 0.15f,
                wobbleSpeed = rand.nextFloat() * 3f + 1f
            )
        }
    }

    val particleTime = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastNano = 0L
        while (true) {
            withFrameNanos { nano ->
                if (lastNano != 0L) {
                    val dt = ((nano - lastNano) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
                    particleTime.floatValue += dt

                    for (p in panelParticles) {
                        p.x += p.speedX * dt
                        p.y += p.speedY * dt

                        if (p.y < -0.05f) {
                            p.y = 1.05f
                            p.x = Random.nextFloat()
                        }
                        if (p.x < -0.05f) p.x = 1.05f
                        if (p.x > 1.05f) p.x = -0.05f
                    }
                }
                lastNano = nano
            }
        }
    }

    Box(
        modifier = Modifier
            .width(285.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // MAIN GLASS CARD (offset downwards so the emblem sits seamlessly attached on top)
        Box(
            modifier = Modifier
                .padding(top = 26.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(22.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF2161616),
                            Color(0xF80E0E0E),
                            Color(0xFA070707)
                        )
                    )
                )
                .border(
                    width = 1.3.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x88FFFFFF),
                            Color(0x22FFFFFF),
                            Color(0x44FFFFFF)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            // Live animated particles behind the switches
            Canvas(modifier = Modifier.fillMaxSize()) {
                val t = particleTime.floatValue
                val w = size.width
                val h = size.height

                for (p in panelParticles) {
                    val cx = (p.x + sin(t * p.wobbleSpeed) * 0.02f) * w
                    val cy = p.y * h
                    drawCircle(
                        color = Color.White.copy(alpha = p.alpha),
                        radius = p.size,
                        center = Offset(cx, cy)
                    )
                }
            }

            // Card Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Top Header Row with title & close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "MAXO CONTROL",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 1.5.sp,
                            color = PureWhite
                        )
                        Text(
                            text = "USER: ${if (userKey.length > 10) userKey.take(10) + "..." else userKey.ifBlank { "ACTIVE" }}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            color = Color(0xFFB0B0B0)
                        )
                    }

                    // Close Button [X]
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222222))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // TELEMETRY HUD BAR (Real FPS & Real Temperature)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x99111111))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // FPS Indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = Color(0xFFEEEEEE),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$fps FPS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = PureWhite
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(12.dp)
                            .background(Color(0x33FFFFFF))
                    )

                    // Temperature Indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeviceThermostat,
                            contentDescription = null,
                            tint = Color(0xFFEEEEEE),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = temperature,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = PureWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4 ULTRA-LUXURY 3D SWITCHES
                LuxurySwitchItem(
                    title = "AIM BOT",
                    checked = aimBot,
                    onCheckedChange = { aimBot = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                LuxurySwitchItem(
                    title = "AIM LOCK",
                    checked = aimLock,
                    onCheckedChange = { aimLock = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                LuxurySwitchItem(
                    title = "BOOST AIM",
                    checked = boostAim,
                    onCheckedChange = { boostAim = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                LuxurySwitchItem(
                    title = "SPEED MOBILE",
                    checked = speedMobile,
                    onCheckedChange = { speedMobile = it }
                )

                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // TRANSPARENT CHROME "M" EMBLEM (ATTACHED EXACTLY ATOP THE PANEL)
        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(12.dp, CircleShape, spotColor = Color.White)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xEE1C1C1C),
                            Color(0xFF090909)
                        )
                    )
                )
                .border(
                    width = 1.8.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFF777777),
                            Color(0xFFFFFFFF)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_maxo_emblem_transparent),
                contentDescription = "MAXO Emblem",
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

@Composable
fun LuxurySwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (checked) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x442C2C2C),
                            Color(0x221A1A1A)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x33141414),
                            Color(0x220D0D0D)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (checked) Color(0x4DFFFFFF) else Color(0x1AFFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (checked) PureWhite else Color(0xFF444444))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = if (checked) PureWhite else Color(0xFFC0C0C0)
            )
        }

        Luxury3DSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

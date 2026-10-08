package com.example.service

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.components.MaxoSwitch
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PureWhite
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private data class TinyParticle(
    var x: Float,
    var y: Float,
    val speedX: Float,
    val speedY: Float,
    val size: Float,
    val alpha: Float,
    val wobble: Float
)

@Composable
fun FloatingOverlayUi(
    userKey: String,
    fps: Int,
    temperature: String,
    isPanelExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit
) {
    if (!isPanelExpanded) {
        // COLLAPSED: PURE CIRCULAR DOLLAR ICON (52dp) - NO FAINT CUBE OR EXTRA BORDERS
        Box(
            modifier = Modifier
                .size(52.dp)
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDrag += abs(dragAmount.x) + abs(dragAmount.y)
                            onDragDelta(dragAmount.x, dragAmount.y)
                        },
                        onDragEnd = {
                            if (totalDrag < 10f) {
                                onToggleExpand()
                            }
                        }
                    )
                }
                .clip(CircleShape)
                .clickable { onToggleExpand() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_dollar_button),
                contentDescription = "MAXO Dollar",
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // EXPANDED: DOLLAR BUTTON AT TOP-LEFT ATTACHED TO THE COMPACT MAXO PANEL
        Column(
            modifier = Modifier
                .width(245.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.x, dragAmount.y)
                    }
                }
        ) {
            // TOP-LEFT DOLLAR BUTTON (Tap to collapse back)
            Box(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable { onToggleExpand() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_dollar_button),
                    contentDescription = "Close Menu",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // COMPACT SLEEK FROSTED GLASS CARD (NOT ELONGATED)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xF2141414),
                                Color(0xF80C0C0C),
                                Color(0xFA070707)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        color = CardBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                // Background live micro-particles inside the card
                PanelParticlesBackground()

                // Card content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    // Header: Title + Telemetry (FPS & Temp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MAXO",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            letterSpacing = 2.sp,
                            color = PureWhite
                        )

                        // Real FPS & Real Temperature in a compact pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E1E1E))
                                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$fps FPS",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF888888))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = temperature,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = PureWhite
                            )
                        }
                    }

                    // User Line
                    val cleanKey = if (userKey.length > 12) userKey.take(12) + "..." else userKey.ifBlank { "ACTIVE" }
                    Text(
                        text = "USER: $cleanKey",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFAAAAAA),
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x1FFFFFFF))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4 SWITCHES - USING EXACT CLEAN MaxoSwitch FROM LOGIN
                    PanelSwitchRow(title = "AIM BOT")
                    Spacer(modifier = Modifier.height(6.dp))

                    PanelSwitchRow(title = "AIM LOCK")
                    Spacer(modifier = Modifier.height(6.dp))

                    PanelSwitchRow(title = "BOOST AIM")
                    Spacer(modifier = Modifier.height(6.dp))

                    PanelSwitchRow(title = "SPEED MOBILE")
                }
            }
        }
    }
}

@Composable
private fun PanelSwitchRow(
    title: String
) {
    var isChecked by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x80181818))
            .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.8.sp,
            color = PureWhite
        )

        // Exact clean MaxoSwitch from login screen (thick, rounded, B&W, no dots, no halos)
        MaxoSwitch(
            checked = isChecked,
            onCheckedChange = { isChecked = it },
            switchWidth = 44.dp,
            switchHeight = 24.dp
        )
    }
}

@Composable
private fun PanelParticlesBackground() {
    val particles = remember {
        val rand = Random(42)
        List(22) {
            TinyParticle(
                x = rand.nextFloat(),
                y = rand.nextFloat(),
                speedX = (rand.nextFloat() - 0.5f) * 0.03f,
                speedY = -(rand.nextFloat() * 0.04f + 0.015f),
                size = rand.nextFloat() * 2f + 1f,
                alpha = rand.nextFloat() * 0.35f + 0.1f,
                wobble = rand.nextFloat() * 3f + 1f
            )
        }
    }

    val tick = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastNano = 0L
        while (true) {
            withFrameNanos { nano ->
                if (lastNano != 0L) {
                    val dt = ((nano - lastNano) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
                    tick.floatValue += dt

                    for (p in particles) {
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

    Canvas(modifier = Modifier.fillMaxSize()) {
        val t = tick.floatValue
        val w = size.width
        val h = size.height

        for (p in particles) {
            val cx = (p.x + sin(t * p.wobble) * 0.015f) * w
            val cy = p.y * h
            drawCircle(
                color = Color.White.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(cx, cy)
            )
        }
    }
}

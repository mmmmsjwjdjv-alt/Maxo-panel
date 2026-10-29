package com.example.service

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.MaxoSwitch
import com.example.ui.components.bouncyClick
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private data class MixedMicroParticle(
    var x: Float,
    var y: Float,
    val speedX: Float,
    val speedY: Float,
    val size: Float,
    val baseAlpha: Float,
    val isDiamond: Boolean,
    val rotationSpeed: Float,
    var rotation: Float = 0f
)

@Composable
fun FloatingOverlayUi(
    userKey: String,
    fps: Int,
    temperature: String,
    isPanelExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
    // Combat switches
    aimBot: Boolean,
    onAimBotChange: (Boolean) -> Unit,
    aimLock: Boolean,
    onAimLockChange: (Boolean) -> Unit,
    boostAim: Boolean,
    onBoostAimChange: (Boolean) -> Unit,
    // Boost switches
    speedMobile: Boolean,
    onSpeedMobileChange: (Boolean) -> Unit,
    ultraFps: Boolean,
    onUltraFpsChange: (Boolean) -> Unit,
    touchSensitivity: Boolean,
    onTouchSensitivityChange: (Boolean) -> Unit
) {
    if (!isPanelExpanded) {
        // COLLAPSED: LUXURY 3D DOLLAR WITH AMBIENT BREATHING AURA & PARTICLES
        CollapsedFloatingDollar(
            onToggleExpand = onToggleExpand,
            onDragDelta = onDragDelta
        )
    } else {
        // EXPANDED: MULTI-TAB LUXURY CONTROL SUITE
        ExpandedControlSuite(
            userKey = userKey,
            fps = fps,
            temperature = temperature,
            onToggleExpand = onToggleExpand,
            onDragDelta = onDragDelta,
            aimBot = aimBot,
            onAimBotChange = onAimBotChange,
            aimLock = aimLock,
            onAimLockChange = onAimLockChange,
            boostAim = boostAim,
            onBoostAimChange = onBoostAimChange,
            speedMobile = speedMobile,
            onSpeedMobileChange = onSpeedMobileChange,
            ultraFps = ultraFps,
            onUltraFpsChange = onUltraFpsChange,
            touchSensitivity = touchSensitivity,
            onTouchSensitivityChange = onTouchSensitivityChange
        )
    }
}

@Composable
private fun CollapsedFloatingDollar(
    onToggleExpand: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    Box(
        modifier = Modifier
            .size(58.dp)
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
            .bouncyClick(scaleDown = 0.88f) { onToggleExpand() },
        contentAlignment = Alignment.Center
    ) {
        // Ambient Glowing Halo Ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = haloAlpha),
                        Color.White.copy(alpha = haloAlpha * 0.3f),
                        Color.Transparent
                    )
                ),
                radius = (size.minDimension / 2f) * pulseScale
            )
        }

        // Inner 3D Dollar Button
        Image(
            painter = painterResource(id = R.drawable.ic_dollar_button),
            contentDescription = "MAXO Dollar",
            modifier = Modifier.size(50.dp)
        )
    }
}

@Composable
private fun ExpandedControlSuite(
    userKey: String,
    fps: Int,
    temperature: String,
    onToggleExpand: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
    aimBot: Boolean,
    onAimBotChange: (Boolean) -> Unit,
    aimLock: Boolean,
    onAimLockChange: (Boolean) -> Unit,
    boostAim: Boolean,
    onBoostAimChange: (Boolean) -> Unit,
    speedMobile: Boolean,
    onSpeedMobileChange: (Boolean) -> Unit,
    ultraFps: Boolean,
    onUltraFpsChange: (Boolean) -> Unit,
    touchSensitivity: Boolean,
    onTouchSensitivityChange: (Boolean) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: COMBAT, 1: BOOST, 2: HUD

    Column(
        modifier = Modifier
            .width(260.dp)
            .wrapContentHeight()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        // TOP-LEFT DOLLAR BUTTON (Tap to collapse)
        Box(
            modifier = Modifier
                .padding(bottom = 6.dp)
                .size(46.dp)
                .bouncyClick(scaleDown = 0.88f) { onToggleExpand() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_dollar_button),
                contentDescription = "Close Menu",
                modifier = Modifier.fillMaxSize()
            )
        }

        // MAIN GLASS CARD (COMPACT HEIGHT, LUXURY BEVEL, NO OVERFLOW)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF5181818),
                            Color(0xF90E0E0E),
                            Color(0xFC080808)
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x80FFFFFF),
                            Color(0x20FFFFFF),
                            Color(0x40FFFFFF)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            // Live multi-layer cosmic stardust background
            SuiteParticlesBackground()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 14.dp, vertical = 13.dp)
            ) {
                // Header: Logo + Live Telemetry Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_launcher_foreground_image),
                            contentDescription = "Red John",
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Column {
                            Text(
                                text = "RED JOHN",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.5.sp,
                                letterSpacing = 2.sp,
                                color = PureWhite
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00FF66))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ACTIVE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00FF66)
                                )
                            }
                        }
                    }

                    // Real-Time Hardware Capsule
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF202020))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$fps FPS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF888888))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = temperature,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = PureWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SEGMENTED LUXURY TABS (COMBAT / BOOST / HUD)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF141414))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TabButton(
                        title = "COMBAT",
                        isSelected = selectedTab == 0,
                        modifier = Modifier.weight(1f)
                    ) { selectedTab = 0 }

                    TabButton(
                        title = "BOOST",
                        isSelected = selectedTab == 1,
                        modifier = Modifier.weight(1f)
                    ) { selectedTab = 1 }

                    TabButton(
                        title = "HUD",
                        isSelected = selectedTab == 2,
                        modifier = Modifier.weight(1f)
                    ) { selectedTab = 2 }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // TAB CONTENT WITH ANIMATED ROWS
                when (selectedTab) {
                    0 -> {
                        // COMBAT SECTION
                        SuiteSwitchRow(
                            title = "AIM BOT",
                            icon = Icons.Default.CenterFocusStrong,
                            checked = aimBot,
                            onCheckedChange = onAimBotChange
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        SuiteSwitchRow(
                            title = "AIM LOCK",
                            icon = Icons.Default.Lock,
                            checked = aimLock,
                            onCheckedChange = onAimLockChange
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        SuiteSwitchRow(
                            title = "BOOST AIM",
                            icon = Icons.Default.RocketLaunch,
                            checked = boostAim,
                            onCheckedChange = onBoostAimChange
                        )
                    }
                    1 -> {
                        // BOOST SECTION
                        SuiteSwitchRow(
                            title = "SPEED MOBILE",
                            icon = Icons.Default.Bolt,
                            checked = speedMobile,
                            onCheckedChange = onSpeedMobileChange
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        SuiteSwitchRow(
                            title = "ULTRA 120 FPS",
                            icon = Icons.Default.Speed,
                            checked = ultraFps,
                            onCheckedChange = onUltraFpsChange
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        SuiteSwitchRow(
                            title = "TOUCH RESPONSE",
                            icon = Icons.Default.TouchApp,
                            checked = touchSensitivity,
                            onCheckedChange = onTouchSensitivityChange
                        )
                    }
                    2 -> {
                        // HUD & TELEMETRY SECTION
                        HudTelemetryPane(fps = fps, temperature = temperature, userKey = userKey)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) PureWhite else Color.Transparent,
        animationSpec = tween(180),
        label = "tabBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) PureBlack else Color(0xFF999999),
        animationSpec = tween(180),
        label = "tabText"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .bouncyClick(scaleDown = 0.94f) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            color = textColor
        )
    }
}

@Composable
private fun SuiteSwitchRow(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val rowBgColor by animateColorAsState(
        targetValue = if (checked) Color(0x33333333) else Color(0x60161616),
        animationSpec = tween(durationMillis = 200),
        label = "rowBg"
    )

    val rowBorderColor by animateColorAsState(
        targetValue = if (checked) Color(0x80FFFFFF) else Color(0x18FFFFFF),
        animationSpec = tween(durationMillis = 200),
        label = "rowBorder"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(rowBgColor)
            .border(1.dp, rowBorderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) PureWhite else Color(0xFFAAAAAA),
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                letterSpacing = 0.6.sp,
                color = if (checked) PureWhite else Color(0xFFCCCCCC)
            )
        }

        MaxoSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            switchWidth = 44.dp,
            switchHeight = 22.dp
        )
    }
}

@Composable
private fun HudTelemetryPane(
    fps: Int,
    temperature: String,
    userKey: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x50121212))
            .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        // License Key
        val cleanKey = if (userKey.length > 14) userKey.take(14) + "..." else userKey.ifBlank { "AUTHORIZED" }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "LICENSE",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Color(0xFF888888)
            )
            Text(
                text = cleanKey,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = PureWhite
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Live FPS Gauge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FPS REFRESH",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Color(0xFF888888)
            )
            Text(
                text = "$fps / 120 MAX",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = PureWhite
            )
        }
        val fpsFraction = (fps / 120f).coerceIn(0.1f, 1f)
        LinearProgressIndicator(
            progress = { fpsFraction },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = PureWhite,
            trackColor = Color(0xFF282828),
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Device Thermal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "THERMAL STATE",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Color(0xFF888888)
            )
            Text(
                text = "$temperature [OPTIMAL]",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = Color(0xFF00FF66)
            )
        }
    }
}

@Composable
private fun BoxScope.SuiteParticlesBackground() {
    val particles = remember {
        val rand = Random(55)
        List(22) { idx ->
            MixedMicroParticle(
                x = rand.nextFloat(),
                y = rand.nextFloat(),
                speedX = (rand.nextFloat() - 0.5f) * 0.035f,
                speedY = -(rand.nextFloat() * 0.045f + 0.015f),
                size = rand.nextFloat() * 2.5f + 1f,
                baseAlpha = rand.nextFloat() * 0.35f + 0.1f,
                isDiamond = idx % 2 == 0,
                rotationSpeed = (rand.nextFloat() - 0.5f) * 40f,
                rotation = rand.nextFloat() * 360f
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
                        p.rotation += p.rotationSpeed * dt

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

    Canvas(modifier = Modifier.matchParentSize()) {
        val t = tick.floatValue
        val w = size.width
        val h = size.height

        // Constellation links
        val maxDist = 70f
        val maxDistSq = maxDist * maxDist
        for (i in particles.indices) {
            val p1 = particles[i]
            val x1 = p1.x * w
            val y1 = p1.y * h

            for (j in (i + 1) until particles.size) {
                val p2 = particles[j]
                val x2 = p2.x * w
                val y2 = p2.y * h
                val dx = x1 - x2
                val dy = y1 - y2
                val distSq = dx * dx + dy * dy
                if (distSq < maxDistSq) {
                    val alpha = (1f - sqrt(distSq) / maxDist) * 0.12f
                    drawLine(
                        color = Color.White.copy(alpha = alpha),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 0.8f
                    )
                }
            }
        }

        // Draw sparkling particles
        for (p in particles) {
            val cx = (p.x + sin(t * 2f + p.size) * 0.015f) * w
            val cy = p.y * h
            val pulse = sin(t * 3f + p.size) * 0.15f
            val alpha = (p.baseAlpha + pulse).coerceIn(0.05f, 0.9f)

            if (p.isDiamond) {
                rotate(degrees = p.rotation, pivot = Offset(cx, cy)) {
                    val s = p.size * 1.2f
                    drawRect(
                        color = Color.White.copy(alpha = alpha),
                        topLeft = Offset(cx - s, cy - s),
                        size = Size(s * 2f, s * 2f)
                    )
                }
            } else {
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = p.size,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

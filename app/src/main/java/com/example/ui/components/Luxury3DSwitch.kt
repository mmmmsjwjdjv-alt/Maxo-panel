package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

@Composable
fun Luxury3DSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val switchWidth = 54.dp
    val switchHeight = 28.dp
    val thumbSize = 22.dp
    val padding = 3.dp

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) switchWidth - thumbSize - padding else padding,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "thumb3dOffset"
    )

    val trackBorderColor by animateColorAsState(
        targetValue = if (checked) Color(0xFFFFFFFF) else Color(0x38FFFFFF),
        animationSpec = tween(180),
        label = "trackBorder"
    )

    val trackGradient = if (checked) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFE2E2E2),
                Color(0xFFC8C8C8)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A1A1A),
                Color(0xFF101010),
                Color(0xFF090909)
            )
        )
    }

    val thumbGradient = if (checked) {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF111111),
                Color(0xFF050505)
            )
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFEAEAEA),
                Color(0xFFAAAAAA)
            )
        )
    }

    val glowAlpha by animateColorAsState(
        targetValue = if (checked) Color(0x66FFFFFF) else Color.Transparent,
        animationSpec = tween(180),
        label = "glowColor"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(switchWidth, switchHeight)
            .drawBehind {
                if (checked) {
                    drawCircle(
                        color = glowAlpha,
                        radius = size.height * 0.9f
                    )
                }
            }
            .clip(RoundedCornerShape(16.dp))
            .background(trackGradient)
            .border(
                width = 1.3.dp,
                color = trackBorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White),
                enabled = enabled,
                role = Role.Switch
            ) {
                try {
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    vibrator?.let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            it.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            it.vibrate(25)
                        }
                    }
                } catch (_: Exception) {}
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // 3D Metallic Thumb
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .shadow(
                    elevation = if (checked) 6.dp else 3.dp,
                    shape = CircleShape,
                    spotColor = if (checked) PureBlack else Color.Black
                )
                .clip(CircleShape)
                .background(thumbGradient)
                .border(
                    width = 1.dp,
                    color = if (checked) Color(0x88000000) else Color(0x99FFFFFF),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Specular metallic center dot
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(
                        if (checked) Color(0x55FFFFFF) else Color(0x44000000)
                    )
            )
        }
    }
}

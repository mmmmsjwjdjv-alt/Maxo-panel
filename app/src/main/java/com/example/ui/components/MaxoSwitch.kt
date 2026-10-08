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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

@Composable
fun MaxoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    switchWidth: Dp = 50.dp,
    switchHeight: Dp = 26.dp
) {
    val context = LocalContext.current
    val padding = 3.dp
    val thumbSize = switchHeight - (padding * 2)

    // Bouncy Spring Animation for visible, tactile sliding
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) switchWidth - thumbSize - padding else padding,
        animationSpec = spring(
            dampingRatio = 0.65f, // Bouncy spring feel
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "thumbOffset"
    )

    val trackBgColor by animateColorAsState(
        targetValue = if (checked) PureWhite else PureBlack,
        animationSpec = tween(durationMillis = 220),
        label = "trackBgColor"
    )

    val trackBorderColor by animateColorAsState(
        targetValue = if (checked) PureWhite else CardBorder,
        animationSpec = tween(durationMillis = 220),
        label = "trackBorderColor"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) PureBlack else PureWhite,
        animationSpec = tween(durationMillis = 220),
        label = "thumbColor"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(switchWidth, switchHeight)
            .clip(RoundedCornerShape(switchHeight / 2))
            .background(trackBgColor)
            .border(
                width = 1.3.dp,
                color = trackBorderColor,
                shape = RoundedCornerShape(switchHeight / 2)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.Gray),
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
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .shadow(elevation = 3.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

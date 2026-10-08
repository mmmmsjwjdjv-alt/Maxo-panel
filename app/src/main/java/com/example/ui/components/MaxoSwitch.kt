package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

@Composable
fun MaxoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val switchWidth = 52.dp
    val switchHeight = 28.dp
    val thumbSize = 22.dp
    val padding = 3.dp

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) switchWidth - thumbSize - padding else padding,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "thumbOffset"
    )

    val trackBgColor by animateColorAsState(
        targetValue = if (checked) PureWhite else PureBlack,
        animationSpec = tween(durationMillis = 200),
        label = "trackBgColor"
    )

    val trackBorderColor by animateColorAsState(
        targetValue = if (checked) PureWhite else CardBorder,
        animationSpec = tween(durationMillis = 200),
        label = "trackBorderColor"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) PureBlack else PureWhite,
        animationSpec = tween(durationMillis = 200),
        label = "thumbColor"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(switchWidth, switchHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(trackBgColor)
            .border(
                width = 1.5.dp,
                color = trackBorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.Gray),
                enabled = enabled,
                role = Role.Switch
            ) {
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

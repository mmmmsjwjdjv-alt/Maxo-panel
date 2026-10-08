package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 8.dp,
    borderAlpha: Float = 0.22f,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = borderAlpha),
            Color.White.copy(alpha = borderAlpha * 0.4f),
            Color.White.copy(alpha = borderAlpha * 0.15f)
        )
    )

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xEE181818),
            Color(0xF5101010),
            Color(0xFA0B0B0B)
        )
    )

    Surface(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, spotColor = Color.Black)
            .border(
                border = BorderStroke(1.dp, borderBrush),
                shape = shape
            )
            .clip(shape),
        shape = shape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush)
        ) {
            content()
        }
    }
}

package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MaxoDarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    primaryContainer = GrayDark,
    onPrimaryContainer = PureWhite,
    secondary = GrayLight,
    onSecondary = PureBlack,
    secondaryContainer = GraySubtle,
    onSecondaryContainer = PureWhite,
    background = DarkBackground,
    onBackground = PureWhite,
    surface = CardBackground,
    onSurface = PureWhite,
    surfaceVariant = GraySubtle,
    onSurfaceVariant = GrayLight,
    outline = CardBorder,
    outlineVariant = CardBorderSubtle
)

@Composable
fun MaxoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MaxoDarkColorScheme,
        typography = Typography,
        content = content
    )
}

// Keep alias for backwards compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaxoTheme(content = content)
}

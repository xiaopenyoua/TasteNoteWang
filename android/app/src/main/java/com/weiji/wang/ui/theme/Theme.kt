package com.weiji.wang.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = SystemOrange,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE8CC),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF3A1D00),
    secondary = SystemGray,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = SystemGray6,
    onSecondaryContainer = LabelLight,
    tertiary = SystemGreen,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    background = BackgroundLight,
    onBackground = LabelLight,
    surface = SurfaceLight,
    onSurface = LabelLight,
    surfaceVariant = SystemGray5,
    onSurfaceVariant = SecondaryLabelLight,
    outline = SystemGray3,
    outlineVariant = SystemGray5,
    error = SystemRed,
    onError = androidx.compose.ui.graphics.Color.White,
    errorContainer = androidx.compose.ui.graphics.Color(0xFFFFE5E3),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFF3B0A08),
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFFF9F0A),
    onPrimary = androidx.compose.ui.graphics.Color.Black,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF5A3200),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFFFD9A8),
    secondary = SystemGray2,
    onSecondary = androidx.compose.ui.graphics.Color.Black,
    secondaryContainer = SurfaceElevatedDark,
    onSecondaryContainer = LabelDark,
    tertiary = androidx.compose.ui.graphics.Color(0xFF30D158),
    onTertiary = androidx.compose.ui.graphics.Color.Black,
    background = BackgroundDark,
    onBackground = LabelDark,
    surface = SurfaceDark,
    onSurface = LabelDark,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = SecondaryLabelDark,
    outline = SeparatorDark,
    outlineVariant = SurfaceElevatedDark,
    error = androidx.compose.ui.graphics.Color(0xFFFF453A),
    onError = androidx.compose.ui.graphics.Color.Black,
    errorContainer = androidx.compose.ui.graphics.Color(0xFF5A1A17),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD7),
)

private val WeijiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun WeijiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = WeijiTypography,
        shapes = WeijiShapes,
        content = content,
    )
}

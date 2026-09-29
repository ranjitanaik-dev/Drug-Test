package com.ncb.drugtestcompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PremiumDualToneColorScheme = lightColorScheme(
    primary = PrimaryNavy,
    onPrimary = Color.White,
    primaryContainer = CardTintBlue,
    onPrimaryContainer = PrimaryNavy,
    secondary = TealAccent,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = PrimaryNavy,
    tertiary = ElectricBlue,
    onTertiary = Color.White,
    background = CanvasBackground,
    onBackground = TextCharcoal,
    surface = SurfaceWhite,
    onSurface = TextCharcoal,
    surfaceVariant = CardTintGray,
    onSurfaceVariant = TextMuted,
    outline = BorderGray,
    outlineVariant = BorderBlueLight
)

@Composable
fun DrugTestCompanionTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PremiumDualToneColorScheme,
        typography = Typography,
        content = content
    )
}

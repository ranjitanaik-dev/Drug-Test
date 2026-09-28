package com.ncb.drugtestcompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FieldOfficerLightColorScheme = lightColorScheme(
    primary = DeepTrustNavy,
    onPrimary = Color.White,
    primaryContainer = ContainerTintBlue,
    onPrimaryContainer = DeepTrustNavy,
    secondary = PrimaryNavyBlue,
    onSecondary = Color.White,
    secondaryContainer = ContainerTintGray,
    onSecondaryContainer = DeepTrustNavy,
    background = CanvasBackground,
    onBackground = DeepTrustNavy,
    surface = SurfaceWhite,
    onSurface = DeepTrustNavy,
    surfaceVariant = ContainerTintBlue,
    onSurfaceVariant = Color(0xFF475569),
    outline = HairlineBorder,
    outlineVariant = Color(0xFFCBD5E1)
)

@Composable
fun DrugTestCompanionTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FieldOfficerLightColorScheme,
        typography = Typography,
        content = content
    )
}

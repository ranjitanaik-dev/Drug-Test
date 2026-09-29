package com.ncb.drugtestcompanion.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Global Premium Forensic Visual System (Dark-and-Light Dual-Tone Identity)

// 1. Deep Navy & Midnight Blue Surfaces
val MidnightNavy = Color(0xFF06101E)       // Deepest background midnight navy
val DeepNavy = Color(0xFF081426)           // Primary dark header & hero navy surface
val PrimaryNavy = Color(0xFF0B1B36)        // Rich dark navy surface
val PrimaryNavyLight = Color(0xFF1E3A5F)   // Soft navy blue secondary

// 2. Royal & Electric Blue Highlights
val RoyalBlue = Color(0xFF0A58CA)          // Royal blue accent
val ElectricBlue = Color(0xFF0284C7)       // Professional electric blue
val SkyCyan = Color(0xFF00B4D8)            // Cyan highlight

// 3. Professional Teal & Cyan Accents
val TealAccent = Color(0xFF00C9A7)         // Professional cyan-teal highlight
val TealContainer = Color(0xFFCCFBF1)       // Light teal container
val CyanHighlight = Color(0xFF0EA5E9)      // Cyan accent highlight

// 4. Light Surfaces & Background Canvas
val CanvasBackground = Color(0xFFF4F7FA)    // Soft cool-white / light blue-gray background canvas
val SurfaceWhite = Color(0xFFFFFFFF)        // Clean white card surface
val CardTintBlue = Color(0xFFEFF6FF)        // Very light blue-tinted card container
val CardTintGray = Color(0xFFF1F5F9)        // Cool gray card background

// 5. Typography & Text Colors
val TextCharcoal = Color(0xFF0F172A)        // Dark navy/charcoal for light background
val TextMuted = Color(0xFF64748B)           // Muted slate text for light background
val TextWhite = Color(0xFFFFFFFF)           // Crisp white for dark navy backgrounds
val TextWhiteMuted = Color(0xFF94A3B8)      // Muted light blue-gray for dark navy backgrounds

// 6. Accent & Status Indicators
val StatusGreen = Color(0xFF16A34A)         // Professional green
val StatusGreenContainer = Color(0xFFDCFCE7)// Soft green container
val StatusRed = Color(0xFFDC2626)           // Controlled red
val StatusRedContainer = Color(0xFFFEE2E2)  // Soft red container
val StatusAmber = Color(0xFFD97706)         // Warning amber
val StatusAmberContainer = Color(0xFFFEF3C7)// Soft amber container
val StatusInfo = Color(0xFF0284C7)          // Info blue
val StatusInfoContainer = Color(0xFFE0F2FE)  // Soft info container

// 7. Subtle Borders & Shadows
val BorderGray = Color(0xFFE2E8F0)          // Clean border gray
val BorderBlueLight = Color(0xFFDBEAFE)     // Subtle blue-tinted border

// 8. Premium Dual-Tone Brushes & Gradients
val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF0B1B36), Color(0xFF0A58CA), Color(0xFF00C9A7))
)

val ActionButtonGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF0284C7), Color(0xFF00C9A7))
)

val MidnightGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF06101E), Color(0xFF081426), Color(0xFF0B1B36))
)

val NavyCyanGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF081426), Color(0xFF0A58CA), Color(0xFF00B4D8))
)

val DeepBlueTealGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0B1B36), Color(0xFF00C9A7))
)

val HeroCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF081426), Color(0xFF1E3A5F))
)

// Soft Light Blue Mixed With White Background & Card Gradients
val SoftLightBlueBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFEBF3FF), // Soft light sky blue
        Color(0xFFF4F8FF), // Light blue-white transition
        Color(0xFFE2EDFF)  // Soft light blue tint base
    )
)

val LightBlueCardGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFFFFFF), // Crisp top light
        Color(0xFFEFF6FF)  // Light blue bottom gradient
    )
)

// Backwards compatibility aliases
val DeepTrustNavy = DeepNavy
val PrimaryNavyBlue = PrimaryNavy
val SecondaryBlue = PrimaryNavyLight
val HairlineBorder = BorderGray
val ContainerTintBlue = CardTintBlue
val ContainerTintGray = CardTintGray
val PositiveCrimson = StatusRed
val PositiveRoseContainer = StatusRedContainer
val NegativeEmerald = StatusGreen
val NegativeMintContainer = StatusGreenContainer
val InconclusiveAmber = StatusAmber
val InconclusiveCreamContainer = StatusAmberContainer
val SecurityTeal = TealAccent
val SecurityTealContainer = TealContainer

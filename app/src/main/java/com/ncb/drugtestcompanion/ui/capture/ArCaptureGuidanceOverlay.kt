package com.ncb.drugtestcompanion.ui.capture

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncb.drugtestcompanion.R
import com.ncb.drugtestcompanion.ui.theme.StatusGreen
import com.ncb.drugtestcompanion.ui.theme.StatusGreenContainer
import com.ncb.drugtestcompanion.ui.theme.StatusRed
import com.ncb.drugtestcompanion.ui.theme.StatusRedContainer

/**
 * Realistic Face-Recognition Style AR Capture Guidance Reticle Overlay
 * Displays dynamic target tracking corners, searching reticle when card is missing,
 * real-time accelerometer tilt telemetry, and green readiness verification.
 */
@Composable
fun ArCaptureGuidanceOverlay(
    guidanceInfo: ArGuidanceFrameInfo,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arScanningLine")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLinePosition"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 1. AR Dynamic Target Bounding Box & Face-Recognition Style Reticle Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val left = w * 0.12f
            val top = h * 0.22f
            val boxW = w * 0.76f
            val boxH = h * 0.50f
            val cornerLen = 28f
            val strokeW = 5f

            if (!guidanceInfo.cardDetected) {
                // CARD NOT DETECTED: Red/Amber Dashed Search Reticle
                val searchColor = Color(0xFFEF4444).copy(alpha = pulseAlpha)
                
                // Translucent red warning fill
                drawRoundRect(
                    color = Color(0xFFEF4444).copy(alpha = 0.08f),
                    topLeft = Offset(left, top),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(16f, 16f)
                )

                // Outer Dashed Searching Border
                drawRoundRect(
                    color = searchColor,
                    topLeft = Offset(left, top),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(16f, 16f),
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
                    )
                )

                // Corner Searching Brackets (Red)
                // Top-Left
                drawPath(Path().apply {
                    moveTo(left, top + cornerLen)
                    lineTo(left, top)
                    lineTo(left + cornerLen, top)
                }, color = searchColor, style = Stroke(width = strokeW))

                // Top-Right
                drawPath(Path().apply {
                    moveTo(left + boxW - cornerLen, top)
                    lineTo(left + boxW, top)
                    lineTo(left + boxW, top + cornerLen)
                }, color = searchColor, style = Stroke(width = strokeW))

                // Bottom-Left
                drawPath(Path().apply {
                    moveTo(left, top + boxH - cornerLen)
                    lineTo(left, top + boxH)
                    lineTo(left + cornerLen, top + boxH)
                }, color = searchColor, style = Stroke(width = strokeW))

                // Bottom-Right
                drawPath(Path().apply {
                    moveTo(left + boxW - cornerLen, top + boxH)
                    lineTo(left + boxW, top + boxH)
                    lineTo(left + boxW, top + boxH - cornerLen)
                }, color = searchColor, style = Stroke(width = strokeW))

            } else {
                // CARD DETECTED: Active Tracking Reticle (Cyan when adjusting, Green when ready)
                val strokeColor = if (guidanceInfo.isReadyToCapture) Color(0xFF34D399) else Color(0xFF00E5FF)
                val shadowFillColor = if (guidanceInfo.isReadyToCapture) Color(0xFF34D399).copy(alpha = 0.12f) else Color(0xFF00E5FF).copy(alpha = 0.06f)

                // Translucent Target Area
                drawRoundRect(
                    color = shadowFillColor,
                    topLeft = Offset(left, top),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(16f, 16f)
                )

                // Outer Solid Bounding Rect
                drawRoundRect(
                    color = strokeColor.copy(alpha = 0.6f),
                    topLeft = Offset(left, top),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(16f, 16f),
                    style = Stroke(width = 2f)
                )

                // Face-Recognition Style Corner Target Brackets
                // Top-Left
                drawPath(Path().apply {
                    moveTo(left, top + cornerLen)
                    lineTo(left, top)
                    lineTo(left + cornerLen, top)
                }, color = strokeColor, style = Stroke(width = strokeW))

                // Top-Right
                drawPath(Path().apply {
                    moveTo(left + boxW - cornerLen, top)
                    lineTo(left + boxW, top)
                    lineTo(left + boxW, top + cornerLen)
                }, color = strokeColor, style = Stroke(width = strokeW))

                // Bottom-Left
                drawPath(Path().apply {
                    moveTo(left, top + boxH - cornerLen)
                    lineTo(left, top + boxH)
                    lineTo(left + cornerLen, top + boxH)
                }, color = strokeColor, style = Stroke(width = strokeW))

                // Bottom-Right
                drawPath(Path().apply {
                    moveTo(left + boxW - cornerLen, top + boxH)
                    lineTo(left + boxW, top + boxH)
                    lineTo(left + boxW, top + boxH - cornerLen)
                }, color = strokeColor, style = Stroke(width = strokeW))

                // AR Scanning Laser Line
                val currentScanY = top + (boxH * scanLineY)
                drawLine(
                    color = strokeColor.copy(alpha = 0.75f),
                    start = Offset(left + 8f, currentScanY),
                    end = Offset(left + boxW - 8f, currentScanY),
                    strokeWidth = 3f
                )
            }
        }

        // 2. AR Status & Guidance Badges Overlay (Top Center)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main Readiness / Card Detection Status Chip
            Surface(
                shape = RoundedCornerShape(50),
                color = when {
                    guidanceInfo.isReadyToCapture -> StatusGreenContainer
                    !guidanceInfo.cardDetected -> StatusRedContainer
                    else -> Color.Black.copy(alpha = 0.80f)
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        guidanceInfo.isReadyToCapture -> StatusGreen
                        !guidanceInfo.cardDetected -> StatusRed
                        else -> Color.White.copy(alpha = 0.3f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    guidanceInfo.isReadyToCapture -> StatusGreen
                                    !guidanceInfo.cardDetected -> StatusRed
                                    else -> Color(0xFFFFB74D)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            guidanceInfo.isReadyToCapture -> "✓ " + stringResource(R.string.ar_status_ready_to_capture)
                            !guidanceInfo.cardDetected -> "⚠️ " + stringResource(R.string.ar_status_card_not_detected)
                            else -> stringResource(R.string.ar_status_improve_alignment)
                        },
                        color = when {
                            guidanceInfo.isReadyToCapture -> StatusGreen
                            !guidanceInfo.cardDetected -> StatusRed
                            else -> Color.White
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Real-Time Distance & Angle Telemetry Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distance Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F2942).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.ar_label_distance) + ": ",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                !guidanceInfo.cardDetected -> stringResource(R.string.ar_status_card_not_detected)
                                guidanceInfo.distanceStatus == DistanceStatus.TOO_FAR -> stringResource(R.string.ar_label_too_far) + " (↓)"
                                guidanceInfo.distanceStatus == DistanceStatus.TOO_CLOSE -> stringResource(R.string.ar_label_too_close) + " (↑)"
                                guidanceInfo.distanceStatus == DistanceStatus.GOOD_DISTANCE -> stringResource(R.string.ar_label_hold_position) + " (✓)"
                                else -> stringResource(R.string.ar_status_card_not_detected)
                            },
                            color = when {
                                !guidanceInfo.cardDetected -> Color(0xFFF87171)
                                guidanceInfo.distanceStatus == DistanceStatus.GOOD_DISTANCE -> Color(0xFF34D399)
                                else -> Color(0xFFFFB74D)
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Angle / Alignment Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F2942).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.ar_label_angle) + ": ",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                !guidanceInfo.cardDetected -> stringResource(R.string.ar_status_card_not_detected)
                                guidanceInfo.alignmentStatus == AlignmentStatus.GOOD_ALIGNMENT -> stringResource(R.string.ar_status_good_alignment) + " (✓)"
                                guidanceInfo.alignmentStatus == AlignmentStatus.NEED_STRAIGHTEN -> stringResource(R.string.ar_status_straighten_phone)
                                else -> stringResource(R.string.ar_status_align_phone)
                            },
                            color = when {
                                !guidanceInfo.cardDetected -> Color(0xFFF87171)
                                guidanceInfo.alignmentStatus == AlignmentStatus.GOOD_ALIGNMENT -> Color(0xFF34D399)
                                else -> Color(0xFFFFB74D)
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

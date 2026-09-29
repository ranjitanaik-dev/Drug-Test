package com.ncb.drugtestcompanion.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncb.drugtestcompanion.ui.theme.PrimaryNavy
import com.ncb.drugtestcompanion.ui.theme.StatusGreen
import com.ncb.drugtestcompanion.ui.theme.TealAccent

/**
 * Reusable NiRIKSH Field Testing Brand Logo
 * Matches the official inspiration header branding (Test Tube + Magnifying Glass + Checkmark)
 */
@Composable
fun TestTubeIcon(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // 1. Draw Test Tube Outline (Dark Navy)
        val tubeWidth = w * 0.32f
        val tubeHeight = h * 0.65f
        val tubeLeft = w * 0.28f
        val tubeTop = h * 0.12f

        // Lip at top
        drawRoundRect(
            color = PrimaryNavy,
            topLeft = Offset(tubeLeft - w * 0.04f, tubeTop),
            size = Size(tubeWidth + w * 0.08f, h * 0.08f),
            cornerRadius = CornerRadius(4f, 4f),
            style = Fill
        )

        // Tube body
        val tubePath = Path().apply {
            moveTo(tubeLeft, tubeTop + h * 0.06f)
            lineTo(tubeLeft, tubeTop + tubeHeight - tubeWidth * 0.5f)
            cubicTo(
                tubeLeft, tubeTop + tubeHeight,
                tubeLeft + tubeWidth, tubeTop + tubeHeight,
                tubeLeft + tubeWidth, tubeTop + tubeHeight - tubeWidth * 0.5f
            )
            lineTo(tubeLeft + tubeWidth, tubeTop + h * 0.06f)
            close()
        }

        drawPath(
            path = tubePath,
            color = PrimaryNavy,
            style = Stroke(width = 3.dp.toPx())
        )

        // Liquid inside tube
        val liquidPath = Path().apply {
            val liquidTop = tubeTop + tubeHeight * 0.45f
            moveTo(tubeLeft + 2.dp.toPx(), liquidTop)
            lineTo(tubeLeft + 2.dp.toPx(), tubeTop + tubeHeight - tubeWidth * 0.5f)
            cubicTo(
                tubeLeft + 2.dp.toPx(), tubeTop + tubeHeight - 2.dp.toPx(),
                tubeLeft + tubeWidth - 2.dp.toPx(), tubeTop + tubeHeight - 2.dp.toPx(),
                tubeLeft + tubeWidth - 2.dp.toPx(), tubeTop + tubeHeight - tubeWidth * 0.5f
            )
            lineTo(tubeLeft + tubeWidth - 2.dp.toPx(), liquidTop)
            close()
        }
        drawPath(path = liquidPath, color = Color(0xFF0284C7), style = Fill)

        // 2. Magnifying Glass Badge on Right Side
        val circleCenterX = w * 0.62f
        val circleCenterY = h * 0.52f
        val circleRadius = w * 0.24f

        drawCircle(
            color = Color.White,
            radius = circleRadius,
            center = Offset(circleCenterX, circleCenterY)
        )
        drawCircle(
            color = PrimaryNavy,
            radius = circleRadius,
            center = Offset(circleCenterX, circleCenterY),
            style = Stroke(width = 3.dp.toPx())
        )

        // Teal Checkmark inside badge circle
        val checkPath = Path().apply {
            moveTo(circleCenterX - circleRadius * 0.45f, circleCenterY)
            lineTo(circleCenterX - circleRadius * 0.1f, circleCenterY + circleRadius * 0.35f)
            lineTo(circleCenterX + circleRadius * 0.45f, circleCenterY - circleRadius * 0.35f)
        }
        drawPath(
            path = checkPath,
            color = StatusGreen,
            style = Stroke(width = 3.dp.toPx())
        )

        // Magnifying handle
        val handlePath = Path().apply {
            moveTo(circleCenterX + circleRadius * 0.7f, circleCenterY + circleRadius * 0.7f)
            lineTo(circleCenterX + circleRadius * 1.3f, circleCenterY + circleRadius * 1.3f)
        }
        drawPath(
            path = handlePath,
            color = PrimaryNavy,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
fun NirikshBrandHeader(
    modifier: Modifier = Modifier,
    iconSize: Dp = 56.dp,
    showSubtext: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TestTubeIcon(size = iconSize)
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "NiRIKSH",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryNavy,
                letterSpacing = 1.sp
            )
        }
        if (showSubtext) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "DIGITAL FIELD TESTING",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryNavy.copy(alpha = 0.85f),
                letterSpacing = 1.5.sp
            )
        }
    }
}

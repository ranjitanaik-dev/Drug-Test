package com.ncb.drugtestcompanion.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ShieldIcon(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = Color(0xFF0F2942)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val path = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w, h * 0.2f)
            lineTo(w, h * 0.55f)
            cubicTo(w, h * 0.85f, w * 0.5f, h, w * 0.5f, h)
            cubicTo(w * 0.5f, h, 0f, h * 0.85f, 0f, h * 0.55f)
            lineTo(0f, h * 0.2f)
            close()
        }

        drawPath(path = path, color = color, style = Fill)
    }
}

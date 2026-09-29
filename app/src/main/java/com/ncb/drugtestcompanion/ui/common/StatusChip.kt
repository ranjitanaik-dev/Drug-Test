package com.ncb.drugtestcompanion.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncb.drugtestcompanion.ui.theme.StatusAmber
import com.ncb.drugtestcompanion.ui.theme.StatusAmberContainer
import com.ncb.drugtestcompanion.ui.theme.StatusGreen
import com.ncb.drugtestcompanion.ui.theme.StatusGreenContainer
import com.ncb.drugtestcompanion.ui.theme.StatusRed
import com.ncb.drugtestcompanion.ui.theme.StatusRedContainer
import com.ncb.drugtestcompanion.ui.theme.TealAccent
import com.ncb.drugtestcompanion.ui.theme.TealContainer

/**
 * Compact pill-shaped status chip system.
 */
@Composable
fun StatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, dotColor, labelText) = when (status.uppercase()) {
        "POSITIVE" -> Quad(StatusRedContainer, StatusRed, StatusRed, getLocalizedResultCategory("POSITIVE"))
        "NEGATIVE" -> Quad(StatusGreenContainer, StatusGreen, StatusGreen, getLocalizedResultCategory("NEGATIVE"))
        "INCONCLUSIVE" -> Quad(StatusAmberContainer, StatusAmber, StatusAmber, getLocalizedResultCategory("INCONCLUSIVE"))
        "COMPLETED", "VERIFIED", "ACTIVE" -> Quad(StatusGreenContainer, StatusGreen, StatusGreen, status)
        "PROCESSING" -> Quad(TealContainer, TealAccent, TealAccent, status)
        else -> Quad(Color(0xFFF1F5F9), Color(0xFF64748B), Color(0xFF64748B), status)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = labelText,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

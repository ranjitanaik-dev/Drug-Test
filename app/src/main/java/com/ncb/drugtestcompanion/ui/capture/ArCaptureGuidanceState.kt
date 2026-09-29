package com.ncb.drugtestcompanion.ui.capture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

enum class DistanceStatus {
    NOT_DETECTED,
    TOO_FAR,
    TOO_CLOSE,
    GOOD_DISTANCE
}

enum class AlignmentStatus {
    NOT_DETECTED,
    POOR_ALIGNMENT,
    NEED_STRAIGHTEN,
    GOOD_ALIGNMENT
}

sealed class ArCaptureGuidanceState {
    data object ReferenceCardNotDetected : ArCaptureGuidanceState()
    data class TooFar(val relativeSize: Float) : ArCaptureGuidanceState()
    data class TooClose(val relativeSize: Float) : ArCaptureGuidanceState()
    data class PoorAlignment(val suggestion: String) : ArCaptureGuidanceState()
    data class AdjustPosition(val distance: DistanceStatus, val alignment: AlignmentStatus) : ArCaptureGuidanceState()
    data class Ready(
        val confidence: Float,
        val cardBounds: Rect? = null,
        val corners: List<Offset> = emptyList()
    ) : ArCaptureGuidanceState()
}

data class ArGuidanceFrameInfo(
    val cardDetected: Boolean = false,
    val distanceStatus: DistanceStatus = DistanceStatus.NOT_DETECTED,
    val alignmentStatus: AlignmentStatus = AlignmentStatus.NOT_DETECTED,
    val guidanceState: ArCaptureGuidanceState = ArCaptureGuidanceState.ReferenceCardNotDetected,
    val isReadyToCapture: Boolean = false,
    val cardCorners: List<Offset> = emptyList(),
    val estimatedAngleDegrees: Float = 0f,
    val relativeCardAreaRatio: Float = 0f
)

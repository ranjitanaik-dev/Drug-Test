package com.ncb.drugtestcompanion.ui.capture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.ncb.drugtestcompanion.cv.CardDetectionResult
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

class ArCaptureGuidanceAnalyzer {

    fun analyzeGuidance(
        detectionResult: CardDetectionResult,
        previewWidth: Float,
        previewHeight: Float
    ): ArGuidanceFrameInfo {
        if (previewWidth <= 0f || previewHeight <= 0f) {
            return ArGuidanceFrameInfo(
                cardDetected = false,
                guidanceState = ArCaptureGuidanceState.ReferenceCardNotDetected
            )
        }

        return when (detectionResult) {
            is CardDetectionResult.NotDetected, CardDetectionResult.FramingError -> {
                ArGuidanceFrameInfo(
                    cardDetected = false,
                    distanceStatus = DistanceStatus.NOT_DETECTED,
                    alignmentStatus = AlignmentStatus.NOT_DETECTED,
                    guidanceState = ArCaptureGuidanceState.ReferenceCardNotDetected,
                    isReadyToCapture = false
                )
            }

            is CardDetectionResult.Detected -> {
                val corners = detectionResult.corners.map { Offset(it.x, it.y) }
                if (corners.size < 4) {
                    return ArGuidanceFrameInfo(
                        cardDetected = false,
                        guidanceState = ArCaptureGuidanceState.ReferenceCardNotDetected
                    )
                }

                val tl = corners[0]
                val tr = corners[1]
                val br = corners[2]
                val bl = corners[3]

                val minX = minOf(tl.x, tr.x, br.x, bl.x)
                val maxX = maxOf(tl.x, tr.x, br.x, bl.x)
                val minY = minOf(tl.y, tr.y, br.y, bl.y)
                val maxY = maxOf(tl.y, tr.y, br.y, bl.y)

                val cardWidth = maxX - minX
                val cardHeight = maxY - minY
                val cardArea = cardWidth * cardHeight
                val frameArea = previewWidth * previewHeight
                val areaRatio = if (frameArea > 0) cardArea / frameArea else 0f

                val distanceStatus = when {
                    areaRatio < 0.10f -> DistanceStatus.TOO_FAR
                    areaRatio > 0.50f -> DistanceStatus.TOO_CLOSE
                    else -> DistanceStatus.GOOD_DISTANCE
                }

                val topEdge = hypot((tr.x - tl.x).toDouble(), (tr.y - tl.y).toDouble())
                val bottomEdge = hypot((br.x - bl.x).toDouble(), (br.y - bl.y).toDouble())
                val leftEdge = hypot((bl.x - tl.x).toDouble(), (bl.y - tl.y).toDouble())
                val rightEdge = hypot((br.x - tr.x).toDouble(), (br.y - tr.y).toDouble())

                val horizontalRatio = if (bottomEdge > 0) abs(topEdge - bottomEdge) / bottomEdge else 0.0
                val verticalRatio = if (rightEdge > 0) abs(leftEdge - rightEdge) / rightEdge else 0.0
                val totalSkew = (horizontalRatio + verticalRatio).toFloat()

                val deltaY = (tr.y - tl.y).toDouble()
                val deltaX = (tr.x - tl.x).toDouble()
                val angleRad = atan2(deltaY, deltaX)
                val angleDeg = Math.toDegrees(angleRad).toFloat()

                val alignmentStatus = when {
                    totalSkew > 0.25f || abs(angleDeg) > 18f -> AlignmentStatus.POOR_ALIGNMENT
                    totalSkew > 0.12f || abs(angleDeg) > 8f -> AlignmentStatus.NEED_STRAIGHTEN
                    else -> AlignmentStatus.GOOD_ALIGNMENT
                }

                val isReady = distanceStatus == DistanceStatus.GOOD_DISTANCE &&
                        alignmentStatus == AlignmentStatus.GOOD_ALIGNMENT

                val guidanceState = when {
                    distanceStatus == DistanceStatus.TOO_FAR -> ArCaptureGuidanceState.TooFar(areaRatio)
                    distanceStatus == DistanceStatus.TOO_CLOSE -> ArCaptureGuidanceState.TooClose(areaRatio)
                    alignmentStatus == AlignmentStatus.POOR_ALIGNMENT -> ArCaptureGuidanceState.PoorAlignment("Straighten phone")
                    alignmentStatus == AlignmentStatus.NEED_STRAIGHTEN -> ArCaptureGuidanceState.AdjustPosition(distanceStatus, alignmentStatus)
                    isReady -> ArCaptureGuidanceState.Ready(
                        confidence = detectionResult.confidence,
                        cardBounds = Rect(minX, minY, maxX, maxY),
                        corners = corners
                    )
                    else -> ArCaptureGuidanceState.AdjustPosition(distanceStatus, alignmentStatus)
                }

                ArGuidanceFrameInfo(
                    cardDetected = true,
                    distanceStatus = distanceStatus,
                    alignmentStatus = alignmentStatus,
                    guidanceState = guidanceState,
                    isReadyToCapture = isReady,
                    cardCorners = corners,
                    estimatedAngleDegrees = angleDeg,
                    relativeCardAreaRatio = areaRatio
                )
            }
        }
    }

    fun evaluateGuidanceWithSensors(
        cardDetected: Boolean,
        cardAreaRatio: Float,
        pitchDegrees: Float,
        rollDegrees: Float
    ): ArGuidanceFrameInfo {
        if (!cardDetected) {
            return ArGuidanceFrameInfo(
                cardDetected = false,
                distanceStatus = DistanceStatus.NOT_DETECTED,
                alignmentStatus = AlignmentStatus.NOT_DETECTED,
                guidanceState = ArCaptureGuidanceState.ReferenceCardNotDetected,
                isReadyToCapture = false
            )
        }

        val distanceStatus = when {
            cardAreaRatio < 0.12f -> DistanceStatus.TOO_FAR
            cardAreaRatio > 0.52f -> DistanceStatus.TOO_CLOSE
            else -> DistanceStatus.GOOD_DISTANCE
        }

        val tiltMagnitude = hypot(pitchDegrees.toDouble(), rollDegrees.toDouble()).toFloat()

        val alignmentStatus = when {
            tiltMagnitude > 22f -> AlignmentStatus.POOR_ALIGNMENT
            tiltMagnitude > 12f -> AlignmentStatus.NEED_STRAIGHTEN
            else -> AlignmentStatus.GOOD_ALIGNMENT
        }

        val isReady = distanceStatus == DistanceStatus.GOOD_DISTANCE &&
                alignmentStatus == AlignmentStatus.GOOD_ALIGNMENT

        val state = when {
            distanceStatus == DistanceStatus.TOO_FAR -> ArCaptureGuidanceState.TooFar(cardAreaRatio)
            distanceStatus == DistanceStatus.TOO_CLOSE -> ArCaptureGuidanceState.TooClose(cardAreaRatio)
            alignmentStatus == AlignmentStatus.POOR_ALIGNMENT -> ArCaptureGuidanceState.PoorAlignment("Straighten phone")
            alignmentStatus == AlignmentStatus.NEED_STRAIGHTEN -> ArCaptureGuidanceState.AdjustPosition(distanceStatus, alignmentStatus)
            isReady -> ArCaptureGuidanceState.Ready(confidence = 0.95f)
            else -> ArCaptureGuidanceState.AdjustPosition(distanceStatus, alignmentStatus)
        }

        return ArGuidanceFrameInfo(
            cardDetected = true,
            distanceStatus = distanceStatus,
            alignmentStatus = alignmentStatus,
            guidanceState = state,
            isReadyToCapture = isReady,
            estimatedAngleDegrees = tiltMagnitude,
            relativeCardAreaRatio = cardAreaRatio
        )
    }
}

package com.ncb.drugtestcompanion.domain.usecase

import com.ncb.drugtestcompanion.cv.ColorCalibrator
import com.ncb.drugtestcompanion.cv.RoiFeatureVector
import com.ncb.drugtestcompanion.cv.Thresholds
import com.ncb.drugtestcompanion.domain.model.ClassificationResult
import com.ncb.drugtestcompanion.domain.model.TestKitCentroids
import com.ncb.drugtestcompanion.domain.model.TestResultCategory
import javax.inject.Inject

/**
 * ClassifyResultUseCase performs deterministic classical colour-distance classification on an extracted
 * test-region feature vector against a kit profile's reference centroids in CIE L*a*b* space.
 */
class ClassifyResultUseCase @Inject constructor(
    private val colorCalibrator: ColorCalibrator
) {

    operator fun invoke(
        featureVector: RoiFeatureVector,
        centroids: TestKitCentroids
    ): ClassificationResult {
        val measuredLab = featureVector.averageLab

        // 1. Calculate CIE Delta E (1976) distances to Positive and Negative centroids
        val distPositive = colorCalibrator.calculateDeltaE(measuredLab, centroids.positiveCentroidLab)
        val distNegative = colorCalibrator.calculateDeltaE(measuredLab, centroids.negativeCentroidLab)

        // 2. Determine nearest centroid candidate and calculate distance margin
        val totalDist = (distPositive + distNegative).coerceAtLeast(0.0001)
        val (candidateCategory, winnerDist, loserDist, winningCentroidName) = if (distPositive < distNegative) {
            Tuple4(TestResultCategory.POSITIVE, distPositive, distNegative, "POSITIVE_CENTROID")
        } else {
            Tuple4(TestResultCategory.NEGATIVE, distNegative, distPositive, "NEGATIVE_CENTROID")
        }

        // 3. Compute relative confidence score (0.0f to 1.0f) based on margin ratio
        val marginRatio = (loserDist - winnerDist) / totalDist
        val rawConfidence = (0.5 + marginRatio * 0.5).toFloat().coerceIn(0.0f, 1.0f)

        // 4. Apply classification confidence cutoff threshold from Thresholds.kt
        val (finalCategory, statusMessage) = if (winnerDist > Thresholds.MAX_DELTA_E_DISTANCE_LIMIT ||
            rawConfidence < Thresholds.CLASSIFICATION_CONFIDENCE_THRESHOLD) {
            Pair(
                TestResultCategory.INCONCLUSIVE,
                "Classification confidence below threshold (${Thresholds.CLASSIFICATION_CONFIDENCE_THRESHOLD})"
            )
        } else {
            Pair(
                candidateCategory,
                "Deterministic classification completed successfully"
            )
        }

        // 5. Build component scores map for evidence payload logging
        val componentScores = mapOf(
            "deltaE_positive" to distPositive,
            "deltaE_negative" to distNegative,
            "margin_ratio" to marginRatio,
            "calculated_confidence" to rawConfidence.toDouble(),
            "average_lab_l" to measuredLab.l,
            "average_lab_a" to measuredLab.a,
            "average_lab_b" to measuredLab.b,
            "average_hsv_h" to featureVector.averageHsv.getOrElse(0) { 0f }.toDouble(),
            "average_hsv_s" to featureVector.averageHsv.getOrElse(1) { 0f }.toDouble(),
            "average_hsv_v" to featureVector.averageHsv.getOrElse(2) { 0f }.toDouble()
        )

        return ClassificationResult(
            result = finalCategory,
            confidence = rawConfidence,
            distance = winnerDist,
            targetCentroidName = winningCentroidName,
            componentScores = componentScores,
            statusMessage = statusMessage
        )
    }

    private data class Tuple4<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}

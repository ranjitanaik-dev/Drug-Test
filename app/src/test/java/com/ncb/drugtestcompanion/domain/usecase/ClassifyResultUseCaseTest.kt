package com.ncb.drugtestcompanion.domain.usecase

import com.ncb.drugtestcompanion.cv.ColorCalibrator
import com.ncb.drugtestcompanion.cv.LabColor
import com.ncb.drugtestcompanion.cv.RoiFeatureVector
import com.ncb.drugtestcompanion.domain.model.TestKitCentroids
import com.ncb.drugtestcompanion.domain.model.TestResultCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test for [ClassifyResultUseCase] using synthetic feature vector fixtures.
 *
 * NOTE: These test fixtures use synthetic demo centroids explicitly constructed for unit testing,
 * pending physical spectrophotometer measurements against real test-kit chemistry.
 */
class ClassifyResultUseCaseTest {

    private lateinit var colorCalibrator: ColorCalibrator
    private lateinit var classifyResultUseCase: ClassifyResultUseCase

    // Synthetic demo centroids for unit testing
    // Positive centroid (e.g. strong red reaction line): L=45.0, a=55.0, b=28.0
    // Negative centroid (e.g. clear/control line only): L=90.0, a=0.0, b=0.0
    private val demoCentroids = TestKitCentroids(
        kitVariantId = "VARIANT_A",
        positiveCentroidLab = LabColor(l = 45.0, a = 55.0, b = 28.0),
        negativeCentroidLab = LabColor(l = 90.0, a = 0.0, b = 0.0)
    )

    @Before
    fun setUp() {
        colorCalibrator = ColorCalibrator()
        classifyResultUseCase = ClassifyResultUseCase(colorCalibrator)
    }

    @Test
    fun `TEST 1 - Synthetic known-positive feature vector classifies as POSITIVE`() {
        // Feature vector close to positive centroid (L=44.0, a=54.0, b=27.0)
        val positiveFeatureVector = RoiFeatureVector(
            averageLab = LabColor(l = 44.0, a = 54.0, b = 27.0),
            averageHsv = floatArrayOf(0.0f, 0.75f, 0.70f)
        )

        val result = classifyResultUseCase(positiveFeatureVector, demoCentroids)

        assertEquals(TestResultCategory.POSITIVE, result.result)
        assertTrue(result.confidence > 0.60f)
        assertEquals("POSITIVE_CENTROID", result.targetCentroidName)
        assertTrue(result.distance < 10.0)
    }

    @Test
    fun `TEST 2 - Synthetic known-negative feature vector classifies as NEGATIVE`() {
        // Feature vector close to negative centroid (L=89.0, a=1.0, b=1.0)
        val negativeFeatureVector = RoiFeatureVector(
            averageLab = LabColor(l = 89.0, a = 1.0, b = 1.0),
            averageHsv = floatArrayOf(0.0f, 0.05f, 0.90f)
        )

        val result = classifyResultUseCase(negativeFeatureVector, demoCentroids)

        assertEquals(TestResultCategory.NEGATIVE, result.result)
        assertTrue(result.confidence > 0.60f)
        assertEquals("NEGATIVE_CENTROID", result.targetCentroidName)
        assertTrue(result.distance < 10.0)
    }

    @Test
    fun `TEST 3 - Ambiguous feature vector with low confidence classifies as INCONCLUSIVE`() {
        // Feature vector midpoint equidistant to both positive and negative centroids
        // Midpoint: L = (45 + 90)/2 = 67.5, a = (55 + 0)/2 = 27.5, b = (28 + 0)/2 = 14.0
        val ambiguousFeatureVector = RoiFeatureVector(
            averageLab = LabColor(l = 67.5, a = 27.5, b = 14.0),
            averageHsv = floatArrayOf(0.0f, 0.30f, 0.50f)
        )

        val result = classifyResultUseCase(ambiguousFeatureVector, demoCentroids)

        assertEquals(TestResultCategory.INCONCLUSIVE, result.result)
        assertTrue(result.statusMessage.contains("below threshold"))
    }

    @Test
    fun `TEST 4 - Component scores map contains all metrics required for evidence logging`() {
        val featureVector = RoiFeatureVector(
            averageLab = LabColor(l = 44.0, a = 54.0, b = 27.0),
            averageHsv = floatArrayOf(10.0f, 0.8f, 0.7f)
        )

        val result = classifyResultUseCase(featureVector, demoCentroids)

        val scores = result.componentScores
        assertNotNull(scores["deltaE_positive"])
        assertNotNull(scores["deltaE_negative"])
        assertNotNull(scores["margin_ratio"])
        assertNotNull(scores["calculated_confidence"])
        assertNotNull(scores["average_lab_l"])
        assertNotNull(scores["average_lab_a"])
        assertNotNull(scores["average_lab_b"])
        assertNotNull(scores["average_hsv_h"])
        assertNotNull(scores["average_hsv_s"])
        assertNotNull(scores["average_hsv_v"])
    }
}

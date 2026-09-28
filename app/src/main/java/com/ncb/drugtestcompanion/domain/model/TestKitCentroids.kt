package com.ncb.drugtestcompanion.domain.model

import com.ncb.drugtestcompanion.cv.LabColor

/**
 * Domain model representing target reference color centroids for a kit profile in CIE L*a*b* space.
 *
 * NOTE: Values marked as synthetic demo targets for unit testing until physical spectrophotometer
 * measurements against real test-kit chemistry are completed.
 */
data class TestKitCentroids(
    val kitVariantId: String,
    val positiveCentroidLab: LabColor,
    val negativeCentroidLab: LabColor
)

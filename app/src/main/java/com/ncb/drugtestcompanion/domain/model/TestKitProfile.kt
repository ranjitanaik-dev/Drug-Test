package com.ncb.drugtestcompanion.domain.model

/**
 * Domain model representing an offline test-kit profile.
 * Maps kit selection directly to its associated reference-card profile, ROI specification, and calibration centroids.
 */
data class TestKitProfile(
    val kitId: String,
    val displayName: String,
    val drugCategory: String,
    val kitType: String,
    val referenceCardProfileId: String,
    val roiSpec: RoiSpec,
    val centroids: TestKitCentroids
)

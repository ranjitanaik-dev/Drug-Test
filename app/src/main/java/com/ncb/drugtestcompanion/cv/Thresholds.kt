package com.ncb.drugtestcompanion.cv

object Thresholds {
    // TUNED USING REAL PHYSICAL CAPTURE DATA (Xerox card setup)
    const val LAPLACIAN_VARIANCE_THRESHOLD = 45.0f

    // PLACEHOLDER — MUST BE TUNED USING REAL CAPTURE DATA
    const val UNDEREXPOSURE_MEAN_THRESHOLD = 40.0f

    // PLACEHOLDER — MUST BE TUNED USING REAL CAPTURE DATA
    const val OVEREXPOSURE_MEAN_THRESHOLD = 220.0f

    // Minimum image dimension requirement
    const val MIN_RESOLUTION_WIDTH = 480
    const val MIN_RESOLUTION_HEIGHT = 480

    // Minimum card area ratio relative to full image (10%)
    const val MIN_CARD_AREA_RATIO = 0.10f

    // PLACEHOLDER — pending empirical validation (Phase 5 Classification Confidence Threshold)
    const val CLASSIFICATION_CONFIDENCE_THRESHOLD = 0.60f

    // PLACEHOLDER — pending empirical validation (Max Delta E Distance Limit)
    const val MAX_DELTA_E_DISTANCE_LIMIT = 80.0
}

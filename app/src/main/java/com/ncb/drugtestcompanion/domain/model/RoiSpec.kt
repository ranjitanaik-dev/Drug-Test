package com.ncb.drugtestcompanion.domain.model

/**
 * Domain specification representing a kit-specific region of interest (ROI) bounding box
 * in canonical 640x960 reference-card pixel coordinate space.
 */
data class RoiSpec(
    val xMin: Int,
    val yMin: Int,
    val width: Int,
    val height: Int,
    val description: String = "Test-strip result area"
) {
    val xMax: Int get() = xMin + width
    val yMax: Int get() = yMin + height
}

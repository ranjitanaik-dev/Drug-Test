package com.ncb.drugtestcompanion.domain.model

/**
 * Local offline configuration model representing a drug-test kit variant and its matching reference-card profile.
 */
data class ReferenceCardProfile(
    val variantId: String,
    val kitFormat: String,
    val cardId: String,
    val cardTitle: String,
    val arucoDictionary: String = "DICT_4X4_50",
    val markerIds: List<Int> = listOf(1, 2, 3, 4),
    val canonicalWidthPx: Int = 640,
    val canonicalHeightPx: Int = 960,
    val roiSpec: RoiSpec = RoiSpec(xMin = 140, yMin = 250, width = 360, height = 160),
    val patchRois: Map<String, List<Int>> = mapOf(
        "White" to listOf(160, 432, 115, 115),
        "Gray" to listOf(275, 432, 115, 115),
        "Black" to listOf(390, 432, 115, 115),
        "Red" to listOf(160, 605, 115, 115),
        "Green" to listOf(275, 605, 115, 115),
        "Blue" to listOf(390, 605, 115, 115)
    )
)

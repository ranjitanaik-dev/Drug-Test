package com.ncb.drugtestcompanion.domain.model

/**
 * Pure Kotlin domain model representing an evidence test record with digital signature and location address.
 */
data class TestRecord(
    val testId: String,
    val timestamp: Long,
    val kitId: String,
    val referenceCardProfileId: String,
    val result: String,
    val confidence: Float,
    val distance: Double,
    val imageSha256: String,
    val imagePath: String = "",
    val operatorId: String = "OP-LOCAL",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val locationStatus: String = "UNAVAILABLE",
    val signature: String = "",
    val signatureAlgorithm: String = "SHA256withRSA"
)

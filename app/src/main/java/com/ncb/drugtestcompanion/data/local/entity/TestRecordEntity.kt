package com.ncb.drugtestcompanion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ncb.drugtestcompanion.domain.model.TestRecord

@Entity(tableName = "test_records")
data class TestRecordEntity(
    @PrimaryKey val testId: String,
    val timestamp: Long,
    val kitId: String,
    val referenceCardProfileId: String,
    val result: String,
    val confidence: Float,
    val distance: Double,
    val imageSha256: String,
    val imagePath: String,
    val operatorId: String,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val locationStatus: String,
    val signature: String,
    val signatureAlgorithm: String
) {
    fun toDomain(): TestRecord = TestRecord(
        testId = testId,
        timestamp = timestamp,
        kitId = kitId,
        referenceCardProfileId = referenceCardProfileId,
        result = result,
        confidence = confidence,
        distance = distance,
        imageSha256 = imageSha256,
        imagePath = imagePath,
        operatorId = operatorId,
        latitude = latitude,
        longitude = longitude,
        address = address,
        locationStatus = locationStatus,
        signature = signature,
        signatureAlgorithm = signatureAlgorithm
    )

    companion object {
        fun fromDomain(domain: TestRecord): TestRecordEntity = TestRecordEntity(
            testId = domain.testId,
            timestamp = domain.timestamp,
            kitId = domain.kitId,
            referenceCardProfileId = domain.referenceCardProfileId,
            result = domain.result,
            confidence = domain.confidence,
            distance = domain.distance,
            imageSha256 = domain.imageSha256,
            imagePath = domain.imagePath,
            operatorId = domain.operatorId,
            latitude = domain.latitude,
            longitude = domain.longitude,
            address = domain.address,
            locationStatus = domain.locationStatus,
            signature = domain.signature,
            signatureAlgorithm = domain.signatureAlgorithm
        )
    }
}

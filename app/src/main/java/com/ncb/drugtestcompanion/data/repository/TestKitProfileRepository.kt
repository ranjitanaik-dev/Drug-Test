package com.ncb.drugtestcompanion.data.repository

import com.ncb.drugtestcompanion.cv.LabColor
import com.ncb.drugtestcompanion.domain.model.ReferenceCardProfile
import com.ncb.drugtestcompanion.domain.model.RoiSpec
import com.ncb.drugtestcompanion.domain.model.TestKitCentroids
import com.ncb.drugtestcompanion.domain.model.TestKitProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth repository resolving kitId -> TestKitProfile -> ReferenceCardProfile -> RoiSpec.
 */
@Singleton
class TestKitProfileRepository @Inject constructor(
    private val referenceCardProfileRepository: ReferenceCardProfileRepository
) {

    private val kitProfiles = listOf(
        TestKitProfile(
            kitId = "KIT_A",
            displayName = "5-Panel Cassette Kit",
            drugCategory = "Multi-Panel Urine Screen",
            kitType = "Cassette",
            referenceCardProfileId = "VARIANT_A",
            roiSpec = RoiSpec(
                xMin = 140,
                yMin = 250,
                width = 360,
                height = 160,
                description = "5-Panel Cassette Result Window (Upper Region)"
            ),
            centroids = TestKitCentroids(
                kitVariantId = "VARIANT_A",
                positiveCentroidLab = LabColor(l = 45.0, a = 55.0, b = 28.0),
                negativeCentroidLab = LabColor(l = 90.0, a = 0.0, b = 0.0)
            )
        ),
        TestKitProfile(
            kitId = "KIT_B",
            displayName = "10-Panel Cassette Kit",
            drugCategory = "Multi-Panel Urine Screen",
            kitType = "Cassette",
            referenceCardProfileId = "VARIANT_B",
            roiSpec = RoiSpec(
                xMin = 100,
                yMin = 240,
                width = 440,
                height = 180,
                description = "10-Panel Multi-Strip Window (Upper Region)"
            ),
            centroids = TestKitCentroids(
                kitVariantId = "VARIANT_B",
                positiveCentroidLab = LabColor(l = 45.0, a = 55.0, b = 28.0),
                negativeCentroidLab = LabColor(l = 90.0, a = 0.0, b = 0.0)
            )
        ),
        TestKitProfile(
            kitId = "KIT_C",
            displayName = "12-Panel Cassette Kit",
            drugCategory = "High-Density Urine Screen",
            kitType = "Cassette",
            referenceCardProfileId = "VARIANT_C",
            roiSpec = RoiSpec(
                xMin = 80,
                yMin = 230,
                width = 480,
                height = 195,
                description = "12-Panel Wide Strip Window (Upper Region)"
            ),
            centroids = TestKitCentroids(
                kitVariantId = "VARIANT_C",
                positiveCentroidLab = LabColor(l = 45.0, a = 55.0, b = 28.0),
                negativeCentroidLab = LabColor(l = 90.0, a = 0.0, b = 0.0)
            )
        ),
        TestKitProfile(
            kitId = "KIT_D",
            displayName = "Multi-Panel Urine Cup Kit",
            drugCategory = "Integrated Urine Cup Screen",
            kitType = "Cup",
            referenceCardProfileId = "VARIANT_D",
            roiSpec = RoiSpec(
                xMin = 120,
                yMin = 730,
                width = 400,
                height = 120,
                description = "Urine Cup Test Panel Window (Lower Region)"
            ),
            centroids = TestKitCentroids(
                kitVariantId = "VARIANT_D",
                positiveCentroidLab = LabColor(l = 45.0, a = 55.0, b = 28.0),
                negativeCentroidLab = LabColor(l = 90.0, a = 0.0, b = 0.0)
            )
        ),
        TestKitProfile(
            kitId = "KIT_E",
            displayName = "Oral Fluid Device Kit",
            drugCategory = "Saliva / Oral-Fluid Screen",
            kitType = "Oral Fluid Device",
            referenceCardProfileId = "VARIANT_E",
            roiSpec = RoiSpec(
                xMin = 180,
                yMin = 250,
                width = 280,
                height = 170,
                description = "Oral Fluid Result Window (Upper-Right Region)"
            ),
            centroids = TestKitCentroids(
                kitVariantId = "VARIANT_E",
                positiveCentroidLab = LabColor(l = 45.0, a = 55.0, b = 28.0),
                negativeCentroidLab = LabColor(l = 90.0, a = 0.0, b = 0.0)
            )
        )
    )

    fun getAllKitProfiles(): List<TestKitProfile> = kitProfiles

    fun getKitProfileByKitId(kitId: String): TestKitProfile {
        return kitProfiles.find { it.kitId == kitId || it.referenceCardProfileId == kitId } ?: kitProfiles.first()
    }

    fun getReferenceCardProfileForKit(kitId: String): ReferenceCardProfile {
        val kitProfile = getKitProfileByKitId(kitId)
        return referenceCardProfileRepository.getProfileByVariantId(kitProfile.referenceCardProfileId)
    }
}

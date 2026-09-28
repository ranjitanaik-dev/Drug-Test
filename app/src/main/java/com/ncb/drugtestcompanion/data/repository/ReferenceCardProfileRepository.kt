package com.ncb.drugtestcompanion.data.repository

import com.ncb.drugtestcompanion.domain.model.ReferenceCardProfile
import com.ncb.drugtestcompanion.domain.model.RoiSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local offline repository resolving matching ReferenceCardProfile and kit-specific RoiSpec
 * for selected test kit variants (VARIANT_A through VARIANT_E).
 */
@Singleton
class ReferenceCardProfileRepository @Inject constructor() {

    private val profiles = listOf(
        ReferenceCardProfile(
            variantId = "VARIANT_A",
            kitFormat = "5-Panel Urine Drug Test Cassette",
            cardId = "CARD_VAR_5P_CASSETTE",
            cardTitle = "5-PANEL CASSETTE REFERENCE CARD",
            roiSpec = RoiSpec(
                xMin = 140,
                yMin = 250,
                width = 360,
                height = 160,
                description = "5-Panel Cassette Result Window (Upper Region)"
            )
        ),
        ReferenceCardProfile(
            variantId = "VARIANT_B",
            kitFormat = "10-Panel Urine Drug Test Cassette",
            cardId = "CARD_VAR_10P_CASSETTE",
            cardTitle = "10-PANEL CASSETTE REFERENCE CARD",
            roiSpec = RoiSpec(
                xMin = 100,
                yMin = 240,
                width = 440,
                height = 180,
                description = "10-Panel Multi-Strip Window (Upper Region)"
            )
        ),
        ReferenceCardProfile(
            variantId = "VARIANT_C",
            kitFormat = "12-Panel Urine Drug Test Cassette",
            cardId = "CARD_VAR_12P_CASSETTE",
            cardTitle = "12-PANEL CASSETTE REFERENCE CARD",
            roiSpec = RoiSpec(
                xMin = 80,
                yMin = 230,
                width = 480,
                height = 195,
                description = "12-Panel Wide Strip Window (Upper Region)"
            )
        ),
        ReferenceCardProfile(
            variantId = "VARIANT_D",
            kitFormat = "Multi-Panel Urine Drug Test Cup",
            cardId = "CARD_VAR_CUP",
            cardTitle = "URINE CUP REFERENCE CARD",
            roiSpec = RoiSpec(
                xMin = 120,
                yMin = 730,
                width = 400,
                height = 120,
                description = "Urine Cup Test Panel Window (Lower Region)"
            )
        ),
        ReferenceCardProfile(
            variantId = "VARIANT_E",
            kitFormat = "Multi-Panel Saliva / Oral-Fluid Device",
            cardId = "CARD_VAR_ORAL_FLUID",
            cardTitle = "ORAL FLUID REFERENCE CARD",
            roiSpec = RoiSpec(
                xMin = 180,
                yMin = 250,
                width = 280,
                height = 170,
                description = "Oral Fluid Result Window (Upper-Right Region)"
            )
        )
    )

    fun getAllProfiles(): List<ReferenceCardProfile> = profiles

    fun getProfileByVariantId(variantId: String): ReferenceCardProfile {
        return profiles.find { it.variantId == variantId } ?: profiles.first()
    }
}

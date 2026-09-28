package com.ncb.drugtestcompanion.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class ReferenceCardProfileRepositoryTest {

    private lateinit var repository: ReferenceCardProfileRepository

    @Before
    fun setUp() {
        repository = ReferenceCardProfileRepository()
    }

    @Test
    fun `getAllProfiles returns all five supported spec variants`() {
        val profiles = repository.getAllProfiles()
        assertEquals(5, profiles.size)
        val variantIds = profiles.map { it.variantId }
        assertEquals(
            listOf("VARIANT_A", "VARIANT_B", "VARIANT_C", "VARIANT_D", "VARIANT_E"),
            variantIds
        )
    }

    @Test
    fun `TEST 1 - Kit A resolves to expected reference-card profile and ROI`() {
        val profile = repository.getProfileByVariantId("VARIANT_A")
        assertNotNull(profile)
        assertEquals("VARIANT_A", profile.variantId)
        assertEquals("5-Panel Urine Drug Test Cassette", profile.kitFormat)
        assertEquals(140, profile.roiSpec.xMin)
        assertEquals(250, profile.roiSpec.yMin)
        assertEquals(360, profile.roiSpec.width)
        assertEquals(160, profile.roiSpec.height)
    }

    @Test
    fun `TEST 2 - Kit B resolves to expected reference-card profile and ROI`() {
        val profile = repository.getProfileByVariantId("VARIANT_B")
        assertNotNull(profile)
        assertEquals("VARIANT_B", profile.variantId)
        assertEquals("10-Panel Urine Drug Test Cassette", profile.kitFormat)
        assertEquals(100, profile.roiSpec.xMin)
        assertEquals(240, profile.roiSpec.yMin)
        assertEquals(440, profile.roiSpec.width)
        assertEquals(180, profile.roiSpec.height)
    }

    @Test
    fun `TEST 3 - Kit C resolves to expected reference-card profile and ROI`() {
        val profile = repository.getProfileByVariantId("VARIANT_C")
        assertNotNull(profile)
        assertEquals("VARIANT_C", profile.variantId)
        assertEquals("12-Panel Urine Drug Test Cassette", profile.kitFormat)
        assertEquals(80, profile.roiSpec.xMin)
        assertEquals(230, profile.roiSpec.yMin)
        assertEquals(480, profile.roiSpec.width)
        assertEquals(195, profile.roiSpec.height)
    }

    @Test
    fun `TEST 4 - Kit D resolves to expected reference-card profile and ROI`() {
        val profile = repository.getProfileByVariantId("VARIANT_D")
        assertNotNull(profile)
        assertEquals("VARIANT_D", profile.variantId)
        assertEquals("Multi-Panel Urine Drug Test Cup", profile.kitFormat)
        assertEquals(120, profile.roiSpec.xMin)
        assertEquals(730, profile.roiSpec.yMin)
        assertEquals(400, profile.roiSpec.width)
        assertEquals(120, profile.roiSpec.height)
    }

    @Test
    fun `TEST 5 - Kit E resolves to expected reference-card profile and ROI`() {
        val profile = repository.getProfileByVariantId("VARIANT_E")
        assertNotNull(profile)
        assertEquals("VARIANT_E", profile.variantId)
        assertEquals("Multi-Panel Saliva / Oral-Fluid Device", profile.kitFormat)
        assertEquals(180, profile.roiSpec.xMin)
        assertEquals(250, profile.roiSpec.yMin)
        assertEquals(280, profile.roiSpec.width)
        assertEquals(170, profile.roiSpec.height)
    }

    @Test
    fun `TEST 12 - ROIs differ across kit profiles with distinct test geometries`() {
        val profileA = repository.getProfileByVariantId("VARIANT_A")
        val profileD = repository.getProfileByVariantId("VARIANT_D")

        assertNotEquals(profileA.roiSpec, profileD.roiSpec)
        assertNotEquals(profileA.roiSpec.yMin, profileD.roiSpec.yMin)
    }
}

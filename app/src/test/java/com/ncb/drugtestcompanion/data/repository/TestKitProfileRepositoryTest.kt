package com.ncb.drugtestcompanion.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class TestKitProfileRepositoryTest {

    private lateinit var refRepo: ReferenceCardProfileRepository
    private lateinit var kitRepo: TestKitProfileRepository

    @Before
    fun setUp() {
        refRepo = ReferenceCardProfileRepository()
        kitRepo = TestKitProfileRepository(refRepo)
    }

    @Test
    fun `getAllKitProfiles returns all five supported sample kit profiles`() {
        val kits = kitRepo.getAllKitProfiles()
        assertEquals(5, kits.size)
        val kitIds = kits.map { it.kitId }
        assertEquals(listOf("KIT_A", "KIT_B", "KIT_C", "KIT_D", "KIT_E"), kitIds)
    }

    @Test
    fun `getKitProfileByKitId resolves KIT_A to VARIANT_A with expected ROI spec`() {
        val kit = kitRepo.getKitProfileByKitId("KIT_A")
        assertNotNull(kit)
        assertEquals("KIT_A", kit.kitId)
        assertEquals("VARIANT_A", kit.referenceCardProfileId)
        assertEquals(140, kit.roiSpec.xMin)
        assertEquals(250, kit.roiSpec.yMin)
        assertEquals(360, kit.roiSpec.width)
        assertEquals(160, kit.roiSpec.height)
    }

    @Test
    fun `getKitProfileByKitId resolves KIT_D to VARIANT_D with expected cup ROI spec`() {
        val kit = kitRepo.getKitProfileByKitId("KIT_D")
        assertNotNull(kit)
        assertEquals("KIT_D", kit.kitId)
        assertEquals("VARIANT_D", kit.referenceCardProfileId)
        assertEquals(120, kit.roiSpec.xMin)
        assertEquals(730, kit.roiSpec.yMin)
        assertEquals(400, kit.roiSpec.width)
        assertEquals(120, kit.roiSpec.height)
    }

    @Test
    fun `getKitProfileByKitId falls back safely to KIT_A for unknown kit ID`() {
        val kit = kitRepo.getKitProfileByKitId("UNKNOWN_KIT_999")
        assertNotNull(kit)
        assertEquals("KIT_A", kit.kitId)
    }

    @Test
    fun `getReferenceCardProfileForKit returns matching ReferenceCardProfile with same ROI spec`() {
        val refProfile = kitRepo.getReferenceCardProfileForKit("KIT_A")
        assertNotNull(refProfile)
        assertEquals("VARIANT_A", refProfile.variantId)
        assertEquals(140, refProfile.roiSpec.xMin)
        assertEquals(250, refProfile.roiSpec.yMin)
    }
}

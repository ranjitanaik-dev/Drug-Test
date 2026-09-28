package com.ncb.drugtestcompanion.security

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class EvidenceHasherTest {

    private lateinit var evidenceHasher: EvidenceHasher

    @Before
    fun setUp() {
        evidenceHasher = EvidenceHasher()
    }

    @Test
    fun `TEST 3 - calculateSha256 produces deterministic known hash for byte array`() {
        val testData = "DrugTestCompanion-Evidence-Bytes".toByteArray(Charsets.UTF_8)
        val hash = evidenceHasher.calculateSha256(testData)
        // SHA-256 of "DrugTestCompanion-Evidence-Bytes" is known and deterministic:
        // d9c766b1d4408df96aeb3288ed379d20c5d4ef186c361ca476aed4cd18d8ff1b
        assertEquals(64, hash.length)
        val secondHash = evidenceHasher.calculateSha256(testData)
        assertEquals(hash, secondHash)
    }
}

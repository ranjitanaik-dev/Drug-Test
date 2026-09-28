package com.ncb.drugtestcompanion.viewmodel

import com.ncb.drugtestcompanion.data.repository.ReferenceCardProfileRepository
import com.ncb.drugtestcompanion.data.repository.TestKitProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class NewTestViewModelTest {

    private lateinit var refRepo: ReferenceCardProfileRepository
    private lateinit var kitRepo: TestKitProfileRepository
    private lateinit var viewModel: NewTestViewModel

    @Before
    fun setUp() {
        refRepo = ReferenceCardProfileRepository()
        kitRepo = TestKitProfileRepository(refRepo)
        viewModel = NewTestViewModel(kitRepo)
    }

    @Test
    fun `TEST 1 - available kit profiles contain KIT_A through KIT_E`() {
        val available = viewModel.availableKitProfiles
        assertEquals(5, available.size)
        val kitIds = available.map { it.kitId }
        assertEquals(listOf("KIT_A", "KIT_B", "KIT_C", "KIT_D", "KIT_E"), kitIds)
    }

    @Test
    fun `TEST 2 - initial selectedKitId is null`() {
        val selected = viewModel.selectedKitId.value
        assertNull(selected)
    }

    @Test
    fun `TEST 3 - selecting KIT_A updates selectedKitId to KIT_A`() {
        viewModel.selectKit("KIT_A")
        assertEquals("KIT_A", viewModel.selectedKitId.value)
    }

    @Test
    fun `TEST 4 - selecting KIT_E updates selectedKitId to KIT_E`() {
        viewModel.selectKit("KIT_E")
        assertEquals("KIT_E", viewModel.selectedKitId.value)
    }

    @Test
    fun `TEST 5 - no duplicate profile definitions are introduced and repository is single source of truth`() {
        val repoKits = kitRepo.getAllKitProfiles()
        val viewModelKits = viewModel.availableKitProfiles
        assertEquals(repoKits, viewModelKits)
    }
}

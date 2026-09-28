package com.ncb.drugtestcompanion.viewmodel

import androidx.lifecycle.ViewModel
import com.ncb.drugtestcompanion.data.repository.TestKitProfileRepository
import com.ncb.drugtestcompanion.domain.model.TestKitProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel managing kit selection state for [NewTestScreen].
 * Delegates profile resolution directly to [TestKitProfileRepository] without duplicating configuration.
 */
@HiltViewModel
class NewTestViewModel @Inject constructor(
    private val testKitProfileRepository: TestKitProfileRepository
) : ViewModel() {

    val availableKitProfiles: List<TestKitProfile> = testKitProfileRepository.getAllKitProfiles()

    private val _selectedKitId = MutableStateFlow<String?>(null)
    val selectedKitId: StateFlow<String?> = _selectedKitId.asStateFlow()

    fun selectKit(kitId: String) {
        val resolvedKit = testKitProfileRepository.getKitProfileByKitId(kitId)
        _selectedKitId.value = resolvedKit.kitId
    }
}

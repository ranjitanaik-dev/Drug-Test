package com.ncb.drugtestcompanion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncb.drugtestcompanion.data.repository.ReferenceCardProfileRepository
import com.ncb.drugtestcompanion.data.repository.TestKitProfileRepository
import com.ncb.drugtestcompanion.domain.model.ReferenceCardProfile
import com.ncb.drugtestcompanion.domain.usecase.CaptureImageUseCase
import com.ncb.drugtestcompanion.ui.capture.CaptureState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing kit selection and camera capture UI state.
 * Automatically resolves selected kit_id -> TestKitProfile -> ReferenceCardProfile -> RoiSpec.
 */
@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val captureImageUseCase: CaptureImageUseCase,
    private val profileRepository: ReferenceCardProfileRepository,
    private val testKitProfileRepository: TestKitProfileRepository
) : ViewModel() {

    private val availableProfiles = profileRepository.getAllProfiles()
    private val _selectedProfile = MutableStateFlow(availableProfiles.first())
    private val _isCameraReady = MutableStateFlow(false)

    private val _uiState = MutableStateFlow<CaptureState>(
        CaptureState.Idle(
            selectedProfile = _selectedProfile.value,
            isCameraReady = false
        )
    )
    val uiState: StateFlow<CaptureState> = _uiState.asStateFlow()

    fun getAvailableProfiles(): List<ReferenceCardProfile> = availableProfiles

    fun selectKitVariant(kitIdOrVariantId: String) {
        val kitProfile = testKitProfileRepository.getKitProfileByKitId(kitIdOrVariantId)
        val refCardProfile = testKitProfileRepository.getReferenceCardProfileForKit(kitProfile.kitId)
        _selectedProfile.value = refCardProfile
        _uiState.value = CaptureState.Idle(
            selectedProfile = refCardProfile,
            isCameraReady = _isCameraReady.value
        )
    }

    fun onCameraReady(isReady: Boolean) {
        _isCameraReady.value = isReady
        val currentState = _uiState.value
        if (currentState is CaptureState.Idle) {
            _uiState.value = CaptureState.Idle(
                selectedProfile = _selectedProfile.value,
                isCameraReady = isReady
            )
        }
    }

    fun capturePhoto() {
        if (!_isCameraReady.value || _uiState.value is CaptureState.Capturing) return

        viewModelScope.launch {
            _uiState.value = CaptureState.Capturing(selectedProfile = _selectedProfile.value)
            val result = captureImageUseCase()
            _uiState.value = result.fold(
                onSuccess = { path ->
                    CaptureState.Captured(
                        imagePath = path,
                        selectedProfile = _selectedProfile.value,
                        isCameraReady = _isCameraReady.value
                    )
                },
                onFailure = { error ->
                    CaptureState.Error(
                        message = error.message ?: "Image capture failed",
                        selectedProfile = _selectedProfile.value,
                        isCameraReady = _isCameraReady.value
                    )
                }
            )
        }
    }

    fun resetCapture() {
        _uiState.value = CaptureState.Idle(
            selectedProfile = _selectedProfile.value,
            isCameraReady = _isCameraReady.value
        )
    }
}

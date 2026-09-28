package com.ncb.drugtestcompanion.ui.capture

import com.ncb.drugtestcompanion.domain.model.ReferenceCardProfile

/**
 * UI State for ambient-light camera capture flow, including kit variant selection.
 */
sealed class CaptureState {
    abstract val isCameraReady: Boolean
    abstract val selectedProfile: ReferenceCardProfile

    data class Idle(
        override val selectedProfile: ReferenceCardProfile,
        override val isCameraReady: Boolean = false
    ) : CaptureState()

    data class Capturing(
        override val selectedProfile: ReferenceCardProfile,
        override val isCameraReady: Boolean = true
    ) : CaptureState()

    data class Captured(
        val imagePath: String,
        override val selectedProfile: ReferenceCardProfile,
        override val isCameraReady: Boolean = true
    ) : CaptureState()

    data class Error(
        val message: String,
        override val selectedProfile: ReferenceCardProfile,
        override val isCameraReady: Boolean = false
    ) : CaptureState()
}

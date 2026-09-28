package com.ncb.drugtestcompanion.domain.usecase

import com.ncb.drugtestcompanion.domain.repository.CameraRepository
import javax.inject.Inject

/**
 * UseCase for capturing ambient-light test images.
 * Wraps CameraX ImageCapture through [CameraRepository] and returns the saved file path/URI.
 */
class CaptureImageUseCase @Inject constructor(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(): Result<String> {
        return cameraRepository.captureAndSaveImage()
    }
}

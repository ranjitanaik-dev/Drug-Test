package com.ncb.drugtestcompanion.domain.repository

/**
 * Repository interface for camera operations.
 * Framework-agnostic contract allowing domain layer to remain pure Kotlin and testable.
 */
interface CameraRepository {
    suspend fun captureAndSaveImage(): Result<String>
}

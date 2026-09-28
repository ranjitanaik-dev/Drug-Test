package com.ncb.drugtestcompanion.domain.usecase

import com.ncb.drugtestcompanion.domain.repository.CameraRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CaptureImageUseCaseTest {

    private lateinit var cameraRepository: CameraRepository
    private lateinit var useCase: CaptureImageUseCase

    @Before
    fun setUp() {
        cameraRepository = mockk()
        useCase = CaptureImageUseCase(cameraRepository)
    }

    @Test
    fun `invoke calls captureAndSaveImage on repository and returns success`() = runTest {
        val expectedPath = "/data/user/0/com.ncb.drugtestcompanion/files/captures/capture_12345.jpg"
        coEvery { cameraRepository.captureAndSaveImage() } returns Result.success(expectedPath)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(expectedPath, result.getOrNull())
        coVerify(exactly = 1) { cameraRepository.captureAndSaveImage() }
    }

    @Test
    fun `invoke calls captureAndSaveImage on repository and returns failure on error`() = runTest {
        val exception = RuntimeException("Camera error")
        coEvery { cameraRepository.captureAndSaveImage() } returns Result.failure(exception)

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { cameraRepository.captureAndSaveImage() }
    }
}

package com.ncb.drugtestcompanion.viewmodel

import app.cash.turbine.test
import com.ncb.drugtestcompanion.data.repository.ReferenceCardProfileRepository
import com.ncb.drugtestcompanion.data.repository.TestKitProfileRepository
import com.ncb.drugtestcompanion.domain.usecase.CaptureImageUseCase
import com.ncb.drugtestcompanion.ui.capture.CaptureState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var captureImageUseCase: CaptureImageUseCase
    private lateinit var profileRepository: ReferenceCardProfileRepository
    private lateinit var testKitProfileRepository: TestKitProfileRepository
    private lateinit var viewModel: CaptureViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        captureImageUseCase = mockk()
        profileRepository = ReferenceCardProfileRepository()
        testKitProfileRepository = TestKitProfileRepository(profileRepository)
        viewModel = CaptureViewModel(captureImageUseCase, profileRepository, testKitProfileRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle with default profile and camera not ready`() {
        val state = viewModel.uiState.value
        assertTrue(state is CaptureState.Idle)
        assertEquals("VARIANT_A", state.selectedProfile.variantId)
        assertFalse(state.isCameraReady)
    }

    @Test
    fun `selectKitVariant updates selected profile in UI state`() {
        viewModel.selectKitVariant("VARIANT_B")
        val state = viewModel.uiState.value
        assertEquals("VARIANT_B", state.selectedProfile.variantId)
        assertEquals("10-Panel Urine Drug Test Cassette", state.selectedProfile.kitFormat)
    }

    @Test
    fun `capturePhoto does not invoke use case when camera is not ready`() = runTest {
        viewModel.capturePhoto()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { captureImageUseCase() }
        val state = viewModel.uiState.value
        assertTrue(state is CaptureState.Idle)
        assertFalse(state.isCameraReady)
    }

    @Test
    fun `capturePhoto transitions state from Capturing to Captured on success when camera is ready`() = runTest {
        val expectedPath = "/data/user/0/com.ncb.drugtestcompanion/files/captures/test.jpg"
        val defaultProfile = profileRepository.getProfileByVariantId("VARIANT_A")
        coEvery { captureImageUseCase() } returns Result.success(expectedPath)

        viewModel.uiState.test {
            assertEquals(CaptureState.Idle(selectedProfile = defaultProfile, isCameraReady = false), awaitItem())

            viewModel.onCameraReady(true)
            assertEquals(CaptureState.Idle(selectedProfile = defaultProfile, isCameraReady = true), awaitItem())

            viewModel.capturePhoto()

            assertEquals(CaptureState.Capturing(selectedProfile = defaultProfile, isCameraReady = true), awaitItem())
            val capturedItem = awaitItem()
            assertTrue(capturedItem is CaptureState.Captured)
            assertEquals(expectedPath, (capturedItem as CaptureState.Captured).imagePath)
            assertEquals(defaultProfile, (capturedItem as CaptureState.Captured).selectedProfile)
        }
    }

    @Test
    fun `capturePhoto transitions state to Error on failure when camera is ready`() = runTest {
        val errorMessage = "Failed to write file"
        val defaultProfile = profileRepository.getProfileByVariantId("VARIANT_A")
        coEvery { captureImageUseCase() } returns Result.failure(RuntimeException(errorMessage))

        viewModel.uiState.test {
            assertEquals(CaptureState.Idle(selectedProfile = defaultProfile, isCameraReady = false), awaitItem())

            viewModel.onCameraReady(true)
            assertEquals(CaptureState.Idle(selectedProfile = defaultProfile, isCameraReady = true), awaitItem())

            viewModel.capturePhoto()

            assertEquals(CaptureState.Capturing(selectedProfile = defaultProfile, isCameraReady = true), awaitItem())
            val errorItem = awaitItem()
            assertTrue(errorItem is CaptureState.Error)
            assertEquals(errorMessage, (errorItem as CaptureState.Error).message)
            assertEquals(defaultProfile, (errorItem as CaptureState.Error).selectedProfile)
        }
    }

    @Test
    fun `resetCapture resets state back to Idle with current camera readiness and profile`() = runTest {
        val expectedPath = "/data/user/0/com.ncb.drugtestcompanion/files/captures/test.jpg"
        coEvery { captureImageUseCase() } returns Result.success(expectedPath)

        viewModel.onCameraReady(true)
        viewModel.capturePhoto()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.resetCapture()
        val state = viewModel.uiState.value
        assertTrue(state is CaptureState.Idle)
        assertTrue(state.isCameraReady)
        assertEquals("VARIANT_A", state.selectedProfile.variantId)
    }
}

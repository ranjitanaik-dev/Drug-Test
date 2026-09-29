package com.ncb.drugtestcompanion.viewmodel

import android.content.Context
import com.ncb.drugtestcompanion.domain.usecase.GeneratePdfReportUseCase
import com.ncb.drugtestcompanion.domain.usecase.VerifyEvidenceIntegrityUseCase
import com.ncb.drugtestcompanion.location.LocationInfo
import com.ncb.drugtestcompanion.location.LocationProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.FileNotFoundException

@OptIn(ExperimentalCoroutinesApi::class)
class MlTestViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var locationProvider: LocationProvider
    private lateinit var generatePdfReportUseCase: GeneratePdfReportUseCase
    private lateinit var verifyEvidenceIntegrityUseCase: VerifyEvidenceIntegrityUseCase
    private lateinit var viewModel: MlTestViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        locationProvider = mockk(relaxed = true) {
            every { getCurrentLocation() } returns LocationInfo(status = "UNAVAILABLE")
        }
        generatePdfReportUseCase = mockk(relaxed = true)
        verifyEvidenceIntegrityUseCase = mockk(relaxed = true)

        viewModel = MlTestViewModel(
            context = context,
            locationProvider = locationProvider,
            generatePdfReportUseCase = generatePdfReportUseCase,
            verifyEvidenceIntegrityUseCase = verifyEvidenceIntegrityUseCase
        ).apply {
            ioDispatcher = testDispatcher
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has null demoResult, null error, and false isLoading`() {
        assertNull(viewModel.demoResult.value)
        assertNull(viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `runMlTest sets error state when asset is missing or unreadable`() = runTest {
        every { context.assets.open("test_rois/positive_test_roi.jpg") } throws FileNotFoundException("Asset missing")

        viewModel.runMlTest()

        assertNull(viewModel.demoResult.value)
        assertEquals("Asset missing", viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }
}

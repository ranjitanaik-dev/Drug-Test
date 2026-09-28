package com.ncb.drugtestcompanion.pdf

import com.ncb.drugtestcompanion.domain.model.TestRecord
import com.ncb.drugtestcompanion.domain.usecase.GeneratePdfReportUseCase
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PdfReportGeneratorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var pdfReportGenerator: PdfReportGenerator
    private lateinit var generatePdfReportUseCase: GeneratePdfReportUseCase

    @Before
    fun setUp() {
        pdfReportGenerator = mockk()
        generatePdfReportUseCase = GeneratePdfReportUseCase(pdfReportGenerator)
    }

    @Test
    fun `generateReport creates a valid non-empty PDF report file`() {
        val dummyPdfFile = tempFolder.newFile("DrugTestCompanion_DT-20260920-PDF001.pdf").apply {
            writeBytes("PDF-1.4 Header Content Bytes".toByteArray())
        }

        val record = TestRecord(
            testId = "DT-20260920-PDF001",
            timestamp = 1789818302000L,
            kitId = "KIT_A",
            referenceCardProfileId = "VARIANT_A",
            result = "NEGATIVE",
            confidence = 0.85f,
            distance = 3.25,
            imageSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            imagePath = "/fake/path.jpg",
            operatorId = "OP-LOCAL",
            locationStatus = "UNAVAILABLE",
            signature = "validDigitalSignatureBase64String123"
        )

        every { pdfReportGenerator.generateReport(record) } returns Result.success(dummyPdfFile)

        val result = generatePdfReportUseCase(record)

        assertTrue(result.isSuccess)
        val pdfFile = result.getOrNull()
        assertTrue(pdfFile != null && pdfFile.exists())
        assertTrue("PDF file size must be greater than zero", pdfFile!!.length() > 0)
        assertEquals("DrugTestCompanion_DT-20260920-PDF001.pdf", pdfFile.name)
    }
}

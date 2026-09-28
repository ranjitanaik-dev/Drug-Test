package com.ncb.drugtestcompanion.domain.usecase

import com.ncb.drugtestcompanion.domain.model.TestRecord
import com.ncb.drugtestcompanion.pdf.PdfReportGenerator
import java.io.File
import javax.inject.Inject

/**
 * UseCase for generating evidence PDF report files from a saved [TestRecord].
 */
class GeneratePdfReportUseCase @Inject constructor(
    private val pdfReportGenerator: PdfReportGenerator
) {
    operator fun invoke(record: TestRecord): Result<File> {
        return pdfReportGenerator.generateReport(record)
    }
}

package com.ncb.drugtestcompanion.pdf

import java.io.File

sealed class ReportState {
    data object Idle : ReportState()
    data object Generating : ReportState()
    data class Ready(val pdfFile: File) : ReportState()
    data class Error(val message: String) : ReportState()
}

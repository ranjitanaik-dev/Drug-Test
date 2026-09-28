package com.ncb.drugtestcompanion.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Utility helper for creating FileProvider URIs, opening PDFs via Intent.ACTION_VIEW,
 * and sharing PDFs via Intent.ACTION_SEND.
 */
object ReportIntentHelper {

    fun getFileProviderUri(context: Context, pdfFile: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, pdfFile)
    }

    fun openPdfReport(context: Context, pdfFile: File): Boolean {
        return try {
            val contentUri = getFileProviderUri(context, pdfFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun sharePdfReport(context: Context, pdfFile: File): Boolean {
        return try {
            val contentUri = getFileProviderUri(context, pdfFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Field Test Evidence Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (_: Throwable) {
            false
        }
    }
}

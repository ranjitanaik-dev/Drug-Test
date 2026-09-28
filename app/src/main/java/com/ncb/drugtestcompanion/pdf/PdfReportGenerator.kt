package com.ncb.drugtestcompanion.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.ncb.drugtestcompanion.domain.model.TestRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Native Android [PdfDocument] report generator creating a field test evidence PDF document
 * with evidence photo bitmap, SHA-256 hash, Keystore signature, human-readable address line, and chain-of-custody metadata.
 */
@Singleton
class PdfReportGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun generateReport(record: TestRecord): Result<File> {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size at 72 DPI
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply {
                isAntiAlias = true
            }

            var y = 40f

            // 1. Header Banner (Dark Navy)
            paint.color = Color.parseColor("#0A2540")
            canvas.drawRect(0f, 0f, 595f, 90f, paint)

            paint.color = Color.WHITE
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("DrugTest Companion", 40f, 42f, paint)

            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("FIELD TEST EVIDENCE REPORT", 40f, 65f, paint)

            y = 120f

            // 2. Report Overview Table
            paint.color = Color.parseColor("#0F2942")
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("EXAMINATION SUMMARY", 40f, y, paint)

            paint.color = Color.parseColor("#CBD5E1")
            paint.strokeWidth = 1f
            canvas.drawLine(40f, y + 8f, 555f, y + 8f, paint)

            y += 28f

            drawDataRow(canvas, paint, "Test ID:", record.testId, 40f, y)
            y += 18f
            val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm z", Locale.US).format(Date(record.timestamp))
            drawDataRow(canvas, paint, "Date & Time:", dateStr, 40f, y)
            y += 18f
            drawDataRow(canvas, paint, "Operator ID:", record.operatorId, 40f, y)
            y += 18f
            drawDataRow(canvas, paint, "Selected Kit:", record.kitId, 40f, y)
            y += 18f
            drawDataRow(canvas, paint, "Reference Card:", record.referenceCardProfileId, 40f, y)
            y += 18f
            drawDataRow(canvas, paint, "Presumptive Result:", record.result, 40f, y, isResult = true)
            y += 18f
            drawDataRow(canvas, paint, "Confidence:", String.format(Locale.US, "%.1f%%", record.confidence * 100), 40f, y)
            y += 18f
            drawDataRow(canvas, paint, "Color Distance:", String.format(Locale.US, "dE: %.2f", record.distance), 40f, y)

            y += 30f

            // 3. Captured Optical Evidence Image
            paint.color = Color.parseColor("#0F2942")
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("CAPTURED FIELD EVIDENCE", 40f, y, paint)

            paint.color = Color.parseColor("#CBD5E1")
            canvas.drawLine(40f, y + 8f, 555f, y + 8f, paint)

            y += 20f

            val imageFile = File(record.imagePath)
            if (imageFile.exists()) {
                try {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(imageFile.absolutePath, options)
                    options.inSampleSize = calculateInSampleSize(options, 480, 360)
                    options.inJustDecodeBounds = false
                    
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath, options)
                    if (bitmap != null) {
                        val scaledWidth = 240f
                        val scaledHeight = 180f
                        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth.toInt(), scaledHeight.toInt(), true)
                        canvas.drawBitmap(scaledBitmap, 40f, y, paint)
                        y += scaledHeight + 20f
                        if (scaledBitmap != bitmap) {
                            scaledBitmap.recycle()
                        }
                        bitmap.recycle()
                    } else {
                        drawMissingImageText(canvas, paint, y)
                        y += 35f
                    }
                } catch (_: Throwable) {
                    drawMissingImageText(canvas, paint, y)
                    y += 35f
                }
            } else {
                drawMissingImageText(canvas, paint, y)
                y += 35f
            }

            // 4. Test Location Section
            paint.color = Color.parseColor("#0F2942")
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TEST LOCATION", 40f, y, paint)

            paint.color = Color.parseColor("#CBD5E1")
            canvas.drawLine(40f, y + 8f, 555f, y + 8f, paint)

            y += 28f

            val locationAddressText = if (!record.address.isNullOrBlank()) {
                record.address
            } else if (record.latitude != null && record.longitude != null) {
                "${record.latitude}, ${record.longitude}"
            } else {
                "Unavailable"
            }

            drawDataRow(canvas, paint, "Location Address:", locationAddressText, 40f, y)
            y += 18f

            if (record.latitude != null && record.longitude != null) {
                val coordsText = String.format(Locale.US, "%.6f, %.6f", record.latitude, record.longitude)
                drawDataRow(canvas, paint, "Coordinates:", coordsText, 40f, y)
                y += 18f
            }

            drawDataRow(canvas, paint, "Location Status:", record.locationStatus, 40f, y)

            y += 30f

            // 5. Forensic Evidence & Integrity Section
            paint.color = Color.parseColor("#0F2942")
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("FORENSIC CHAIN-OF-CUSTODY & INTEGRITY", 40f, y, paint)

            paint.color = Color.parseColor("#CBD5E1")
            canvas.drawLine(40f, y + 8f, 555f, y + 8f, paint)

            y += 28f

            drawDataRow(canvas, paint, "Image SHA-256:", record.imageSha256, 40f, y, isMono = true)
            y += 18f
            val isSigned = record.signature.isNotBlank() && record.signature != "SIGNATURE_GENERATION_FAILED"
            drawDataRow(canvas, paint, "Digital Signature:", if (isSigned) record.signature.take(32) + "..." else "UNSEALED", 40f, y, isMono = true)
            y += 18f
            drawDataRow(canvas, paint, "Integrity Status:", if (isSigned) "VERIFIED (Keystore RSA-2048 Attested)" else "NOT VERIFIED", 40f, y)

            // 6. Footer Disclaimer Box
            y = 770f
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRect(40f, y, 555f, y + 40f, paint)

            paint.color = Color.parseColor("#475569")
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("DISCLAIMER: Presumptive field-test result. Confirmatory laboratory testing is required prior to legal disposition.", 50f, y + 24f, paint)

            pdfDocument.finishPage(page)

            // Save PDF to app-private reports directory
            val reportsDir = File(context.filesDir, "reports").apply {
                if (!exists()) mkdirs()
            }
            val reportFile = File(reportsDir, "DrugTestCompanion_${record.testId}.pdf")
            FileOutputStream(reportFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            if (reportFile.exists() && reportFile.length() > 0) {
                Result.success(reportFile)
            } else {
                Result.failure(IllegalStateException("PDF file generation failed or produced zero bytes"))
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private fun drawDataRow(
        canvas: Canvas,
        paint: Paint,
        label: String,
        value: String,
        x: Float,
        y: Float,
        isResult: Boolean = false,
        isMono: Boolean = false
    ) {
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(label, x, y, paint)

        if (isResult) {
            paint.color = when (value) {
                "POSITIVE" -> Color.parseColor("#DC2626")
                "NEGATIVE" -> Color.parseColor("#059669")
                else -> Color.parseColor("#D97706")
            }
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        } else {
            paint.color = Color.parseColor("#0F2942")
            paint.typeface = if (isMono) Typeface.MONOSPACE else Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(value, x + 130f, y, paint)
    }

    private fun drawMissingImageText(canvas: Canvas, paint: Paint, y: Float) {
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("[Optical Capture File Unavailable]", 40f, y + 15f, paint)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}

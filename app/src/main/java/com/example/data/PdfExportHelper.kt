package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.ReportCard
import com.example.model.School
import java.io.File
import java.io.FileOutputStream

object PdfExportHelper {
    fun exportReportCardPdf(
        context: Context,
        school: School,
        report: ReportCard
    ): Result<Intent> {
        return try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val headerPaint = Paint().apply {
                color = Color.parseColor("#0F2537")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, 595f, 110f, headerPaint)

            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 20f
                isFakeBoldText = true
            }
            val subHeaderPaint = Paint().apply {
                color = Color.parseColor("#D1FAE5")
                textSize = 11f
            }
            val labelPaint = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = 11f
                isFakeBoldText = true
            }
            val bodyPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 12f
            }
            val dividerPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                strokeWidth = 1f
            }

            canvas.drawText(school.name.uppercase(), 36f, 44f, titlePaint)
            canvas.drawText(
                "${school.city}, ${school.country}  •  ${report.academicYear} (${report.term})",
                36f,
                68f,
                subHeaderPaint
            )
            canvas.drawText(
                "OFFICIAL STUDENT REPORT CARD  •  v${report.version} [${report.status}]",
                36f,
                88f,
                subHeaderPaint
            )

            var y = 148f
            canvas.drawText("STUDENT INFORMATION", 36f, y, labelPaint)
            y += 20f
            canvas.drawText("Student Name: ${report.studentName}", 36f, y, bodyPaint)
            canvas.drawText("Class: ${report.className}", 340f, y, bodyPaint)
            y += 18f
            canvas.drawText("Template: ${report.templateName}", 36f, y, bodyPaint)
            canvas.drawText(
                "Overall Average: ${"%.1f".format(report.averageScore)}%",
                340f,
                y,
                bodyPaint
            )

            y += 18f
            canvas.drawLine(36f, y, 559f, y, dividerPaint)
            y += 26f

            canvas.drawText("SUBJECT PERFORMANCE SUMMARY", 36f, y, labelPaint)
            y += 20f
            report.subjectGradesSummary.split(",", ";", "\n").forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && y < 740f) {
                    canvas.drawText("• $trimmed", 44f, y, bodyPaint)
                    y += 18f
                }
            }

            y += 12f
            canvas.drawLine(36f, y, 559f, y, dividerPaint)
            y += 24f

            canvas.drawText("TEACHER REMARKS", 36f, y, labelPaint)
            y += 18f
            y = drawWrappedText(
                canvas,
                report.teacherRemarks?.ifBlank { "No remarks recorded." } ?: "No remarks recorded.",
                36f,
                y,
                520f,
                bodyPaint
            )

            if (!report.aiSummary.isNullOrBlank()) {
                y += 16f
                canvas.drawText("AI ACADEMIC INSIGHT", 36f, y, labelPaint)
                y += 18f
                y = drawWrappedText(canvas, report.aiSummary, 36f, y, 520f, bodyPaint)
            }

            y += 16f
            canvas.drawText("HEADTEACHER / PRINCIPAL APPROVAL", 36f, y, labelPaint)
            y += 18f
            y = drawWrappedText(
                canvas,
                report.headteacherRemarks?.ifBlank { "Pending principal remarks." }
                    ?: "Pending principal remarks.",
                36f,
                y,
                520f,
                bodyPaint
            )

            y += 24f
            canvas.drawText(
                "Submitted by: ${report.submittedBy ?: "Class Teacher"}   |   Reviewed by: ${report.reviewedBy ?: "Pending"}",
                36f,
                y,
                labelPaint
            )

            document.finishPage(page)

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val safeName = report.studentName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(reportsDir, "GSMS_Report_${safeName}_v${report.version}.pdf")
            FileOutputStream(file).use { out -> document.writeTo(out) }
            document.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "${school.name} - ${report.studentName} Report Card")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Result.success(Intent.createChooser(shareIntent, "Export Report Card PDF"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun drawWrappedText(
        canvas: android.graphics.Canvas,
        text: String,
        startX: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint
    ): Float {
        var y = startY
        val words = text.split(" ")
        val line = StringBuilder()
        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(testLine) > maxWidth) {
                canvas.drawText(line.toString(), startX, y, paint)
                y += 17f
                line.clear()
                line.append(word)
            } else {
                line.clear()
                line.append(testLine)
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), startX, y, paint)
            y += 17f
        }
        return y
    }
}

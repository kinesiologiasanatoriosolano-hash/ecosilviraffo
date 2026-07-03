package com.sonocare.mindrayreceiver.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import com.sonocare.mindrayreceiver.data.Exam
import com.sonocare.mindrayreceiver.util.Prefs
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Genera un informe PDF con los datos de la clinica, del examen, y una
 * grilla de imagenes, usando android.graphics.pdf.PdfDocument (nativo del
 * SDK, sin dependencias externas ni problemas de licencia).
 */
class ReportGenerator(private val context: Context, private val prefs: Prefs) {

    private val pageWidth = 595 // A4 a 72dpi aprox
    private val pageHeight = 842

    fun generate(exam: Exam, observations: String = ""): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = 40f
        y = drawHeader(canvas, y)
        y = drawPatientBlock(canvas, exam, y)
        y = drawImagesGrid(canvas, exam, y)
        drawObservations(canvas, observations, y)

        document.finishPage(page)

        val outputDir = File(context.getExternalFilesDir(null), "reports").apply { mkdirs() }
        val outputFile = File(outputDir, "informe_${exam.examId}.pdf")
        FileOutputStream(outputFile).use { document.writeTo(it) }
        document.close()

        return outputFile
    }

    private fun drawHeader(canvas: Canvas, startY: Float): Float {
        var y = startY
        val titlePaint = Paint().apply {
            color = 0xFF1A3C6E.toInt()
            textSize = 18f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            color = 0xFF666666.toInt()
            textSize = 10f
        }

        canvas.drawText(prefs.clinicName, 40f, y, titlePaint)
        y += 20f
        canvas.drawText("Informe de Estudio Ecografico", 40f, y, subtitlePaint)
        y += 14f
        if (prefs.doctorName.isNotBlank()) {
            canvas.drawText("Dr/a. ${prefs.doctorName}", 40f, y, subtitlePaint)
            y += 14f
        }
        if (prefs.doctorLicense.isNotBlank()) {
            canvas.drawText("Matricula: ${prefs.doctorLicense}", 40f, y, subtitlePaint)
            y += 14f
        }

        y += 6f
        val linePaint = Paint().apply { color = 0xFF1A3C6E.toInt(); strokeWidth = 1.5f }
        canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
        return y + 20f
    }

    private fun drawPatientBlock(canvas: Canvas, exam: Exam, startY: Float): Float {
        var y = startY
        val labelPaint = Paint().apply { color = 0xFF000000.toInt(); textSize = 11f; isFakeBoldText = true }
        val valuePaint = Paint().apply { color = 0xFF333333.toInt(); textSize = 11f }

        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es")).format(Date(exam.receivedAt))

        canvas.drawText("Paciente:", 40f, y, labelPaint)
        canvas.drawText(exam.patientName, 130f, y, valuePaint)
        canvas.drawText("Fecha:", 350f, y, labelPaint)
        canvas.drawText(dateStr, 400f, y, valuePaint)
        y += 18f
        canvas.drawText("ID Examen:", 40f, y, labelPaint)
        canvas.drawText(exam.examId, 130f, y, valuePaint)
        y += 25f
        return y
    }

    private fun drawImagesGrid(canvas: Canvas, exam: Exam, startY: Float): Float {
        var y = startY
        val titlePaint = Paint().apply { color = 0xFF000000.toInt(); textSize = 13f; isFakeBoldText = true }
        canvas.drawText("Imagenes del Estudio (${exam.imageList().size})", 40f, y, titlePaint)
        y += 15f

        val cellW = 240f
        val cellH = 180f
        var x = 40f
        var col = 0

        exam.imageList().take(4).forEach { path ->
            val file = File(path)
            if (!file.exists()) return@forEach
            val bmp = decodeScaledBitmap(file, cellW.toInt(), cellH.toInt()) ?: return@forEach

            val destRect = Rect(x.toInt(), y.toInt(), (x + cellW).toInt(), (y + cellH).toInt())
            canvas.drawBitmap(bmp, null, destRect, null)

            col++
            if (col % 2 == 0) {
                x = 40f
                y += cellH + 15f
            } else {
                x = 40f + cellW + 15f
            }
        }
        if (col % 2 != 0) y += cellH + 15f

        return y + 10f
    }

    private fun drawObservations(canvas: Canvas, observations: String, startY: Float) {
        var y = startY
        val titlePaint = Paint().apply { color = 0xFF000000.toInt(); textSize = 13f; isFakeBoldText = true }
        val textPaint = Paint().apply { color = 0xFF333333.toInt(); textSize = 10f }

        canvas.drawText("Hallazgos / Observaciones", 40f, y, titlePaint)
        y += 18f

        val text = observations.ifBlank { "________________________________________________" }
        text.split("\n").forEach { line ->
            canvas.drawText(line, 40f, y, textPaint)
            y += 14f
        }
    }

    private fun decodeScaledBitmap(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)

            var sampleSize = 1
            while (options.outWidth / sampleSize > reqWidth * 2 || options.outHeight / sampleSize > reqHeight * 2) {
                sampleSize *= 2
            }

            val finalOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(file.absolutePath, finalOptions)
        } catch (e: Exception) {
            null
        }
    }
}

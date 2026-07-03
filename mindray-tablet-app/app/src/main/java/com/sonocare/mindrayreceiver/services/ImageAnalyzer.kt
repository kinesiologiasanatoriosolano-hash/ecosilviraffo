package com.sonocare.mindrayreceiver.services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.sonocare.mindrayreceiver.data.Exam
import java.io.File
import kotlin.math.sqrt

/**
 * Placeholder de "Analisis de imagen". Calcula metricas basicas de
 * nitidez (varianza de intensidad) y contraste (desvio estandar) sobre
 * cada imagen del examen. Punto de extension para enchufar en el futuro
 * un modelo real de IA (clasificador, deteccion de estructuras, etc.).
 */
class ImageAnalyzer {

    fun analyze(exam: Exam): String {
        val results = StringBuilder()

        exam.imageList().forEach { path ->
            val file = File(path)
            if (!file.exists()) return@forEach

            val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: return@forEach
            val metrics = computeMetrics(bmp)
            results.append("${file.name}: nitidez=${"%.1f".format(metrics.first)}, contraste=${"%.1f".format(metrics.second)}\n")
        }

        return if (results.isEmpty()) "No hay imagenes analizables." else results.toString()
    }

    /** Devuelve (varianza, desvio_estandar) de la luminancia de la imagen, submuestreada para performance. */
    private fun computeMetrics(bitmap: Bitmap): Pair<Double, Double> {
        val step = 4 // submuestreo para no recorrer pixel a pixel en imagenes grandes
        val values = mutableListOf<Double>()

        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val luminance = 0.299 * r + 0.587 * g + 0.114 * b
                values.add(luminance)
                x += step
            }
            y += step
        }

        if (values.isEmpty()) return Pair(0.0, 0.0)

        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        val stdDev = sqrt(variance)
        return Pair(variance, stdDev)
    }
}

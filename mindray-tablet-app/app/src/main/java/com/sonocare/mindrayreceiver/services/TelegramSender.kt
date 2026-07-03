package com.sonocare.mindrayreceiver.services

import com.sonocare.mindrayreceiver.util.Prefs
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Envia imagenes por Telegram llamando directamente al endpoint HTTP del
 * Bot API (sendPhoto / sendDocument), sin depender de un SDK pesado.
 */
class TelegramSender(private val prefs: Prefs) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Envia cada imagen del examen; lanza excepcion si falla algun request. */
    fun sendExam(examId: String, imagePaths: List<String>) {
        require(prefs.telegramToken.isNotBlank() && prefs.telegramChatId.isNotBlank()) {
            "Telegram no esta configurado (falta token o chat_id)"
        }

        imagePaths.forEach { path ->
            val file = File(path)
            if (!file.exists()) return@forEach
            sendPhoto(file, caption = "Examen: $examId")
        }
    }

    private fun sendPhoto(file: File, caption: String) {
        val url = "https://api.telegram.org/bot${prefs.telegramToken}/sendPhoto"

        val mediaType = if (file.extension.lowercase() == "png") "image/png" else "image/jpeg"
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", prefs.telegramChatId)
            .addFormDataPart("caption", caption)
            .addFormDataPart(
                "photo", file.name,
                file.asRequestBody(mediaType.toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Error Telegram (${response.code}): ${response.body?.string()}")
            }
        }
    }

    fun sendDocument(file: File, caption: String = "") {
        require(prefs.telegramToken.isNotBlank() && prefs.telegramChatId.isNotBlank()) {
            "Telegram no esta configurado"
        }
        val url = "https://api.telegram.org/bot${prefs.telegramToken}/sendDocument"
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", prefs.telegramChatId)
            .addFormDataPart("caption", caption)
            .addFormDataPart(
                "document", file.name,
                file.asRequestBody("application/pdf".toMediaTypeOrNull())
            )
            .build()
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Error Telegram (${response.code}): ${response.body?.string()}")
            }
        }
    }
}

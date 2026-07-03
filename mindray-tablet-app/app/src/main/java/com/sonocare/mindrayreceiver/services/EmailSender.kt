package com.sonocare.mindrayreceiver.services

import com.sonocare.mindrayreceiver.util.Prefs
import java.io.File
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

/**
 * Envia examenes/informes por correo via SMTP usando la libreria
 * com.sun.mail:android-mail (puerto de JavaMail compatible con Android).
 *
 * IMPORTANTE: esta llamada es bloqueante (red), siempre debe invocarse
 * desde un hilo de background (ver uso en MainActivity con coroutines).
 */
class EmailSender(private val prefs: Prefs) {

    private fun buildSession(): Session {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", prefs.smtpServer)
            put("mail.smtp.port", prefs.smtpPort.toString())
        }
        return Session.getInstance(props, object : javax.mail.Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(prefs.emailUser, prefs.emailPassword)
            }
        })
    }

    fun sendExam(examId: String, imagePaths: List<String>, recipient: String? = null) {
        require(prefs.emailUser.isNotBlank() && prefs.emailPassword.isNotBlank()) {
            "Email no esta configurado"
        }
        val to = recipient ?: prefs.emailDefaultRecipient
        require(to.isNotBlank()) { "No se especifico destinatario de email" }

        val session = buildSession()
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(prefs.emailUser))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            subject = "Examen ecografico - $examId"
        }

        val multipart = MimeMultipart()
        val textPart = MimeBodyPart().apply {
            setText("Se adjuntan las imagenes del examen $examId.\n\nEnviado automaticamente por Mindray Receiver.")
        }
        multipart.addBodyPart(textPart)

        imagePaths.forEach { path ->
            val file = File(path)
            if (!file.exists()) return@forEach
            val attachment = MimeBodyPart().apply {
                dataHandler = DataHandler(FileDataSource(file))
                fileName = file.name
            }
            multipart.addBodyPart(attachment)
        }

        message.setContent(multipart)
        Transport.send(message)
    }

    fun sendReport(pdfFile: File, recipient: String? = null) {
        require(prefs.emailUser.isNotBlank() && prefs.emailPassword.isNotBlank()) {
            "Email no esta configurado"
        }
        val to = recipient ?: prefs.emailDefaultRecipient
        require(to.isNotBlank()) { "No se especifico destinatario de email" }

        val session = buildSession()
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(prefs.emailUser))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            subject = "Informe ecografico - ${pdfFile.nameWithoutExtension}"
        }

        val multipart = MimeMultipart()
        multipart.addBodyPart(MimeBodyPart().apply {
            setText("Se adjunta el informe en PDF.\n\nEnviado automaticamente por Mindray Receiver.")
        })
        multipart.addBodyPart(MimeBodyPart().apply {
            dataHandler = DataHandler(FileDataSource(pdfFile))
            fileName = pdfFile.name
        })

        message.setContent(multipart)
        Transport.send(message)
    }
}

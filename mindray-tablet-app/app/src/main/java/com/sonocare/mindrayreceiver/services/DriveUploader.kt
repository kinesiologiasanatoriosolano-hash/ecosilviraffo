package com.sonocare.mindrayreceiver.services

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import com.sonocare.mindrayreceiver.util.Prefs
import java.io.File as JFile

/**
 * Sube examenes a Google Drive.
 *
 * Requiere que el usuario haya iniciado sesion previamente con
 * GoogleSignIn (ver SettingsActivity: boton "Conectar Google Drive"),
 * ya que la subida necesita un Account valido para construir las
 * credenciales OAuth2.
 */
class DriveUploader(private val context: Context, private val prefs: Prefs) {

    private fun buildDriveService(): Drive {
        val account = GoogleSignIn.getLastSignedInAccount(context)?.account
            ?: throw IllegalStateException("No hay sesion de Google Drive iniciada. Andá a Configuracion.")

        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        ).setSelectedAccount(account)

        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Mindray Receiver").build()
    }

    private fun getOrCreateFolder(drive: Drive, name: String, parentId: String? = null): String {
        val queryParts = mutableListOf(
            "mimeType='application/vnd.google-apps.folder'",
            "name='$name'",
            "trashed=false"
        )
        if (parentId != null) queryParts.add("'$parentId' in parents")

        val result = drive.files().list()
            .setQ(queryParts.joinToString(" and "))
            .setSpaces("drive")
            .execute()

        result.files?.firstOrNull()?.let { return it.id }

        val metadata = DriveFile().setName(name).setMimeType("application/vnd.google-apps.folder")
        if (parentId != null) metadata.parents = listOf(parentId)
        return drive.files().create(metadata).setFields("id").execute().id
    }

    /** Sube todas las imagenes del examen a una subcarpeta con su ID. Devuelve el link a la carpeta. */
    fun uploadExam(examId: String, imagePaths: List<String>): String {
        val drive = buildDriveService()
        val rootId = getOrCreateFolder(drive, prefs.driveFolderName)
        val examFolderId = getOrCreateFolder(drive, examId, rootId)

        imagePaths.forEach { path ->
            val file = JFile(path)
            if (!file.exists()) return@forEach

            val mimeType = if (file.extension.lowercase() == "png") "image/png" else "image/jpeg"
            val metadata = DriveFile().setName(file.name).setParents(listOf(examFolderId))
            val content = com.google.api.client.http.FileContent(mimeType, file)
            drive.files().create(metadata, content).setFields("id").execute()
        }

        return "https://drive.google.com/drive/folders/$examFolderId"
    }
}

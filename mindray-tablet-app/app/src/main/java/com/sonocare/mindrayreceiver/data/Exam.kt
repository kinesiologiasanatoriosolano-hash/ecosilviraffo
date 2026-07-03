package com.sonocare.mindrayreceiver.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un examen (conjunto de imagenes llegadas
 * juntas). Como las imagenes llegan como JPEG/PNG planos (sin metadata
 * DICOM), los datos de paciente son opcionales/editables manualmente
 * desde la app si se desea asociarlos.
 */
@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey val examId: String,
    val receivedAt: Long,
    /** Rutas absolutas de las imagenes del examen, separadas por "|" */
    val imagePaths: String,
    val patientName: String = "Desconocido",
    val thumbnailPath: String? = null,

    val uploadedDrive: Boolean = false,
    val driveLink: String? = null,
    val sentTelegram: Boolean = false,
    val sentEmail: Boolean = false,
    val reportGenerated: Boolean = false,
    val reportPath: String? = null,
    val analysisNotes: String? = null,
) {
    fun imageList(): List<String> =
        if (imagePaths.isBlank()) emptyList() else imagePaths.split("|")
}

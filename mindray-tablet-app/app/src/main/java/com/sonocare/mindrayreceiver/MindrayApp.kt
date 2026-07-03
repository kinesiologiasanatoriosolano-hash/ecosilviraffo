package com.sonocare.mindrayreceiver

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.topjohnwu.superuser.Shell

/**
 * Clase Application principal.
 *
 * Configura libsu (acceso root) en modo "global" para que cualquier parte
 * de la app pueda pedir una shell root sin reinicializar, y crea el canal
 * de notificaciones usado por el servicio de fondo (SmbBridgeService).
 */
class MindrayApp : Application() {

    companion object {
        const val CHANNEL_ID_EXAMS = "mindray_exams"
        const val CHANNEL_ID_SERVICE = "mindray_service"
    }

    override fun onCreate() {
        super.onCreate()

        // Configuracion global de libsu: reutiliza una unica shell root,
        // con timeout generoso porque el primer pedido de "su" puede tardar
        // si el usuario debe confirmar en la UI de Magisk.
        Shell.enableVerboseLogging = BuildConfig_DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(15)
        )

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val examsChannel = NotificationChannel(
                CHANNEL_ID_EXAMS,
                "Examenes recibidos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificacion al llegar un nuevo examen del ecografo"
                enableVibration(true)
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                "Servicio en segundo plano",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Indica que el bridge SMB esta activo y vigilando"
            }

            manager.createNotificationChannel(examsChannel)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}

// Pequeno helper para no depender directamente de BuildConfig.DEBUG en este
// archivo (evita problemas de import segun variante de build configurada).
private val BuildConfig_DEBUG: Boolean
    get() = com.sonocare.mindrayreceiver.BuildConfig.DEBUG

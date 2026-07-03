package com.sonocare.mindrayreceiver.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.sonocare.mindrayreceiver.MainActivity
import com.sonocare.mindrayreceiver.MindrayApp

/**
 * Dispara la notificacion (con sonido por defecto del sistema, ya que el
 * canal de notificaciones se configura con sonido en MindrayApp) cuando
 * llega un nuevo examen.
 */
object NotificationHelper {

    fun notifyNewExam(context: Context, examId: String, imageCount: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("exam_id", examId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, examId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, MindrayApp.CHANNEL_ID_EXAMS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Nueva ecografia recibida")
            .setContentText("$imageCount imagen(es) - $examId")
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(examId.hashCode(), notification)
    }
}

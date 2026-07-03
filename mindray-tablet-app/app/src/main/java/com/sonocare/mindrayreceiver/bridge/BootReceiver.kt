package com.sonocare.mindrayreceiver.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.sonocare.mindrayreceiver.util.Prefs

/**
 * Arranca automaticamente el bridge SMB cuando el tablet se enciende,
 * si el usuario dejo activada la opcion "Iniciar automaticamente" en
 * Configuracion. Util para que el consultorio no tenga que abrir la app
 * manualmente cada vez que prende el tablet.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = Prefs(context)
        if (!prefs.bridgeAutoStart) return

        val serviceIntent = Intent(context, SmbBridgeService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}

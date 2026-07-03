package com.sonocare.mindrayreceiver.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Wrapper simple sobre SharedPreferences para toda la configuracion de la
 * app: credenciales de la carpeta compartida (que el V6 va a usar),
 * credenciales de Telegram/Email, y datos del informe PDF.
 *
 * Equivalente al config.json de la version de escritorio, pero persistido
 * de forma nativa en Android.
 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("mindray_prefs", Context.MODE_PRIVATE)

    // ------------------------------------------------------------------
    // Carpeta compartida (bridge SMB)
    // ------------------------------------------------------------------
    var shareName: String
        get() = sp.getString("share_name", "MindrayShare") ?: "MindrayShare"
        set(v) = sp.edit().putString("share_name", v).apply()

    var shareUser: String
        get() = sp.getString("share_user", "mindray") ?: "mindray"
        set(v) = sp.edit().putString("share_user", v).apply()

    var sharePassword: String
        get() = sp.getString("share_password", "mindray123") ?: "mindray123"
        set(v) = sp.edit().putString("share_password", v).apply()

    /** Puerto interno real donde escucha JFileServer (sin privilegios). */
    var internalSmbPort: Int
        get() = sp.getInt("internal_smb_port", 8445)
        set(v) = sp.edit().putInt("internal_smb_port", v).apply()

    var bridgeAutoStart: Boolean
        get() = sp.getBoolean("bridge_autostart", true)
        set(v) = sp.edit().putBoolean("bridge_autostart", v).apply()

    // ------------------------------------------------------------------
    // Google Drive
    // ------------------------------------------------------------------
    var driveEnabled: Boolean
        get() = sp.getBoolean("drive_enabled", false)
        set(v) = sp.edit().putBoolean("drive_enabled", v).apply()

    var driveAutoUpload: Boolean
        get() = sp.getBoolean("drive_auto_upload", false)
        set(v) = sp.edit().putBoolean("drive_auto_upload", v).apply()

    var driveFolderName: String
        get() = sp.getString("drive_folder_name", "Mindray_Ecografias") ?: "Mindray_Ecografias"
        set(v) = sp.edit().putString("drive_folder_name", v).apply()

    // ------------------------------------------------------------------
    // Telegram
    // ------------------------------------------------------------------
    var telegramEnabled: Boolean
        get() = sp.getBoolean("tg_enabled", false)
        set(v) = sp.edit().putBoolean("tg_enabled", v).apply()

    var telegramAutoSend: Boolean
        get() = sp.getBoolean("tg_autosend", false)
        set(v) = sp.edit().putBoolean("tg_autosend", v).apply()

    var telegramToken: String
        get() = sp.getString("tg_token", "") ?: ""
        set(v) = sp.edit().putString("tg_token", v).apply()

    var telegramChatId: String
        get() = sp.getString("tg_chat_id", "") ?: ""
        set(v) = sp.edit().putString("tg_chat_id", v).apply()

    // ------------------------------------------------------------------
    // Email
    // ------------------------------------------------------------------
    var emailEnabled: Boolean
        get() = sp.getBoolean("email_enabled", false)
        set(v) = sp.edit().putBoolean("email_enabled", v).apply()

    var emailAutoSend: Boolean
        get() = sp.getBoolean("email_autosend", false)
        set(v) = sp.edit().putBoolean("email_autosend", v).apply()

    var smtpServer: String
        get() = sp.getString("smtp_server", "smtp.gmail.com") ?: "smtp.gmail.com"
        set(v) = sp.edit().putString("smtp_server", v).apply()

    var smtpPort: Int
        get() = sp.getInt("smtp_port", 587)
        set(v) = sp.edit().putInt("smtp_port", v).apply()

    var emailUser: String
        get() = sp.getString("email_user", "") ?: ""
        set(v) = sp.edit().putString("email_user", v).apply()

    var emailPassword: String
        get() = sp.getString("email_password", "") ?: ""
        set(v) = sp.edit().putString("email_password", v).apply()

    var emailDefaultRecipient: String
        get() = sp.getString("email_recipient", "") ?: ""
        set(v) = sp.edit().putString("email_recipient", v).apply()

    // ------------------------------------------------------------------
    // Informe PDF
    // ------------------------------------------------------------------
    var clinicName: String
        get() = sp.getString("clinic_name", "Consultorio de Ecografia") ?: "Consultorio de Ecografia"
        set(v) = sp.edit().putString("clinic_name", v).apply()

    var doctorName: String
        get() = sp.getString("doctor_name", "") ?: ""
        set(v) = sp.edit().putString("doctor_name", v).apply()

    var doctorLicense: String
        get() = sp.getString("doctor_license", "") ?: ""
        set(v) = sp.edit().putString("doctor_license", v).apply()
}

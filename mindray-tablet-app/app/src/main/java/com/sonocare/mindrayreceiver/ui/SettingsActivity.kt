package com.sonocare.mindrayreceiver.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.services.drive.DriveScopes
import com.sonocare.mindrayreceiver.bridge.SmbBridgeService
import com.sonocare.mindrayreceiver.databinding.ActivitySettingsBinding
import com.sonocare.mindrayreceiver.util.Prefs
import java.net.NetworkInterface
import java.util.Collections

/**
 * Pantalla de configuracion. Muestra ademas la IP actual del tablet en
 * la red Wi-Fi, que es el dato mas importante que hay que cargar en el
 * V6 (campo "Direccion IP" de Almacenamiento de red).
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: Prefs
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs(this)

        setupGoogleSignIn()
        loadValuesIntoForm()
        showDeviceIp()

        binding.btnConnectDrive.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
        }

        binding.btnSave.setOnClickListener { saveAndRestart() }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_FILE))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val account = GoogleSignIn.getLastSignedInAccount(this)
            if (account != null) {
                Toast.makeText(this, "Cuenta conectada: ${account.email}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No se pudo conectar la cuenta de Google", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeviceIp() {
        val ip = getLocalWifiIp() ?: "No detectada (¿estas conectado al Wi-Fi local?)"
        binding.txtDeviceIp.text = "IP del tablet (usar en el V6): $ip"
    }

    private fun getLocalWifiIp(): String? {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                        return addr.hostAddress
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    // ------------------------------------------------------------------
    private fun loadValuesIntoForm() {
        binding.edtShareName.setText(prefs.shareName)
        binding.edtShareUser.setText(prefs.shareUser)
        binding.edtSharePassword.setText(prefs.sharePassword)
        binding.switchAutoStart.isChecked = prefs.bridgeAutoStart

        binding.switchDriveEnabled.isChecked = prefs.driveEnabled
        binding.switchDriveAutoUpload.isChecked = prefs.driveAutoUpload

        binding.switchTelegramEnabled.isChecked = prefs.telegramEnabled
        binding.switchTelegramAutoSend.isChecked = prefs.telegramAutoSend
        binding.edtTelegramToken.setText(prefs.telegramToken)
        binding.edtTelegramChatId.setText(prefs.telegramChatId)

        binding.switchEmailEnabled.isChecked = prefs.emailEnabled
        binding.switchEmailAutoSend.isChecked = prefs.emailAutoSend
        binding.edtSmtpServer.setText(prefs.smtpServer)
        binding.edtSmtpPort.setText(prefs.smtpPort.toString())
        binding.edtEmailUser.setText(prefs.emailUser)
        binding.edtEmailPassword.setText(prefs.emailPassword)
        binding.edtEmailRecipient.setText(prefs.emailDefaultRecipient)

        binding.edtClinicName.setText(prefs.clinicName)
        binding.edtDoctorName.setText(prefs.doctorName)
        binding.edtDoctorLicense.setText(prefs.doctorLicense)
    }

    private fun saveAndRestart() {
        prefs.shareName = binding.edtShareName.text.toString().ifBlank { "MindrayShare" }
        prefs.shareUser = binding.edtShareUser.text.toString()
        prefs.sharePassword = binding.edtSharePassword.text.toString()
        prefs.bridgeAutoStart = binding.switchAutoStart.isChecked

        prefs.driveEnabled = binding.switchDriveEnabled.isChecked
        prefs.driveAutoUpload = binding.switchDriveAutoUpload.isChecked

        prefs.telegramEnabled = binding.switchTelegramEnabled.isChecked
        prefs.telegramAutoSend = binding.switchTelegramAutoSend.isChecked
        prefs.telegramToken = binding.edtTelegramToken.text.toString()
        prefs.telegramChatId = binding.edtTelegramChatId.text.toString()

        prefs.emailEnabled = binding.switchEmailEnabled.isChecked
        prefs.emailAutoSend = binding.switchEmailAutoSend.isChecked
        prefs.smtpServer = binding.edtSmtpServer.text.toString()
        prefs.smtpPort = binding.edtSmtpPort.text.toString().toIntOrNull() ?: 587
        prefs.emailUser = binding.edtEmailUser.text.toString()
        prefs.emailPassword = binding.edtEmailPassword.text.toString()
        prefs.emailDefaultRecipient = binding.edtEmailRecipient.text.toString()

        prefs.clinicName = binding.edtClinicName.text.toString()
        prefs.doctorName = binding.edtDoctorName.text.toString()
        prefs.doctorLicense = binding.edtDoctorLicense.text.toString()

        // Reinicia el servicio para que tome la nueva configuracion
        // (nombre de carpeta, usuario, contrasena, etc.)
        stopService(Intent(this, SmbBridgeService::class.java))
        startService(Intent(this, SmbBridgeService::class.java))

        Toast.makeText(this, "Configuracion guardada. Bridge reiniciado.", Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        private const val RC_SIGN_IN = 200
    }
}

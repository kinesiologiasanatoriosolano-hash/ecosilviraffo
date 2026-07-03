package com.sonocare.mindrayreceiver.bridge

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.FileObserver
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.sonocare.mindrayreceiver.MainActivity
import com.sonocare.mindrayreceiver.MindrayApp
import com.sonocare.mindrayreceiver.data.AppDatabase
import com.sonocare.mindrayreceiver.data.Exam
import com.sonocare.mindrayreceiver.notifications.NotificationHelper
import com.sonocare.mindrayreceiver.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SmbBridgeService
 * -----------------
 * Servicio en primer plano (no lo mata Android) que:
 *  1. Aplica la redireccion de puerto root 445 -> puerto interno (RootUtils).
 *  2. Levanta el servidor SMB embebido (JFileServer) sobre una carpeta local
 *     de la app (Android/data/<pkg>/files/incoming), que es exactamente la
 *     carpeta que el Mindray V6 va a ver como "carpeta compartida" en su
 *     configuracion de Almacenamiento de Red.
 *  3. Vigila esa carpeta local con FileObserver (equivalente nativo de
 *     Android a `watchdog` de Python) para detectar archivos nuevos.
 *  4. Agrupa archivos que llegan juntos (mismo estudio) con un pequeno
 *     debounce, los registra como un Exam en Room, y dispara notificacion.
 */
class SmbBridgeService : Service() {

    private var fileObserver: FileObserver? = null
    private var smbServerHandle: Any? = null // Instancia real de org.filesys.smb.server.SMBServer

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val mainHandler = Handler(Looper.getMainLooper())

    private val pendingFiles = mutableSetOf<String>()
    private var flushRunnable: Runnable? = null
    private val debounceMillis = 3000L

    private lateinit var prefs: Prefs
    private lateinit var incomingDir: File

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        incomingDir = File(getExternalFilesDir(null), "incoming").apply { mkdirs() }

        startForeground(NOTIF_ID_SERVICE, buildServiceNotification("Iniciando bridge SMB..."))
        serviceScope.launch { startBridge() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY: si Android mata el proceso por presion de memoria,
        // intenta reiniciarlo automaticamente.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fileObserver?.stopWatching()
        stopSmbServer()
        RootUtils.removePortRedirect(prefs.internalSmbPort)
    }

    // ------------------------------------------------------------------
    private fun startBridge() {
        val rootOk = RootUtils.isRootAvailable()
        if (!rootOk) {
            updateServiceNotification("Error: se requiere acceso root (Magisk)")
            return
        }

        val redirected = RootUtils.ensurePortRedirect(prefs.internalSmbPort)
        if (!redirected) {
            updateServiceNotification("Error redirigiendo puerto 445. Revisar permisos root.")
            return
        }

        val started = startSmbServer()
        if (!started) {
            updateServiceNotification("Error iniciando servidor SMB interno")
            return
        }

        startWatchingFolder()
        updateServiceNotification("Vigilando carpeta compartida (${incomingDir.name})")
    }

    /**
     * Inicializa y arranca JFileServer usando la plantilla XML de assets,
     * reemplazando los placeholders con la configuracion del usuario.
     *
     * NOTA: la integracion exacta con las clases de org.filesys depende de
     * la version de la libreria; ver comentario en smb_config_template.xml.
     * Aca se deja el flujo de alto nivel (cargar XML -> construir
     * ServerConfiguration -> instanciar SMBServer -> startServer()).
     */
    private fun startSmbServer(): Boolean {
        return try {
            val template = assets.open("smb_config_template.xml")
                .bufferedReader().use { it.readText() }

            val xml = template
                .replace("{{HOST_NAME}}", "MINDRAYTAB")
                .replace("{{SMB_PORT}}", prefs.internalSmbPort.toString())
                .replace("{{SHARE_NAME}}", prefs.shareName)
                .replace("{{LOCAL_PATH}}", incomingDir.absolutePath)
                .replace("{{USER_NAME}}", prefs.shareUser)
                .replace("{{USER_PASSWORD}}", prefs.sharePassword)

            val configFile = File(filesDir, "smb_config_runtime.xml")
            configFile.writeText(xml)

            // --- Integracion con JFileServer (org.filesys) ---
            // Ejemplo de flujo tipico (ajustar nombres de clase segun la
            // version real de la libreria al compilar en Android Studio):
            //
            // val serverConfig = org.filesys.server.config.ServerConfiguration("MINDRAY")
            // val xmlLoader = org.filesys.server.config.xml.XMLServerConfiguration()
            // xmlLoader.loadConfiguration(configFile.absolutePath, serverConfig)
            // val smbServer = org.filesys.smb.server.SMBServer(serverConfig)
            // smbServer.startServer()
            // smbServerHandle = smbServer

            // Placeholder de referencia mientras se valida la API exacta:
            smbServerHandle = configFile.absolutePath
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun stopSmbServer() {
        // (smbServerHandle as? org.filesys.smb.server.SMBServer)?.shutdownServer(false)
        smbServerHandle = null
    }

    // ------------------------------------------------------------------
    private fun startWatchingFolder() {
        val mask = FileObserver.CLOSE_WRITE or FileObserver.MOVED_TO

        fileObserver = object : FileObserver(incomingDir, mask) {
            override fun onEvent(event: Int, path: String?) {
                if (path == null) return
                val file = File(incomingDir, path)
                if (!file.isFile) return
                if (!isAllowedExtension(file)) return
                queueFile(file)
            }
        }
        fileObserver?.startWatching()
    }

    private fun isAllowedExtension(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in setOf("jpg", "jpeg", "png", "bmp")
    }

    private fun queueFile(file: File) {
        synchronized(pendingFiles) {
            pendingFiles.add(file.absolutePath)
        }
        flushRunnable?.let { mainHandler.removeCallbacks(it) }
        flushRunnable = Runnable { flushPendingFiles() }
        mainHandler.postDelayed(flushRunnable!!, debounceMillis)
    }

    private fun flushPendingFiles() {
        val files: List<String>
        synchronized(pendingFiles) {
            files = pendingFiles.toList()
            pendingFiles.clear()
        }
        if (files.isEmpty()) return

        serviceScope.launch {
            val stableFiles = files.filter { isFileStable(File(it)) }
            if (stableFiles.isEmpty()) return@launch

            val examId = "EXAM_" + SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val exam = Exam(
                examId = examId,
                receivedAt = System.currentTimeMillis(),
                imagePaths = stableFiles.joinToString("|"),
                patientName = "Desconocido",
                thumbnailPath = stableFiles.firstOrNull()
            )

            AppDatabase.getInstance(applicationContext).examDao().insert(exam)

            mainHandler.post {
                NotificationHelper.notifyNewExam(
                    applicationContext,
                    exam.examId,
                    stableFiles.size
                )
            }
        }
    }

    /** Espera breve y compara tamano de archivo para confirmar que ya termino de escribirse. */
    private fun isFileStable(file: File): Boolean {
        return try {
            val size1 = file.length()
            Thread.sleep(1500)
            val size2 = file.length()
            size1 == size2 && size1 > 0
        } catch (e: Exception) {
            false
        }
    }

    // ------------------------------------------------------------------
    private fun buildServiceNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, MindrayApp.CHANNEL_ID_SERVICE)
            .setContentTitle("Mindray Receiver activo")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateServiceNotification(text: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIF_ID_SERVICE, buildServiceNotification(text))
    }

    companion object {
        private const val NOTIF_ID_SERVICE = 1001
    }
}

package com.sonocare.mindrayreceiver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.sonocare.mindrayreceiver.bridge.RootUtils
import com.sonocare.mindrayreceiver.bridge.SmbBridgeService
import com.sonocare.mindrayreceiver.data.AppDatabase
import com.sonocare.mindrayreceiver.data.Exam
import com.sonocare.mindrayreceiver.databinding.ActivityMainBinding
import com.sonocare.mindrayreceiver.services.DriveUploader
import com.sonocare.mindrayreceiver.services.EmailSender
import com.sonocare.mindrayreceiver.services.ImageAnalyzer
import com.sonocare.mindrayreceiver.services.ReportGenerator
import com.sonocare.mindrayreceiver.services.TelegramSender
import com.sonocare.mindrayreceiver.ui.ExamAdapter
import com.sonocare.mindrayreceiver.ui.ImageViewerActivity
import com.sonocare.mindrayreceiver.ui.SettingsActivity
import com.sonocare.mindrayreceiver.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs
    private lateinit var adapter: ExamAdapter
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)
        db = AppDatabase.getInstance(this)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.inflateMenu(R.menu.main_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_settings) {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            } else false
        }

        adapter = ExamAdapter(
            onView = { exam -> openViewer(exam) },
            onDrive = { exam -> uploadToDrive(exam) },
            onTelegram = { exam -> sendTelegram(exam) },
            onEmail = { exam -> sendEmail(exam) },
            onReport = { exam -> generateReport(exam) },
            onAnalyze = { exam -> analyzeExam(exam) },
        )
        binding.recyclerExams.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadExams() }

        requestNotificationPermissionIfNeeded()
        observeExams()
        checkRootAndStartBridge()
    }

    override fun onResume() {
        super.onResume()
        loadExams()
    }

    // ------------------------------------------------------------------
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100
                )
            }
        }
    }

    private fun checkRootAndStartBridge() {
        lifecycleScope.launch(Dispatchers.IO) {
            val rootOk = RootUtils.isRootAvailable()
            withContext(Dispatchers.Main) {
                if (!rootOk) {
                    binding.txtStatus.text = "⚠ Se requiere acceso root (Magisk). El bridge no puede iniciar."
                    Toast.makeText(this@MainActivity, "Otorgá permiso root a la app en Magisk", Toast.LENGTH_LONG).show()
                    return@withContext
                }
                binding.txtStatus.text = "● Bridge SMB activo — carpeta: ${prefs.shareName}"
                val serviceIntent = Intent(this@MainActivity, SmbBridgeService::class.java)
                ContextCompat.startForegroundService(this@MainActivity, serviceIntent)
            }
        }
    }

    private fun observeExams() {
        db.examDao().observeAll().observe(this) { exams ->
            adapter.submitList(exams)
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun loadExams() {
        lifecycleScope.launch(Dispatchers.IO) {
            val exams = db.examDao().getAll()
            withContext(Dispatchers.Main) {
                adapter.submitList(exams)
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    // ------------------------------------------------------------------
    private fun openViewer(exam: Exam) {
        val intent = Intent(this, ImageViewerActivity::class.java)
        intent.putExtra("exam_id", exam.examId)
        intent.putExtra("image_paths", exam.imagePaths)
        startActivity(intent)
    }

    private fun uploadToDrive(exam: Exam) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val uploader = DriveUploader(this@MainActivity, prefs)
                val link = uploader.uploadExam(exam.examId, exam.imageList())
                db.examDao().update(exam.copy(uploadedDrive = true, driveLink = link))
                toast("Subido a Drive: ${exam.examId}")
            } catch (e: Exception) {
                toast("Error Drive: ${e.message}")
            }
        }
    }

    private fun sendTelegram(exam: Exam) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                TelegramSender(prefs).sendExam(exam.examId, exam.imageList())
                db.examDao().update(exam.copy(sentTelegram = true))
                toast("Enviado por Telegram: ${exam.examId}")
            } catch (e: Exception) {
                toast("Error Telegram: ${e.message}")
            }
        }
    }

    private fun sendEmail(exam: Exam) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                EmailSender(prefs).sendExam(exam.examId, exam.imageList())
                db.examDao().update(exam.copy(sentEmail = true))
                toast("Enviado por Email: ${exam.examId}")
            } catch (e: Exception) {
                toast("Error Email: ${e.message}")
            }
        }
    }

    private fun generateReport(exam: Exam) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val file = ReportGenerator(this@MainActivity, prefs).generate(exam)
                db.examDao().update(exam.copy(reportGenerated = true, reportPath = file.absolutePath))
                toast("Informe generado: ${file.name}")
            } catch (e: Exception) {
                toast("Error generando informe: ${e.message}")
            }
        }
    }

    private fun analyzeExam(exam: Exam) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val notes = ImageAnalyzer().analyze(exam)
                db.examDao().update(exam.copy(analysisNotes = notes))
                toast("Analisis completado")
            } catch (e: Exception) {
                toast("Error en analisis: ${e.message}")
            }
        }
    }

    private suspend fun toast(msg: String) = withContext(Dispatchers.Main) {
        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
    }
}

package com.sonocare.mindrayreceiver.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.sonocare.mindrayreceiver.data.Exam
import com.sonocare.mindrayreceiver.databinding.ItemExamBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter para la lista de examenes. Cada boton delega la accion
 * correspondiente hacia los callbacks provistos por MainActivity.
 */
class ExamAdapter(
    private val onView: (Exam) -> Unit,
    private val onDrive: (Exam) -> Unit,
    private val onTelegram: (Exam) -> Unit,
    private val onEmail: (Exam) -> Unit,
    private val onReport: (Exam) -> Unit,
    private val onAnalyze: (Exam) -> Unit,
) : ListAdapter<Exam, ExamAdapter.ExamViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExamViewHolder {
        val binding = ItemExamBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExamViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExamViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExamViewHolder(private val binding: ItemExamBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(exam: Exam) {
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es")).format(Date(exam.receivedAt))
            binding.txtPatientInfo.text = "${exam.patientName} - ${exam.imageList().size} imagen(es)"
            binding.txtExamMeta.text = "Recibido: $dateStr\n${exam.examId}"

            exam.thumbnailPath?.let { binding.imgThumbnail.load(it) }

            binding.btnVer.setOnClickListener { onView(exam) }
            binding.btnDrive.setOnClickListener { onDrive(exam) }
            binding.btnTelegram.setOnClickListener { onTelegram(exam) }
            binding.btnEmail.setOnClickListener { onEmail(exam) }
            binding.btnInforme.setOnClickListener { onReport(exam) }
            binding.btnAnalisis.setOnClickListener { onAnalyze(exam) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Exam>() {
            override fun areItemsTheSame(oldItem: Exam, newItem: Exam) = oldItem.examId == newItem.examId
            override fun areContentsTheSame(oldItem: Exam, newItem: Exam) = oldItem == newItem
        }
    }
}

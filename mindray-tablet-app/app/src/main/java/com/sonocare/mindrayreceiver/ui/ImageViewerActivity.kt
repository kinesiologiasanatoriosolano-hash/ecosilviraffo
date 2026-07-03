package com.sonocare.mindrayreceiver.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.sonocare.mindrayreceiver.databinding.ActivityImageViewerBinding

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding
    private var images: List<String> = emptyList()
    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val raw = intent.getStringExtra("image_paths") ?: ""
        images = if (raw.isBlank()) emptyList() else raw.split("|")

        binding.btnPrev.setOnClickListener {
            if (images.isNotEmpty()) {
                index = (index - 1 + images.size) % images.size
                showCurrent()
            }
        }
        binding.btnNext.setOnClickListener {
            if (images.isNotEmpty()) {
                index = (index + 1) % images.size
                showCurrent()
            }
        }

        showCurrent()
    }

    private fun showCurrent() {
        if (images.isEmpty()) {
            binding.txtCounter.text = "Este examen no tiene imagenes"
            return
        }
        binding.imgFull.load(images[index])
        binding.txtCounter.text = "${index + 1} / ${images.size}"
    }
}

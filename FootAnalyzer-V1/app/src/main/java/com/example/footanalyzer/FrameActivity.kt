package com.example.footanalyzer

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class FrameActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton

    private var framesList: ArrayList<String> = arrayListOf()
    private var currentIndex = 0



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_frame)

        imageView = findViewById(R.id.frameImageView)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)

        framesList = intent.getStringArrayListExtra("frames_list") ?: arrayListOf()

        if (framesList.isNotEmpty()) {
            showImage(0)
        }

        btnPrev.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                showImage(currentIndex)
            }
        }

        btnNext.setOnClickListener {
            if (currentIndex < framesList.size - 1) {
                currentIndex++
                showImage(currentIndex)
            }
        }
    }

    private fun showImage(index: Int) {
        val imgFile = File(framesList[index])
        if (imgFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            imageView.setImageBitmap(bitmap)

            // Solo si realmente necesitas limitar el tamaño, puedes forzarlo así (opcional)
            imageView.layoutParams.width = (resources.displayMetrics.widthPixels * 0.8).toInt()
            imageView.layoutParams.height = RelativeLayout.LayoutParams.WRAP_CONTENT
            imageView.requestLayout()
        }
    }


}

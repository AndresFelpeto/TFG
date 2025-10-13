package com.example.footanalyzer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity


class HomeActivity : AppCompatActivity() {

    private lateinit var runAlyzerController: RunAlyzerController

    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        runAlyzerController = RunAlyzerController(
            context = this,
            fragmentManager = supportFragmentManager,
            onSuccess = { videoPath, angles, zipPath ->
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("video_path", videoPath)
                    putExtra("angle_left_foot", angles.first)
                    putExtra("angle_right_foot", angles.second)
                    putExtra("frames_zip_path", zipPath)
                }
                startActivity(intent)
            },
            onError = { message ->
                val intent = Intent(this, ErrorActivity::class.java)
                intent.putExtra("error_message", message)
                startActivity(intent)
            }
        )

        val videoPickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                runAlyzerController.handleVideoSelectionResult(result)
            }

        runAlyzerController.setVideoLauncher(videoPickerLauncher)

        findViewById<ImageButton>(R.id.botonSeleccionarVideo).setOnClickListener {
            runAlyzerController.launchVideoPicker()
        }

        findViewById<Button>(R.id.boton_info).setOnClickListener {
            val intent = Intent(this, ExplainingActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

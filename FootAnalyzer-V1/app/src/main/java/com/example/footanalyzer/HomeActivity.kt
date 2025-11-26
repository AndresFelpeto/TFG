package com.example.footanalyzer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.window.OnBackInvokedDispatcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
class HomeActivity : AppCompatActivity() {

    private lateinit var runAlyzerController: RunAlyzerController
    private var resultActivityLaunched = false
    var isActive = true

    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        runAlyzerController = RunAlyzerController(
            context = this,
            fragmentManager = supportFragmentManager,
            onSuccess = { videoPath, angles, zipPath ->
                if (!resultActivityLaunched) {
                    resultActivityLaunched = true
                    val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("video_path", videoPath)
                    putExtra("angle_left_foot", angles.first)
                    putExtra("angle_right_foot", angles.second)
                    putExtra("frames_zip_path", zipPath)
                    }
                    startActivity(intent)
                }
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

    override fun onResume() {
        super.onResume()
        resultActivityLaunched = false
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        isActive=true
    }

    override fun onStop() {
        super.onStop()
        isActive = false
        runAlyzerController.cancelAnalysis()
        LoadingDialogManager.cancel()
    }


    override fun getOnBackInvokedDispatcher(): OnBackInvokedDispatcher {
        runAlyzerController.cancelAnalysis()
        return super.getOnBackInvokedDispatcher()
    }

    fun cancelAnalysisDesdeDialog() {
        runAlyzerController.cancelAnalysis()
    }

}

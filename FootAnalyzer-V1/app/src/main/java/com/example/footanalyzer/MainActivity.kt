package com.example.footanalyzer

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var videoExtractor: VideoExtractor
    private var loadingDialog: LoadingDialogFragment? = null
    private var videoBytes: ByteArray? = null

    private val handler = Handler(Looper.getMainLooper())
    private var polling = false
    private val pollingIntervalMs = 3000L

    private lateinit var serverCommunicator: ServerCommunicator
    private val serverURL = "http://192.168.1.102:5000"

    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serverCommunicator = ServerCommunicator(this)

        val videoPickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                videoExtractor.handleResult(result)
            }

        videoExtractor = VideoExtractor(
            context = this,
            launcher = videoPickerLauncher,
            onVideoSelected = { uri: Uri ->

                loadingDialog?.dismissAllowingStateLoss()
                loadingDialog = LoadingDialogFragment()

                val existingDialog = supportFragmentManager.findFragmentByTag("loading")
                if (existingDialog != null) {
                    (existingDialog as? DialogFragment)?.dismissAllowingStateLoss()
                }

                loadingDialog?.show(supportFragmentManager, "loading")
                LoadingDialogManager.startProgress(loadingDialog!!)

                serverCommunicator.sendVideoToPC(uri, serverURL) { success ->
                    if (success) {
                        startPollingRequest()
                    } else {
                        runOnUiThread {
                            LoadingDialogManager.taskComplete()
                            val intent = Intent(this, ErrorActivity::class.java)
                            intent.putExtra("error_message", "Error al subir el video.")
                            startActivity(intent)
                        }
                    }
                }
            },
            onVideoInvalid = { errorMsg: String ->
                val intent = Intent(this, ErrorActivity::class.java)
                intent.putExtra("error_message", errorMsg)
                startActivity(intent)
            }
        )

        val botonSeleccionarVideo = findViewById<ImageButton>(R.id.botonSeleccionarVideo)
        botonSeleccionarVideo.setOnClickListener {
            videoExtractor.selectVideoFromGallery()
        }
    }

    private fun startPollingRequest() {
        polling = true
        fun tick() {
            if (!polling) return

            serverCommunicator.requestVideoFromPC(
                serverUrl = serverURL,
                onProgress = { remaining ->
                    val progreso = 100 - remaining.coerceIn(0, 100)
                    Log.d("Client", "Progreso actual: $progreso%")
                    LoadingDialogManager.updateProgress(progreso)

                    // Vuelve a hacer el request luego de un intervalo
                    handler.postDelayed({ tick() }, pollingIntervalMs)
                },
                onVideo = { bytes ->
                    polling = false
                    runOnUiThread {
                        videoBytes = bytes
                        if (videoBytes != null) {
                            Log.d("App", "Vídeo recibido con tamaño: ${videoBytes!!.size}")
                            val videoFile = File(cacheDir, "resultado_video.mp4")
                            videoFile.writeBytes(videoBytes!!)

                            // Obtener ángulos del servidor
                            serverCommunicator.requestResultsFromServer(serverURL) { result ->
                                runOnUiThread {
                                    if (result != null) {
                                        val json = JSONObject(result)
                                        val leftFoot = json.optInt("angle_left_foot", -1)
                                        val rightFoot = json.optInt("angle_right_foot", -1)

                                        if (leftFoot != -1 && rightFoot != -1) {
                                            val intent = Intent(this, ResultActivity::class.java)
                                            intent.putExtra("video_path", videoFile.absolutePath)
                                            intent.putExtra("angle_left_foot", leftFoot)
                                            intent.putExtra("angle_right_foot", rightFoot)
                                            startActivity(intent)
                                        } else {
                                            showError("No se recibieron los resultados del servidor.")
                                        }
                                    } else {
                                        showError("Fallo en la conexión al recibir resultados.")
                                    }
                                    LoadingDialogManager.taskComplete()
                                }
                            }
                        } else {
                            showError("Fallo al recibir el video del servidor.")
                            LoadingDialogManager.taskComplete()
                        }
                    }
                },
                onError = { err ->
                    Log.e("Client", "Error en polling /request: ${err?.message}")
                    // Reintenta en el próximo ciclo
                    handler.postDelayed({ tick() }, pollingIntervalMs)
                }
            )
        }

        // Inicia el primer ciclo de polling
        handler.post { tick() }
    }

    private fun showError(message: String) {
        runOnUiThread {
            polling = false
            LoadingDialogManager.taskComplete()
            val intent = Intent(this, ErrorActivity::class.java)
            intent.putExtra("error_message", message)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        polling = false
        handler.removeCallbacksAndMessages(null)
    }
}

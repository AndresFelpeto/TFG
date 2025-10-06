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
    private val serverURL = "https://andrestfg.es"

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

                serverCommunicator.sendVideoToServer(uri, serverURL) { success ->
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

            serverCommunicator.requestVideoFromServer(
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
                            requestResults() { leftFoot, rightFoot ->
                                requestZip(){ zipFile->
                                    runOnUiThread {
                                        if (leftFoot != -1.0 && rightFoot != -1.0) {
                                            val intent = Intent(this, ResultActivity::class.java)
                                            intent.putExtra("video_path", videoFile.absolutePath)
                                            intent.putExtra("angle_left_foot", leftFoot)
                                            intent.putExtra("angle_right_foot", rightFoot)
                                            intent.putExtra("frames_zip_path", zipFile.absolutePath)
                                            startActivity(intent)
                                        } else {
                                            showError("No se recibieron los resultados del servidor.")
                                        }
                                        LoadingDialogManager.taskComplete()
                                    }
                                }
                            }


                        } else {
                            showError("Fallo al recibir el video del servidor.")
                            LoadingDialogManager.taskComplete()
                        }
                    }
                },
                onError = { err ->
                    polling = false
                    runOnUiThread {
                        LoadingDialogManager.taskComplete()
                        val intent = Intent(this, ErrorActivity::class.java)
                        intent.putExtra("error_message", err?.message ?: "Error desconocido al consultar el servidor.")
                        startActivity(intent)
                    }
                }

            )
        }
        handler.post { tick() }
    }

    private fun requestResults(onSuccess: (leftFoot: Double, rightFoot: Double) -> Unit) {
        serverCommunicator.requestResultsFromServer(serverURL) { result ->
            if (result != null) {
                var json = JSONObject(result)
                val leftFoot = json.optDouble("angle_left_foot", -1.0)
                val rightFoot = json.optDouble("angle_right_foot", -1.0)
                onSuccess(leftFoot, rightFoot)
            } else {
                runOnUiThread {
                    showError("Fallo en la conexión al recibir resultados.")
                    LoadingDialogManager.taskComplete()
                }
            }
        }
    }


    private fun requestZip(onSuccess: (zipFile: File) -> Unit){
        serverCommunicator.requestFramesZipFromServer(
            serverUrl = serverURL,
            onZip = { zipBytes ->
                val zipFile = File(cacheDir, "frames_seleccionados.zip")
                zipFile.writeBytes(zipBytes)
                Log.d("App", "ZIP guardado en: ${zipFile.absolutePath}")
                onSuccess(zipFile)
            },
            onError = { err ->
                runOnUiThread {
                    showError("Error al recibir ZIP: ${err?.message}")
                    LoadingDialogManager.taskComplete()
                }
            }
        )
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

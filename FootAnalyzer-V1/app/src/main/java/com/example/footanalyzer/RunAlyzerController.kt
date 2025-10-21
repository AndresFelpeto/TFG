package com.example.footanalyzer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentManager
import org.json.JSONObject
import java.io.File

class RunAlyzerController(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val onSuccess: (videoPath: String, angles: Pair<Double, Double>, zipPath: String) -> Unit,
    private val onError: (String) -> Unit
) {
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private lateinit var videoExtractor: VideoExtractor
    private lateinit var serverCommunicator: ServerCommunicator

    private val serverUrl = "https://andrestfg.es"
    private val handler = Handler(Looper.getMainLooper())
    private val pollingIntervalMs = 3000L
    private var polling = false

    private var videoPath: String?=null
    private var angles: Pair<Double, Double>?=null
    private var zipPath: String?=null

    init {
        serverCommunicator = ServerCommunicator(context)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setVideoLauncher(launcher: ActivityResultLauncher<Intent>) {
        this.launcher = launcher
        videoExtractor = VideoExtractor(
            context = context,
            launcher = launcher,
            onVideoSelected = { uri -> startAnalysis(uri) },
            onVideoInvalid = { errorMsg -> onError(errorMsg) }
        )
    }

    fun launchVideoPicker() {
        videoExtractor.selectVideoFromGallery()
    }

    fun handleVideoSelectionResult(result: ActivityResult) {
        videoExtractor.handleResult(result)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startAnalysis(uri: Uri) {
        LoadingDialogManager.show(fragmentManager)
        LoadingDialogManager.setUploadingText()
        serverCommunicator.sendVideoToServer(uri, serverUrl) { success ->
            if (success) {
                LoadingDialogManager.setAnalyzingText()
                startPolling()
            } else {
                stopLoading()
                onError("Error al subir el video.")
            }
        }
    }

    private fun startPolling() {
        polling = true
        fun tick() {
            if (!polling) return
            serverCommunicator.requestVideoFromServer(
                serverUrl = serverUrl,
                onProgress = { remaining ->
                    val progreso = 100 - remaining.coerceIn(0, 100)
                    Log.d("Client", "Progreso actual: $progreso%")
                    LoadingDialogManager.updateProgress(progreso)
                    handler.postDelayed({ tick() }, pollingIntervalMs)
                },
                onVideo = { bytes ->
                    polling = false
                    val videoFile = File(context.cacheDir, "resultado_video.mp4")
                    videoFile.writeBytes(bytes)
                    videoPath=videoFile.absolutePath
                    fetchResults()
                    fetchZip()
                },
                onError = { err ->
                    polling = false
                    stopLoading()
                    onError(err?.message ?: "Error desconocido al consultar el servidor.")
                }
            )
        }
        handler.post { tick() }
    }

    private fun fetchResults() {
        serverCommunicator.requestResultsFromServer(serverUrl) { result ->
            if (result != null) {
                val json = JSONObject(result)
                val leftFoot = json.optDouble("angle_left_foot", -1.0)
                val rightFoot = json.optDouble("angle_right_foot", -1.0)
                if (leftFoot != -1.0 && rightFoot != -1.0) {
                    angles= Pair(leftFoot,rightFoot)
                    tryFinishAnalysis()
                } else {
                    stopLoading()
                    onError("No se recibieron los resultados del servidor.")
                }
            } else {
                stopLoading()
                onError("Fallo en la conexión al recibir resultados.")
            }
        }
    }

    private fun fetchZip() {
        serverCommunicator.requestFramesZipFromServer(
            serverUrl = serverUrl,
            onZip = { zipBytes ->
                val zipFile = File(context.cacheDir, "frames_seleccionados.zip")
                zipFile.writeBytes(zipBytes)
                zipPath=zipFile.absolutePath
                tryFinishAnalysis()
            },
            onError = { err ->
                stopLoading()
                onError("Error al recibir ZIP: ${err?.message}")
            }
        )
    }

    private fun tryFinishAnalysis() {
        val video = videoPath
        val a = angles
        val zip = zipPath

        if (video != null && a != null && zip != null) {
            stopLoading()
            onSuccess(video, a, zip)
        }
    }

    private fun stopLoading() {
        polling = false
        handler.removeCallbacksAndMessages(null)
        LoadingDialogManager.taskComplete()
    }
}

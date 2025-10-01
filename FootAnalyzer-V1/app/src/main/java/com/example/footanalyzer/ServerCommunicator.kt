package com.example.footanalyzer

import android.content.Context
import java.util.concurrent.TimeUnit
import android.net.Uri
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream


class ServerCommunicator(private val context: Context) {


    var processId: String? = null
    val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(210, TimeUnit.SECONDS)
        .callTimeout(210, TimeUnit.SECONDS)
        .build()


    fun sendVideoToPC(videoUri: Uri, serverUrl: String, onProcessIdReady: (Boolean) -> Unit) {
        val inputStream: InputStream? = context.contentResolver.openInputStream(videoUri)
        if (inputStream == null) {
            Log.d("VideoSender", "No se pudo abrir el archivo")
            onProcessIdReady(false)  // avisa error
            return
        }

        val videoBytes = inputStream.readBytes()
        inputStream.close()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "video",
                "video.mp4",
                RequestBody.create("video/mp4".toMediaTypeOrNull(), videoBytes)
            )
            .build()

        val request = Request.Builder()
            .url("$serverUrl/upload_video")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("VideoSender", "Error al enviar el video: ${e.message}")
                onProcessIdReady(false) // avisa error
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onProcessIdReady(false)
                    return
                }
                val jsonString = response.body?.string()
                jsonString?.let {
                    val jsonObject = JSONObject(it)
                    processId = jsonObject.optString("process_id", null)
                    Log.d("VideoSender", "Process ID guardado: $processId")
                    onProcessIdReady(true) // éxito
                } ?: onProcessIdReady(false)
            }
        })
    }


    fun requestVideoFromPC(
        serverUrl: String,
        onProgress: (remaining: Int) -> Unit,
        onVideo: (videoBytes: ByteArray) -> Unit,
        onError: (Throwable?) -> Unit = {}
    ) {
        val pid = processId
        if (pid.isNullOrEmpty()) {
            onError(IllegalStateException("processId nulo"))
            return
        }

        val request = Request.Builder()
            .url("$serverUrl/request_video?process_id=$pid")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("VideoSender", "Error al solicitar el video: ${e.message}")
                onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val contentType = it.header("Content-Type") ?: ""
                    if (contentType.contains("video")) {
                        val bodyBytes = it.body?.bytes()
                        if (bodyBytes != null) {
                            Log.d("VideoSender", "Video recibido (${bodyBytes.size} bytes)")
                            onVideo(bodyBytes)
                        } else {
                            onError(IOException("Body de vídeo vacío"))
                        }
                    } else {
                        val body = it.body?.string()
                        if (body.isNullOrEmpty()) {
                            onError(IOException("Respuesta JSON vacía"))
                            return 
                        }
                        try {
                            val json = JSONObject(body)
                            val status = json.optString("status", "error")
                            if (status == "processing") {
                                val remaining = json.optInt("remaining", 1)
                                onProgress(remaining)
                            } else{
                                onError(IOException(json.optString("message", "Error servidor")))

                            }
                        } catch (e: Exception) {
                            onError(e)
                        }
                    }
                }
            }
        })
    }

    fun requestResultsFromServer(serverUrl: String, callback: (String?) -> Unit) {
        val request = Request.Builder()
            .url("$serverUrl/get_results?process_id=$processId")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("VideoSender", "Error al recibir los resultados: ${e.message}")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string()
                Log.d("VideoSender", "Resultados recibidos: $json")
                callback(json)
            }
        })
    }

    fun requestFramesZipFromServer(
        serverUrl: String,
        onZip: (zipBytes: ByteArray) -> Unit,
        onError: (Throwable?) -> Unit = {}
    ) {
        val pid = processId
        if (pid.isNullOrEmpty()) {
            onError(IllegalStateException("processId nulo"))
            return
        }

        val request = Request.Builder()
            .url("$serverUrl/get_frames_zip?process_id=$pid")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("VideoSender", "Error al recibir ZIP: ${e.message}")
                onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val contentType = it.header("Content-Type") ?: ""
                    if (contentType.contains("zip")) {
                        val bodyBytes = it.body?.bytes()
                        if (bodyBytes != null) {
                            Log.d("VideoSender", "ZIP recibido (${bodyBytes.size} bytes)")
                            onZip(bodyBytes)
                        } else {
                            onError(IOException("Body de ZIP vacío"))
                        }
                    } else {
                        val body = it.body?.string()
                        onError(IOException("Respuesta inesperada: $body"))
                    }
                }
            }
        })
    }


}

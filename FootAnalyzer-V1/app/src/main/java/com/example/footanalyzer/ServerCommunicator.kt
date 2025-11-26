package com.example.footanalyzer

import android.content.Context
import java.util.concurrent.TimeUnit
import android.net.Uri
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream


class ServerCommunicator(private val context: Context){
    var token: String? = null
    val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(210, TimeUnit.SECONDS)
        .callTimeout(210, TimeUnit.SECONDS)
        .build()

    fun sendVideoToServer(videoUri: Uri, serverUrl: String, onTokenReady: (Boolean) -> Unit) {
        val inputStream: InputStream? = context.contentResolver.openInputStream(videoUri)
        if (inputStream == null) {
            Log.d("VideoSender", "No se pudo abrir el archivo")
            onTokenReady(false)
            return
        }

        val videoBytes = inputStream.readBytes()
        inputStream.close()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "video",
                "video.mp4",
                videoBytes.toRequestBody("video/mp4".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("$serverUrl/upload_video")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onTokenReady(false)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onTokenReady(false)
                    return
                }
                val jsonString = response.body?.string()
                jsonString?.let {
                    val jsonObject = JSONObject(it)
                    token = jsonObject.optString("token", null)
                    onTokenReady(!token.isNullOrEmpty())
                } ?: run {
                    onTokenReady(false)
                }
            }
        })
    }

    private fun buildAuthorizedRequest(baseUrl: String): Request? {
        val t = token ?: return null
        val urlToken = "$baseUrl?token=$t"

        return Request.Builder()
            .url(urlToken)
            .get()
            .build()
    }


    fun requestVideoFromServer(serverUrl: String, onProgress: (Int) -> Unit, onVideo: (ByteArray) -> Unit, onError: (Throwable?) -> Unit = {}) {
        val request = buildAuthorizedRequest("$serverUrl/request_video")
        if (request == null) {
            onError(IllegalStateException("Token no disponible"))
            return
        }

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onError(e)
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val contentType = it.header("Content-Type") ?: ""
                    if (contentType.contains("video")) {
                        val bytes = it.body?.bytes()
                        if (bytes != null) onVideo(bytes)
                        else onError(IOException("Video vacío"))
                    } else {
                        val json = JSONObject(it.body?.string() ?: "")
                        if (json.optString("status") == "processing") {
                            onProgress(json.optInt("remaining", 1))
                        } else {
                            onError(IOException(json.optString("message", "Error desconocido")))
                        }
                    }
                }
            }
        })
    }

    fun requestResultsFromServer(serverUrl: String, onResult: (String) -> Unit, onError: (Throwable?) -> Unit = {}) {
        val request = buildAuthorizedRequest("$serverUrl/get_results") ?: return onError(IllegalStateException("Token no disponible"))
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onError(e)
            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { onResult(it) } ?: onError(IOException("Respuesta vacía"))
            }
        })
    }

    fun requestFramesZipFromServer(serverUrl: String, onZip: (ByteArray) -> Unit, onError: (Throwable?) -> Unit = {}) {
        val request = buildAuthorizedRequest("$serverUrl/get_frames_zip") ?: return onError(IllegalStateException("Token no disponible"))
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onError(e)
            override fun onResponse(call: Call, response: Response) {
                val contentType = response.header("Content-Type") ?: ""
                if (contentType.contains("zip")) {
                    val bytes = response.body?.bytes()
                    if (bytes != null) onZip(bytes) else onError(IOException("ZIP vacío"))
                } else {
                    onError(IOException("Respuesta inesperada: ${response.body?.string()}"))
                }
            }
        })
    }
}

package com.example.footanalyzer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher

class VideoExtractor(
    private val context: Context,
    private val launcher: ActivityResultLauncher<Intent>,
    private val onVideoSelected: (Uri) -> Unit,
    private val onVideoInvalid: (String) -> Unit
) {

    fun selectVideoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "video/*"
        launcher.launch(intent)
    }

    fun handleResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val videoUri = result.data?.data
            videoUri?.let {
                val durationSec = getVideoDuration(it)
                if (durationSec in 10..30) {
                    onVideoSelected(it)
                } else {
                    onVideoInvalid("El video debe durar entre 10 y 30 segundos")
                }
            }
        }
    }

    private fun getVideoDuration(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            durationMs / 1000
        } finally {
            retriever.release()
        }
    }
}

package com.example.footanalyzer

import android.os.Handler
import android.os.Looper

object LoadingDialogManager {

    private var progress = 0
    private var isTaskComplete = false
    private val handler = Handler(Looper.getMainLooper())
    private var dialogFragment: LoadingDialogFragment? = null

    // Reiniciar progreso
    private fun resetProgress() {
        progress = 0
        isTaskComplete = false
    }

    fun startProgress(dialog: LoadingDialogFragment) {
        dialogFragment = dialog
        resetProgress()
        dialogFragment?.updateProgress(0)
    }

    fun updateProgress(value: Int) {
        progress = value.coerceIn(0, 100)
        handler.post {
            dialogFragment?.updateProgress(progress)
        }
    }

    fun taskComplete() {
        isTaskComplete = true
        updateProgress(100)
        handler.post {
            if (dialogFragment?.isAdded == true) {
                dialogFragment?.dismissAllowingStateLoss()
                dialogFragment=null
            }
        }
    }
}

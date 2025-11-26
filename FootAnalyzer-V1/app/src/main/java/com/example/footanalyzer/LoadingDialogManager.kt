package com.example.footanalyzer

import android.os.Handler
import android.os.Looper
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

object LoadingDialogManager {

    private var progress = 0
    private var isTaskComplete = false
    private val handler = Handler(Looper.getMainLooper())
    private var dialogFragment: LoadingDialogFragment? = null
    private var isActive = true

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

    fun show(fragmentManager: FragmentManager) {

        dialogFragment?.dismissAllowingStateLoss()
        dialogFragment = LoadingDialogFragment()

        fragmentManager.findFragmentByTag("loading")?.let {
            (it as? DialogFragment)?.dismissAllowingStateLoss()
        }

        dialogFragment?.show(fragmentManager, "loading")
        startProgress(dialogFragment!!)
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

    fun setUploadingText() {
        handler.post {
            dialogFragment?.updateProgressText("Subiendo video (1/2)")
        }
    }

    fun setAnalyzingText() {
        handler.post {
            dialogFragment?.updateProgressText("Analizando (2/2)")
        }
    }

    fun cancel() {
        isActive = false
        handler.removeCallbacksAndMessages(null)
        dialogFragment?.dismissAllowingStateLoss()
        dialogFragment = null
    }


}

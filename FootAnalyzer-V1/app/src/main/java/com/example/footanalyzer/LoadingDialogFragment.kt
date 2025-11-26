package com.example.footanalyzer

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment
import android.widget.TextView
import androidx.activity.OnBackPressedCallback

class LoadingDialogFragment : DialogFragment() {

    private var progressBar: ProgressBar? = null
    private var progressTextView: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.dialog_loading, container, false)
        isCancelable = true
        progressBar = view.findViewById(R.id.progressBar)
        progressTextView = view.findViewById(R.id.progressText)
        return view
    }
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawableResource(R.color.background)
        dialog?.setCanceledOnTouchOutside(false)
    }


    fun updateProgress(value: Int) {
        progressBar?.progress = value
    }

    fun updateProgressText(text: String) {
        progressTextView?.text = text
    }

    override fun onResume() {
        super.onResume()
        dialog?.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                LoadingDialogManager.cancel()
                (activity as? HomeActivity)?.cancelAnalysisDesdeDialog()
                dismissAllowingStateLoss()
                true
            } else false
        }
    }

}

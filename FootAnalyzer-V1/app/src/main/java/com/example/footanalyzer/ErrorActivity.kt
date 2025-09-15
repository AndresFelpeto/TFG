package com.example.footanalyzer

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ErrorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error)

        val errorConnection= intent.getStringExtra("error_message") ?: "Fallo en la conexión"
        val errorTextView: TextView = findViewById(R.id.errorTextView)
        errorTextView.text = errorConnection
    }
}
package com.example.footanalyzer

data class ExplainingContent(
    val text: String,
    val image: Int? = null,
    val video: Int? = null // o String? si lo cargas por ruta/URI
)

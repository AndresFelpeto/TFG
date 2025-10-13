package com.example.footanalyzer

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class ExplainingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: TextPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explaining)

        val pages = listOf(
            "Existen 3 tipos de pisada en los corredores",

            "<b>Pisada Pronadora</b><br>El pie tiende a inclinarse hacia adentro durante el apoyo.<br>Es común, pero un exceso de pronación puede causar lesiones si no se corrige con calzado adecuado.",

            "<b>Pisada Neutra</b><br>El pie impacta de forma equilibrada, apoyando de manera uniforme.<br>Es el tipo de pisada más eficiente y menos propenso a causar lesiones.",

            "<b>Pisada Supinadora</b><br>El pie se inclina hacia afuera al apoyar.<br>Es menos común, y puede estar asociada a rigidez en el pie y sobrecarga en zonas externas.",
            "Para analizar un video tendrás que seguir los siguientes pasos",

            "Seleccionar un video de tu galería donde se vea tu pisada claramente.",

            "El video debe durar entre 10 y 30 segundos",

            "El video será enviado al servidor para ser analizado",

            "Luego de que termine la carga, podrás ver los resultados del análisis",

            "Además, es posible ver imagenes de la pisada y guardar el video generado en tu galería"
        )


        viewPager = findViewById(R.id.viewPager)
        adapter = TextPagerAdapter(pages)
        viewPager.adapter = adapter

        findViewById<Button>(R.id.boton_izquierda).setOnClickListener {
            if (viewPager.currentItem > 0) viewPager.currentItem -= 1
        }

        findViewById<Button>(R.id.boton_derecha).setOnClickListener {
            if (viewPager.currentItem < pages.size - 1) viewPager.currentItem += 1
        }
    }
}

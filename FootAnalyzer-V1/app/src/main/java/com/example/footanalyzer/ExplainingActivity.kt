package com.example.footanalyzer

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class ExplainingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ExplainingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explaining)

        val pages = listOf(
            ExplainingContent("Existen 3 tipos de pisada en los corredores",null),

            ExplainingContent("<b>Pisada Pronadora</b><br>El pie rota hacia dentro en el momento de la pisada.<br>Es común, pero un exceso de pronación puede causar lesiones si no se corrige con calzado adecuado.",R.drawable.pisada_pronada),

            ExplainingContent("<b>Pisada Neutra</b><br>El pie impacta de forma equilibrada, apoyando de manera uniforme.<br>Es menos probable sufrir lesiones, debido que los pesos se distribuyen de forma equilibrada por todo el pie",R.drawable.pisada_neutra),

            ExplainingContent("<b>Pisada Supinadora</b><br>El pie se inclina hacia afuera al apoyar.<br>Es menos común, y puede estar asociada a rigidez en el pie y sobrecarga en zonas externas.",R.drawable.pisada_supina),

            ExplainingContent("Para analizar un video tendrás que seguir los siguientes pasos",null),

            ExplainingContent("<b>1</b><br>Grabar un video corriendo en la cinta a una velocidad entre 8 y 10 km/h.<br> Debe ser grabado desde atrás, con el movil desde el suelo. El suelo debe estar recto en el eje &quot;x&quot; y el cuerpo tiene que verse completo, como se ve en el video.", null,R.raw.video_explaining),

            ExplainingContent("<b>2</b><br>El video debe durar entre 10 y 30 segundos",R.drawable.duracion_video),

            ExplainingContent("<b>3</b><br>Seleccionar el video desde tu galería presionando el símbolo &quot;+&quot;.", R.drawable.menu_inicio),

            ExplainingContent("<b>4</b><br>El video será enviado al servidor para ser analizado",R.drawable.analisis),

            ExplainingContent("<b>5</b><br>Luego de que termine la carga, podrás ver los resultados del análisis", R.drawable.result_activity),

            ExplainingContent("Además, es posible ver imagenes de la pisada y guardar el video generado en tu galería",R.drawable.result_activity_2)
        )


        viewPager = findViewById(R.id.viewPager)
        adapter = ExplainingAdapter(pages)
        viewPager.adapter = adapter

        findViewById<Button>(R.id.boton_izquierda).setOnClickListener {
            if (viewPager.currentItem > 0) viewPager.currentItem -= 1
        }

        findViewById<Button>(R.id.boton_derecha).setOnClickListener {
            if (viewPager.currentItem < pages.size - 1) viewPager.currentItem += 1
        }
    }
}

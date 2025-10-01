package com.example.footanalyzer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import org.json.JSONException
import android.widget.Button
import android.widget.Toast
import android.provider.MediaStore
import android.content.ContentValues
import android.content.Intent
import android.os.Environment
import java.io.File
import java.io.OutputStream
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView

class ResultActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val rootView = findViewById<View>(R.id.root_layout2) // ID del layout raíz
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_result)
        rootView.startAnimation(fadeIn)

        val valor_izquierda = intent.getIntExtra("angle_left_foot",0)
        val valor_derecha = intent.getIntExtra("angle_right_foot",0)
        val videoPath = intent.getStringExtra("video_path")
        val zipPath = intent.getStringExtra("frames_zip_path")
        var allFrames: List<File> = emptyList()

        val indicator_hacia_abajo = findViewById<ImageView>(R.id.marcador_hacia_abajo)
        val indicator_hacia_arriba = findViewById<ImageView>(R.id.marcador_hacia_arriba)
        val bar = findViewById<View>(R.id.tipos_pisada)

        try {
            bar.post {
                val minValor = 0.0
                val maxValor = 10.5
                val percent_izquierda = (valor_izquierda - minValor) / (maxValor - minValor)
                val offset_izquierda = percent_izquierda * bar.width
                val percent_derecha = (valor_derecha - minValor) / (maxValor - minValor)
                val offset_derecha = percent_derecha * bar.width


                indicator_hacia_abajo.translationX = offset_izquierda.toFloat() - (indicator_hacia_abajo.width / 2)
                indicator_hacia_arriba.translationX = offset_derecha.toFloat() - (indicator_hacia_arriba.width / 2)
            }
            val textView_izquierda = findViewById<TextView>(R.id.textView)
            if(valor_izquierda>7){
                textView_izquierda.text = "Pronador"
            }
            else if(valor_izquierda<3.5){
                textView_izquierda.text = "Supinador"
            }
            else{
                textView_izquierda.text = "Neutro"
            }
            val textView_derecha = findViewById<TextView>(R.id.textView2)
            if(valor_derecha>7){
                textView_derecha.text = "Pronador"
            }
            else if(valor_derecha<3.5){
                textView_derecha.text = "Supinador"
            }
            else{
                textView_derecha.text = "Neutro"
            }
        } catch (e: JSONException) {
            Log.d("ResultActivity", "Error al parsear el JSON: ${e.message}")
            val resultTextView: TextView = findViewById(R.id.textView)
            resultTextView.text = "Error al procesar primer resultado"

            val resultTextView2: TextView = findViewById(R.id.textView2)
            resultTextView2.text = "Error al procesar segundo resultado"
        }

        if (zipPath != null) {
            allFrames = extractFramesFromZip(zipPath)
        }
        findViewById<Button>(R.id.verPisadaIzquierda).setOnClickListener {
            val leftFrames = allFrames.filter { it.name.startsWith("izquierda") }
            val intent = Intent(this, FrameActivity::class.java).apply {
                putStringArrayListExtra(
                    "frames_list",
                    ArrayList(leftFrames.map { it.absolutePath })
                )
            }
            startActivity(intent)
        }

        findViewById<Button>(R.id.verPisadaDerecha).setOnClickListener {
            val rightFrames = allFrames.filter { it.name.startsWith("derecha") }
            val intent = Intent(this, FrameActivity::class.java)
            intent.putStringArrayListExtra("frames_list", ArrayList(rightFrames.map { it.absolutePath }))
            startActivity(intent)
        }

        val botonGuardar: Button = findViewById(R.id.botonGuardarVideoEnGaleria)
        botonGuardar.setOnClickListener {
            if (videoPath != null) {
                guardarVideoEnGaleria(this, videoPath)
            } else {
                Toast.makeText(this, "No se encontro el video", Toast.LENGTH_SHORT).show()
            }
        }
        Log.d("ResultActivity", "ResultActivity lanzada correctamente")
    }

    private fun guardarVideoEnGaleria(context: Context, videoPath: String) {
        val videoBytes: ByteArray? =  File(videoPath).readBytes()

        val resolver = context.contentResolver
        val videoCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val nombreArchivo = "video_runAlyze_${System.currentTimeMillis()}.mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, nombreArchivo)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/FootAnalyzer")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val videoUri = resolver.insert(videoCollection, contentValues)

        if (videoUri != null) {
            try {
                val outputStream: OutputStream? = resolver.openOutputStream(videoUri)
                outputStream?.write(videoBytes)
                outputStream?.close()

                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(videoUri, contentValues, null, null)

                Toast.makeText(context, "Video guardado en la galeria", Toast.LENGTH_SHORT).show()
                Log.d("ResultActivity", "Video guardado en la galeria")

            } catch (e: Exception) {
                Log.e("ResultActivity", "Error al guardar el video: ${e.message}")
                Toast.makeText(context, "Error al guardar el video", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("ResultActivity", "No se pudo guardar el video")
            Toast.makeText(context, "No se pudo guardar el video", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractFramesFromZip(zipPath: String): List<File> {
        val outputDir = File(cacheDir, "frames_unzip")
        if (!outputDir.exists()) outputDir.mkdirs()

        val frameFiles = mutableListOf<File>()
        val zipFile = java.util.zip.ZipFile(zipPath)

        zipFile.use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!entry.isDirectory && entry.name.endsWith(".jpg")) {
                    val outFile = File(outputDir, entry.name)
                    zip.getInputStream(entry).use { input ->
                        outFile.outputStream().use { output ->
                            input.copyTo(output, bufferSize = 32 * 1024) // buffer de 32KB
                        }
                    }
                    frameFiles.add(outFile)
                }
            }
        }
        return frameFiles
    }



}

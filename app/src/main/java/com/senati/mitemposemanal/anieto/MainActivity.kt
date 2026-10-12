package com.senati.mitemposemanal.anieto

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val tvDeveloperInfo = findViewById<TextView>(R.id.tvDeveloperInfo)
        tvDeveloperInfo.text = "Desarrollador: Anthony Nieto\nVersión: v0.2.0 (2)"

        val btnCategorias = findViewById<Button>(R.id.btnCategorias)
        val btnTareas = findViewById<Button>(R.id.btnTareas)
        val btnEventos = findViewById<Button>(R.id.btnEventos)

        btnCategorias.setOnClickListener {
            startActivity(Intent(this, CategoriasActivity::class.java))
        }

        btnTareas.setOnClickListener {
            startActivity(Intent(this, TareasActivity::class.java))
        }

        btnEventos.setOnClickListener {
            startActivity(Intent(this, EventosActivity::class.java))
        }
    }
}

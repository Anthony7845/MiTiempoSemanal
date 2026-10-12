package com.senati.mitemposemanal.anieto

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EventosActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var btnBack: ImageButton
    private lateinit var actvTareaBusqueda: AutoCompleteTextView
    private lateinit var tvCategoriaSeleccionada: TextView
    private lateinit var layoutEventoActivo: LinearLayout
    private lateinit var tvActivoInfo: TextView
    private lateinit var btnFinalizarEvento: Button
    private lateinit var btnIniciarEvento: Button
    private lateinit var lvEventos: ListView

    private var eventoActivo: Evento? = null
    private var tareasSugeridas = listOf<Triple<Long, String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eventos)

        dbHelper = DatabaseHelper(this)

        btnBack = findViewById(R.id.btnBack)
        actvTareaBusqueda = findViewById(R.id.actvTareaBusqueda)
        tvCategoriaSeleccionada = findViewById(R.id.tvCategoriaSeleccionada)
        layoutEventoActivo = findViewById(R.id.layoutEventoActivo)
        tvActivoInfo = findViewById(R.id.tvActivoInfo)
        btnFinalizarEvento = findViewById(R.id.btnFinalizarEvento)
        btnIniciarEvento = findViewById(R.id.btnIniciarEvento)
        lvEventos = findViewById(R.id.lvEventos)

        btnBack.setOnClickListener { finish() }

        actvTareaBusqueda.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                if (query.length >= 2) {
                    tareasSugeridas = dbHelper.searchTareas(query)
                    val nombres = tareasSugeridas.map { it.second }
                    val adapter = ArrayAdapter(this@EventosActivity, android.R.layout.simple_dropdown_item_1line, nombres)
                    actvTareaBusqueda.setAdapter(adapter)
                    actvTareaBusqueda.showDropDown()

                    val match = tareasSugeridas.find { it.second.equals(query, ignoreCase = true) }
                    if (match != null) {
                        tvCategoriaSeleccionada.text = "Categoría: ${match.third}"
                    } else {
                        tvCategoriaSeleccionada.text = "Categoría: (Nueva tarea - se elegirá al iniciar)"
                    }
                } else {
                    tvCategoriaSeleccionada.text = "Categoría: (Ninguna)"
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        actvTareaBusqueda.setOnItemClickListener { _, _, position, _ ->
            if (position in tareasSugeridas.indices) {
                val selected = tareasSugeridas[position]
                tvCategoriaSeleccionada.text = "Categoría: ${selected.third}"
            }
        }

        btnIniciarEvento.setOnClickListener {
            val nombreTarea = actvTareaBusqueda.text.toString().trim()
            if (nombreTarea.isEmpty()) {
                Toast.makeText(this, "Ingrese o seleccione el nombre de una tarea", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tareaExistente = dbHelper.getTareaByNombre(nombreTarea)
            if (tareaExistente != null) {
                iniciarEventoConTarea(tareaExistente.id)
            } else {
                mostrarDialogoSeleccionarCategoria(nombreTarea)
            }
        }

        btnFinalizarEvento.setOnClickListener {
            finalizarEventoActivo()
        }

        cargarHistorialEventos()
    }

    override fun onResume() {
        super.onResume()
        checkEventoActivo()
        cargarHistorialEventos()
    }

    private fun checkEventoActivo() {
        eventoActivo = dbHelper.getEventoEnCurso()
        if (eventoActivo != null) {
            layoutEventoActivo.visibility = LinearLayout.VISIBLE
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            val inicioFormat = try {
                LocalDateTime.parse(eventoActivo!!.inicio).format(formatter)
            } catch (e: Exception) {
                eventoActivo!!.inicio
            }
            tvActivoInfo.text = "Tarea: ${eventoActivo!!.tareaNombre}\nCategoría: ${eventoActivo!!.categoriaNombre}\nInicio: $inicioFormat"

            btnIniciarEvento.isEnabled = false
            btnIniciarEvento.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_gray))
            btnIniciarEvento.alpha = 0.5f
            btnIniciarEvento.text = "Evento en curso..."

            btnFinalizarEvento.isEnabled = true
            actvTareaBusqueda.isEnabled = false
        } else {
            layoutEventoActivo.visibility = LinearLayout.GONE

            btnIniciarEvento.isEnabled = true
            btnIniciarEvento.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_green))
            btnIniciarEvento.alpha = 1.0f
            btnIniciarEvento.text = "Iniciar Evento"

            btnFinalizarEvento.isEnabled = false
            actvTareaBusqueda.isEnabled = true
        }
    }

    private fun iniciarEventoConTarea(tareaId: Long) {
        val nowIso = LocalDateTime.now().toString()
        val id = dbHelper.insertEvento(tareaId, nowIso)
        if (id != -1L) {
            Toast.makeText(this, "Evento iniciado", Toast.LENGTH_SHORT).show()
            actvTareaBusqueda.text.clear()
            tvCategoriaSeleccionada.text = "Categoría: (Ninguna)"
            checkEventoActivo()
            cargarHistorialEventos()
        } else {
            Toast.makeText(this, "Error al iniciar evento", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoSeleccionarCategoria(nombreTarea: String) {
        val categorias = dbHelper.getAllCategorias()
        if (categorias.isEmpty()) {
            Toast.makeText(this, "Debe registrar categorías primero", Toast.LENGTH_SHORT).show()
            return
        }

        val nombresCat = categorias.map { it.nombre }.toTypedArray()
        var categoriaSeleccionadaIndex = 0

        AlertDialog.Builder(this)
            .setTitle("Seleccionar Categoría para '$nombreTarea'")
            .setSingleChoiceItems(nombresCat, 0) { _, which ->
                categoriaSeleccionadaIndex = which
            }
            .setPositiveButton("Crear e Iniciar") { _, _ ->
                val categoriaId = categorias[categoriaSeleccionadaIndex].id
                val nuevaTareaId = dbHelper.insertTarea(nombreTarea, categoriaId)
                if (nuevaTareaId != -1L) {
                    iniciarEventoConTarea(nuevaTareaId)
                } else {
                    Toast.makeText(this, "Error al crear la tarea", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun finalizarEventoActivo() {
        val evento = eventoActivo ?: return
        val fin = LocalDateTime.now()
        val inicio = try {
            LocalDateTime.parse(evento.inicio)
        } catch (e: Exception) {
            fin
        }
        val duracionSeg = Duration.between(inicio, fin).seconds

        val success = dbHelper.finalizarEvento(evento.id, fin.toString(), duracionSeg)
        if (success) {
            Toast.makeText(this, "Evento finalizado con éxito", Toast.LENGTH_SHORT).show()
            checkEventoActivo()
            cargarHistorialEventos()
        } else {
            Toast.makeText(this, "Error al finalizar evento", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarHistorialEventos() {
        val eventos = dbHelper.getAllEventos()
        val displayList = eventos.map { ev ->
            val formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")
            val iniStr = try { LocalDateTime.parse(ev.inicio).format(formatter) } catch (e: Exception) { ev.inicio }
            val finStr = if (ev.fin != null) {
                try { LocalDateTime.parse(ev.fin).format(formatter) } catch (e: Exception) { ev.fin }
            } else {
                "En curso"
            }
            val duracionMin = if (ev.duracionSeg != null) "${ev.duracionSeg / 60} min (${ev.duracionSeg}s)" else "-"
            "${ev.tareaNombre} · ${ev.categoriaNombre}\nInicio: $iniStr | Fin: $finStr\nDuración: $duracionMin"
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        lvEventos.adapter = adapter
    }
}

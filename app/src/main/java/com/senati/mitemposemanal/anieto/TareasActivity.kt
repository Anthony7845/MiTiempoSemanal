package com.senati.mitemposemanal.anieto

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class TareasActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etTareaNombre: EditText
    private lateinit var spinnerCategorias: Spinner
    private lateinit var btnGuardarTarea: Button
    private lateinit var lvTareas: ListView

    private var tareaEnEdicionId: Long? = null
    private var listaCategorias = listOf<Categoria>()
    private var listaTareas = listOf<Tarea>()
    private lateinit var tareaAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tareas)

        dbHelper = DatabaseHelper(this)
        etTareaNombre = findViewById(R.id.etTareaNombre)
        spinnerCategorias = findViewById(R.id.spinnerCategorias)
        btnGuardarTarea = findViewById(R.id.btnGuardarTarea)
        lvTareas = findViewById(R.id.lvTareas)

        cargarSpinnerCategorias()
        cargarTareas()

        btnGuardarTarea.setOnClickListener {
            val nombreTarea = etTareaNombre.text.toString().trim()
            if (nombreTarea.isEmpty()) {
                Toast.makeText(this, "El nombre de la tarea es obligatorio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (listaCategorias.isEmpty()) {
                Toast.makeText(this, "Debe registrar al menos una categoría primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedPosition = spinnerCategorias.selectedItemPosition
            if (selectedPosition < 0 || selectedPosition >= listaCategorias.size) {
                Toast.makeText(this, "Seleccione una categoría válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val categoriaId = listaCategorias[selectedPosition].id

            if (tareaEnEdicionId == null) {
                // Insertar
                val id = dbHelper.insertTarea(nombreTarea, categoriaId)
                if (id != -1L) {
                    Toast.makeText(this, "Tarea creada", Toast.LENGTH_SHORT).show()
                    etTareaNombre.text.clear()
                    cargarTareas()
                } else {
                    Toast.makeText(this, "Error: La tarea ya existe o es inválida", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Actualizar
                val success = dbHelper.updateTarea(tareaEnEdicionId!!, nombreTarea, categoriaId)
                if (success) {
                    Toast.makeText(this, "Tarea actualizada", Toast.LENGTH_SHORT).show()
                    etTareaNombre.text.clear()
                    tareaEnEdicionId = null
                    btnGuardarTarea.text = "Guardar Tarea"
                    cargarTareas()
                } else {
                    Toast.makeText(this, "Error: No se pudo actualizar (nombre duplicado o vacío)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        lvTareas.setOnItemClickListener { _, _, position, _ ->
            val tarea = listaTareas[position]
            val options = arrayOf("Editar", "Eliminar")
            AlertDialog.Builder(this)
                .setTitle("Tarea: ${tarea.nombre}")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> { // Editar
                            tareaEnEdicionId = tarea.id
                            etTareaNombre.setText(tarea.nombre)
                            btnGuardarTarea.text = "Actualizar Tarea"
                            val catIndex = listaCategorias.indexOfFirst { it.id == tarea.categoriaId }
                            if (catIndex != -1) {
                                spinnerCategorias.setSelection(catIndex)
                            }
                        }
                        1 -> { // Eliminar con confirmación
                            confirmarEliminarTarea(tarea)
                        }
                    }
                }.show()
        }
    }

    private fun cargarSpinnerCategorias() {
        listaCategorias = dbHelper.getAllCategorias()
        val nombresCat = listaCategorias.map { it.nombre }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresCat)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategorias.adapter = adapter
    }

    private fun cargarTareas() {
        listaTareas = dbHelper.getAllTareas()
        val displayList = listaTareas.map { it.displayFormat }
        tareaAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        lvTareas.adapter = tareaAdapter
    }

    private fun confirmarEliminarTarea(tarea: Tarea) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Desea eliminar la tarea '${tarea.nombre}'?")
            .setPositiveButton("Sí") { _, _ ->
                val deleted = dbHelper.deleteTarea(tarea.id)
                if (deleted) {
                    Toast.makeText(this, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                    cargarTareas()
                } else {
                    Toast.makeText(this, "Error al eliminar tarea", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        cargarSpinnerCategorias()
        cargarTareas()
    }
}

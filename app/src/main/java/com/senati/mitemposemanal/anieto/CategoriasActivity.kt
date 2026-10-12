package com.senati.mitemposemanal.anieto

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class CategoriasActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etCategoriaNombre: EditText
    private lateinit var btnGuardarCategoria: Button
    private lateinit var lvCategorias: ListView

    private var categoriaEnEdicionId: Long? = null
    private lateinit var adapter: ArrayAdapter<String>
    private var listaCategorias = listOf<Categoria>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categorias)

        dbHelper = DatabaseHelper(this)
        etCategoriaNombre = findViewById(R.id.etCategoriaNombre)
        btnGuardarCategoria = findViewById(R.id.btnGuardarCategoria)
        lvCategorias = findViewById(R.id.lvCategorias)

        cargarCategorias()

        btnGuardarCategoria.setOnClickListener {
            val nombre = etCategoriaNombre.text.toString().trim()
            if (nombre.isEmpty()) {
                Toast.makeText(this, "El nombre de la categoría no puede estar vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (categoriaEnEdicionId == null) {
                // Insertar
                val id = dbHelper.insertCategoria(nombre)
                if (id != -1L) {
                    Toast.makeText(this, "Categoría creada", Toast.LENGTH_SHORT).show()
                    etCategoriaNombre.text.clear()
                    cargarCategorias()
                } else {
                    Toast.makeText(this, "Error: La categoría ya existe o es inválida", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Actualizar
                val success = dbHelper.updateCategoria(categoriaEnEdicionId!!, nombre)
                if (success) {
                    Toast.makeText(this, "Categoría actualizada", Toast.LENGTH_SHORT).show()
                    etCategoriaNombre.text.clear()
                    categoriaEnEdicionId = null
                    btnGuardarCategoria.text = "Guardar"
                    cargarCategorias()
                } else {
                    Toast.makeText(this, "Error: No se pudo actualizar (nombre duplicado o vacío)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        lvCategorias.setOnItemClickListener { _, _, position, _ ->
            val cat = listaCategorias[position]
            val options = arrayOf("Editar", "Eliminar")
            AlertDialog.Builder(this)
                .setTitle("Categoría: ${cat.nombre}")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> { // Editar
                            categoriaEnEdicionId = cat.id
                            etCategoriaNombre.setText(cat.nombre)
                            btnGuardarCategoria.text = "Actualizar"
                        }
                        1 -> { // Eliminar con confirmación y validación de tareas asociadas
                            confirmarEliminarCategoria(cat)
                        }
                    }
                }.show()
        }
    }

    private fun cargarCategorias() {
        listaCategorias = dbHelper.getAllCategorias()
        val nombres = listaCategorias.map { it.nombre }
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, nombres)
        lvCategorias.adapter = adapter
    }

    private fun confirmarEliminarCategoria(cat: Categoria) {
        val count = dbHelper.countTareasByCategoria(cat.id)
        if (count > 0) {
            AlertDialog.Builder(this)
                .setTitle("No se puede eliminar")
                .setMessage("La categoría '${cat.nombre}' tiene $count tarea(s) asociada(s). Debe eliminar o reasignar las tareas primero.")
                .setPositiveButton("Aceptar", null)
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Desea eliminar la categoría '${cat.nombre}'?")
            .setPositiveButton("Sí") { _, _ ->
                val deleted = dbHelper.deleteCategoria(cat.id)
                if (deleted) {
                    Toast.makeText(this, "Categoría eliminada", Toast.LENGTH_SHORT).show()
                    cargarCategorias()
                } else {
                    Toast.makeText(this, "Error al eliminar categoría", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}

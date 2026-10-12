package com.senati.mitemposemanal.anieto

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "mitemposemanal.db"
        private const val DATABASE_VERSION = 2

        const val TABLE_CATEGORIAS = "categorias"
        const val COLUMN_CATEGORIA_ID = "id"
        const val COLUMN_CATEGORIA_NOMBRE = "nombre"

        const val TABLE_TAREAS = "tareas"
        const val COLUMN_TAREA_ID = "id"
        const val COLUMN_TAREA_NOMBRE = "nombre"
        const val COLUMN_TAREA_CATEGORIA_ID = "categoria_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys = ON;")

        val createTableCategorias = """
            CREATE TABLE $TABLE_CATEGORIAS (
                $COLUMN_CATEGORIA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CATEGORIA_NOMBRE TEXT UNIQUE NOT NULL
            )
        """.trimIndent()

        val createTableTareas = """
            CREATE TABLE $TABLE_TAREAS (
                $COLUMN_TAREA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TAREA_NOMBRE TEXT UNIQUE NOT NULL,
                $COLUMN_TAREA_CATEGORIA_ID INTEGER NOT NULL,
                FOREIGN KEY($COLUMN_TAREA_CATEGORIA_ID) REFERENCES $TABLE_CATEGORIAS($COLUMN_CATEGORIA_ID)
            )
        """.trimIndent()

        db.execSQL(createTableCategorias)
        db.execSQL(createTableTareas)

        val defaultCategories = listOf("Estudio", "Trabajo", "Descanso", "Ocio")
        for (category in defaultCategories) {
            val values = ContentValues().apply {
                put(COLUMN_CATEGORIA_NOMBRE, category)
            }
            db.insertWithOnConflict(TABLE_CATEGORIAS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TAREAS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIAS")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.execSQL("PRAGMA foreign_keys = ON;")
    }

    // --- Categoria CRUD ---

    fun insertCategoria(nombre: String): Long {
        val trimmed = nombre.trim()
        if (trimmed.isEmpty()) return -1L
        
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CATEGORIA_NOMBRE, trimmed)
        }
        return try {
            db.insertOrThrow(TABLE_CATEGORIAS, null, values)
        } catch (e: Exception) {
            -1L
        }
    }

    fun getAllCategorias(): List<Categoria> {
        val lista = mutableListOf<Categoria>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT $COLUMN_CATEGORIA_ID, $COLUMN_CATEGORIA_NOMBRE FROM $TABLE_CATEGORIAS ORDER BY $COLUMN_CATEGORIA_NOMBRE ASC", null)
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_CATEGORIA_ID))
                    val nombre = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORIA_NOMBRE))
                    lista.add(Categoria(id, nombre))
                } while (it.moveToNext())
            }
        }
        return lista
    }

    fun updateCategoria(id: Long, nuevoNombre: String): Boolean {
        val trimmed = nuevoNombre.trim()
        if (trimmed.isEmpty()) return false

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CATEGORIA_NOMBRE, trimmed)
        }
        return try {
            val rows = db.update(TABLE_CATEGORIAS, values, "$COLUMN_CATEGORIA_ID = ?", arrayOf(id.toString()))
            rows > 0
        } catch (e: Exception) {
            false
        }
    }

    fun countTareasByCategoria(categoriaId: Long): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_TAREAS WHERE $COLUMN_TAREA_CATEGORIA_ID = ?", arrayOf(categoriaId.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }
        return 0
    }

    fun deleteCategoria(id: Long): Boolean {
        if (countTareasByCategoria(id) > 0) {
            return false
        }
        val db = writableDatabase
        val rows = db.delete(TABLE_CATEGORIAS, "$COLUMN_CATEGORIA_ID = ?", arrayOf(id.toString()))
        return rows > 0
    }

    // --- Tarea CRUD ---

    fun insertTarea(nombre: String, categoriaId: Long): Long {
        val trimmed = nombre.trim()
        if (trimmed.isEmpty()) return -1L

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TAREA_NOMBRE, trimmed)
            put(COLUMN_TAREA_CATEGORIA_ID, categoriaId)
        }
        return try {
            db.insertOrThrow(TABLE_TAREAS, null, values)
        } catch (e: Exception) {
            -1L
        }
    }

    fun getAllTareas(): List<Tarea> {
        val lista = mutableListOf<Tarea>()
        val db = readableDatabase
        val query = """
            SELECT t.$COLUMN_TAREA_ID, t.$COLUMN_TAREA_NOMBRE, t.$COLUMN_TAREA_CATEGORIA_ID, c.$COLUMN_CATEGORIA_NOMBRE 
            FROM $TABLE_TAREAS t
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COLUMN_TAREA_CATEGORIA_ID = c.$COLUMN_CATEGORIA_ID
            ORDER BY t.$COLUMN_TAREA_NOMBRE ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getLong(0)
                    val nombre = it.getString(1)
                    val catId = it.getLong(2)
                    val catNombre = it.getString(3)
                    lista.add(Tarea(id, nombre, catId, catNombre))
                } while (it.moveToNext())
            }
        }
        return lista
    }

    fun updateTarea(id: Long, nuevoNombre: String, nuevaCategoriaId: Long): Boolean {
        val trimmed = nuevoNombre.trim()
        if (trimmed.isEmpty()) return false

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TAREA_NOMBRE, trimmed)
            put(COLUMN_TAREA_CATEGORIA_ID, nuevaCategoriaId)
        }
        return try {
            val rows = db.update(TABLE_TAREAS, values, "$COLUMN_TAREA_ID = ?", arrayOf(id.toString()))
            rows > 0
        } catch (e: Exception) {
            false
        }
    }

    fun deleteTarea(id: Long): Boolean {
        val db = writableDatabase
        val rows = db.delete(TABLE_TAREAS, "$COLUMN_TAREA_ID = ?", arrayOf(id.toString()))
        return rows > 0
    }
}

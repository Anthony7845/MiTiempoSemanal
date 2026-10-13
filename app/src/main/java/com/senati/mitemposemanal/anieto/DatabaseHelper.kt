package com.senati.mitemposemanal.anieto

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "mitemposemanal.db"
        private const val DATABASE_VERSION = 4

        const val TABLE_CATEGORIAS = "categorias"
        const val COLUMN_CATEGORIA_ID = "id"
        const val COLUMN_CATEGORIA_NOMBRE = "nombre"

        const val TABLE_TAREAS = "tareas"
        const val COLUMN_TAREA_ID = "id"
        const val COLUMN_TAREA_NOMBRE = "nombre"
        const val COLUMN_TAREA_CATEGORIA_ID = "categoria_id"

        const val TABLE_EVENTOS = "eventos"
        const val COLUMN_EVENTO_ID = "id"
        const val COLUMN_EVENTO_TAREA_ID = "tarea_id"
        const val COLUMN_EVENTO_INICIO = "inicio"
        const val COLUMN_EVENTO_FIN = "fin"
        const val COLUMN_EVENTO_DURACION = "duracion_seg"

        const val TABLE_REPORTES = "reportes_semanales"
        const val COLUMN_REPORTE_ID = "id"
        const val COLUMN_REPORTE_SEMANA_INICIO = "semana_inicio"
        const val COLUMN_REPORTE_SEMANA_FIN = "semana_fin"
        const val COLUMN_REPORTE_CATEGORIA_ID = "categoria_id"
        const val COLUMN_REPORTE_MINUTOS = "minutos"
        const val COLUMN_REPORTE_PORCENTAJE = "porcentaje"
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

        val createTableEventos = """
            CREATE TABLE $TABLE_EVENTOS (
                $COLUMN_EVENTO_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_EVENTO_TAREA_ID INTEGER NOT NULL,
                $COLUMN_EVENTO_INICIO TEXT NOT NULL,
                $COLUMN_EVENTO_FIN TEXT,
                $COLUMN_EVENTO_DURACION INTEGER,
                FOREIGN KEY($COLUMN_EVENTO_TAREA_ID) REFERENCES $TABLE_TAREAS($COLUMN_TAREA_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        val createTableReportes = """
            CREATE TABLE $TABLE_REPORTES (
                $COLUMN_REPORTE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_REPORTE_SEMANA_INICIO TEXT NOT NULL,
                $COLUMN_REPORTE_SEMANA_FIN TEXT NOT NULL,
                $COLUMN_REPORTE_CATEGORIA_ID INTEGER NOT NULL,
                $COLUMN_REPORTE_MINUTOS INTEGER NOT NULL,
                $COLUMN_REPORTE_PORCENTAJE REAL NOT NULL,
                FOREIGN KEY($COLUMN_REPORTE_CATEGORIA_ID) REFERENCES $TABLE_CATEGORIAS($COLUMN_CATEGORIA_ID) ON DELETE CASCADE,
                UNIQUE($COLUMN_REPORTE_SEMANA_INICIO, $COLUMN_REPORTE_CATEGORIA_ID)
            )
        """.trimIndent()

        db.execSQL(createTableCategorias)
        db.execSQL(createTableTareas)
        db.execSQL(createTableEventos)
        db.execSQL(createTableReportes)

        val defaultCategories = listOf("Estudio", "Trabajo", "Descanso", "Ocio")
        for (category in defaultCategories) {
            val values = ContentValues().apply {
                put(COLUMN_CATEGORIA_NOMBRE, category)
            }
            db.insertWithOnConflict(TABLE_CATEGORIAS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("PRAGMA foreign_keys = ON;")
        if (oldVersion < 3) {
            val createTableEventos = """
                CREATE TABLE IF NOT EXISTS $TABLE_EVENTOS (
                    $COLUMN_EVENTO_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_EVENTO_TAREA_ID INTEGER NOT NULL,
                    $COLUMN_EVENTO_INICIO TEXT NOT NULL,
                    $COLUMN_EVENTO_FIN TEXT,
                    $COLUMN_EVENTO_DURACION INTEGER,
                    FOREIGN KEY($COLUMN_EVENTO_TAREA_ID) REFERENCES $TABLE_TAREAS($COLUMN_TAREA_ID) ON DELETE CASCADE
                )
            """.trimIndent()
            db.execSQL(createTableEventos)
        }
        if (oldVersion < 4) {
            val createTableReportes = """
                CREATE TABLE IF NOT EXISTS $TABLE_REPORTES (
                    $COLUMN_REPORTE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_REPORTE_SEMANA_INICIO TEXT NOT NULL,
                    $COLUMN_REPORTE_SEMANA_FIN TEXT NOT NULL,
                    $COLUMN_REPORTE_CATEGORIA_ID INTEGER NOT NULL,
                    $COLUMN_REPORTE_MINUTOS INTEGER NOT NULL,
                    $COLUMN_REPORTE_PORCENTAJE REAL NOT NULL,
                    FOREIGN KEY($COLUMN_REPORTE_CATEGORIA_ID) REFERENCES $TABLE_CATEGORIAS($COLUMN_CATEGORIA_ID) ON DELETE CASCADE,
                    UNIQUE($COLUMN_REPORTE_SEMANA_INICIO, $COLUMN_REPORTE_CATEGORIA_ID)
                )
            """.trimIndent()
            db.execSQL(createTableReportes)
        }
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

    // --- Sprint 2: Eventos y Búsqueda de Tareas ---

    fun searchTareas(query: String): List<Triple<Long, String, String>> {
        val lista = mutableListOf<Triple<Long, String, String>>()
        val db = readableDatabase
        val sql = """
            SELECT t.$COLUMN_TAREA_ID, t.$COLUMN_TAREA_NOMBRE, c.$COLUMN_CATEGORIA_NOMBRE
            FROM $TABLE_TAREAS t
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COLUMN_TAREA_CATEGORIA_ID = c.$COLUMN_CATEGORIA_ID
            WHERE t.$COLUMN_TAREA_NOMBRE LIKE ?
            ORDER BY t.$COLUMN_TAREA_NOMBRE ASC
        """.trimIndent()
        val cursor = db.rawQuery(sql, arrayOf("%$query%"))
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getLong(0)
                    val nombre = it.getString(1)
                    val catNombre = it.getString(2)
                    lista.add(Triple(id, nombre, catNombre))
                } while (it.moveToNext())
            }
        }
        return lista
    }

    fun getTareaByNombre(nombre: String): Tarea? {
        val db = readableDatabase
        val sql = """
            SELECT t.$COLUMN_TAREA_ID, t.$COLUMN_TAREA_NOMBRE, t.$COLUMN_TAREA_CATEGORIA_ID, c.$COLUMN_CATEGORIA_NOMBRE
            FROM $TABLE_TAREAS t
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COLUMN_TAREA_CATEGORIA_ID = c.$COLUMN_CATEGORIA_ID
            WHERE t.$COLUMN_TAREA_NOMBRE = ?
        """.trimIndent()
        val cursor = db.rawQuery(sql, arrayOf(nombre.trim()))
        cursor.use {
            if (it.moveToFirst()) {
                return Tarea(it.getLong(0), it.getString(1), it.getLong(2), it.getString(3))
            }
        }
        return null
    }

    fun insertEvento(tareaId: Long, inicioIso: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_EVENTO_TAREA_ID, tareaId)
            put(COLUMN_EVENTO_INICIO, inicioIso)
        }
        return db.insert(TABLE_EVENTOS, null, values)
    }

    fun getEventoEnCurso(): Evento? {
        val db = readableDatabase
        val sql = """
            SELECT e.$COLUMN_EVENTO_ID, e.$COLUMN_EVENTO_TAREA_ID, t.$COLUMN_TAREA_NOMBRE, c.$COLUMN_CATEGORIA_NOMBRE, e.$COLUMN_EVENTO_INICIO, e.$COLUMN_EVENTO_FIN, e.$COLUMN_EVENTO_DURACION
            FROM $TABLE_EVENTOS e
            INNER JOIN $TABLE_TAREAS t ON e.$COLUMN_EVENTO_TAREA_ID = t.$COLUMN_TAREA_ID
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COLUMN_TAREA_CATEGORIA_ID = c.$COLUMN_CATEGORIA_ID
            WHERE e.$COLUMN_EVENTO_FIN IS NULL
            LIMIT 1
        """.trimIndent()
        val cursor = db.rawQuery(sql, null)
        cursor.use {
            if (it.moveToFirst()) {
                return Evento(
                    id = it.getLong(0),
                    tareaId = it.getLong(1),
                    tareaNombre = it.getString(2),
                    categoriaNombre = it.getString(3),
                    inicio = it.getString(4),
                    fin = it.getString(5),
                    duracionSeg = if (it.isNull(6)) null else it.getLong(6)
                )
            }
        }
        return null
    }

    fun finalizarEvento(eventoId: Long, finIso: String, duracionSeg: Long): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_EVENTO_FIN, finIso)
            put(COLUMN_EVENTO_DURACION, duracionSeg)
        }
        val rows = db.update(TABLE_EVENTOS, values, "$COLUMN_EVENTO_ID = ?", arrayOf(eventoId.toString()))
        return rows > 0
    }

    fun getAllEventos(): List<Evento> {
        val lista = mutableListOf<Evento>()
        val db = readableDatabase
        val sql = """
            SELECT e.$COLUMN_EVENTO_ID, e.$COLUMN_EVENTO_TAREA_ID, t.$COLUMN_TAREA_NOMBRE, c.$COLUMN_CATEGORIA_NOMBRE, e.$COLUMN_EVENTO_INICIO, e.$COLUMN_EVENTO_FIN, e.$COLUMN_EVENTO_DURACION
            FROM $TABLE_EVENTOS e
            INNER JOIN $TABLE_TAREAS t ON e.$COLUMN_EVENTO_TAREA_ID = t.$COLUMN_TAREA_ID
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COLUMN_TAREA_CATEGORIA_ID = c.$COLUMN_CATEGORIA_ID
            ORDER BY e.$COLUMN_EVENTO_INICIO DESC
        """.trimIndent()
        val cursor = db.rawQuery(sql, null)
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    lista.add(
                        Evento(
                            id = it.getLong(0),
                            tareaId = it.getLong(1),
                            tareaNombre = it.getString(2),
                            categoriaNombre = it.getString(3),
                            inicio = it.getString(4),
                            fin = it.getString(5),
                            duracionSeg = if (it.isNull(6)) null else it.getLong(6)
                        )
                    )
                } while (it.moveToNext())
            }
        }
        return lista
    }

    fun getResumenEventosSemana(inicioIso: String, finIso: String): List<CategoriaPorcentaje> {
        val lista = mutableListOf<CategoriaPorcentaje>()
        val db = readableDatabase
        val sql = """
            SELECT c.$COLUMN_CATEGORIA_NOMBRE, SUM(e.$COLUMN_EVENTO_DURACION) as total_seg
            FROM $TABLE_CATEGORIAS c
            LEFT JOIN $TABLE_TAREAS t ON c.$COLUMN_CATEGORIA_ID = t.$COLUMN_TAREA_CATEGORIA_ID
            LEFT JOIN $TABLE_EVENTOS e ON t.$COLUMN_TAREA_ID = e.$COLUMN_EVENTO_TAREA_ID 
                 AND e.$COLUMN_EVENTO_FIN IS NOT NULL
                 AND e.$COLUMN_EVENTO_INICIO >= ? AND e.$COLUMN_EVENTO_INICIO < ?
            GROUP BY c.$COLUMN_CATEGORIA_ID, c.$COLUMN_CATEGORIA_NOMBRE
            ORDER BY total_seg DESC
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(inicioIso, finIso))
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val catNombre = it.getString(0)
                    val totalSeg = if (it.isNull(1)) 0L else it.getLong(1)
                    lista.add(CategoriaPorcentaje(catNombre, totalSeg))
                } while (it.moveToNext())
            }
        }
        return lista
    }

    // --- Sprint 4: Cierre Automático de Semanas y Reportes ---

    fun cerrarSemanasPendientes() {
        val db = writableDatabase
        val now = LocalDateTime.now()
        val (currentWeekStart, _, _) = WeekUtils.obtenerRangoSemanaActual(now)

        val sqlEvents = """
            SELECT DISTINCT $COLUMN_EVENTO_INICIO 
            FROM $TABLE_EVENTOS 
            WHERE $COLUMN_EVENTO_FIN IS NOT NULL 
              AND $COLUMN_EVENTO_INICIO < ?
            ORDER BY $COLUMN_EVENTO_INICIO ASC
        """.trimIndent()

        val pastWeekStarts = mutableSetOf<LocalDateTime>()
        val cursor = db.rawQuery(sqlEvents, arrayOf(currentWeekStart.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val inicioStr = it.getString(0)
                    try {
                        val inicioDt = LocalDateTime.parse(inicioStr)
                        val (weekStart, _, _) = WeekUtils.obtenerRangoSemanaActual(inicioDt)
                        if (weekStart.isBefore(currentWeekStart)) {
                            pastWeekStarts.add(weekStart)
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                } while (it.moveToNext())
            }
        }

        for (weekStart in pastWeekStarts) {
            val weekEnd = weekStart.plusWeeks(1)
            val weekStartIso = weekStart.toString()
            val weekEndIso = weekEnd.toString()

            val checkSql = "SELECT COUNT(*) FROM $TABLE_REPORTES WHERE $COLUMN_REPORTE_SEMANA_INICIO = ?"
            val checkCursor = db.rawQuery(checkSql, arrayOf(weekStartIso))
            var hasReport = false
            checkCursor.use {
                if (it.moveToFirst()) {
                    hasReport = it.getInt(0) > 0
                }
            }

            if (!hasReport) {
                val resumen = getResumenEventosSemana(weekStartIso, weekEndIso)
                val totalMinSemana = 10080.0

                db.beginTransaction()
                try {
                    for (item in resumen) {
                        val minutos = item.totalSegundos / 60
                        val porcentaje = (minutos.toDouble() / totalMinSemana) * 100.0

                        val catId = getCategoriaIdByName(item.categoriaNombre, db)
                        if (catId != -1L) {
                            val values = ContentValues().apply {
                                put(COLUMN_REPORTE_SEMANA_INICIO, weekStartIso)
                                put(COLUMN_REPORTE_SEMANA_FIN, weekEndIso)
                                put(COLUMN_REPORTE_CATEGORIA_ID, catId)
                                put(COLUMN_REPORTE_MINUTOS, minutos)
                                put(COLUMN_REPORTE_PORCENTAJE, porcentaje)
                            }
                            db.insertWithOnConflict(TABLE_REPORTES, null, values, SQLiteDatabase.CONFLICT_IGNORE)
                        }
                    }
                    db.setTransactionSuccessful()
                    Log.d("MiTempoSemanal", "Reporte generado y guardado para semana: $weekStartIso")
                } catch (e: Exception) {
                    Log.e("MiTempoSemanal", "Error al generar reporte para semana $weekStartIso", e)
                } finally {
                    db.endTransaction()
                }
            }
        }
    }

    private fun getCategoriaIdByName(nombre: String, db: SQLiteDatabase): Long {
        val cursor = db.rawQuery("SELECT $COLUMN_CATEGORIA_ID FROM $TABLE_CATEGORIAS WHERE $COLUMN_CATEGORIA_NOMBRE = ?", arrayOf(nombre))
        cursor.use {
            if (it.moveToFirst()) {
                return it.getLong(0)
            }
        }
        return -1L
    }

    fun getSemanasConReporte(): List<Triple<String, String, String>> {
        val lista = mutableListOf<Triple<String, String, String>>()
        val db = readableDatabase
        val sql = """
            SELECT DISTINCT $COLUMN_REPORTE_SEMANA_INICIO, $COLUMN_REPORTE_SEMANA_FIN
            FROM $TABLE_REPORTES
            ORDER BY $COLUMN_REPORTE_SEMANA_INICIO DESC
        """.trimIndent()

        val cursor = db.rawQuery(sql, null)
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val inicioIso = it.getString(0)
                    val finIso = it.getString(1)
                    try {
                        val inicioDt = LocalDateTime.parse(inicioIso)
                        val domingoDt = inicioDt.plusDays(6)
                        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        val textoRango = "${inicioDt.format(formatter)} – ${domingoDt.format(formatter)}"
                        lista.add(Triple(inicioIso, finIso, textoRango))
                    } catch (e: Exception) {
                        lista.add(Triple(inicioIso, finIso, "$inicioIso – $finIso"))
                    }
                } while (it.moveToNext())
            }
        }
        return lista
    }

    fun getReporteDetalleSemana(semanaInicioIso: String): List<ReporteCategoriaItem> {
        val lista = mutableListOf<ReporteCategoriaItem>()
        val db = readableDatabase
        val sql = """
            SELECT c.$COLUMN_CATEGORIA_NOMBRE, r.$COLUMN_REPORTE_MINUTOS, r.$COLUMN_REPORTE_PORCENTAJE
            FROM $TABLE_REPORTES r
            INNER JOIN $TABLE_CATEGORIAS c ON r.$COLUMN_REPORTE_CATEGORIA_ID = c.$COLUMN_CATEGORIA_ID
            WHERE r.$COLUMN_REPORTE_SEMANA_INICIO = ?
            ORDER BY r.$COLUMN_REPORTE_PORCENTAJE DESC
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(semanaInicioIso))
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val catNombre = it.getString(0)
                    val minutos = it.getLong(1)
                    val porcentaje = it.getDouble(2)
                    lista.add(ReporteCategoriaItem(catNombre, minutos, porcentaje))
                } while (it.moveToNext())
            }
        }
        return lista
    }

    fun getEventosSemana(semanaInicioIso: String, semanaFinIso: String): List<Evento> {
        val lista = mutableListOf<Evento>()
        val db = readableDatabase
        val sql = """
            SELECT e.$COLUMN_EVENTO_ID, e.$COLUMN_EVENTO_TAREA_ID, t.$COLUMN_TAREA_NOMBRE, c.$COLUMN_CATEGORIA_NOMBRE, e.$COLUMN_EVENTO_INICIO, e.$COLUMN_EVENTO_FIN, e.$COLUMN_EVENTO_DURACION
            FROM $TABLE_EVENTOS e
            INNER JOIN $TABLE_TAREAS t ON e.$COLUMN_EVENTO_TAREA_ID = t.$COLUMN_TAREA_ID
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COLUMN_TAREA_CATEGORIA_ID = c.$COLUMN_CATEGORIA_ID
            WHERE e.$COLUMN_EVENTO_FIN IS NOT NULL
              AND e.$COLUMN_EVENTO_INICIO >= ? AND e.$COLUMN_EVENTO_INICIO < ?
            ORDER BY e.$COLUMN_EVENTO_INICIO DESC
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(semanaInicioIso, semanaFinIso))
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    lista.add(
                        Evento(
                            id = it.getLong(0),
                            tareaId = it.getLong(1),
                            tareaNombre = it.getString(2),
                            categoriaNombre = it.getString(3),
                            inicio = it.getString(4),
                            fin = it.getString(5),
                            duracionSeg = if (it.isNull(6)) null else it.getLong(6)
                        )
                    )
                } while (it.moveToNext())
            }
        }
        return lista
    }
}

package com.senati.mitemposemanal.anieto

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ReportesActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var btnBack: ImageButton
    private lateinit var spinnerSemanas: Spinner
    private lateinit var tvSinReportes: TextView
    private lateinit var layoutContenidoReporte: LinearLayout
    private lateinit var layoutReporteCategorias: LinearLayout
    private lateinit var lvReporteEventos: ListView

    private var semanasList = listOf<Triple<String, String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportes)

        dbHelper = DatabaseHelper(this)

        btnBack = findViewById(R.id.btnBack)
        spinnerSemanas = findViewById(R.id.spinnerSemanas)
        tvSinReportes = findViewById(R.id.tvSinReportes)
        layoutContenidoReporte = findViewById(R.id.layoutContenidoReporte)
        layoutReporteCategorias = findViewById(R.id.layoutReporteCategorias)
        lvReporteEventos = findViewById(R.id.lvReporteEventos)

        btnBack.setOnClickListener { finish() }

        cargarSemanas()
    }

    private fun cargarSemanas() {
        semanasList = dbHelper.getSemanasConReporte()
        if (semanasList.isEmpty()) {
            tvSinReportes.visibility = View.VISIBLE
            layoutContenidoReporte.visibility = View.GONE
            spinnerSemanas.isEnabled = false
        } else {
            tvSinReportes.visibility = View.GONE
            layoutContenidoReporte.visibility = View.VISIBLE
            spinnerSemanas.isEnabled = true

            val textos = semanasList.map { it.third }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, textos)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSemanas.adapter = adapter

            spinnerSemanas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position in semanasList.indices) {
                        val seleccionada = semanasList[position]
                        cargarDetalleSemana(seleccionada.first, seleccionada.second)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun cargarDetalleSemana(inicioIso: String, finIso: String) {
        val detalle = dbHelper.getReporteDetalleSemana(inicioIso)
        layoutReporteCategorias.removeAllViews()

        val totalMinSemana = 10080L
        var totalMinRegistrados = 0L
        val inflater = LayoutInflater.from(this)

        for (item in detalle) {
            totalMinRegistrados += item.minutos

            val horas = item.minutos / 60
            val mins = item.minutos % 60
            val porcStr = String.format(java.util.Locale.US, "%.1f%%", item.porcentaje)

            val itemView = inflater.inflate(R.layout.item_categoria_porcentaje, layoutReporteCategorias, false)
            val tvCatNombre = itemView.findViewById<TextView>(R.id.tvCatNombre)
            val tvCatTiempoPorcentaje = itemView.findViewById<TextView>(R.id.tvCatTiempoPorcentaje)
            val pbCatProgreso = itemView.findViewById<ProgressBar>(R.id.pbCatProgreso)

            tvCatNombre.text = item.categoriaNombre
            tvCatTiempoPorcentaje.text = "${horas}h ${mins}m ($porcStr)"
            pbCatProgreso.progress = (item.porcentaje * 10).toInt().coerceIn(0, 1000)

            layoutReporteCategorias.addView(itemView)
        }

        // Fila "Sin registrar"
        val minSinRegistrar = (totalMinSemana - totalMinRegistrados).coerceAtLeast(0L)
        val hSin = minSinRegistrar / 60
        val mSin = minSinRegistrar % 60
        val porcSin = (minSinRegistrar.toDouble() / totalMinSemana.toDouble()) * 100.0
        val porcSinStr = String.format(java.util.Locale.US, "%.1f%%", porcSin)

        val itemSinView = inflater.inflate(R.layout.item_categoria_porcentaje, layoutReporteCategorias, false)
        val tvCatNombreSin = itemSinView.findViewById<TextView>(R.id.tvCatNombre)
        val tvCatTiempoPorCientoSin = itemSinView.findViewById<TextView>(R.id.tvCatTiempoPorcentaje)
        val pbCatProgresoSin = itemSinView.findViewById<ProgressBar>(R.id.pbCatProgreso)

        tvCatNombreSin.text = "Sin registrar"
        tvCatTiempoPorCientoSin.text = "${hSin}h ${mSin}m ($porcSinStr)"
        pbCatProgresoSin.progress = (porcSin * 10).toInt().coerceIn(0, 1000)
        pbCatProgresoSin.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_gray))

        layoutReporteCategorias.addView(itemSinView)

        // Cargar eventos de esta semana
        val eventos = dbHelper.getEventosSemana(inicioIso, finIso)
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
        lvReporteEventos.adapter = adapter
        setListViewHeightBasedOnChildren(lvReporteEventos)
    }

    private fun setListViewHeightBasedOnChildren(listView: ListView) {
        val listAdapter = listView.adapter ?: return
        var totalHeight = 0
        for (i in 0 until listAdapter.count) {
            val listItem = listAdapter.getView(i, null, listView)
            listItem.measure(
                View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            totalHeight += listItem.measuredHeight
        }
        val params = listView.layoutParams
        params.height = totalHeight + (listView.dividerHeight * (listAdapter.count - 1))
        listView.layoutParams = params
        listView.requestLayout()
    }
}

package com.senati.mitemposemanal.anieto

import android.util.Log
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object WeekUtils {
    fun obtenerRangoSemanaActual(now: LocalDateTime = LocalDateTime.now()): Triple<LocalDateTime, LocalDateTime, String> {
        val inicioSemana = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay()
        val finSemana = inicioSemana.plusWeeks(1)
        
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val domingoSemana = inicioSemana.plusDays(6)
        val textoRango = "Semana ${inicioSemana.format(dateFormatter)} – ${domingoSemana.format(dateFormatter)}"
        
        Log.d("MiTempoSemanal", "Semana calculada -> Inicio: $inicioSemana, Fin: $finSemana, Texto: $textoRango")
        return Triple(inicioSemana, finSemana, textoRango)
    }
}

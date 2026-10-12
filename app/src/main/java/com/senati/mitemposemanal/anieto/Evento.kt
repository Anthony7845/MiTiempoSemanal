package com.senati.mitemposemanal.anieto

data class Evento(
    val id: Long,
    val tareaId: Long,
    val tareaNombre: String,
    val categoriaNombre: String,
    val inicio: String,
    val fin: String?,
    val duracionSeg: Long?
)

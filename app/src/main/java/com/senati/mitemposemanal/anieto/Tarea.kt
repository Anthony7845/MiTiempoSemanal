package com.senati.mitemposemanal.anieto

data class Tarea(
    val id: Long,
    val nombre: String,
    val categoriaId: Long,
    val categoriaNombre: String = ""
) {
    val displayFormat: String
        get() = "$nombre · $categoriaNombre"
}

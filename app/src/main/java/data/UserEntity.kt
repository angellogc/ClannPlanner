package com.angello.clannplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val email: String,
    val password: String,
    val avatar: String = "👨",
    val bateria: Int = 100,
    val estadoTexto: String = "🏠 En casa",
    val colorHex: String = "#6750A4",
    val puntos: Int = 0 // 👈 Nuevo campo para el ranking de tareas
)
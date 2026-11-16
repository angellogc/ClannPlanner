package com.angello.clannplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val remitenteId: Long,
    val texto: String,
    val timestamp: Long
)
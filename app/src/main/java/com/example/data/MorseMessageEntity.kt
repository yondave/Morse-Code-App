package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "morse_messages")
data class MorseMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "SENT", "RECEIVED", "FAVORITE"
    val text: String,
    val morseCode: String,
    val timestamp: Long = System.currentTimeMillis(),
    val wpm: Int = 15
)

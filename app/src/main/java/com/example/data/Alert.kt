package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class Alert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "AMBER", "WEATHER", "HAZMAT"
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distanceMiles: Double? = null,
    val severity: String = "WARNING", // "INFO", "WARNING", "CRITICAL"
    val isRead: Boolean = false
)

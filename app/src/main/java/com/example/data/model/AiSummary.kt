package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "summaries")
data class AiSummary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val overview: String,
    val keyTakeaways: String, // Stored as bulleted string
    val actionItems: String, // Stored as newline-delimited checklist items
    val notificationCount: Int,
    val urgencyLevel: String, // "CRÍTICA", "ALTA", "MODERADA", "NORMAL"
    val engineType: String = "NotiAI Edge Engine v2.4",
    val timestamp: Long = System.currentTimeMillis(),
    val timeSavedMinutes: Int = 5,
    val noiseReductionPercent: Int = 70
)

package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NotificationCategory(val displayName: String, val iconName: String) {
    ALL("Todas", "FilterList"),
    URGENT("Urgentes", "Warning"),
    WORK("Trabajo", "Work"),
    MESSAGING("Mensajes", "Chat"),
    FINANCE("Finanzas", "AccountBalance"),
    SOCIAL("Social", "People"),
    SYSTEM("Sistema", "Settings"),
    PROMO("Promociones", "LocalOffer")
}

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = NotificationCategory.MESSAGING.name,
    val priority: Int = 2, // 1: Baja, 2: Media, 3: Alta, 4: Crítica
    val isRead: Boolean = false,
    val isStarred: Boolean = false,
    val summaryId: Long? = null
)

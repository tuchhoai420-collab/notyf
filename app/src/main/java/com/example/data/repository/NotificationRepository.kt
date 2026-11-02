package com.example.data.repository

import android.content.Context
import com.example.ai.NotiAiEngine
import com.example.data.database.AppDatabase
import com.example.data.model.AiSummary
import com.example.data.model.NotificationItem
import kotlinx.coroutines.flow.Flow

class NotificationRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val notiDao = db.notificationDao()
    private val summaryDao = db.summaryDao()

    val allNotifications: Flow<List<NotificationItem>> = notiDao.getAllNotifications()
    val unreadNotifications: Flow<List<NotificationItem>> = notiDao.getUnreadNotifications()
    val starredNotifications: Flow<List<NotificationItem>> = notiDao.getStarredNotifications()
    val urgentNotifications: Flow<List<NotificationItem>> = notiDao.getUrgentNotifications()
    val unreadCount: Flow<Int> = notiDao.getUnreadCount()
    val urgentCount: Flow<Int> = notiDao.getUrgentCount()
    val totalCount: Flow<Int> = notiDao.getNotificationCount()
    val allSummaries: Flow<List<AiSummary>> = summaryDao.getAllSummaries()
    val latestSummary: Flow<AiSummary?> = summaryDao.getLatestSummary()

    fun getByCategory(category: String): Flow<List<NotificationItem>> =
        notiDao.getNotificationsByCategory(category)

    fun search(query: String): Flow<List<NotificationItem>> =
        notiDao.searchNotifications(query)

    suspend fun addNotification(
        packageName: String,
        title: String,
        content: String,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        val appName = NotiAiEngine.resolveFriendlyAppName(packageName)
        val category = NotiAiEngine.detectCategory(packageName, title, content)
        val priority = NotiAiEngine.calculatePriority(category, title, content)

        val item = NotificationItem(
            packageName = packageName,
            appName = appName,
            title = title,
            content = content,
            timestamp = timestamp,
            category = category.name,
            priority = priority,
            isRead = false,
            isStarred = false
        )
        return notiDao.insert(item)
    }

    suspend fun markAllAsRead() = notiDao.markAllAsRead()

    suspend fun setRead(id: Long, isRead: Boolean) = notiDao.updateReadStatus(id, isRead)

    suspend fun setStarred(id: Long, isStarred: Boolean) = notiDao.updateStarredStatus(id, isStarred)

    suspend fun delete(id: Long) = notiDao.deleteById(id)

    suspend fun clearAll() {
        notiDao.clearAll()
        summaryDao.clearAll()
    }

    suspend fun generateAndSaveSummary(modelPreference: String): AiSummary {
        val currentList = notiDao.getRecentList(50)
        val summary = NotiAiEngine.generateBriefing(currentList, modelPreference)
        summaryDao.insert(summary)
        return summary
    }
}

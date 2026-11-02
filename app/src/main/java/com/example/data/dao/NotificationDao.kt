package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.NotificationItem
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationItem>>

    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadNotifications(): Flow<List<NotificationItem>>

    @Query("SELECT * FROM notifications WHERE isStarred = 1 ORDER BY timestamp DESC")
    fun getStarredNotifications(): Flow<List<NotificationItem>>

    @Query("SELECT * FROM notifications WHERE category = :category ORDER BY timestamp DESC")
    fun getNotificationsByCategory(category: String): Flow<List<NotificationItem>>

    @Query("SELECT * FROM notifications WHERE priority >= 3 ORDER BY timestamp DESC")
    fun getUrgentNotifications(): Flow<List<NotificationItem>>

    @Query("""
        SELECT * FROM notifications 
        WHERE (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR appName LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun searchNotifications(query: String): Flow<List<NotificationItem>>

    @Query("SELECT COUNT(*) FROM notifications")
    fun getNotificationCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE priority >= 3")
    fun getUrgentCount(): Flow<Int>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentList(limit: Int = 50): List<NotificationItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: NotificationItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<NotificationItem>)

    @Update
    suspend fun update(item: NotificationItem)

    @Delete
    suspend fun delete(item: NotificationItem)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("UPDATE notifications SET isRead = :isRead WHERE id = :id")
    suspend fun updateReadStatus(id: Long, isRead: Boolean)

    @Query("UPDATE notifications SET isStarred = :isStarred WHERE id = :id")
    suspend fun updateStarredStatus(id: Long, isStarred: Boolean)
}

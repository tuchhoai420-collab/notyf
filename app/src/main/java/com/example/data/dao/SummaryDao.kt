package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AiSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    @Query("SELECT * FROM summaries ORDER BY timestamp DESC")
    fun getAllSummaries(): Flow<List<AiSummary>>

    @Query("SELECT * FROM summaries ORDER BY timestamp DESC LIMIT 1")
    fun getLatestSummary(): Flow<AiSummary?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: AiSummary): Long

    @Delete
    suspend fun delete(summary: AiSummary)

    @Query("DELETE FROM summaries")
    suspend fun clearAll()
}

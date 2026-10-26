package com.auraflow.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomPracticeDao {
    @Query("SELECT * FROM custom_practices ORDER BY created_at DESC")
    fun observeAll(): Flow<List<CustomPracticeEntity>>

    @Query("SELECT * FROM custom_practices WHERE id = :id")
    suspend fun get(id: Long): CustomPracticeEntity?

    @Upsert
    suspend fun upsert(practice: CustomPracticeEntity): Long

    @Query("DELETE FROM custom_practices WHERE id = :id")
    suspend fun delete(id: Long)
}

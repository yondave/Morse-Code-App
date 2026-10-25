package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MorseDao {

    @Query("SELECT * FROM morse_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MorseMessageEntity>>

    @Query("SELECT * FROM morse_messages WHERE type = :type ORDER BY timestamp DESC")
    fun getMessagesByType(type: String): Flow<List<MorseMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MorseMessageEntity): Long

    @Query("DELETE FROM morse_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    @Query("DELETE FROM morse_messages")
    suspend fun clearAll()
}

package com.example.data

import kotlinx.coroutines.flow.Flow

class MorseRepository(private val morseDao: MorseDao) {

    val allMessages: Flow<List<MorseMessageEntity>> = morseDao.getAllMessages()

    suspend fun saveMessage(type: String, text: String, morseCode: String, wpm: Int): Long {
        val entity = MorseMessageEntity(
            type = type,
            text = text,
            morseCode = morseCode,
            wpm = wpm
        )
        return morseDao.insertMessage(entity)
    }

    suspend fun deleteMessage(id: Long) {
        morseDao.deleteMessageById(id)
    }

    suspend fun clearHistory() {
        morseDao.clearAll()
    }
}

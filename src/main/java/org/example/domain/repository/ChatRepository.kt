package org.example.domain.repository

import org.example.domain.model.ChatMessage
import org.example.domain.model.ChatSession

interface ChatRepository {
    suspend fun sendMessage(
        session: ChatSession,
        userMessage: String
    ): Result<ChatMessage>
    
    suspend fun getHistory(sessionId: String): Result<List<ChatMessage>>
}

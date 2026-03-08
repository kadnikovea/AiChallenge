package org.example.data.context

import org.example.domain.model.ChatMessage
import org.example.domain.model.ChatSession

/**
 * Отвечает за формирование списка сообщений, отправляемых в LLM,
 * с учётом политики контекстного окна и summary.
 */
interface ContextWindowManager {
    suspend fun buildContextMessages(session: ChatSession): List<ChatMessage>
}

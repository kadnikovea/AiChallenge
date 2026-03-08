package org.example.data.context

import org.example.domain.model.ChatMessage

/**
 * Сервис, отвечающий за получение/обновление текстового summary ранней части диалога.
 *
 * Фактически это вспомогательный клиент к LLM, который вызывает модель с
 * "сервисным" промптом для суммаризации и возвращает только текст summary.
 */
interface ConversationSummarizer {

    suspend fun summarize(
        sessionId: String,
        existingSummary: String?,
        messagesToSummarize: List<ChatMessage>
    ): String
}

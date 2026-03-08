package org.example.data.persistence

/**
 * Состояние summary для конкретной сессии чата.
 *
 * Хранится отдельно от полной истории сообщений и описывает,
 * до какого сообщения диалог уже был «сжат» в summary и какой
 * именно текст summary сейчас считается актуальным.
 */
data class ConversationSummaryState(
    val sessionId: String,
    /** ID последнего сообщения, уже учтённого в summary (или null, если суммаризации ещё не было). */
    val lastSummarizedMessageId: String?,
    /** Текст summary всей истории до lastSummarizedMessageId (включительно). */
    val summary: String,
    val updatedAt: Long = System.currentTimeMillis()
)

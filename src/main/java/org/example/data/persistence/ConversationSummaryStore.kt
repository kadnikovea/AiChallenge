package org.example.data.persistence

/**
 * Отдельное хранилище для summary по сессиям.
 *
 * Полная история сообщений остаётся в HistoryStore, а здесь мы
 * поддерживаем компактное представление ранней части диалога.
 */
interface ConversationSummaryStore {
    suspend fun getSummary(sessionId: String): ConversationSummaryState?
    suspend fun saveSummary(state: ConversationSummaryState)
}

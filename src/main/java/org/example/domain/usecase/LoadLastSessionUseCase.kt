package org.example.domain.usecase

import org.example.data.config.LoggerFactory
import org.example.data.persistence.HistoryStore
import org.example.domain.model.ChatSession

private val loadSessionLogger = LoggerFactory.getLogger()

/**
 * Use case для загрузки последней сохранённой сессии.
 *
 * Домен знает только абстракцию HistoryStore, а конкретная реализация
 * (SQLite, JSON, InMemory) выбирается на data-слое через DI.
 */
class LoadLastSessionUseCase(
    private val historyStore: HistoryStore
) {
    suspend operator fun invoke(): ChatSession? {
        return try {
            val sessionIds = historyStore.listSessions()
            val lastId = sessionIds.firstOrNull() ?: return null

            val session = historyStore.load(lastId)
            if (session == null) {
                loadSessionLogger.warn { "HistoryStore returned null for session id $lastId" }
            } else {
                loadSessionLogger.info { "Loaded last session $lastId from history store" }
            }
            session
        } catch (e: Exception) {
            loadSessionLogger.error(e) { "Failed to load last session from history store" }
            null
        }
    }
}

package org.example.domain.usecase

import org.example.data.config.LoggerFactory
import org.example.data.persistence.HistoryStore
import org.example.domain.model.TokenUsage
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
                // Enrich messages with usage information, if available
                runCatching {
                    val usageRecords = historyStore.getUsageForSession(lastId)
                    if (usageRecords.isNotEmpty()) {
                        val usageByMessageId = usageRecords.associate { record ->
                            record.messageId to TokenUsage(
                                inputTokens = record.usage.input_tokens,
                                outputTokens = record.usage.output_tokens,
                                totalTokens = record.usage.total_tokens
                            )
                        }

                        val messages = session.messages
                        for (i in messages.indices) {
                            val msg = messages[i]
                            val usage = usageByMessageId[msg.id]
                            if (usage != null) {
                                messages[i] = msg.copy(usage = usage)
                            }
                        }
                    }
                }.onFailure {
                    loadSessionLogger.warn(it) { "Failed to enrich session $lastId with usage information" }
                }

                loadSessionLogger.info { "Loaded last session $lastId from history store" }
            }
            session
        } catch (e: Exception) {
            loadSessionLogger.error(e) { "Failed to load last session from history store" }
            null
        }
    }
}

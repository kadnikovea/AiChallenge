package org.example.data.context

import org.example.data.config.Config
import org.example.data.config.LoggerFactory
import org.example.data.persistence.ConversationSummaryState
import org.example.data.persistence.ConversationSummaryStore
import org.example.domain.model.ChatMessage
import org.example.domain.model.ChatSession
import org.example.domain.model.Role

private val contextLogger = LoggerFactory.getLogger()

class ContextWindowManagerImpl(
    private val config: Config,
    private val summaryStore: ConversationSummaryStore,
    private val summarizer: ConversationSummarizer
) : ContextWindowManager {

    override suspend fun buildContextMessages(session: ChatSession): List<ChatMessage> {
        val messages = session.messages

        // Если суммаризация выключена или сообщений мало, отправляем всё как есть
        if (!config.enableContextSummarization || messages.size <= config.maxContextMessages) {
            val allMessages = mutableListOf<ChatMessage>()
            if (session.systemPrompt.isNotEmpty()) {
                allMessages += ChatMessage(role = Role.SYSTEM, content = session.systemPrompt)
            }
            allMessages += messages
            return allMessages
        }

        val contextMessages = mutableListOf<ChatMessage>()

        // 1. Всегда добавляем исходный system prompt
        if (session.systemPrompt.isNotEmpty()) {
            contextMessages += ChatMessage(role = Role.SYSTEM, content = session.systemPrompt)
        }

        // 2. Вычисляем границу между «старой» частью и последними N сообщениями
        val contextStartIndex = (messages.size - config.maxContextMessages).coerceAtLeast(0)

        // 3. Обновляем summary при необходимости
        val updatedSummaryState = updateSummaryIfNeeded(session, contextStartIndex)

        // 4. Если есть актуальное summary — добавляем его отдельным system-сообщением
        val summaryText = updatedSummaryState?.summary?.takeIf { it.isNotBlank() }
        if (summaryText != null) {
            contextMessages += ChatMessage(
                role = Role.SYSTEM,
                content = "Summary of earlier conversation (use this instead of missing messages):\n$summaryText"
            )
        }

        // 5. Добавляем последние N сообщений «как есть»
        contextMessages += messages.takeLast(config.maxContextMessages)

        contextLogger.debug {
            "Built context for session=${session.id}: " +
                "totalMessages=${messages.size}, " +
                "sentMessages=${contextMessages.size}, " +
                "withSummary=${summaryText != null}"
        }

        return contextMessages
    }

    private suspend fun updateSummaryIfNeeded(
        session: ChatSession,
        contextStartIndex: Int
    ): ConversationSummaryState? {
        if (contextStartIndex <= 0) return null

        val messages = session.messages

        val currentState = summaryStore.getSummary(session.id)

        // Если summary ещё не было — суммаризируем всё до contextStartIndex
        if (currentState == null) {
            val toSummarize = messages.subList(0, contextStartIndex)
            if (toSummarize.isEmpty()) return null

            val summaryText = summarizer.summarize(session.id, null, toSummarize)
            if (summaryText.isBlank()) return null

            val lastId = messages[contextStartIndex - 1].id
            val newState = ConversationSummaryState(
                sessionId = session.id,
                lastSummarizedMessageId = lastId,
                summary = summaryText.trim()
            )
            summaryStore.saveSummary(newState)
            return newState
        }

        // Если summary уже есть — проверяем, накопилось ли достаточно новых сообщений
        val lastIdx = messages.indexOfFirst { it.id == currentState.lastSummarizedMessageId }
        val startIdx = if (lastIdx == -1) 0 else lastIdx + 1
        val endIdx = contextStartIndex

        if (endIdx <= startIdx) {
            // Новых сообщений в «старой» части нет — используем существующее summary
            return currentState
        }

        val newCount = endIdx - startIdx
        if (newCount < config.summaryUpdateStep) {
            // Ещё рано пересуммаризировать
            return currentState
        }

        val toSummarize = messages.subList(startIdx, endIdx)
        if (toSummarize.isEmpty()) return currentState

        val mergedSummary = summarizer.summarize(session.id, currentState.summary, toSummarize)
        if (mergedSummary.isBlank()) return currentState

        val lastId = messages[contextStartIndex - 1].id
        val newState = currentState.copy(
            lastSummarizedMessageId = lastId,
            summary = mergedSummary.trim(),
            updatedAt = System.currentTimeMillis()
        )
        summaryStore.saveSummary(newState)
        return newState
    }
}

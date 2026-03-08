package org.example.data.persistence

/**
 * In-memory реализация хранилища summary.
 * Используется, когда persistHistory=false или для тестов.
 */
class InMemoryConversationSummaryStore : ConversationSummaryStore {

    private val summaries = mutableMapOf<String, ConversationSummaryState>()

    override suspend fun getSummary(sessionId: String): ConversationSummaryState? =
        summaries[sessionId]

    override suspend fun saveSummary(state: ConversationSummaryState) {
        summaries[state.sessionId] = state
    }
}

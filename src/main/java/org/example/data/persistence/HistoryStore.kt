package org.example.data.persistence

import org.example.data.api.model.Usage
import org.example.domain.model.ChatSession

/**
 * Abstraction for chat history persistence.
 *
 * In addition to storing full chat sessions, this interface also exposes
 * per-call token usage information coming from the LLM provider response
 * (see [org.example.data.api.model.Usage]).
 */
interface HistoryStore {
    /** Persist full chat session state. */
    suspend fun save(session: ChatSession)

    /** Load full chat session state by id. */
    suspend fun load(sessionId: String): ChatSession?

    /** List known session ids in reverse chronological order when possible. */
    suspend fun listSessions(): List<String>

    /**
     * Persist usage information for a single LLM call (typically one assistant message).
     *
     * Implementations may choose to ignore null usage.
     */
    suspend fun saveUsage(sessionId: String, messageId: String, usage: Usage?)

    /** Return all usage records for a given session, ordered by insertion. */
    suspend fun getUsageForSession(sessionId: String): List<UsageRecord>

    /** Return usage for a particular message id, or null if none recorded. */
    suspend fun getUsageForMessage(messageId: String): UsageRecord?
}

/**
 * Wrapper combining a message identifier, its owning session and the raw
 * [Usage] object returned by the provider.
 */
data class UsageRecord(
    val sessionId: String,
    val messageId: String,
    val usage: Usage
)

package org.example.data.persistence

import org.example.domain.model.ChatSession

interface HistoryStore {
    suspend fun save(session: ChatSession)
    suspend fun load(sessionId: String): ChatSession?
    suspend fun listSessions(): List<String>
}

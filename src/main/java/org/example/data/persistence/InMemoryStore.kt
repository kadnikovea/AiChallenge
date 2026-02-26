package org.example.data.persistence

import mu.KotlinLogging
import org.example.domain.model.ChatSession

private val logger = KotlinLogging.logger {}

class InMemoryStore : HistoryStore {
    private val sessions = mutableMapOf<String, ChatSession>()
    
    override suspend fun save(session: ChatSession) {
        sessions[session.id] = session
        logger.debug { "Session ${session.id} saved to memory" }
    }
    
    override suspend fun load(sessionId: String): ChatSession? {
        return sessions[sessionId]
    }
    
    override suspend fun listSessions(): List<String> {
        return sessions.keys.toList()
    }
}

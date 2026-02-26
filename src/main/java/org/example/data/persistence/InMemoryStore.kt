package org.example.data.persistence

import mu.KotlinLogging
import org.example.data.config.LoggerFactory
import org.example.domain.model.ChatSession

private val logger = LoggerFactory.getLogger()

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

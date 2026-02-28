package org.example.data.persistence

import org.example.data.config.LoggerFactory
import org.example.data.api.model.Usage
import org.example.domain.model.ChatSession

private val logger = LoggerFactory.getLogger()

class InMemoryStore : HistoryStore {
    private val sessions = mutableMapOf<String, ChatSession>()
    private val usageBySession = mutableMapOf<String, MutableList<UsageRecord>>()
    
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

    override suspend fun saveUsage(sessionId: String, messageId: String, usage: Usage?) {
        if (usage == null) return
        val list = usageBySession.getOrPut(sessionId) { mutableListOf() }
        list.add(UsageRecord(sessionId = sessionId, messageId = messageId, usage = usage))
        logger.debug { "Usage saved in memory for session=$sessionId, message=$messageId" }
    }

    override suspend fun getUsageForSession(sessionId: String): List<UsageRecord> {
        return usageBySession[sessionId]?.toList() ?: emptyList()
    }

    override suspend fun getUsageForMessage(messageId: String): UsageRecord? {
        // Linear search is fine for in-memory store; can be optimized later if needed.
        return usageBySession.values
            .asSequence()
            .flatMap { it.asSequence() }
            .firstOrNull { it.messageId == messageId }
    }
}

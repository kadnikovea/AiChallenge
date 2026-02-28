package org.example.data.persistence

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.example.data.config.LoggerFactory
import org.example.data.api.model.Usage
import org.example.domain.model.ChatSession
import java.io.File

private val logger = LoggerFactory.getLogger()

class JsonHistoryStore(private val historyPath: String) : HistoryStore {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    
    init {
        File(historyPath).mkdirs()
        logger.info { "History store initialized at: $historyPath" }
    }
    
    override suspend fun save(session: ChatSession) {
        try {
            val file = File(historyPath, "${session.id}.json")
            file.writeText(gson.toJson(session))
            logger.debug { "Session ${session.id} saved to ${file.absolutePath}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to save session ${session.id}" }
        }
    }
    
    override suspend fun load(sessionId: String): ChatSession? {
        return try {
            val file = File(historyPath, "$sessionId.json")
            if (file.exists()) {
                gson.fromJson(file.readText(), ChatSession::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to load session $sessionId" }
            null
        }
    }
    
    override suspend fun listSessions(): List<String> {
        return try {
            File(historyPath)
                .listFiles { file -> file.extension == "json" }
                ?.map { it.nameWithoutExtension }
                ?: emptyList()
        } catch (e: Exception) {
            logger.error(e) { "Failed to list sessions" }
            emptyList()
        }
    }

    override suspend fun saveUsage(sessionId: String, messageId: String, usage: Usage?) {
        // Not required for your task; keep behavior explicit
        if (usage == null) return
        logger.warn { "Usage persistence not implemented for JsonHistoryStore; ignoring usage for session=$sessionId, message=$messageId" }
    }

    override suspend fun getUsageForSession(sessionId: String): List<UsageRecord> {
        logger.warn { "Usage retrieval by session not implemented for JsonHistoryStore; returning empty list for session=$sessionId" }
        return emptyList()
    }

    override suspend fun getUsageForMessage(messageId: String): UsageRecord? {
        logger.warn { "Usage retrieval by message not implemented for JsonHistoryStore; returning null for message=$messageId" }
        return null
    }
}

package org.example.data.persistence

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import mu.KotlinLogging
import org.example.domain.model.ChatSession
import java.io.File

private val logger = KotlinLogging.logger {}

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
}

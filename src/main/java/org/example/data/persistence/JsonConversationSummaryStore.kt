package org.example.data.persistence

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.example.data.config.LoggerFactory
import java.io.File

private val jsonSummaryLogger = LoggerFactory.getLogger()

/**
 * JSON-персистентность для summary: по одному файлу на сессию.
 * Файл хранится рядом с основной историей: `<sessionId>.summary.json`.
 */
class JsonConversationSummaryStore(private val historyPath: String) : ConversationSummaryStore {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    init {
        File(historyPath).mkdirs()
        jsonSummaryLogger.info { "ConversationSummaryStore (JSON) initialized at: $historyPath" }
    }

    override suspend fun getSummary(sessionId: String): ConversationSummaryState? {
        return try {
            val file = File(historyPath, "$sessionId.summary.json")
            if (!file.exists()) return null
            gson.fromJson(file.readText(), ConversationSummaryState::class.java)
        } catch (e: Exception) {
            jsonSummaryLogger.error(e) { "Failed to load summary for session=$sessionId" }
            null
        }
    }

    override suspend fun saveSummary(state: ConversationSummaryState) {
        try {
            val file = File(historyPath, "${state.sessionId}.summary.json")
            file.writeText(gson.toJson(state))
            jsonSummaryLogger.debug { "Summary for session=${state.sessionId} saved to ${file.absolutePath}" }
        } catch (e: Exception) {
            jsonSummaryLogger.error(e) { "Failed to save summary for session=${state.sessionId}" }
        }
    }
}

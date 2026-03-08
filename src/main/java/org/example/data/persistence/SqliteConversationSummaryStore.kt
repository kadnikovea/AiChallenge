package org.example.data.persistence

import org.example.data.config.LoggerFactory
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

private val sqliteSummaryLogger = LoggerFactory.getLogger()

/**
 * SQLite-хранилище summary. Использует ту же БД, что и SqliteHistoryStore,
 * но отдельную таблицу `session_summaries`.
 */
class SqliteConversationSummaryStore(private val dbPath: String) : ConversationSummaryStore {

    private val jdbcUrl: String = "jdbc:sqlite:$dbPath"

    init {
        // Ensure parent directory exists if dbPath has one
        File(dbPath).parentFile?.let { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }

        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS session_summaries (
                            session_id TEXT PRIMARY KEY,
                            last_message_id TEXT,
                            summary TEXT NOT NULL,
                            updated_at INTEGER NOT NULL
                        );
                        """.trimIndent()
                    )
                }
            }

            sqliteSummaryLogger.info { "SQLite ConversationSummaryStore initialized at: $dbPath" }
        } catch (e: Exception) {
            sqliteSummaryLogger.error(e) { "Failed to initialize SQLite ConversationSummaryStore at $dbPath" }
        }
    }

    private fun getConnection(): Connection = DriverManager.getConnection(jdbcUrl)

    override suspend fun getSummary(sessionId: String): ConversationSummaryState? {
        return try {
            getConnection().use { conn ->
                conn.prepareStatement(
                    """
                    SELECT session_id, last_message_id, summary, updated_at
                    FROM session_summaries
                    WHERE session_id = ?
                    LIMIT 1
                    """.trimIndent()
                ).use { ps ->
                    ps.setString(1, sessionId)
                    ps.executeQuery().use { rs ->
                        if (!rs.next()) return null

                        ConversationSummaryState(
                            sessionId = rs.getString("session_id"),
                            lastSummarizedMessageId = rs.getString("last_message_id"),
                            summary = rs.getString("summary"),
                            updatedAt = rs.getLong("updated_at")
                        )
                    }
                }
            }
        } catch (e: Exception) {
            sqliteSummaryLogger.error(e) { "Failed to load summary for session=$sessionId from SQLite" }
            null
        }
    }

    override suspend fun saveSummary(state: ConversationSummaryState) {
        try {
            getConnection().use { conn ->
                conn.prepareStatement(
                    """
                    INSERT INTO session_summaries (session_id, last_message_id, summary, updated_at)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT(session_id) DO UPDATE SET
                        last_message_id = excluded.last_message_id,
                        summary = excluded.summary,
                        updated_at = excluded.updated_at
                    """.trimIndent()
                ).use { ps ->
                    ps.setString(1, state.sessionId)
                    ps.setString(2, state.lastSummarizedMessageId)
                    ps.setString(3, state.summary)
                    ps.setLong(4, state.updatedAt)
                    ps.executeUpdate()
                }
            }
            sqliteSummaryLogger.debug { "Summary for session=${state.sessionId} saved to SQLite at $dbPath" }
        } catch (e: Exception) {
            sqliteSummaryLogger.error(e) { "Failed to save summary for session=${state.sessionId} to SQLite" }
        }
    }
}

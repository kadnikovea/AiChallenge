package org.example.data.persistence

import org.example.data.api.model.InputTokensDetails
import org.example.data.api.model.OutputTokensDetails
import org.example.data.api.model.Usage
import org.example.data.config.LoggerFactory
import org.example.domain.model.ChatMessage
import org.example.domain.model.ChatSession
import org.example.domain.model.Role
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

private val sqliteLogger = LoggerFactory.getLogger()

class SqliteHistoryStore(private val dbPath: String) : HistoryStore {

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
                        CREATE TABLE IF NOT EXISTS sessions (
                            id TEXT PRIMARY KEY,
                            system_prompt TEXT NOT NULL,
                            created_at INTEGER NOT NULL
                        );
                        """.trimIndent()
                    )

                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS messages (
                            id TEXT PRIMARY KEY,
                            session_id TEXT NOT NULL,
                            role TEXT NOT NULL,
                            content TEXT NOT NULL,
                            timestamp INTEGER NOT NULL,
                            seq INTEGER NOT NULL,
                            FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE
                        );
                        """.trimIndent()
                    )

                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS usage (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            session_id TEXT NOT NULL,
                            message_id TEXT NOT NULL,
                            input_tokens INTEGER,
                            cached_tokens INTEGER,
                            output_tokens INTEGER,
                            reasoning_tokens INTEGER,
                            total_tokens INTEGER,
                            created_at INTEGER NOT NULL,
                            FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE
                        );
                        """.trimIndent()
                    )

                    stmt.executeUpdate(
                        """
                        CREATE INDEX IF NOT EXISTS idx_messages_session_seq
                        ON messages(session_id, seq);
                        """.trimIndent()
                    )

                    stmt.executeUpdate(
                        """
                        CREATE INDEX IF NOT EXISTS idx_usage_session
                        ON usage(session_id);
                        """.trimIndent()
                    )

                    stmt.executeUpdate(
                        """
                        CREATE INDEX IF NOT EXISTS idx_usage_message
                        ON usage(message_id);
                        """.trimIndent()
                    )
                }
            }

            sqliteLogger.info { "SQLite history store initialized at: $dbPath" }
        } catch (e: Exception) {
            sqliteLogger.error(e) { "Failed to initialize SQLite history store at $dbPath" }
        }
    }

    private fun getConnection(): Connection = DriverManager.getConnection(jdbcUrl)

    override suspend fun save(session: ChatSession) {
        try {
            getConnection().use { conn ->
                conn.autoCommit = false
                try {
                    // Upsert session
                    conn.prepareStatement(
                        """
                        INSERT INTO sessions (id, system_prompt, created_at)
                        VALUES (?, ?, ?)
                        ON CONFLICT(id) DO UPDATE SET
                            system_prompt = excluded.system_prompt,
                            created_at = excluded.created_at
                        """.trimIndent()
                    ).use { ps ->
                        ps.setString(1, session.id)
                        ps.setString(2, session.systemPrompt)
                        ps.setLong(3, session.createdAt)
                        ps.executeUpdate()
                    }

                    // Remove old messages for this session
                    conn.prepareStatement("DELETE FROM messages WHERE session_id = ?").use { ps ->
                        ps.setString(1, session.id)
                        ps.executeUpdate()
                    }

                    // Insert messages in order
                    conn.prepareStatement(
                        """
                        INSERT INTO messages (id, session_id, role, content, timestamp, seq)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """.trimIndent()
                    ).use { ps ->
                        session.messages.forEachIndexed { index, message ->
                            ps.setString(1, message.id)
                            ps.setString(2, session.id)
                            ps.setString(3, message.role.name)
                            ps.setString(4, message.content)
                            ps.setLong(5, message.timestamp)
                            ps.setInt(6, index)
                            ps.addBatch()
                        }
                        ps.executeBatch()
                    }

                    conn.commit()
                    sqliteLogger.debug { "Session ${session.id} saved to SQLite at $dbPath" }
                } catch (e: Exception) {
                    conn.rollback()
                    throw e
                } finally {
                    conn.autoCommit = true
                }
            }
        } catch (e: Exception) {
            sqliteLogger.error(e) { "Failed to save session ${session.id} to SQLite" }
        }
    }

    override suspend fun load(sessionId: String): ChatSession? {
        return try {
            getConnection().use { conn ->
                val session = conn.prepareStatement(
                    "SELECT id, system_prompt, created_at FROM sessions WHERE id = ?"
                ).use { ps ->
                    ps.setString(1, sessionId)
                    ps.executeQuery().use { rs ->
                        if (!rs.next()) return null

                        val id = rs.getString("id")
                        val systemPrompt = rs.getString("system_prompt")
                        val createdAt = rs.getLong("created_at")

                        Triple(id, systemPrompt, createdAt)
                    }
                }

                val messages = mutableListOf<ChatMessage>()
                conn.prepareStatement(
                    "SELECT id, role, content, timestamp, seq FROM messages WHERE session_id = ? ORDER BY seq ASC"
                ).use { ps ->
                    ps.setString(1, sessionId)
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            val id = rs.getString("id")
                            val role = Role.valueOf(rs.getString("role"))
                            val content = rs.getString("content")
                            val timestamp = rs.getLong("timestamp")

                            messages.add(
                                ChatMessage(
                                    id = id,
                                    role = role,
                                    content = content,
                                    timestamp = timestamp
                                )
                            )
                        }
                    }
                }

                ChatSession(
                    id = session.first,
                    systemPrompt = session.second,
                    messages = messages,
                    createdAt = session.third
                )
            }
        } catch (e: Exception) {
            sqliteLogger.error(e) { "Failed to load session $sessionId from SQLite" }
            null
        }
    }

    override suspend fun listSessions(): List<String> {
        return try {
            getConnection().use { conn ->
                conn.prepareStatement(
                    "SELECT id FROM sessions ORDER BY created_at DESC"
                ).use { ps ->
                    ps.executeQuery().use { rs ->
                        val result = mutableListOf<String>()
                        while (rs.next()) {
                            result.add(rs.getString("id"))
                        }
                        result
                    }
                }
            }
        } catch (e: Exception) {
            sqliteLogger.error(e) { "Failed to list sessions from SQLite" }
            emptyList()
        }
    }

    override suspend fun saveUsage(sessionId: String, messageId: String, usage: Usage?) {
        if (usage == null) return
        try {
            getConnection().use { conn ->
                conn.prepareStatement(
                    """
                    INSERT INTO usage (
                        session_id, message_id,
                        input_tokens, cached_tokens,
                        output_tokens, reasoning_tokens,
                        total_tokens, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                ).use { ps ->
                    ps.setString(1, sessionId)
                    ps.setString(2, messageId)
                    ps.setNullableInt(3, usage.input_tokens)
                    ps.setNullableInt(4, usage.input_tokens_details?.cached_tokens)
                    ps.setNullableInt(5, usage.output_tokens)
                    ps.setNullableInt(6, usage.output_tokens_details?.reasoning_tokens)
                    ps.setNullableInt(7, usage.total_tokens)
                    ps.setLong(8, System.currentTimeMillis())
                    ps.executeUpdate()
                }
            }
            sqliteLogger.debug { "Usage saved to SQLite for session=$sessionId, message=$messageId" }
        } catch (e: Exception) {
            sqliteLogger.error(e) { "Failed to save usage for session=$sessionId, message=$messageId" }
        }
    }

    override suspend fun getUsageForSession(sessionId: String): List<UsageRecord> {
        return try {
            getConnection().use { conn ->
                conn.prepareStatement(
                    """
                    SELECT session_id, message_id,
                           input_tokens, cached_tokens,
                           output_tokens, reasoning_tokens,
                           total_tokens
                    FROM usage
                    WHERE session_id = ?
                    ORDER BY id ASC
                    """.trimIndent()
                ).use { ps ->
                    ps.setString(1, sessionId)
                    ps.executeQuery().use { rs ->
                        val result = mutableListOf<UsageRecord>()
                        while (rs.next()) {
                            val messageId = rs.getString("message_id")
                            val inputTokens = rs.getNullableInt("input_tokens")
                            val cachedTokens = rs.getNullableInt("cached_tokens")
                            val outputTokens = rs.getNullableInt("output_tokens")
                            val reasoningTokens = rs.getNullableInt("reasoning_tokens")
                            val totalTokens = rs.getNullableInt("total_tokens")

                            val usage = Usage(
                                input_tokens = inputTokens,
                                input_tokens_details =
                                    if (cachedTokens != null) InputTokensDetails(cachedTokens) else null,
                                output_tokens = outputTokens,
                                output_tokens_details =
                                    if (reasoningTokens != null) OutputTokensDetails(reasoningTokens) else null,
                                total_tokens = totalTokens
                            )

                            result.add(UsageRecord(sessionId, messageId, usage))
                        }
                        result
                    }
                }
            }
        } catch (e: Exception) {
            sqliteLogger.error(e) { "Failed to load usage for session $sessionId from SQLite" }
            emptyList()
        }
    }

    override suspend fun getUsageForMessage(messageId: String): UsageRecord? {
        return try {
            getConnection().use { conn ->
                conn.prepareStatement(
                    """
                    SELECT session_id, message_id,
                           input_tokens, cached_tokens,
                           output_tokens, reasoning_tokens,
                           total_tokens
                    FROM usage
                    WHERE message_id = ?
                    LIMIT 1
                    """.trimIndent()
                ).use { ps ->
                    ps.setString(1, messageId)
                    ps.executeQuery().use { rs ->
                        if (!rs.next()) return null

                        val sessionId = rs.getString("session_id")
                        val inputTokens = rs.getNullableInt("input_tokens")
                        val cachedTokens = rs.getNullableInt("cached_tokens")
                        val outputTokens = rs.getNullableInt("output_tokens")
                        val reasoningTokens = rs.getNullableInt("reasoning_tokens")
                        val totalTokens = rs.getNullableInt("total_tokens")

                        val usage = Usage(
                            input_tokens = inputTokens,
                            input_tokens_details =
                                if (cachedTokens != null) InputTokensDetails(cachedTokens) else null,
                            output_tokens = outputTokens,
                            output_tokens_details =
                                if (reasoningTokens != null) OutputTokensDetails(reasoningTokens) else null,
                            total_tokens = totalTokens
                        )

                        UsageRecord(sessionId, messageId, usage)
                    }
                }
            }
        } catch (e: Exception) {
            sqliteLogger.error(e) { "Failed to load usage for message $messageId from SQLite" }
            null
        }
    }
}

private fun PreparedStatement.setNullableInt(index: Int, value: Int?) {
    if (value != null) setInt(index, value) else setNull(index, Types.INTEGER)
}

private fun ResultSet.getNullableInt(column: String): Int? {
    val value = getInt(column)
    return if (wasNull()) null else value
}

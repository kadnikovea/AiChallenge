package org.example.data.persistence

import org.example.data.config.LoggerFactory
import org.example.domain.model.ChatMessage
import org.example.domain.model.ChatSession
import org.example.domain.model.Role
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

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
                        CREATE INDEX IF NOT EXISTS idx_messages_session_seq
                        ON messages(session_id, seq);
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
}

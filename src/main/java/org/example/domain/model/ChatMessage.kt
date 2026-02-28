package org.example.domain.model

import java.util.UUID

/**
 * Single chat message within a session.
 *
 * Optionally carries [usage] information if this message corresponds
 * to an LLM response for which the provider returned token accounting.
 */

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val usage: TokenUsage? = null
)

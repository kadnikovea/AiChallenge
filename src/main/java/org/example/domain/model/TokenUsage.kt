package org.example.domain.model

/**
 * Domain-level representation of token usage for a single LLM call.
 *
 * This is intentionally decoupled from the data-layer API DTO
 * (org.example.data.api.model.Usage) to respect clean architecture.
 */
data class TokenUsage(
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val totalTokens: Int? = null
)

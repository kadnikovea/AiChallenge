package org.example.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class LlmRequest(
    val model: String,
    val messages: List<LlmMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int? = null
)

@Serializable
data class LlmMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAiProxyRequest(
    val model: String,
    val input: String
)

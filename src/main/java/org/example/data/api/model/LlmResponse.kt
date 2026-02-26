package org.example.data.api.model

import kotlinx.serialization.Serializable

// ProxyAPI Response Format
@Serializable
data class LlmResponse(
    val output: List<OutputEntry>? = null,
    val usage: Usage? = null
)

@Serializable
data class OutputEntry(
    val id: String? = null,
    val type: String? = null,
    val status: String? = null,
    val content: List<Content>? = null,
    val role: String? = null
)

@Serializable
data class Content(
    val type: String? = null,
    val text: String? = null
)

@Serializable
data class Usage(
    val input_tokens: Int? = null,
    val output_tokens: Int? = null,
    val total_tokens: Int? = null
)

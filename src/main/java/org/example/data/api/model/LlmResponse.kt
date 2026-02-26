package org.example.data.api.model

import kotlinx.serialization.Serializable

// OpenAI Response Format (Full)
@Serializable
data class LlmResponse(
    val id: String? = null,
    val `object`: String? = null,
    val created_at: Long? = null,
    val status: String? = null,
    val background: Boolean? = null,
    val billing: Billing? = null,
    val completed_at: Long? = null,
    val error: ApiError? = null,
    val frequency_penalty: Double? = null,
    val incomplete_details: String? = null,
    val instructions: String? = null,
    val max_output_tokens: Int? = null,
    val max_tool_calls: Int? = null,
    val model: String? = null,
    val output: List<OutputEntry>? = null,
    val parallel_tool_calls: Boolean? = null,
    val presence_penalty: Double? = null,
    val previous_response_id: String? = null,
    val prompt_cache_key: String? = null,
    val prompt_cache_retention: String? = null,
    val reasoning: Reasoning? = null,
    val safety_identifier: String? = null,
    val service_tier: String? = null,
    val store: Boolean? = null,
    val temperature: Double? = null,
    val text: TextConfig? = null,
    val tool_choice: String? = null,
    val tools: List<String>? = null,
    val top_logprobs: Int? = null,
    val top_p: Double? = null,
    val truncation: String? = null,
    val usage: Usage? = null,
    val user: String? = null,
    val metadata: Map<String, String>? = null
)

@Serializable
data class Billing(
    val payer: String? = null
)

@Serializable
data class Reasoning(
    val effort: String? = null,
    val summary: String? = null
)

@Serializable
data class TextConfig(
    val format: TextFormat? = null,
    val verbosity: String? = null
)

@Serializable
data class TextFormat(
    val type: String? = null
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
    val annotations: List<String>? = null,
    val logprobs: List<String>? = null,
    val text: String? = null
)

@Serializable
data class Usage(
    val input_tokens: Int? = null,
    val input_tokens_details: InputTokensDetails? = null,
    val output_tokens: Int? = null,
    val output_tokens_details: OutputTokensDetails? = null,
    val total_tokens: Int? = null
)

@Serializable
data class InputTokensDetails(
    val cached_tokens: Int? = null
)

@Serializable
data class OutputTokensDetails(
    val reasoning_tokens: Int? = null
)

@Serializable
data class ApiError(
    val message: String? = null,
    val type: String? = null,
    val param: String? = null,
    val code: String? = null
)

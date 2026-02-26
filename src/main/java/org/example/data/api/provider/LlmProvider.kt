package org.example.data.api.provider

import org.example.data.api.model.LlmResponse
import org.example.domain.model.ChatMessage

interface LlmProvider {
    suspend fun chat(messages: List<ChatMessage>, model: String): LlmResponse
}

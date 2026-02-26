package org.example.domain.model

import java.util.UUID

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val systemPrompt: String,
    val messages: MutableList<ChatMessage> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis()
)

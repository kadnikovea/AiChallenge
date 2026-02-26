package org.example.domain.usecase

import mu.KotlinLogging
import org.example.domain.model.ChatSession

private val logger = KotlinLogging.logger {}

class StartSessionUseCase {
    operator fun invoke(systemPrompt: String): ChatSession {
        logger.info { "Starting new chat session with system prompt: ${systemPrompt.take(50)}..." }
        return ChatSession(systemPrompt = systemPrompt)
    }
}

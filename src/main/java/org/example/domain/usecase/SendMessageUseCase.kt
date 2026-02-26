package org.example.domain.usecase

import mu.KotlinLogging
import org.example.data.config.LoggerFactory
import org.example.domain.model.ChatSession
import org.example.domain.repository.ChatRepository
import org.example.data.persistence.HistoryStore

private val logger = LoggerFactory.getLogger()

class SendMessageUseCase(
    private val repository: ChatRepository,
    private val historyStore: HistoryStore
) {
    suspend operator fun invoke(
        session: ChatSession,
        userMessage: String
    ): Result<Unit> = runCatching {
        logger.info { "Sending message: ${userMessage.take(50)}..." }
        
        val response = repository.sendMessage(session, userMessage)
        response.getOrThrow()
        
        // Persist history if enabled
        historyStore.save(session)
        
        logger.info { "Message sent successfully" }
    }
}

package org.example.data.repository

import org.example.data.api.provider.LlmProvider
import org.example.data.config.Config
import org.example.data.config.LoggerFactory
import org.example.data.context.ContextWindowManager
import org.example.data.persistence.HistoryStore
import org.example.domain.model.ChatMessage
import org.example.domain.model.ChatSession
import org.example.domain.model.Role
import org.example.domain.model.TokenUsage
import org.example.domain.repository.ChatRepository

private val logger = LoggerFactory.getLogger()

class ChatRepositoryImpl(
    private val provider: LlmProvider,
    private val config: Config,
    private val historyStore: HistoryStore,
    private val contextWindowManager: ContextWindowManager
) : ChatRepository {
    
    override suspend fun sendMessage(
        session: ChatSession,
        userMessage: String
    ): Result<ChatMessage> = runCatching {
        // Add user message to session
        val userMsg = ChatMessage(role = Role.USER, content = userMessage)
        session.messages.add(userMsg)

        // Построить контекст для LLM с учётом последних N сообщений и summary ранней части диалога
        val allMessages = contextWindowManager.buildContextMessages(session)

        logger.debug { "Sending ${allMessages.size} messages to LLM (session=${session.id})" }

        // Call LLM provider
        val response = provider.chat(allMessages, config.modelName)
        
        // Extract assistant message from OpenAI format
        val assistantContent = response.output
            ?.firstOrNull { it.role == "assistant" }
            ?.content
            ?.firstOrNull { it.type == "output_text" }
            ?.text
            ?: throw IllegalStateException("No assistant response found")
        
        val apiUsage = response.usage
        val messageUsage = apiUsage?.let {
            TokenUsage(
                inputTokens = it.input_tokens,
                outputTokens = it.output_tokens,
                totalTokens = it.total_tokens
            )
        }

        val assistantMessage = ChatMessage(
            role = Role.ASSISTANT,
            content = assistantContent,
            usage = messageUsage
        )

        session.messages.add(assistantMessage)

        // Persist per-call token usage if available
        runCatching {
            historyStore.saveUsage(session.id, assistantMessage.id, response.usage)
        }.onFailure {
            logger.warn(it) { "Failed to save usage for session=${session.id}, message=${assistantMessage.id}" }
        }

        logger.info { "Received response from LLM (${assistantContent.length} chars)" }

        assistantMessage
    }
    
    override suspend fun getHistory(sessionId: String): Result<List<ChatMessage>> = runCatching {
        // This would be implemented if we had session storage
        emptyList()
    }
}

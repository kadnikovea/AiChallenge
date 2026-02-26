package org.example.data.repository

import mu.KotlinLogging
import org.example.data.api.provider.LlmProvider
import org.example.data.config.Config
import org.example.domain.model.ChatMessage
import org.example.domain.model.ChatSession
import org.example.domain.model.Role
import org.example.domain.repository.ChatRepository

private val logger = KotlinLogging.logger {}

class ChatRepositoryImpl(
    private val provider: LlmProvider,
    private val config: Config
) : ChatRepository {
    
    override suspend fun sendMessage(
        session: ChatSession,
        userMessage: String
    ): Result<ChatMessage> = runCatching {
        // Add user message to session
        val userMsg = ChatMessage(role = Role.USER, content = userMessage)
        session.messages.add(userMsg)
        
        // Prepare messages including system prompt
        val allMessages = mutableListOf<ChatMessage>()
        if (session.systemPrompt.isNotEmpty()) {
            allMessages.add(ChatMessage(role = Role.SYSTEM, content = session.systemPrompt))
        }
        allMessages.addAll(session.messages)
        
        logger.debug { "Sending ${allMessages.size} messages to LLM" }
        
        // Call LLM provider
        val response = provider.chat(allMessages, config.modelName)
        
        // Extract assistant message from OpenAI format
        val assistantContent = response.output
            ?.firstOrNull { it.role == "assistant" }
            ?.content
            ?.firstOrNull { it.type == "output_text" }
            ?.text
            ?: throw IllegalStateException("No assistant response found")
        
        val assistantMessage = ChatMessage(
            role = Role.ASSISTANT,
            content = assistantContent
        )
        
        session.messages.add(assistantMessage)
        
        logger.info { "Received response from LLM (${assistantContent.length} chars)" }
        
        assistantMessage
    }
    
    override suspend fun getHistory(sessionId: String): Result<List<ChatMessage>> = runCatching {
        // This would be implemented if we had session storage
        emptyList()
    }
}

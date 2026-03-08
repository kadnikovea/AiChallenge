package org.example.data.context

import org.example.data.api.provider.LlmProvider
import org.example.data.config.Config
import org.example.data.config.LoggerFactory
import org.example.domain.model.ChatMessage
import org.example.domain.model.Role

private val summarizerLogger = LoggerFactory.getLogger()

/**
 * Реализация summarizer-а через основной LlmProvider.
 * Использует отдельный system-промпт и не меняет доменную модель.
 */
class ConversationSummarizerImpl(
    private val provider: LlmProvider,
    private val config: Config
) : ConversationSummarizer {

    override suspend fun summarize(
        sessionId: String,
        existingSummary: String?,
        messagesToSummarize: List<ChatMessage>
    ): String {
        if (messagesToSummarize.isEmpty()) return existingSummary ?: ""

        val systemPrompt = """
            You are a conversation summarizer for an internal CLI tool.
            Your task is to maintain a concise, up-to-date summary of a chat history.
            The summary must:
            - capture key goals, decisions, constraints and important context
            - be written in the same language as the conversation, if obvious
            - be self-contained (reader should understand the task from summary alone)
            - omit chit-chat and redundant details

            Return ONLY the summary text, without any explanations or formatting.
        """.trimIndent()

        val messages = mutableListOf<ChatMessage>()
        messages += ChatMessage(role = Role.SYSTEM, content = systemPrompt)

        val userContent = buildString {
            if (!existingSummary.isNullOrBlank()) {
                appendLine("Existing summary:")
                appendLine(existingSummary)
                appendLine()
                appendLine("New messages to integrate into the summary:")
            } else {
                appendLine("Conversation messages to summarize:")
            }

            messagesToSummarize.forEach { msg ->
                appendLine("[${msg.role}] ${msg.content}")
            }
        }

        messages += ChatMessage(role = Role.USER, content = userContent)

        summarizerLogger.debug { "Requesting summary update for session=$sessionId, messages=${messagesToSummarize.size}" }

        val response = provider.chat(messages, config.modelName)

        val assistantContent = response.output
            ?.firstOrNull { it.role == "assistant" }
            ?.content
            ?.firstOrNull { it.type == "output_text" }
            ?.text
            ?: ""

        return assistantContent.trim()
    }
}

package org.example.data.promptBuilder

/**
 * Строит профильное system-сообщение для LLM на основе клиентских конфигов
 * (context / constraints / style).
 *
 * Отдельный класс в data-слое, не завязанный на доменную модель.
 */
class PromptBuilder(
    private val contextConfig: String,
    private val constraintsConfig: String,
    private val styleConfig: String,
) {

    /**
     * Собирает единый system-prompt, описывающий контекст проекта,
     * архитектурные ограничения и стиль ответа.
     */
    fun buildProfileInstruction(): String = buildString {
        appendLine("PROJECT CONTEXT:")
        appendLine(contextConfig.trim())
        appendLine()

        appendLine("PROJECT CONSTRAINTS:")
        appendLine(constraintsConfig.trim())
        appendLine()

        appendLine("ANSWER STYLE:")
        appendLine(styleConfig.trim())
    }
}
package org.example

import java.io.File

/**
 * Вспомогательные утилиты для main: Markdown, разбор ответов моделей, работа с git-веткой.
 */
object LlmUtils {

    // === Markdown helpers ===

    /** Добавить строку в Markdown-буфер. */
    fun StringBuilder.printlnMd(text: String = "") {
        this.appendLine(text)
    }

    /** Оформить ответ модели как обычный текст (без блока кода). */
    fun StringBuilder.printModelAnswer(title: String, answer: String) {
        printlnMd("#### $title")
        printlnMd()
        answer.lines().forEach { printlnMd(it) }
        printlnMd()
    }

    // === Разбор ответов моделей ===

    fun getLastAssistantText(response: OpenAIProxyClient.OpenAIResponse?): String {
        val outputs = response?.output.orEmpty()
        val lastAssistant = outputs.lastOrNull { it.role == "assistant" }
        return lastAssistant?.content?.firstOrNull { it.text != null }?.text ?: "[нет ответа]"
    }

    // Gemini helper — безопасно получить текст первого кандидата
    fun getFirstGeminiText(response: OpenAIProxyClient.GeminiResponse?): String =
        response?.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?: "[нет ответа от Gemini]"

    // Claude helper — безопасно получить текст из первого content block
    fun getClaudeText(response: OpenAIProxyClient.ClaudeResponse?): String =
        response?.content
            ?.firstOrNull { it.type == "text" }
            ?.text
            ?: "[нет ответа от Claude]"

    // === Работа с git-веткой для имени файла отчёта ===

    fun currentBranch(): String = try {
        val proc = ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
            .directory(File("."))
            .redirectErrorStream(true)
            .start()
        proc.inputStream.bufferedReader().readLine()?.trim().orEmpty()
    } catch (_: Exception) {
        "unknown-branch"
    }

    fun sanitizeBranch(branch: String): String =
        branch.replace(Regex("[^\\w\\-]"), "_")
            .replace("__+".toRegex(), "_")
            .trim('_')

    // === Стоимость (грубая оценка) ===

    data class Pricing(val totalPer1K: Double)

    // Примерные цены за 1K токенов (условные, можно скорректировать под реальный тариф)
    val pricing: Map<String, Pricing> = mapOf(
        "gpt-4o-mini" to Pricing(totalPer1K = 0.150),
        "gpt-4.1" to Pricing(totalPer1K = 5.000),
        // Для Gemini берём условную цену, т.к. тариф зависит от аккаунта
        "gemini-2.0-flash-lite" to Pricing(totalPer1K = 0.200),
        // Для Claude 3.5 Sonnet берём условную цену
        "claude-3-5-sonnet-20241022" to Pricing(totalPer1K = 3.000)
    )

    fun estimateCostUsd(model: String, totalTokens: Int?): Double? {
        val p = pricing[model] ?: return null
        val tokens = totalTokens ?: return null
        return tokens / 1000.0 * p.totalPer1K
    }
}

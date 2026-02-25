package org.example

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.example.LlmUtils.printlnMd
import org.example.LlmUtils.printModelAnswer
import org.example.LlmUtils.getLastAssistantText
import org.example.LlmUtils.getFirstGeminiText
import org.example.LlmUtils.currentBranch
import org.example.LlmUtils.sanitizeBranch
import org.example.LlmUtils.estimateCostUsd
import kotlinx.coroutines.*
import kotlin.system.measureNanoTime

fun main() = runBlocking {
    val client = OpenAIProxyClient()

    val prompt = "Ты - гадалка. Нагадай в 4х предложениях, какой будет прогресс по моему проетку в этом году."

    data class ModelInfo(val id: String, val displayName: String)
    val models = listOf(
        ModelInfo("gpt-4o-mini", "GPT‑4o-mini"),
        ModelInfo("gpt-4.1", "GPT‑4.1"),
        ModelInfo("gemini-2.0-flash-lite", "Gemini 2.0 Flash Lite")
    )

    data class AnswerBlock(
        val modelId: String,
        val modelName: String,
        val durationMs: Double,
        val totalTokens: Int?,
        val costUsd: Double?,
        val output: String
    )

    val answers = mutableListOf<AnswerBlock>()

    // Асинхронные запросы к моделям
    val jobs = listOf(
        async {
            val t0 = System.nanoTime()
            val resp = client.getResponse(
                question = prompt,
                model = "gpt-4o-mini",
                temperature = 0.7f,
                maxOutputTokens = 300
            )
            val t1 = System.nanoTime()
            val answer = getLastAssistantText(resp)
            val tokens = resp?.usage?.total_tokens
            val cost = estimateCostUsd("gpt-4o-mini", tokens)
            AnswerBlock("gpt-4o-mini", "GPT‑4o-mini", (t1-t0)/1_000_000.0, tokens, cost, answer)
        },
        async {
            val t0 = System.nanoTime()
            val resp = client.getResponse(
                question = prompt,
                model = "gpt-4.1",
                temperature = 0.7f,
                maxOutputTokens = 300
            )
            val t1 = System.nanoTime()
            val answer = getLastAssistantText(resp)
            val tokens = resp?.usage?.total_tokens
            val cost = estimateCostUsd("gpt-4.1", tokens)
            AnswerBlock("gpt-4.1", "GPT‑4.1", (t1-t0)/1_000_000.0, tokens, cost, answer)
        },
        async {
            val t0 = System.nanoTime()
            val resp = client.getGeminiResponse(
                question = prompt,
                model = "gemini-2.0-flash-lite"
            )
            val t1 = System.nanoTime()
            val answer = getFirstGeminiText(resp)
            val tokens = resp?.usageMetadata?.totalTokenCount
            val cost = estimateCostUsd("gemini-2.0-flash-lite", tokens)
            AnswerBlock("gemini-2.0-flash-lite", "Gemini 2.0 Flash Lite", (t1-t0)/1_000_000.0, tokens, cost, answer)
        }
    )

    answers += jobs.awaitAll()

    // --- Генерация отчёта ---
    val resultsDir = File("results")
    if (!resultsDir.exists()) {
        resultsDir.mkdirs()
    }
    val branch = sanitizeBranch(currentBranch())
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    val resultFile = File(resultsDir, "${branch}_llm_fortune_${timestamp}.md")

    val md = StringBuilder()
    md.printlnMd("# Cравнение моделей LLM — гадание по проекту")
    md.printlnMd()
    md.printlnMd("**Дата:** ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))}")
    md.printlnMd("**Ветка:** $branch")
    md.printlnMd()
    md.printlnMd("---")
    md.printlnMd()
    md.printlnMd("### Вопрос для всех моделей")
    md.printlnMd()
    md.printlnMd("> $prompt")
    md.printlnMd()

    md.printlnMd("| Модель            | Время (ms) | Токены | Стоимость |")
    md.printlnMd("|-------------------|------------|--------|-----------|")
    for (ans in answers) {
        val tokensStr = ans.totalTokens?.toString() ?: "N/A"
        val costStr = ans.costUsd?.let { "%.5f".format(it) } ?: "N/A"
        md.printlnMd("| ${ans.modelName}  | ${"%.2f".format(ans.durationMs)} | $tokensStr | $costStr |")
    }
    md.printlnMd()
    for (ans in answers) {
        md.printModelAnswer("Ответ (${ans.modelName})", ans.output)
    }

    // --- Автоматическая оценка от GPT-4.1 ---
    val aggregatorPrompt = """
        Даны три ответа крупных LLM на один и тот же вопрос-гадание (прогноз по проекту в 2026 году):
        
        ${answers.joinToString("\n\n---\n\n") { ans -> "### Ответ модели: ${ans.modelName}\n${ans.output}" }}
        
        1. Оцени каждый ответ по следующим критериям (шкала 0-10):
        - Оригинальность формулировок
        - Достоверность и реалистичность гадания
        - Уникальность стиля и языка
        - Художественная выразительность
        
        2. Итог: выдели лучший ответ, кратко объясни почему.
        3. Составь одну фразу—рекомендацию, какой LLM лучше использовать для создания "пророчеств"/мотивационных гаданий по проектам.
        Пиши оценку в табличном виде + очень коротко в тексте.
    """.trimIndent()

    val tAutoStart = System.nanoTime()
    val verdictResp = client.getResponse(
        question = aggregatorPrompt,
        model = "gpt-4.1",
        temperature = 0.7f,
        maxOutputTokens = 500
    )
    val tAutoStop = System.nanoTime()
    val verdict = getLastAssistantText(verdictResp)

    md.printlnMd("---")
    md.printlnMd("### Auto-анализ и сравнительный вердикт от GPT-4.1")
    md.printlnMd()
    md.printlnMd(verdict)
    md.printlnMd()
    md.printlnMd("> (Время анализа: ${"%.2f".format((tAutoStop-tAutoStart)/1_000_000.0)} ms)")
    md.printlnMd()
    md.printlnMd("---")
    md.printlnMd("_Сгенерировано автоматически: гадание по проекту с помощью LLM — ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))}_")

    resultFile.writeText(md.toString())

    // ───── Красивый вывод в терминал ─────
    fun c(text: String, code: String) = "\u001B[${code}m$text\u001B[0m"
    val B = { t: String -> c(t, "1") }           // bold
    val CY = { t: String -> c(t, "36") }         // cyan
    val GR = { t: String -> c(t, "32") }         // green
    val YL = { t: String -> c(t, "33") }         // yellow
    val RD = { t: String -> c(t, "31") }         // red
    val BL = { t: String -> c(t, "34") }         // blue
    val MG = { t: String -> c(t, "35") }         // magenta

    val frameTop = "╔" + "═".repeat(58) + "╗"
    val frameBot = "╚" + "═".repeat(58) + "╝"
    val project = "🔮 СРАВНЕНИЕ LLM — ГАДАНИЕ ПО ПРОЕКТУ 🔮"
    println()
    println(frameTop)
    println("║" + " ".repeat((58-project.length)/2) + B(MG(project)) + " ".repeat(58 - (58-project.length)/2 - project.length) + "║")
    println(frameBot)
    println()
    println(CY("Дата: ")+LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")))
    println(CY("Файл отчёта: ")+resultFile.absolutePath)
    println()
    println(B("📊 РЕЗУЛЬТАТЫ:"))
    println("┌───────────────────┬─────────────┬────────┬────────────┐")
    println("│ Модель            │ Время (ms)  │ Токены │ Стоимость  │")
    println("├───────────────────┼─────────────┼────────┼────────────┤")
    answers.forEach { ans ->
        val tokensStr = ans.totalTokens?.toString() ?: "N/A"
        val costStr = ans.costUsd?.let { "%.5f".format(it) } ?: "N/A"
        println("│ %-17s │ %9.2f   │ %6s │ %10s │".format(ans.modelName, ans.durationMs, tokensStr, costStr))
    }
    println("└───────────────────┴─────────────┴────────┴────────────┘")
    // Определим лучший ответ (по итогам анализа GPT-4.1)
    val gptVerdict = verdict ?: ""
    val verdictLine = gptVerdict.lines().find { it.contains("Лучший ответ", ignoreCase = true) }
    val best = verdictLine?.replace("Лучший ответ", "")?.replace(":", "")?.replace("-", "")?.trim()
    if (!best.isNullOrEmpty()) {
        println(B(GR("✨ Лучший: $best ✨")))
    }
    println()
    println(YL("→ Открыть полный отчёт:") + " " + resultFile.absolutePath)
    println()
}

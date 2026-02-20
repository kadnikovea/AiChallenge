package org.example

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun getLastAssistantText(response: OpenAIProxyClient.OpenAIResponse?): String {
    val outputs = response?.output.orEmpty()
    val lastAssistant = outputs.lastOrNull { it.role == "assistant" }
    return lastAssistant?.content?.firstOrNull { it.text != null }?.text ?: "[нет ответа]"
}

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
    branch.replace(Regex("[^\\w\\-]"), "_").replace("__+", "_").trim('_')

fun main() {
    val client = OpenAIProxyClient()

    // Создаём папку results, если её нет
    val resultsDir = File("results")
    if (!resultsDir.exists()) {
        resultsDir.mkdirs()
    }

    // Имя ветки и файла
    val branch = sanitizeBranch(currentBranch())
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    val resultFile = File(resultsDir, "${branch}_temperature_experiment_$timestamp.md")

    // StringBuilder для накопления Markdown-контента
    val md = StringBuilder()

    fun printlnMd(text: String = "") {
        println(text)
        md.appendLine(text)
    }

    // ════════════════════════════════════════════════════════════════════════
    // ЗАГОЛОВОК
    // ════════════════════════════════════════════════════════════════════════

    printlnMd("# Эксперимент: Влияние temperature на ответы модели")
    printlnMd()
    printlnMd("**Дата:** ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))}")
    printlnMd("**Ветка:** $branch")
    printlnMd()
    printlnMd("---")
    printlnMd()

    // ════════════════════════════════════════════════════════════════════════
    // ЗАДАЧИ
    // ════════════════════════════════════════════════════════════════════════

    val tasks = listOf(
        Triple(
            "ЛОГИЧЕСКАЯ",
            "Все кошки — животные. Некоторые животные умеют плавать. " +
                    "Можно ли утверждать, что некоторые кошки умеют плавать? Объясни.",
            "логическая задача на проверку правильности силлогизма"
        ),
        Triple(
            "МАТЕМАТИЧЕСКАЯ",
            "Найди сумму всех натуральных чисел от 1 до 100, кратных 3 или 5. " +
                    "Покажи ход решения.",
            "математическая задача на арифметику"
        ),
        Triple(
            "ТВОРЧЕСКАЯ",
            "Напиши короткое стихотворение (4 строки) о том, " +
                    "как искусственный интеллект смотрит на закат.",
            "творческая задача на написание стихотворения"
        )
    )

    val temperatures = listOf(
        0.0f to "точность, предсказуемость",
        0.7f to "баланс",
        1.2f to "креативность, разнообразие"
    )

    // ════════════════════════════════════════════════════════════════════════
    // ВЫПОЛНЕНИЕ ЗАПРОСОВ
    // ════════════════════════════════════════════════════════════════════════

    val answersByTask = mutableListOf<Triple<String, String, List<Pair<Float, String>>>>()

    for ((taskIndex, taskInfo) in tasks.withIndex()) {
        val (taskType, taskQuestion, taskDesc) = taskInfo

        printlnMd("## Задача ${taskIndex + 1}: $taskType")
        printlnMd()
        printlnMd("**Описание:** $taskDesc")
        printlnMd()
        printlnMd("**Вопрос:** $taskQuestion")
        printlnMd()

        val answerVariants = mutableListOf<Pair<Float, String>>()
        for ((temp, tempDesc) in temperatures) {
            printlnMd("### temperature = $temp ($tempDesc)")
            printlnMd()

            val response = client.getResponse(
                question = taskQuestion,
                temperature = temp,
                maxOutputTokens = 500
            )
            val answer = getLastAssistantText(response)
            answerVariants.add(temp to answer)

            printlnMd("```")
            answer.lines().forEach { line ->
                printlnMd(line)
            }
            printlnMd("```")
            printlnMd()
        }

        answersByTask += Triple(taskType, taskQuestion, answerVariants)
        printlnMd("---")
        printlnMd()
    }

    // ════════════════════════════════════════════════════════════════════════
    // Итоговый анализ от модели — по результатам эксперимента
    // ════════════════════════════════════════════════════════════════════════

    val analysisPrompt = """
        Ниже приведён отчёт в формате Markdown с результатами эксперимента по трём задачам
        (логическая, математическая, творческая) и трём значениям параметра temperature (0.0, 0.7, 1.2).

        Твоя задача:
        1) Сравни ответы по точности, креативности и разнообразию для КАЖДОЙ задачи и каждого значения temperature.
        2) Красиво и понятно оформи вывод на русском в формате Markdown (заголовки, списки, таблицы — по желанию).
        3) В конце сделай чёткий итог: для каких типов задач лучше всего подходит каждое значение temperature.

        Вот результаты эксперимента (Markdown):
        ---
        $md
        ---
    """.trimIndent()

    val summaryResponse = client.getResponse(
        question = analysisPrompt,
        temperature = 0.3f,
        maxOutputTokens = 800
    )
    val summary = getLastAssistantText(summaryResponse)

    println()
    printlnMd("## Итоговый анализ модели")
    printlnMd()
    summary.lines().forEach { printlnMd(it) }
    printlnMd()
    printlnMd("*Файл сохранён как `${resultFile.name}`*")
    resultFile.writeText(md.toString())
    println()
    println("═".repeat(70))
    println(" Результат сохранён в: ${resultFile.absolutePath}")
    println("═".repeat(70))
}
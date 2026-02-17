package org.example

fun getLastAssistantText(response: OpenAIProxyClient.OpenAIResponse?): String {
    val outputs = response?.output.orEmpty()
    val lastAssistant = outputs.lastOrNull { it.role == "assistant" }
    return lastAssistant?.content?.firstOrNull { it.text != null }?.text ?: ""
}

fun main() {
    val client = OpenAIProxyClient()
    val prompt = """
        Расскажи об атомной энергетике
        Ответь в виде двух четверостиший (две строфы по четыре строки на русском языке).
    """.trimIndent()
    val model = null // или "gpt-5.1-codex-mini"

    // Первый запрос — с доп. параметрами из config.properties
    val responseWithExtras = client.getResponse(prompt, model, onlyCoreFromConfig = false)
    val textWithExtras = getLastAssistantText(responseWithExtras)

    // Второй запрос — только api_url/api_key/model (без остальных)
    val responseCoreOnly = client.getResponse(prompt, model, onlyCoreFromConfig = true)
    val textCoreOnly = getLastAssistantText(responseCoreOnly)

    println("Вы>   $prompt\n")

    println("=== Ответ 1: с доп. параметрами из config.properties ===")
    println("Модель[extras]>   $textWithExtras\n")

    println("=== Ответ 2: только api_url/api_key/model (без доп. параметров) ===")
    println("Модель[core]>   $textCoreOnly")
}

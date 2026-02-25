package org.example

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.util.*

class OpenAIProxyClient {
    private val config: Properties = Properties()
    private val apiUrl: String
    private val apiKey: String
    private val gson = Gson()
    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000  // 2 минуты — для сложных запросов (эксперты, двухэтапный)
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 120_000
        }
    }

    // Gemini/Google specific config fields
    private val googleApiBase: String
    private val googleApiUrl: String
    private val googleApiKey: String
    private val googleModel: String

    // Anthropic/Claude specific config fields
    private val anthropicApiBase: String
    private val anthropicModel: String
    private val anthropicApiKey: String

    init {
        val resource = this::class.java.classLoader.getResourceAsStream("config.properties")
            ?: throw IllegalStateException("config.properties not found in resources!")
        config.load(resource)
        apiUrl = config.getProperty("api_url").trim()
        apiKey = config.getProperty("api_key", "").trim()

        googleApiBase = config.getProperty("google_api_base", "https://api.proxyapi.ru/google").trim()
        googleApiUrl = config.getProperty("google_api_url", "").trim()
        googleApiKey = config.getProperty("google_api_key", "").trim()
        googleModel = config.getProperty("google_model", "gemini-2.0-flash-lite").trim()

        anthropicApiBase = config.getProperty("anthropic_api_base", "https://api.proxyapi.ru/anthropic").trim()
        anthropicModel = config.getProperty("anthropic_model", "claude-3-5-haiku-20241022").trim()
        anthropicApiKey = config.getProperty("anthropic_api_key", "").trim()
    }

    data class Message(val role: String?, val content: String?)
    data class Content(val type: String?, val text: String?)
    data class OutputEntry(
        val id: String?,
        val type: String?,
        val status: String?,
        val content: List<Content>?,
        val role: String?
    )

    // Информация об использованных токенах (если ProxyAPI её возвращает)
    data class Usage(
        val input_tokens: Int?,
        val output_tokens: Int?,
        val total_tokens: Int?
    )

    data class OpenAIResponse(
        val output: List<OutputEntry>?,
        val usage: Usage?
    )

    // === Gemini / Google ProxyAPI DTOs ===
    // Структура упрощена: берём только текст первого кандидата.
    data class GeminiTextPart(val text: String?)
    data class GeminiContentBlock(val parts: List<GeminiTextPart>?)
    data class GeminiCandidate(val content: GeminiContentBlock?)
    data class GeminiUsageMetadata(val totalTokenCount: Int?)
    data class GeminiResponse(
        val candidates: List<GeminiCandidate>?,
        val usageMetadata: GeminiUsageMetadata?
    )

    // === Anthropic / Claude ProxyAPI DTOs ===
    data class ClaudeContentBlock(val type: String?, val text: String?)
    data class ClaudeUsage(val input_tokens: Int?, val output_tokens: Int?)
    data class ClaudeResponse(
        val id: String?,
        val type: String?,
        val role: String?,
        val content: List<ClaudeContentBlock>?,
        val model: String?,
        val usage: ClaudeUsage?
    )


    /**
     * Получить ответ от OpenAI-совместимого ProxyAPI.
     * @param question - вопрос пользователя.
     * @param modelOverride - переопределить модель (если нужно).
     * @param onlyCoreFromConfig - если true, брать параметры ТОЛЬКО api_url, api_key, model (в payload не попадет ничего больше из config).
     */
    suspend fun getResponse(question: String, modelOverride: String? = null, onlyCoreFromConfig: Boolean = false): OpenAIResponse? {
        val payload = mutableMapOf<String, Any>(
            "model" to (modelOverride?.takeIf { it.isNotEmpty() }
                ?: config.getProperty("model", "gpt-4"))
        )

        // Формируем payload в зависимости от endpoint:
        if (apiUrl.contains("/responses")) {
            payload["input"] = listOf(mapOf("role" to "user", "content" to question))
        } else {
            payload["messages"] = listOf(mapOf("role" to "user", "content" to question))
        }

        if (!onlyCoreFromConfig) {
            // Утилита для аккуратного копирования параметров из config.properties в payload
            fun <T> copy(key: String, parser: (String) -> T?) {
                config.getProperty(key)?.takeIf { it.isNotEmpty() }?.let { value ->
                    parser(value)?.let { payload[key] = it }
                }
            }

            // temperature — насколько креативный/случайный ответ (0.0 = максимально предсказуемо)
            copy("temperature") { it.toFloatOrNull() }

            // top_p — «купол вероятностей», альтернатива temperature
            copy("top_p") { it.toFloatOrNull() }

            // stream — если true, сервер может возвращать ответ частями (стриминг)
            copy("stream") { it.toBooleanStrictOrNull() }

            // max_output_tokens — максимальная длина ответа в токенах
            copy("max_output_tokens") { it.toIntOrNull() }

            // presence_penalty — штраф за повторение уже упомянутых тем/идей
            copy("presence_penalty") { it.toFloatOrNull() }

            // frequency_penalty — штраф за частое повторение одних и тех же слов
            copy("frequency_penalty") { it.toFloatOrNull() }

            // stop_sequences — список стоп-строк; при их появлении генерация останавливается
            copy("stop") { v -> if (v.isNotBlank()) v.split(",") else null }

            // user — ID конечного пользователя (для логирования/безопасности на стороне провайдера)
            copy("user") { it }

            // store — сохранять ли запрос/ответ на стороне провайдера
            copy("store") { it.toBooleanStrictOrNull() }

            // reasoning_effort — сколько «усилий» тратить на рассуждения: low / medium / high
            config.getProperty("reasoning_effort")?.takeIf { it.isNotEmpty() }?.let { effort ->
                payload["reasoning"] = mapOf("effort" to effort)
            }

            // metadata — произвольные служебные данные в JSON-формате, не влияют на текст ответа
            config.getProperty("metadata")?.takeIf { it.isNotEmpty() }?.let { v ->
                try {
                    payload["metadata"] = gson.fromJson(v, Map::class.java)
                } catch (_: Exception) {
                    // игнорируем некорректный JSON в metadata
                }
            }
        }

        val jsonPayload = gson.toJson(payload)
        println("[OpenAIProxyClient] → POST $apiUrl")
        println("[OpenAIProxyClient]   model: ${payload["model"]}")
        println("[OpenAIProxyClient]   payload: $jsonPayload")

        try {
            val response: HttpResponse = client.post(apiUrl) {
                contentType(ContentType.Application.Json)
                setBody(jsonPayload)
                if (apiKey.isNotEmpty())
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
            val responseStr = response.bodyAsText()

            if (!response.status.isSuccess()) {
                System.err.println("[OpenAIProxyClient] HTTP error: ${response.status.value} ${response.status.description}")
                System.err.println("[OpenAIProxyClient] Body: $responseStr")
                return null
            }

            println("[OpenAIProxyClient] ← Response (${response.status.value}):")
            println(responseStr)

            return gson.fromJson(responseStr, OpenAIResponse::class.java)
        } catch (e: Exception) {
            System.err.println("[OpenAIProxyClient] Exception: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            return null
        }
        // Если вдруг ничего не вернулось ни из try, ни из catch
        return null
    }

    /**
     * Получить ответ от OpenAI-совместимого ProxyAPI с явными параметрами.
     * API-ключ и URL берутся из config.properties, остальные параметры передаются напрямую.
     *
     * @param question вопрос пользователя
     * @param model модель (если null — берётся из config)
     * @param temperature креативность (0.0 - 2.0)
     * @param maxOutputTokens максимальная длина ответа в токенах
     * @param topP nucleus sampling
     * @param presencePenalty штраф за повторение тем
     * @param frequencyPenalty штраф за повторение слов
     * @param reasoningEffort уровень рассуждений: "low" / "medium" / "high"
     * @param stop стоп-последовательности
     * @param store сохранять ли запрос/ответ на стороне провайдера
     * @param user ID конечного пользователя
     * @param metadata произвольные метаданные
     */

    suspend fun getResponse(
        question: String,
        model: String? = null,
        temperature: Float? = null,
        maxOutputTokens: Int? = null,
        topP: Float? = null,
        presencePenalty: Float? = null,
        frequencyPenalty: Float? = null,
        reasoningEffort: String? = null,
        stop: List<String>? = null,
        store: Boolean? = null,
        user: String? = null,
        metadata: Map<String, Any>? = null
    ): OpenAIResponse? {
        val payload = mutableMapOf<String, Any>(
            "model" to (model?.takeIf { it.isNotEmpty() }
                ?: config.getProperty("model", "gpt-4"))
        )

        // Формируем payload в зависимости от endpoint:
        if (apiUrl.contains("/responses")) {
            payload["input"] = listOf(mapOf("role" to "user", "content" to question))
        } else {
            payload["messages"] = listOf(mapOf("role" to "user", "content" to question))
        }

        // Применяем явно переданные параметры
        temperature?.let { payload["temperature"] = it }
        maxOutputTokens?.let { payload["max_output_tokens"] = it }
        topP?.let { payload["top_p"] = it }
        presencePenalty?.let { payload["presence_penalty"] = it }
        frequencyPenalty?.let { payload["frequency_penalty"] = it }
        stop?.let { payload["stop"] = it }
        store?.let { payload["store"] = it }
        user?.let { payload["user"] = it }
        metadata?.let { payload["metadata"] = it }
        reasoningEffort?.takeIf { it.isNotEmpty() }?.let { effort ->
            payload["reasoning"] = mapOf("effort" to effort)
        }

        val jsonPayload = gson.toJson(payload)
        println("[OpenAIProxyClient] → POST $apiUrl")
        println("[OpenAIProxyClient]   model: ${payload["model"]}")
        println("[OpenAIProxyClient]   payload: $jsonPayload")

        try {
            val response: HttpResponse = client.post(apiUrl) {
                contentType(ContentType.Application.Json)
                setBody(jsonPayload)
                if (apiKey.isNotEmpty())
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
            val responseStr = response.bodyAsText()

            if (!response.status.isSuccess()) {
                System.err.println("[OpenAIProxyClient] HTTP error: ${response.status.value} ${response.status.description}")
                System.err.println("[OpenAIProxyClient] Body: $responseStr")
                return null
            }

            println("[OpenAIProxyClient] ← Response (${response.status.value}):")
            println(responseStr)

            return gson.fromJson(responseStr, OpenAIResponse::class.java)
        } catch (e: Exception) {
            System.err.println("[OpenAIProxyClient] Exception: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    /**
     * Запрос к Gemini 2.0 Flash Lite (ProxyAPI Google).
     *
     * @param question текст запроса
     * @param model название модели (если null — берётся из конфига или дефолт)
     */
    suspend fun getGeminiResponse(
        question: String,
        model: String? = null,
    ): GeminiResponse? {
        val effectiveModel = model?.takeIf { it.isNotBlank() } ?: googleModel.ifBlank { "gemini-2.0-flash-lite" }
        val url = if (googleApiUrl.isNotEmpty()) googleApiUrl else "$googleApiBase/v1beta/models/$effectiveModel:generateContent"

        val payload = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to question)
                    )
                )
            )
        )
        val jsonPayload = gson.toJson(payload)
        val keyToUse = if (googleApiKey.isNotEmpty()) googleApiKey else apiKey

        println("[OpenAIProxyClient] → POST $url")
        println("[OpenAIProxyClient]   model: $effectiveModel")
        println("[OpenAIProxyClient]   payload: $jsonPayload")

        try {
            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(jsonPayload)
                if (keyToUse.isNotEmpty())
                    header(HttpHeaders.Authorization, "Bearer $keyToUse")
            }
            val responseStr = response.bodyAsText()

            if (!response.status.isSuccess()) {
                System.err.println("[OpenAIProxyClient] Gemini HTTP error: ${response.status.value} ${response.status.description}")
                System.err.println("[OpenAIProxyClient] Body: $responseStr")
                return null
            }

            println("[OpenAIProxyClient] ← Response (${response.status.value}):")
            println(responseStr)

            return gson.fromJson(responseStr, GeminiResponse::class.java)
        } catch (e: Exception) {
            System.err.println("[OpenAIProxyClient] Gemini Exception: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    /**
     * Запрос к Claude (ProxyAPI Anthropic).
     *
     * @param question текст запроса
     * @param model название модели (если null — берётся из конфига или дефолт)
     * @param maxTokens максимальная длина ответа в токенах
     */
    fun getClaudeResponse(
        question: String,
        model: String? = null,
        maxTokens: Int = 1000
    ): ClaudeResponse? = runBlocking {
        val effectiveModel = model?.takeIf { it.isNotBlank() } ?: anthropicModel.ifBlank { "claude-3-5-haiku-20241022" }
        val url = "$anthropicApiBase/v1/messages"

        val payload = mapOf(
            "model" to effectiveModel,
            "max_tokens" to maxTokens,
            "messages" to listOf(
                mapOf(
                    "role" to "user",
                    "content" to question
                )
            )
        )
        val jsonPayload = gson.toJson(payload)
        val keyToUse = if (anthropicApiKey.isNotEmpty()) anthropicApiKey else apiKey

        println("[OpenAIProxyClient] → POST $url")
        println("[OpenAIProxyClient]   model: $effectiveModel")
        println("[OpenAIProxyClient]   payload: $jsonPayload")

        try {
            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(jsonPayload)
                if (keyToUse.isNotEmpty())
                    header(HttpHeaders.Authorization, "Bearer $keyToUse")
                header("anthropic-version", "2023-06-01")
            }
            val responseStr = response.bodyAsText()

            if (!response.status.isSuccess()) {
                System.err.println("[OpenAIProxyClient] Claude HTTP error: ${response.status.value} ${response.status.description}")
                System.err.println("[OpenAIProxyClient] Body: $responseStr")
                return@runBlocking null
            }

            println("[OpenAIProxyClient] ← Response (${response.status.value}):")
            println(responseStr)

            gson.fromJson(responseStr, ClaudeResponse::class.java)
        } catch (e: Exception) {
            System.err.println("[OpenAIProxyClient] Claude Exception: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            null
        }
    }

}

package org.example

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
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
    private val client = HttpClient(CIO)

    init {
        val resource = this::class.java.classLoader.getResourceAsStream("config.properties")
            ?: throw IllegalStateException("config.properties not found in resources!")
        config.load(resource)
        apiUrl = config.getProperty("api_url").trim()
        apiKey = config.getProperty("api_key", "").trim()
    }

    data class Message(val role: String?, val content: String?)
    data class Content(val type: String?, val text: String?)
    data class OutputEntry(val id: String?, val type: String?, val status: String?, val content: List<Content>?, val role: String?)
    data class OpenAIResponse(val output: List<OutputEntry>?)

    /**
     * Получить ответ от OpenAI-совместимого ProxyAPI.
     * @param question - вопрос пользователя.
     * @param modelOverride - переопределить модель (если нужно).
     * @param onlyCoreFromConfig - если true, брать параметры ТОЛЬКО api_url, api_key, model (в payload не попадет ничего больше из config).
     */
    fun getResponse(question: String, modelOverride: String? = null, onlyCoreFromConfig: Boolean = false): OpenAIResponse? = runBlocking {
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
            copy("stop_sequences") { v -> if (v.isNotBlank()) v.split(",") else null }

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
                return@runBlocking null
            }

            gson.fromJson(responseStr, OpenAIResponse::class.java)
        } catch (e: Exception) {
            System.err.println("[OpenAIProxyClient] Exception: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}

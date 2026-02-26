package org.example.data.api.provider

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import mu.KotlinLogging
import org.example.data.api.model.OpenAiProxyRequest
import org.example.data.api.model.LlmResponse
import org.example.data.config.Config
import org.example.domain.model.ChatMessage
import org.example.domain.model.Role

private val logger = KotlinLogging.logger {}

class OpenAiProvider(
    private val config: Config,
    private val client: HttpClient
) : LlmProvider {
    
    override suspend fun chat(messages: List<ChatMessage>, model: String): LlmResponse {
        logger.debug { "Sending request to OpenAI API" }
        
        // Combine all messages into a single input string
        val fullInput = messages.joinToString("\n\n") { message ->
            "${message.role.name.lowercase()}: ${message.content}"
        }
        
        val request = OpenAiProxyRequest(
            model = model,
            input = fullInput
        )
        
        return try {
            val httpResponse = client.post("${config.baseUrl}/openai/v1/responses") {
                contentType(ContentType.Application.Json)
                if (config.apiKey.isNotEmpty()) {
                    header("Authorization", "Bearer ${config.apiKey}")
                }
                setBody(request)
            }

            val responseBody = httpResponse.body<String>()
            logger.info { "Raw API Response: $responseBody" }
            
            val response: LlmResponse = httpResponse.body()
            logger.debug { "Parsed response from OpenAI API" }
            response
        } catch (e: Exception) {
            logger.error(e) { "Error calling OpenAI API" }
            throw e
        }
    }
}

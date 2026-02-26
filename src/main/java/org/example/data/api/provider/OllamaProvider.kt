package org.example.data.api.provider

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import mu.KotlinLogging
import org.example.data.api.model.LlmMessage
import org.example.data.api.model.LlmRequest
import org.example.data.api.model.LlmResponse
import org.example.data.config.Config
import org.example.data.config.LoggerFactory
import org.example.domain.model.ChatMessage

private val logger = LoggerFactory.getLogger()

class OllamaProvider(
    private val config: Config,
    private val client: HttpClient
) : LlmProvider {
    
    override suspend fun chat(messages: List<ChatMessage>, model: String): LlmResponse {
        logger.debug { "Sending request to Ollama API" }
        
        val request = LlmRequest(
            model = model,
            messages = messages.map { 
                LlmMessage(
                    role = it.role.name.lowercase(),
                    content = it.content
                )
            }
        )
        
        return try {
            val response: LlmResponse = client.post("${config.baseUrl}/api/chat") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            
            logger.debug { "Received response from Ollama API" }
            response
        } catch (e: Exception) {
            logger.error(e) { "Error calling Ollama API" }
            throw e
        }
    }
}

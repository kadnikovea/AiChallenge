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
import org.example.domain.model.ChatMessage

private val logger = KotlinLogging.logger {}

class CustomProvider(
    private val config: Config,
    private val client: HttpClient
) : LlmProvider {
    
    override suspend fun chat(messages: List<ChatMessage>, model: String): LlmResponse {
        logger.debug { "Sending request to Custom API" }
        
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
            val response: LlmResponse = client.post(config.baseUrl) {
                contentType(ContentType.Application.Json)
                if (config.apiKey.isNotEmpty()) {
                    header("Authorization", "Bearer ${config.apiKey}")
                }
                setBody(request)
            }.body()
            
            logger.debug { "Received response from Custom API" }
            response
        } catch (e: Exception) {
            logger.error(e) { "Error calling Custom API" }
            throw e
        }
    }
}

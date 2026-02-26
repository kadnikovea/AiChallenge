package org.example.di

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.example.data.api.provider.CustomProvider
import org.example.data.api.provider.LlmProvider
import org.example.data.api.provider.OllamaProvider
import org.example.data.api.provider.OpenAiProvider
import org.example.data.config.Config
import org.example.data.config.ConfigLoader
import org.example.data.config.HistoryStoreType
import org.example.data.config.LoggerFactory
import org.example.data.persistence.HistoryStore
import org.example.data.persistence.InMemoryStore
import org.example.data.persistence.JsonHistoryStore
import org.example.data.persistence.SqliteHistoryStore
import org.example.data.repository.ChatRepositoryImpl
import org.example.domain.repository.ChatRepository
import org.example.domain.usecase.SendMessageUseCase
import org.example.domain.usecase.StartSessionUseCase
import org.example.domain.usecase.LoadLastSessionUseCase

private val logger = LoggerFactory.getLogger()

class AppContainer {
    // Configuration
    val config: Config = ConfigLoader.load()
    
    // HTTP Client
    val httpClient: HttpClient = HttpClient(CIO) {
        if (config.enableLogging) {
            install(Logging) { // 2. Установка плагина
                logger = Logger.DEFAULT
                level = LogLevel.ALL // Уровни: ALL, HEADERS, BODY, INFO, NONE
            }
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeout
            connectTimeoutMillis = 10000
        }
    }
    
    // LLM Provider abstraction
    val llmProvider: LlmProvider = when (config.provider.uppercase()) {
        "OPENAI" -> {
            logger.info { "Using OpenAI provider" }
            OpenAiProvider(config, httpClient)
        }
        "OLLAMA" -> {
            logger.info { "Using Ollama provider" }
            OllamaProvider(config, httpClient)
        }
        "CUSTOM" -> {
            logger.info { "Using Custom provider" }
            CustomProvider(config, httpClient)
        }
        else -> {
            logger.warn { "Unknown provider ${config.provider}, defaulting to OpenAI" }
            OpenAiProvider(config, httpClient)
        }
    }
    
    // Persistence
    val historyStore: HistoryStore = if (!config.persistHistory) {
        logger.info { "Using in-memory history store" }
        InMemoryStore()
    } else {
        when (config.historyStoreType) {
            HistoryStoreType.JSON -> {
                logger.info { "History persistence (JSON) at ${config.historyPath}" }
                JsonHistoryStore(config.historyPath)
            }
            HistoryStoreType.SQLITE -> {
                logger.info { "History persistence (SQLite) at ${config.sqliteDbPath}" }
                SqliteHistoryStore(config.sqliteDbPath)
            }
        }
    }
    
    // Repositories
    val chatRepository: ChatRepository = ChatRepositoryImpl(llmProvider, config)
    
    // Use Cases
    val sendMessageUseCase = SendMessageUseCase(chatRepository, historyStore)
    val startSessionUseCase = StartSessionUseCase()
    val loadLastSessionUseCase = LoadLastSessionUseCase(historyStore)
    
    fun shutdown() {
        logger.info { "Shutting down application..." }
        httpClient.close()
        logger.info { "Application shutdown completed" }
    }
}

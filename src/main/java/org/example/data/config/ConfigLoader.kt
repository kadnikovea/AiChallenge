package org.example.data.config

import java.io.File
import java.util.Properties

private val logger = LoggerFactory.getLogger()

object ConfigLoader {
    fun load(filePath: String = "config.properties"): Config {
        val props = Properties()
        
        // Try to load from resources first, then from file system
        val configStream = this::class.java.classLoader.getResourceAsStream(filePath)
            ?: File(filePath).takeIf { it.exists() }?.inputStream()
            ?: throw IllegalStateException("Config file not found: $filePath")
        
        configStream.use { props.load(it) }
        
        logger.info { "Configuration loaded from $filePath" }

        val historyStoreType = when (props.getProperty("persist.store", "SQLITE").uppercase()) {
            "JSON" -> HistoryStoreType.JSON
            "SQLITE" -> HistoryStoreType.SQLITE
            else -> {
                logger.warn { "Unknown persist.store value '${props.getProperty("persist.store")}', defaulting to SQLITE" }
                HistoryStoreType.SQLITE
            }
        }

        val sqliteDbPath = props.getProperty("sqlite.path", "./history.db")

        return Config(
            baseUrl = props.getProperty("llm.baseUrl")
                ?: throw IllegalStateException("llm.baseUrl is required"),
            modelName = props.getProperty("llm.model")
                ?: throw IllegalStateException("llm.model is required"),
            apiKey = props.getProperty("llm.apiKey", ""),
            provider = props.getProperty("llm.provider", "OPENAI"),
            timeout = props.getProperty("llm.timeout", "30000").toLong(),
            persistHistory = props.getProperty("persist.enabled", "false").toBoolean(),
            historyPath = props.getProperty("persist.path", "./history"),
            enableLogging = props.getProperty("log.enabled", "true").toBoolean(),
            historyStoreType = historyStoreType,
            sqliteDbPath = sqliteDbPath
        )
    }
}

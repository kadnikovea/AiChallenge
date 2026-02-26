package org.example.data.config

enum class HistoryStoreType {
    JSON,
    SQLITE
}

data class Config(
    val baseUrl: String,
    val modelName: String,
    val apiKey: String,
    val provider: String,
    val timeout: Long = 30000,
    val persistHistory: Boolean = false,
    val historyPath: String = "./history",
    val enableLogging: Boolean = false,
    val historyStoreType: HistoryStoreType = HistoryStoreType.SQLITE,
    val sqliteDbPath: String = "./history.db"
)

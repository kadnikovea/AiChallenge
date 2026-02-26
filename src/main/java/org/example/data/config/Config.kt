package org.example.data.config

data class Config(
    val baseUrl: String,
    val modelName: String,
    val apiKey: String,
    val provider: String,
    val timeout: Long = 30000,
    val persistHistory: Boolean = false,
    val historyPath: String = "./history",
    val enableLogging: Boolean = true
)

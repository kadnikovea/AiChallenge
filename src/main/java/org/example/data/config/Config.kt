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
    val sqliteDbPath: String = "./history.db",

    /** Максимальное количество последних сообщений, которые отправляются в LLM "как есть". */
    val maxContextMessages: Int = 5,

    /**
     * Как часто обновлять summary: каждые N новых сообщений, попадающих в "старую" часть диалога.
     * Например, при значении 10 summary будет пересчитываться примерно каждые 10 новых сообщений.
     */
    val summaryUpdateStep: Int = 3,

    /** Включить/выключить механику контекстной суммаризации. */
    val enableContextSummarization: Boolean = true
)

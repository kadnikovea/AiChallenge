package org.example.data.config

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/**
 * Служебный класс для чтения текстовых конфигурационных файлов клиента
 * (например, style.txt).
 *
 * Особенности:
 * - Ищет файлы сначала в classpath (resources), затем в файловой системе
 *   относительно [baseDir].
 * - Возвращает полное содержимое файла одной строкой.
 * - Имеет простой in-memory кэш на время жизни процесса.
 *
 * Расположение по умолчанию: `client-config/<fileName>`.
 */
class TextConfigFileReader(
    private val baseDir: String = "client-config"
) {

    private val cache = ConcurrentHashMap<String, String>()

    /**
     * Считывает содержимое текстового конфига и возвращает его как [String].
     * Результат кэшируется в памяти по пути `baseDir/fileName`.
     *
     * @throws IllegalStateException если файл не найден ни в ресурсах, ни в ФС.
     */
    fun read(fileName: String): String {
        val path = "$baseDir/$fileName"

        return cache.computeIfAbsent(path) { fullPath ->
            // 1. Пытаемся прочитать как ресурс из classpath
            val resourceStream = this::class.java.classLoader.getResourceAsStream(fullPath)

            // 2. Если ресурса нет, пробуем файл в рабочей директории
            val inputStream = resourceStream ?: File(fullPath).takeIf { it.exists() }?.inputStream()
            ?: throw IllegalStateException("Text config file not found: $fullPath")

            inputStream.use { stream ->
                stream.readBytes().toString(StandardCharsets.UTF_8)
            }
        }
    }
}

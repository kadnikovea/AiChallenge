package org.example.ui.component

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.bold
import org.example.domain.model.ChatMessage
import org.example.domain.model.Role
import java.text.SimpleDateFormat
import java.util.*

object Helpers {
    private val dateFormat = SimpleDateFormat("HH:mm:ss")
    
    /**
     * Рендерит сообщение чата с цветовым кодированием по ролям
     * @param message Сообщение для отображения
     * @param echo Функция для вывода (обычно CliktCommand::echo)
     */
    fun renderMessage(message: ChatMessage, echo: (Any?) -> Unit) {
        val timestamp = dateFormat.format(Date(message.timestamp))

        val timePart = gray("[$timestamp] ")

        when (message.role) {
            Role.USER -> {
                val label = brightCyan(bold("Вы"))
                echo(timePart + label + white(": ${message.content}"))
            }
            Role.ASSISTANT -> {
                val label = brightGreen(bold("Ассистент"))
                echo(timePart + label + white(": ${message.content}"))
            }
            Role.SYSTEM -> {
                val label = brightMagenta(bold("Система"))
                echo(timePart + label + gray(": ${message.content}"))
            }
        }
    }
    
    /**
     * Рендерит заголовок с рамкой
     */
    fun renderHeader(title: String, echo: (Any?) -> Unit) {
        echo("")
        echo(cyan(bold(title)))
        echo("")
    }
    
    /**
     * Рендерит сообщение об ошибке
     */
    fun renderError(error: String, echo: (Any?) -> Unit) {
        echo("")
        echo(red("❌ Ошибка: ") + white(error))
        echo("")
    }
    
    /**
     * Рендерит индикатор загрузки
     */
    fun renderLoading(message: String = "Загрузка...", echo: (Any?) -> Unit) {
        echo(yellow("⏳ ") + white(message))
    }
}

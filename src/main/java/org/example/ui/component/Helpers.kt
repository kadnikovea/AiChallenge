package org.example.ui.component

import com.github.ajalt.mordant.rendering.TextColors.*
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
        
        when (message.role) {
            Role.USER -> {
                echo(cyan("[$timestamp] Вы: ") + white(message.content))
            }
            Role.ASSISTANT -> {
                echo(green("[$timestamp] Ассистент: ") + white(message.content))
            }
            Role.SYSTEM -> {
                echo(yellow("[$timestamp] Система: ") + white(message.content))
            }
        }
    }
    
    /**
     * Рендерит заголовок с рамкой
     */
    fun renderHeader(title: String, echo: (Any?) -> Unit) {
        echo("")
        echo(cyan("═".repeat(70)))
        echo(cyan("  $title"))
        echo(cyan("═".repeat(70)))
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

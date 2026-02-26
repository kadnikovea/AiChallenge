package org.example

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.bold
import kotlinx.coroutines.runBlocking
import org.example.data.config.LoggerFactory
import org.example.di.AppContainer
import org.example.domain.model.ChatSession
import org.example.ui.component.Helpers
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger()

class LlmCli : CliktCommand(
    name = "llm-cli"
) {
    private val exitFlag by option("--exit", help = "Выйти из приложения").flag()

    override fun run() {
        if (exitFlag) {
            echo(green("Выход из приложения..."))
            exitProcess(0)
        }

        logger.info { "Starting LLM CLI Agent..." }

        System.setProperty("slf4j.internal.verbosity", "WARN")

        val container = AppContainer()

        // Добавляем shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            container.shutdown()
        })

        try {
            // 1. Приветственный текст
            renderWelcome(container)

            // 2. Пытаемся восстановить последнюю сессию через use case
            val restoredSession: ChatSession? = runBlocking {
                container.loadLastSessionUseCase()
            }

            // 3. Спрашиваем пользователя, хочет ли он восстановить сессию
            val chatSession: ChatSession = if (restoredSession != null && askRestoreSession()) {
                logger.info { "Restoring previous chat session ${restoredSession.id}" }
                restoredSession
            } else {
                // 4. Запрос системного промпта для новой сессии
                val systemPrompt = promptSystemPrompt()

                // 5. Создание новой сессии
                val newSession = container.startSessionUseCase(systemPrompt)
                logger.info { "New chat session started" }
                newSession
            }

            // 6. Бесконечный цикл чата
            chatLoop(container, chatSession)

        } catch (e: Exception) {
            logger.error(e) { "Unexpected error" }
            echo(red("❌ Критическая ошибка: ${e.message}"))
        } finally {
            container.shutdown()
        }
    }

    private fun askRestoreSession(): Boolean {
        echo()
        echo(white("Найдена сохранённая сессия."))
        print(cyan("Восстановить последнюю сессию? (да/нет): "))

        val answer = readlnOrNull()?.trim()?.lowercase() ?: ""
        val yesAnswers = setOf("y", "yes", "д", "да")

        return answer in yesAnswers
    }

    private fun renderWelcome(container: AppContainer) {
        echo()
        echo(cyan(bold("LLM CLI Agent v1.0")))
        echo(white("Добро пожаловать в LLM CLI Agent!"))
        echo(white("Этот инструмент позволяет общаться с языковыми моделями через терминал."))
        echo()
        echo(cyan("Провайдер: ") + yellow(container.config.provider))
        echo(cyan("Модель: ") + yellow(container.config.modelName))
        echo()
        echo(gray("Для выхода введите: exit, quit или bye"))
        echo()
    }

    private fun promptSystemPrompt(): String {
        echo(brightMagenta(bold("Настройка системного промпта")))
        echo(white("Введите системный промпт для чата (или оставьте пустым для значения по умолчанию):"))
        echo()
        print(brightMagenta("Системный промпт: "))
        
        val input = readlnOrNull()?.trim() ?: ""
        
        return if (input.isEmpty()) {
            val defaultPrompt = "Ты полезный ассистент."
            echo(gray("Используется промпт по умолчанию: $defaultPrompt"))
            defaultPrompt
        } else {
            input
        }
    }

    private fun chatLoop(container: AppContainer, chatSession: ChatSession) {
        echo()
        echo(cyan(bold("Чат начат")))
        echo()

        if (chatSession.systemPrompt.isNotEmpty()) {
            echo(brightMagenta(bold("Системный промпт: ")) + gray(chatSession.systemPrompt))
            echo()
        }

        while (true) {
            // Показываем историю сообщений
            renderChatHistory(chatSession)

            // Запрашиваем сообщение пользователя
            print(cyan(bold("Ваше сообщение: ")))
            val userInput = readlnOrNull()?.trim() ?: ""

            // Проверка на выход
            if (userInput.lowercase() in listOf("exit", "quit", "bye", "q")) {
                echo()
                echo(green("👋 До свидания!"))
                logger.info { "User requested exit" }
                break
            }

            // Пропускаем пустые сообщения
            if (userInput.isEmpty()) {
                echo(gray("Пожалуйста, введите сообщение."))
                echo()
                continue
            }

            // Показываем лоадер и отправляем сообщение
            sendMessageWithLoader(container, chatSession, userInput)
        }
    }

    private fun renderChatHistory(chatSession: ChatSession) {
        // Показываем только последние сообщения, чтобы не загромождать экран
        val recentMessages = chatSession.messages.takeLast(10)
        
        if (recentMessages.isNotEmpty()) {
            echo(gray("--- История (последние ${recentMessages.size} сообщений) ---"))
            recentMessages.forEach { message ->
                Helpers.renderMessage(message, {
                    echo(it)
                })
            }
            echo()
        }
    }

    private fun sendMessageWithLoader(
        container: AppContainer,
        chatSession: ChatSession,
        userMessage: String
    ) {
        echo()
        
        // Запускаем лоадер в отдельном потоке
        var isLoading = true
        val loaderThread = Thread {
            val spinnerFrames = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")
            var frameIndex = 0
            
            while (isLoading) {
                print("\r${yellow("${spinnerFrames[frameIndex]} Ожидание ответа...")}")
                frameIndex = (frameIndex + 1) % spinnerFrames.size
                Thread.sleep(100)
            }
            print("\r" + " ".repeat(50) + "\r") // Очищаем строку лоадера
        }
        
        loaderThread.start()

        try {
            // Отправляем сообщение (блокирующий вызов)
            runBlocking {
                val result = container.sendMessageUseCase(chatSession, userMessage)

                if (result.isFailure) {
                    val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    logger.error { "Error sending message: $error" }
                    isLoading = false
                    loaderThread.join()
                    
                    echo(red("❌ Ошибка: $error"))
                    echo()
                } else {
                    logger.info { "Message sent successfully" }
                    isLoading = false
                    loaderThread.join()
                    
                    // Показываем последний ответ ассистента
                    val lastMessage = chatSession.messages.lastOrNull()
                    if (lastMessage != null) {
                        Helpers.renderMessage(lastMessage, ::echo)
                        echo()
                    }
                }
            }
        } catch (e: Exception) {
            isLoading = false
            loaderThread.join()
            
            logger.error(e) { "Error in sendMessageWithLoader" }
            echo(red("❌ Ошибка: ${e.message}"))
            echo()
        }
    }
}

fun main(args: Array<String>) = LlmCli().main(args)

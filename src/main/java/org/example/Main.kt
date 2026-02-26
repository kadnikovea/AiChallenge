package org.example

import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.example.di.AppContainer
import org.example.domain.model.ChatSession
import org.example.ui.component.Helpers.renderError
import org.example.ui.component.Helpers.renderHeader
import org.example.ui.component.Helpers.renderLoading
import org.example.ui.component.Helpers.renderMessage
import org.example.ui.state.AppState

private val logger = KotlinLogging.logger {}

fun main() {
    logger.info { "Starting LLM CLI Agent..." }

    val container = AppContainer()

//     Add shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        container.shutdown()
    })
    session {
        var state by liveVarOf(AppState.WELCOME)
        var chatSession by liveVarOf<ChatSession?>(null)
        var errorMsg by liveVarOf<String?>(null)
        var isProcessing by liveVarOf(false)
        var shouldExit by liveVarOf(false)

        while (!shouldExit) {
            section {
                when (state) {
                    AppState.WELCOME -> {
                        renderHeader("LLM CLI Agent v1.0")
                        textLine("Welcome to the LLM CLI Agent!")
                        textLine("This tool allows you to chat with LLM models via terminal.")
                        textLine()
                        cyan { textLine("Provider: ${container.config.provider}") }
                        cyan { textLine("Model: ${container.config.modelName}") }
                        textLine()
                        textLine("Press ENTER to continue...")
                    }

                    AppState.PROMPT_INPUT -> {
                        renderHeader("System Prompt Configuration")
                        textLine("Enter a system prompt to guide the assistant's behavior.")
                        textLine("Leave blank for default behavior.")
                        textLine()
                        yellow { text("System Prompt: ") }
                    }

                    AppState.CHATTING -> {
                        renderHeader("Chat Session")

                        chatSession?.let { session ->
                            if (session.systemPrompt.isNotEmpty()) {
                                yellow { text("System: ") }
                                textLine(session.systemPrompt)
                                textLine()
                            }

                            // Render chat history
                            session.messages.forEach { message ->
                                renderMessage(message)
                            }
                        }

                        if (isProcessing) {
                            renderLoading()
                        } else {
                            textLine()
                            cyan { text("You: ") }
                        }
                    }

                    AppState.ERROR -> {
                        renderHeader("Error")
                        errorMsg?.let { renderError(it) }
                        textLine("Press ENTER to continue...")
                    }

                    else -> {}
                }

                input()
            }.runUntilInputEntered {
                onInputEntered {
                    val userInput = input.trim()

                    when (state) {
                        AppState.WELCOME -> {
                            state = AppState.PROMPT_INPUT
                        }

                        AppState.PROMPT_INPUT -> {
                            val systemPrompt = userInput.ifEmpty { "You are a helpful assistant." }
                            chatSession = container.startSessionUseCase(systemPrompt)
                            logger.info { "Chat session started" }
                            state = AppState.CHATTING
                        }

                        AppState.CHATTING -> {
                            if (userInput.isEmpty()) {
                                return@onInputEntered
                            }

                            if (userInput.lowercase() in listOf("exit", "quit", "bye")) {
                                logger.info { "User requested exit" }
                                shouldExit = true
                                container.shutdown()
                                return@onInputEntered
                            }

                            isProcessing = true

                            // Send message synchronously (blocking)
                            kotlinx.coroutines.runBlocking {
                                val result = container.sendMessageUseCase(chatSession!!, userInput)

                                if (result.isFailure) {
                                    errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                                    state = AppState.ERROR
                                    logger.error { "Error sending message: $errorMsg" }
                                } else {
                                    logger.info { "Message sent successfully" }
                                }

                                isProcessing = false
                            }
                        }

                        AppState.ERROR -> {
                            state = AppState.CHATTING
                            errorMsg = null
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}

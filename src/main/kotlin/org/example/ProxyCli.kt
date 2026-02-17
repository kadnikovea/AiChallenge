package org.example

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.io.BufferedReader
import java.io.InterruptedIOException
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class ProxyCli : CliktCommand(
    name = "proxy",
    help = "CLI для работы с ProxyAPI",
    invokeWithoutSubcommand = true
) {
    private val interactive by option("-i", "--interactive").flag(default = false)
        .help("Интерактивный режим — ввод команд в цикле")

    override fun run() {
        if (interactive) {
            runInteractiveLoop()
        }
    }

    private fun runInteractiveLoop() {
        echo("Интерактивный режим. Команды: bal, chat, help. Выход: exit, quit, q")
        while (true) {
            val prompt = "proxy> "
            print(prompt)
            val line = safeReadLine()?.trim() ?: break
            if (line.isBlank()) continue
            when (line) {
                "exit", "quit", "q" -> { echo("До свидания!"); break }
                "help", "?" -> {
                    val cli = ProxyCli().subcommands(BalCommand(), ChatCommand())
                    echo(cli.getFormattedHelp())
                    continue
                }
                "chat" -> runChatSession()
                else -> {
                    val args = line.split(Regex("\\s+")).toTypedArray()
                    try {
                        val cli = ProxyCli().subcommands(BalCommand(), ChatCommand())
                        cli.parse(args)
                    } catch (e: CliktError) {
                        echo(e.message ?: "Ошибка", err = true)
                    }
                }
            }
        }
    }

    private fun runChatSession() {
        echo("Режим диалога с gpt-5.1-codex-mini. Выход: exit, quit, q")
        val client = ProxyAPIClient()
        while (true) {
            print("вы> ")
            val message = safeReadLine()?.trim() ?: break
            if (message.isBlank()) continue
            when (message.lowercase()) {
                "exit", "quit", "q" -> { echo("Выход из диалога."); break }
            }
            try {
                val response = chatWithLoader(client, message)
                echo(response)
            } catch (e: Exception) {
                echo("Ошибка: ${e.message}", err = true)
            }
        }
    }
}

class BalCommand : CliktCommand(
    name = "bal",
    help = "Запросить баланс аккаунта"
) {
    override fun run() {
        try {
            val client = ProxyAPIClient()
            val balance = client.getBalanceResponse()
            echo("Баланс: ${balance.balance} ${balance.currency}")
        } catch (e: Exception) {
            echo("Ошибка при получении баланса: ${e.message}", err = true)
        }
    }
}

class ChatCommand : CliktCommand(
    name = "chat",
    help = "Интерактивный диалог с моделью gpt-5.1-codex-mini"
) {
    override fun run() {
        echo("Режим диалога с gpt-5.1-codex-mini. Выход: exit, quit, q")
        val client = ProxyAPIClient()
        while (true) {
            print("вы> ")
            val message = safeReadLine()?.trim() ?: break
            if (message.isBlank()) continue
            when (message.lowercase()) {
                "exit", "quit", "q" -> { echo("Выход из диалога."); break }
                else -> {
                    try {
                        val response = chatWithLoader(client, message)
                        echo(response)
                    } catch (e: Exception) {
                        echo("Ошибка: ${e.message}", err = true)
                    }
                }
            }
        }
    }
}

private val stdinReader by lazy {
    val decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)
    BufferedReader(InputStreamReader(System.`in`, decoder))
}

private fun safeReadLine(): String? = try {
    stdinReader.readLine()
} catch (e: Exception) {
    null
}

private fun chatWithLoader(client: ProxyAPIClient, message: String): String {
    // Новая строка — сохраняем введённый текст, лоадер на следующей
    println()
    val running = AtomicBoolean(true)
    // Потребляем ввод во время загрузки — запрет ввода до ответа модели
    val discardDecoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)
    val discardThread = thread(isDaemon = true) {
        try {
            val reader = BufferedReader(InputStreamReader(System.`in`, discardDecoder))
            while (running.get()) {
                reader.readLine()
            }
        } catch (_: InterruptedIOException) {}
        catch (_: Exception) {}
    }
    val loaderThread = thread {
        val frames = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")
        var i = 0
        while (running.get()) {
            print("\rмодель> ${frames[i % frames.size]} ")
            System.out.flush()
            i++
            Thread.sleep(80)
        }
    }
    return try {
        client.chat(message)
    } finally {
        running.set(false)
        discardThread.interrupt()
        discardThread.join(200)
        loaderThread.join(150)
        print("\rмодель> ")
        System.out.flush()
    }
}

fun main(args: Array<String>) = ProxyCli()
    .subcommands(BalCommand(), ChatCommand())
    .main(args)

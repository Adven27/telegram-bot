package io.adven27.telegram.bots

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.request.AnswerCallbackQuery
import com.pengrad.telegrambot.request.EditMessageText
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.request.SendSticker
import com.pengrad.telegrambot.response.BaseResponse
import com.pengrad.telegrambot.response.SendResponse
import io.adven27.telegram.bots.mamot.commands.Sticker
import io.adven27.telegram.bots.watcher.eval
import mu.KotlinLogging
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TelegramBotApplication(var bots: List<Bot>?) : CommandLineRunner {
    override fun run(vararg args: String?) {
        println("Scripting works: ${eval<Int>("1+1")}")
        bots?.forEach { it.start() }
    }
}

interface Bot {
    fun start()
}

fun main(args: Array<String>) {
    runApplication<TelegramBotApplication>(*args)
}

fun TelegramBot.send(chat: Chat, sticker: Sticker, block: SendSticker.() -> Unit = { }): SendResponse =
    send(chat.id(), sticker, block)

fun TelegramBot.send(chat: Long, sticker: Sticker, block: SendSticker.() -> Unit = { }): SendResponse =
    execute(
        SendSticker(chat, sticker.id()).apply { block(this).apply { logger.info("Sending {}", parameters) } }
    ).apply { if (!isOk) logger.error("Fail to send message: {} {}", errorCode(), description()) }

fun TelegramBot.send(chat: Chat, message: String, block: SendMessage.() -> Unit = { }): SendResponse =
    send(chat.id(), message, block)

fun TelegramBot.send(chat: Long, message: String, block: SendMessage.() -> Unit = { }): SendResponse =
    execute(
        SendMessage(chat, message).apply { block(this).apply { logger.info("Sending {}", parameters) } }
    ).apply { if (!isOk) logger.error("Fail to send message: {} {}", errorCode(), description()) }

fun TelegramBot.answer(cb: CallbackQuery, message: String): BaseResponse =
    execute(AnswerCallbackQuery(cb.id()).text(message))

fun TelegramBot.edit(chat: Chat, messageId: Int, newText: String, block: EditMessageText.() -> Unit = { }) =
    edit(chat.id(), messageId, newText, block)

fun TelegramBot.edit(
    chat: Long,
    messageId: Int,
    newText: String,
    block: EditMessageText.() -> Unit = { }
): BaseResponse = execute(
    EditMessageText(chat, messageId, newText).apply { block(this).apply { logger.info("Sending {}", parameters) } }
).apply { if (!isOk) logger.error("Fail to edit message: {}", description()) }

fun CallbackQuery.chatId(): Long = this.message().chat().id()
fun CallbackQuery.messageId(): Int = this.message().messageId()

private val logger = KotlinLogging.logger {}

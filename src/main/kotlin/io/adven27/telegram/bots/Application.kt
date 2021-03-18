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
import io.adven27.telegram.bots.watcher.eval
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

fun TelegramBot.send(
    chat: Chat,
    sticker: io.adven27.telegram.bots.mamot.commands.Sticker,
    apply: (SendSticker) -> SendSticker = { it }
): SendResponse =
    send(chat.id(), sticker, apply)

fun TelegramBot.send(
    chat: Long,
    sticker: io.adven27.telegram.bots.mamot.commands.Sticker,
    apply: (SendSticker) -> SendSticker = { it }
): SendResponse =
    execute(SendSticker(chat, sticker.id()).apply { apply(this) })

fun TelegramBot.send(chat: Chat, message: String, apply: (SendMessage) -> SendMessage = { it }): SendResponse =
    send(chat.id(), message, apply)

fun TelegramBot.send(chat: Long, message: String, apply: (SendMessage) -> SendMessage = { it }): SendResponse =
    execute(SendMessage(chat, message).apply { apply(this) })

fun TelegramBot.answer(cb: CallbackQuery, message: String): BaseResponse =
    execute(AnswerCallbackQuery(cb.id()).text(message))

fun TelegramBot.edit(
    chat: Chat,
    messageId: Int,
    newMessage: String,
    apply: (EditMessageText) -> EditMessageText = { it }
): BaseResponse = edit(chat.id(), messageId, newMessage, apply)

fun TelegramBot.edit(
    chat: Long,
    messageId: Int,
    newMessage: String,
    apply: (EditMessageText) -> EditMessageText = { it }
): BaseResponse = execute(EditMessageText(chat, messageId, newMessage).apply { apply(this) })

fun CallbackQuery.chatId(): Long = this.message().chat().id()
fun CallbackQuery.messageId(): Int = this.message().messageId()
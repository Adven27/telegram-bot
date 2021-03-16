package io.adven27.telegram.bots.mamot.commands

import com.google.gson.Gson
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.User
import com.pengrad.telegrambot.model.request.ParseMode.HTML
import io.adven27.telegram.bots.mamot.legacy.MessageCommand
import io.adven27.telegram.bots.send
import mu.KLogging

class AdviceCommand(private val messageFromURL: MessageFromURL) : MessageCommand("/advice", "Fucking great advice") {

    override fun execute(bot: TelegramBot, user: User, chat: Chat, params: String?) {
        var text = ""
        var sticker: Sticker = Sticker.BLA
        try {
            text = messageFromURL.print()
        } catch (e: Exception) {
            sticker = Sticker.ALONE
            text = "Связь с ноосферой потеряна..."
        }
        bot.send(chat.id(), sticker)
        bot.send(chat, text) { it.disableWebPagePreview(true).parseMode(HTML) }
    }
}

class AdvicePrinter : MessagePrinter {
    override fun print(data: Map<String, String>) = String.format("%s", data["text"]!!.replace("&nbsp;", " "))
}

class AdviceResource(private val url: String = "http://fucking-great-advice.ru/api/random") : HttpResource(),
    URLResource {
    override fun fetch(): Map<String, String> {
        val js: String = from(url)
        logger.info(js)
        return Gson().fromJson<Map<*, *>>(js, MutableMap::class.java) as Map<String, String>
    }

    companion object : KLogging()
}

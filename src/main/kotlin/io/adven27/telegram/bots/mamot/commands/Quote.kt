package io.adven27.telegram.bots.mamot.commands

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.User
import com.pengrad.telegrambot.model.request.ParseMode.HTML
import io.adven27.telegram.bots.mamot.legacy.MessageCommand
import io.adven27.telegram.bots.send
import mu.KLogging

class QuoteCommand(private val messageFromURL: MessageFromURL) :
    MessageCommand("/quote", "Print cool quote") {
    override fun execute(bot: TelegramBot, user: User, chat: Chat, params: String?) {
        var text = ""
        var sticker: Sticker = Sticker.THINK
        try {
            text = messageFromURL.print()
        } catch (e: java.lang.Exception) {
            sticker = Sticker.ALONE
            text = "Связь с ноосферой потеряна..."
        }
        bot.send(chat.id(), sticker)
        bot.send(chat, text) { disableWebPagePreview(true).parseMode(HTML) }
    }
}

class QuotePrinter : MessagePrinter {
    override fun print(data: Map<String, String>): String =
        String.format("%s \n\n %s (%s)", data["text"], data["author"], data["link"])
}

class QuoteResource(private val url: String = "http://api.forismatic.com/api/1.0/?method=getQuote&format=json") :
    JsonResource(), URLResource {
    override fun fetch(): Map<String, String> {
        val js = getObjectFrom(url)
        logger.info(js.toString())
        return mapOf(
            "text" to js.getString("quoteText"),
            "author" to js.getString("quoteAuthor"),
            "link" to js.getString("quoteLink")
        )
    }

    companion object : KLogging()
}

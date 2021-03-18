package io.adven27.telegram.bots.mamot.commands

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.User
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.response.SendResponse
import io.adven27.telegram.bots.mamot.legacy.MessageCommand
import io.adven27.telegram.bots.send
import mu.KLogging

class UncleBobCommand(private val printer: EntryPrinter) : MessageCommand("/unclebob", "Latest articles of Uncle Bob blog") {
    override fun execute(bot: TelegramBot, user: User, chat: Chat, params: String?) {
        val feed: Feed = AtomFeed(FEED_PATH)
        if (params?.isNotEmpty() == true) {
            try {
                val number = params.trim().toInt()
                if (number > 0) {
                    sendNumberOfLatest(bot, chat, feed, number)
                } else {
                    sendFormatError(bot, chat)
                }
            } catch (e: NumberFormatException) {
                sendFormatError(bot, chat)
            }
        } else {
            sendLatest(bot, chat, feed)
        }
    }

    private fun sendNumberOfLatest(bot: TelegramBot, chat: Chat, feed: Feed, number: Int) {
        bot.send(chat, Sticker.THINK)
        feed[number]?.forEach { e -> sendArticle(bot, chat, e!!) }
    }

    private fun sendArticle(bot: TelegramBot, chat: Chat, e: Entry): SendResponse {
        return bot.send(chat, printer.print(e)) { parseMode(ParseMode.HTML).disableWebPagePreview(false) }
    }

    private fun sendLatest(bot: TelegramBot, chat: Chat, feed: Feed) {
        bot.send(chat, Sticker.THINK)
        feed.get()?.let { sendArticle(bot, chat, it) }
    }

    private fun sendFormatError(bot: TelegramBot, chat: Chat) {
        bot.send(chat, Sticker.HELP)
        bot.send(chat, COMMAND_FORMAT_ERROR_MESSAGE) { parseMode(ParseMode.HTML).disableWebPagePreview(false) }
    }

    companion object : KLogging() {
        const val COMMAND_FORMAT_ERROR_MESSAGE = "Number of requested articles should be positive."
        private const val FEED_PATH = "http://blog.cleancoder.com/atom.xml"
    }
}

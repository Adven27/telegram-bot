package io.adven27.telegram.bots.mamot.commands

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.User
import io.adven27.telegram.bots.mamot.legacy.MessageCommand
import io.adven27.telegram.bots.send

class SupCommand(private val dao: DAO) : MessageCommand("/sup", "mamot loves you") {
    override fun execute(bot: TelegramBot, user: User, chat: Chat, params: String?): Unit = with(bot) {
        send(chat, Sticker.random())
        send(chat, dao.complement) { it.disableWebPagePreview(true) }
    }
}

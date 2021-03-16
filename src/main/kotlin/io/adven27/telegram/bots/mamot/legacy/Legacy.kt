package io.adven27.telegram.bots.mamot.legacy

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.*
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import mu.KLogging
import java.lang.Exception
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors

class HandlersChainListener(
    private val bot: TelegramBot,
    defaultHandler: UpdateHandler,
    vararg handlers: UpdateHandler
) :
    UpdatesListener {
    private val defaultConsumer: UpdateHandler = defaultHandler
    private val handlers: List<UpdateHandler> = handlers.toList()
    override fun process(updates: List<Update>): Int = updates.forEach(Consumer { this.process(it) }).let {
        UpdatesListener.CONFIRMED_UPDATES_ALL
    }

    private fun process(u: Update) {
        if (handlers.stream().noneMatch { h: UpdateHandler -> h.handle(bot, u) })
            defaultConsumer.handle(bot, u)
    }
}

interface UpdateHandler {
    fun handle(bot: TelegramBot, update: Update): Boolean
}

interface MessageHandler : UpdateHandler {
    fun execute(bot: TelegramBot, user: User, chat: Chat, params: String?)
    fun reply(bot: TelegramBot, user: User, chat: Chat, params: String?, original: Message?)
}

abstract class MessageCommand(commandIdentifier: String, private val desc: String) : MessageHandler {
    companion object : KLogging()

    private val commandIdentifier: String = commandIdentifier.toLowerCase()
    protected fun identifier(): String {
        return commandIdentifier
    }

    fun description(): String {
        return "$commandIdentifier $desc"
    }

    override fun handle(bot: TelegramBot, update: Update): Boolean {
        val msg = update.message()
        return msg != null && handle(bot, msg)
    }

    private fun handle(bot: TelegramBot, msg: Message): Boolean {
        return if (msg.replyToMessage() != null) {
            tryReply(bot, msg, msg.replyToMessage())
        } else {
            val invocations = parseCommandInvocations(msg)
            invocations.isNotEmpty() && tryExecuteCommands(bot, msg, invocations)
        }
    }

    private fun tryExecuteCommands(
        bot: TelegramBot,
        msg: Message,
        commandInvocations: List<CommandInvocation>
    ): Boolean {
        for (inv in commandInvocations) {
            if (commandIdentifier == inv.name) {
                try {
                    execute(bot, msg.from(), msg.chat(), inv.params)
                } catch (e: Exception) {
                    logger.error("MessageCommand", e)
                }
                return true
            }
        }
        return false
    }

    private fun tryReply(bot: TelegramBot, reply: Message, original: Message): Boolean {
        val invocations = parseCommandInvocations(original)
        for (inv in invocations) {
            if (commandIdentifier == inv.name) {
                try {
                    reply(bot, reply.from(), reply.chat(), reply.text(), original)
                } catch (e: Exception) {
                    logger.error("MessageCommand", e)
                }
                return true
            }
        }
        return false
    }

    private fun parseCommandInvocations(msg: Message): List<CommandInvocation> {
        return if (msg.entities() == null) emptyList() else Arrays.stream(msg.entities())
            .filter { e: MessageEntity -> e.type() == MessageEntity.Type.bot_command }
            .sorted(Comparator.comparing { obj: MessageEntity -> obj.offset() }).map { e: MessageEntity ->
                CommandInvocation(
                    msg.text().substring(e.offset(), e.length()),
                    msg.text().substring(e.offset() + e.length())
                )
            }.collect(Collectors.toList())
    }

    override fun reply(bot: TelegramBot, user: User, chat: Chat, params: String?, original: Message?) {}

    private class CommandInvocation(name: String, params: String) {
        val name: String
        val params: String

        init {
            val botNamePostfix = name.indexOf("@")
            this.name = if (botNamePostfix > 0) name.substring(0, botNamePostfix) else name
            this.params = params
        }
    }

}

class KeyboardBuilder(private val sign: String = "") {
    private val rows: MutableList<Array<InlineKeyboardButton>> = ArrayList()
    fun row(vararg textDataPairs: String?): KeyboardBuilder {
        val row: MutableList<InlineKeyboardButton> = ArrayList()
        var i = 0
        while (i < textDataPairs.size) {
            row.add(InlineKeyboardButton(textDataPairs[i]).callbackData(textDataPairs[i + 1]))
            i += 2
        }
        rows.add(row.toTypedArray())
        return this
    }

    fun row(type: Type, vararg vars: String?): KeyboardBuilder {
        when {
            Type.TEXT_DATA_PAIRS == type -> return row(*vars)
            Type.TEXT_EQUALS_DATA_LIST == type -> {
                rows += vars.map { InlineKeyboardButton(it).callbackData(it) }.toTypedArray()
            }
            Type.DATA_EQUALS_SIGNED_TEXT_LIST == type -> {
                rows += vars.map { InlineKeyboardButton(it).callbackData("$sign:$it") }.toTypedArray()
            }
        }
        return this
    }

    fun rowSigned(vararg vars: String?): KeyboardBuilder {
        return row(Type.DATA_EQUALS_SIGNED_TEXT_LIST, *vars)
    }

    fun build(): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(*rows.toTypedArray())
    }

    enum class Type {
        TEXT_DATA_PAIRS, TEXT_EQUALS_DATA_LIST, DATA_EQUALS_SIGNED_TEXT_LIST
    }

    companion object {
        fun keyboard(sign: String = ""): KeyboardBuilder {
            return KeyboardBuilder(sign)
        }
    }
}

abstract class CallbackCommand(commandIdentifier: String, desc: String) : MessageCommand(commandIdentifier, desc) {
    override fun handle(bot: TelegramBot, update: Update): Boolean {
        val cb = update.callbackQuery() ?: return super.handle(bot, update)
        return if (isThisCommandCallback(cb)) callback(bot, cb) else false
    }

    private fun isThisCommandCallback(cb: CallbackQuery) = cb.data().startsWith(identifier())

    abstract fun callback(bot: TelegramBot, cb: CallbackQuery): Boolean

    protected val keyboardBuilder: KeyboardBuilder = KeyboardBuilder.keyboard(identifier())

    protected fun CallbackQuery.unsignedData() = this.data().substring(identifier().length + 1)
}
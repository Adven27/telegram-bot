package io.adven27.telegram.bots.watcher

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import io.adven27.telegram.bots.Bot
import io.adven27.telegram.bots.send
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.testcontainers.shaded.org.apache.commons.lang.StringEscapeUtils
import kotlin.concurrent.fixedRateTimer

@Service
@ConditionalOnProperty(prefix = "bot.watcher", name = ["enabled"], havingValue = "true", matchIfMissing = false)
class Watcher(
    @Value("\${bot.watcher.token}") private val token: String,
    @Value("\${bot.watcher.rate-minutes:15}") private val rateMinutes: Long,
    @Value("\${bot.watcher.admin}") private val admin: Long,
    private val chatRepository: ChatRepository,
    private val scriptsRepository: ScriptsRepository,
    private val fetch: (url: String) -> Item,
) : Bot {
    val dbWizard: DbWizard = DbWizard()

    override fun start() {
        with(TelegramBot(token)) {
            fixedRateTimer("default", true, 0L, 1000 * 60 * rateMinutes) { updateItems() }
            setUpdatesListener { handle(it) }
        }
    }

    private fun TelegramBot.updateItems() {
        try {
            chatRepository.findAll().forEach { chat ->
                chatRepository.save(
                    chat.data
                        .let {
                            it.copy(
                                wishList = WishList(it.wishList!!.items.map { (url, _, oldPrice) ->
                                    fetch(url).apply { notify(chat.chatId, oldPrice, this) }
                                }.fold(listOf<Item>()) { r, i -> r + i }.toSet())
                            )
                        }.let { chat.apply { data = it } }
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to refresh", e)
        }
    }

    private fun TelegramBot.notify(user: Long, oldPrice: Double, item: Item) {
        fun Item.priceDown() = "Цена упала!\nСейчас \"${name}\" стоит:\n${price}"
        fun Item.noMore() = "А всё! Нету больше \"${name}\". Надо было раньше думать..."
        when {
            oldPrice != 0.0 && item.price == 0.0 -> send(user, item.noMore())
            item.price < oldPrice -> send(user, item.priceDown())
        }
    }

    private fun TelegramBot.handle(updates: MutableList<Update>): Int {
        updates.map { it.message() }.forEach { msg ->
            when {
                msg.text().startsWith("http") -> follow(msg)
                msg.chat().id() == admin && msg.text().startsWith("/db") -> db(msg)
                msg.chat().id() == admin && dbWizard.inProgress() -> dbInProgress(msg)
            }
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL
    }

    private fun TelegramBot.dbInProgress(msg: Message) {
        if (dbWizard.finished()) {
            scriptsRepository.save(Script(pattern = dbWizard.steps[1].resp, script = dbWizard.steps[2].resp))
            send(admin, "committed")
            dbWizard.reset()
        } else {
            send(admin, dbWizard.next(msg.text()).msg)
        }
    }

    private fun TelegramBot.db(msg: Message) {
        dbWizard.reset()
        send(
            admin,
            scriptsRepository.findAll()
                .joinToString(separator = "\n\n") { it.toString() } + "\n\n" + dbWizard.next("").msg
        )
    }

    private fun TelegramBot.follow(msg: Message) {
        val text = msg.text()
        logger.info("URL to follow [$text]")
        val chatId = msg.chat().id()
        try {
            val result = fetch(text)
            chatRepository.findByChatId(chatId).ifPresentOrElse(
                { chat ->
                    chatRepository.save(
                        chat.apply {
                            data =
                                data.copy(wishList = data.wishList!!.copy(items = data.wishList!!.items + result))
                        }
                    )
                },
                { chatRepository.save(Chat(chatId = chatId, data = ChatData(WishList(setOf(result))))) }
            )
            send(chatId, "ОК! Буду следить.\nСейчас \"${result.name}\" стоит:\n${result.price}")
        } catch (nf: ScriptNotFound) {
            send(chatId, "Извините... Пока не умею следить за такими ссылками ${nf.message}")
        } catch (e: Exception) {
            logger.error("Fail to follow", e)
            send(chatId, "Извините... Что-то пошло не так =(")
        }
    }

    companion object : KLogging()
}

@Serializable
data class Item(val url: String, var name: String = "noname", var price: Double = 0.0, var quantity: Int = 0) {
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        javaClass != other?.javaClass -> false
        else -> url == (other as Item).url
    }

    override fun hashCode(): Int = url.hashCode()
}

@JsonDeserialize(using = ItemDeserializer::class)
@JsonSerialize(using = ItemSerializer::class)
@Serializable
data class ChatData(val wishList: WishList? = null)

@Serializable
data class WishList(val items: Set<Item>)

class ItemDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<ChatData?>(vc) {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): ChatData =
        with((jp.codec.readTree(jp) as TreeNode).toString()) {
            Json { ignoreUnknownKeys = true }.decodeFromString(
                StringEscapeUtils.unescapeJava(if (startsWith("\"")) subSequence(1, lastIndex).toString() else this)
            )
        }
}

class ItemSerializer @JvmOverloads constructor(vc: Class<ChatData?>? = null) : StdSerializer<ChatData?>(vc) {
    override fun serialize(value: ChatData?, gen: JsonGenerator, provider: SerializerProvider?) =
        gen.writeString(Json { ignoreUnknownKeys = true }.encodeToString(value))
}

class DbWizard(
    val steps: List<Step> = listOf(Step("off"), Step("pattern:"), Step("script:"), Step("commit?")),
    var currentStep: Int = 0
) {
    data class Step(val msg: String, var resp: String = "")

    fun finished() = currentStep == steps.lastIndex

    fun inProgress() = currentStep != 0

    fun next(text: String): Step {
        steps[currentStep].resp = text
        currentStep++
        return steps[currentStep]
    }

    fun reset() {
        currentStep = 0
    }
}
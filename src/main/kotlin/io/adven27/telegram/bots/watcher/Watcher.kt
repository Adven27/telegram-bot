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
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.ParseMode.HTML
import com.pengrad.telegrambot.request.SendMessage
import io.adven27.telegram.bots.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.testcontainers.shaded.org.apache.commons.lang.StringEscapeUtils
import java.util.*
import kotlin.concurrent.fixedRateTimer
import com.pengrad.telegrambot.model.request.InlineKeyboardButton as Button
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup as Keyboard

@Service
@ConditionalOnProperty(prefix = "bot.watcher", name = ["enabled"], havingValue = "true", matchIfMissing = false)
class Watcher(
    @Value("\${bot.watcher.token}") private val token: String,
    @Value("\${bot.watcher.update.rate-minutes:15}") private val rateMinutes: Long,
    @Value("\${bot.watcher.update.enabled:true}") private val updateEnabled: Boolean,
    @Value("\${bot.watcher.admin}") private val admin: Long,
    @Value("\${bot.watcher.intro}") private val introMessage: String,
    private val chatRepository: ChatRepository,
    private val scriptsRepository: ScriptsRepository,
    private val fetch: (url: String) -> Item,
) : Bot {
    val dbWizard: DbWizard = DbWizard()

    override fun start() {
        logger.info("Starting Watcher...")
        with(TelegramBot(token)) {
            if (updateEnabled) {
                fixedRateTimer("default", true, initialDelay = 0L, period = 1000 * 60 * rateMinutes) { updateChats() }
            }
            setUpdatesListener { handle(it) }
        }
        logger.info("Watcher started")
    }

    private fun TelegramBot.updateChats() = try {
        logger.info("Updating chats...")
        chatRepository.findAll().forEach { updateChat(it) }
        logger.info("Updating chats finished")
    } catch (e: Exception) {
        logger.error("Failed to update chats", e)
    }

    private fun TelegramBot.updateChat(info: Chat) {
        logger.info("Updating chat: $info")
        val data = info.data
        val wishList = data.wishList!!
        val items = wishList.items.map { item -> updateAndNotify(item, info.chatId) }.toSet()
        //FIXME
        chatRepository.save(
            info.copy(data = data.copy(wishList = WishList(emptySet())))
        )
        chatRepository.save(
            info.copy(data = data.copy(wishList = WishList(items)))
        )
    }

    private fun TelegramBot.updateChat(chatId: Long) = chatRepository.findByChatId(chatId).ifPresent { updateChat(it) }

    private fun TelegramBot.updateAndNotify(item: Item, chatId: Long): Item =
        fetch(item.url).apply { notify(chatId, item.price, this) }

    private fun TelegramBot.notify(user: Long, oldPrice: Double, item: Item) {
        fun Item.priceDown() = "Цена упала!\n<pre>${name}</pre>\nстоит: <u>${price}</u>\n\n$url"
        fun Item.noMore() = "Похоже закончилось...${EMOJI_SAD}\n<pre>${name}</pre>\n\n$url"
        fun SendMessage.withMarkup() = replyMarkup(itemMarkup(item)).parseMode(HTML)
        when {
            item.price < oldPrice -> send(user, item.priceDown()) { withMarkup() }
            item.price == 0.0 && oldPrice != 0.0 -> send(user, item.noMore()) { withMarkup() }
        }
    }

    private fun TelegramBot.handle(updates: MutableList<Update>): Int = updates.forEach {
        when {
            it.callbackQuery() != null -> handleCallback(it.callbackQuery())
            it.message() != null -> handleMessage(it.message())
        }
    }.let {
        UpdatesListener.CONFIRMED_UPDATES_ALL
    }

    private fun TelegramBot.handleMessage(msg: Message) {
        val url = extractUrl(msg)
        when {
            url.isNotBlank() -> follow(url, msg)
            msg.text().let { it.startsWith("/start") || it.startsWith("/hello") } -> send(msg.chat(), introMessage)
            msg.text().startsWith("/list") -> list(msg.chat().id())
            msg.text().startsWith("/update") -> updateChat(msg.chat().id())
            msg.fromAdmin() && msg.text().startsWith("/updateAll") -> updateChats()
            msg.fromAdmin() && msg.text().startsWith("/db") -> db()
            msg.fromAdmin() && msg.text().startsWith(DB_REMOVE) -> dbRemove(msg)
            msg.fromAdmin() && dbWizard.inProgress() -> dbInProgress(msg)
        }
    }

    private fun Message.fromAdmin() = chat().id() == admin

    private fun extractUrl(msg: Message) =
        "(http|https)://[a-zA-Z0-9\\-.]+\\.[a-zA-Z]{2,3}(/\\S*)?".toRegex().find(msg.text())?.groupValues?.get(0) ?: ""

    private fun TelegramBot.handleCallback(cb: CallbackQuery) {
        when {
            cb.data() == CB_LIST -> listItems(cb.chatId(), cb.messageId())
            cb.data().startsWith(CB_OPEN) -> openItem(cb.chatId(), cb.messageId(), cb.targetId(CB_OPEN))
            cb.data().startsWith(CB_DEL) -> deleteItem(cb.chatId(), cb.messageId(), cb.targetId(CB_DEL))
        }
    }

    private fun TelegramBot.listItems(chatId: Long, messageId: Int) = listItems(chatId).ifPresent { pair ->
        edit(chatId, messageId, "Вот за чем я слежу:") { replyMarkup(pair.second) }
    }

    private fun TelegramBot.openItem(chatId: Long, messageId: Int, id: String) {
        chatRepository.findByChatId(chatId)
            .map { info -> info.data.wishList?.items?.find { it.id == id } }
            .map { item ->
                edit(chatId, messageId, "${item!!.name}\nЦена: ${item.price}\nКол-во: ${item.quantity}\n${item.url}") {
                    replyMarkup(itemMarkup(item))
                }
            }
    }

    private fun TelegramBot.deleteItem(chatId: Long, messageId: Int, id: String) {
        chatRepository.findByChatId(chatId).ifPresent { info ->
            val wishList = info.data.wishList!!
            chatRepository.save(
                info.apply {
                    data = data.copy(
                        wishList = wishList.copy(items = wishList.items.filter { it.id != id }.toSet())
                    )
                }
            )
        }
        listItems(chatId).ifPresent { pair ->
            edit(chatId, messageId, "Вот за чем я слежу:") { replyMarkup(pair.second) }
        }
    }

    private fun itemMarkup(item: Item) = Keyboard(
        arrayOf(Button(EMOJI_GLOBE).url(item.url), Button(EMOJI_DEL).callbackData("$CB_DEL${item.id}")),
        arrayOf(Button(EMOJI_BACK).callbackData(CB_LIST))
    )

    private fun CallbackQuery.targetId(anchor: String) = data().substring(anchor.length)

    private fun listItems(chatId: Long): Optional<Pair<String, Keyboard>> =
        chatRepository.findByChatId(chatId).map { chat ->
            "Вот за чем я слежу:" to Keyboard(
                *chat.data.wishList?.items?.map { item -> item.toButtons() }?.toTypedArray() ?: emptyArray()
            )
        }

    private fun TelegramBot.list(chatId: Long) = listItems(chatId).ifPresentOrElse(
        { pair -> send(chatId, pair.first) { replyMarkup(pair.second) } },
        { send(chatId, "Пока список пуст. Попробуй отправить мне ссылку.") }
    )

    private fun Item.toButtons() = arrayOf(Button("${name.take(25)} - $price").callbackData("$CB_OPEN$id"))

    private fun TelegramBot.dbInProgress(msg: Message) {
        if (dbWizard.finished()) {
            scriptsRepository.save(Script(pattern = dbWizard.steps[1].resp, script = dbWizard.steps[2].resp))
            send(admin, "committed")
            dbWizard.reset()
        } else {
            send(admin, dbWizard.next(msg.text()).msg)
        }
    }

    private fun TelegramBot.db() {
        dbWizard.reset()
        send(
            admin,
            scriptsRepository.findAll()
                .joinToString(separator = "\n\n") { it.toString() } + "\n\n" + dbWizard.next("").msg
        )
    }

    private fun dbRemove(msg: Message) =
        scriptsRepository.deleteById(msg.text().substring(DB_REMOVE.length + 1).toLong())

    private fun TelegramBot.follow(url: String, msg: Message) {
        logger.info("URL to follow [$url], chat ${msg.chat().id()}")
        val chatId = msg.chat().id()
        try {
            fetch(url).also {
                saveItem(chatId, it)
                send(chatId, "ОК! Буду следить.\n<pre>${it.name}</pre>\nстоит: <u>${it.price}</u>") {
                    replyToMessageId(msg.messageId()).replyMarkup(listMarkup()).parseMode(HTML)
                }
            }
        } catch (nf: ScriptNotFound) {
            logger.warn("Script not found", nf)
            send(chatId, "Извините... Пока не умею следить за такими ссылками ${nf.message}")
        } catch (e: Exception) {
            logger.error("Fail to follow", e)
            send(chatId, "Извините... Что-то пошло не так =(")
        }
    }

    private fun saveItem(chatId: Long, result: Item) = chatRepository.findByChatId(chatId).ifPresentOrElse(
        { chat ->
            chatRepository.save(
                chat.apply { data = data.copy(wishList = data.wishList!!.copy(items = data.wishList!!.items + result)) }
            )
        },
        { chatRepository.save(Chat(chatId = chatId, data = ChatData(WishList(setOf(result))))) }
    )

    private fun listMarkup() = Keyboard(Button("↙️").callbackData(CB_LIST))

    companion object : KLogging() {
        const val CB_LIST = "list"
        const val CB_OPEN = "open#"
        const val CB_DEL = "del#"
        const val DB_REMOVE = "/db-remove"
        const val EMOJI_GLOBE = "\uD83C\uDF10"
        const val EMOJI_DEL = "❌"
        const val EMOJI_BACK = "\uD83D\uDD19"
        const val EMOJI_SAD = "\uD83D\uDE1E"
    }
}

@Serializable
data class Item(val url: String, var name: String = "-", var price: Double = 0.0, var quantity: Int = 0) {
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        javaClass != other?.javaClass -> false
        else -> id == (other as Item).id
    }

    val id: String
        get() = UUID.nameUUIDFromBytes(url.toByteArray()).toString()

    override fun hashCode(): Int = id.hashCode()
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
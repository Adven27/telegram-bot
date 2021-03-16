package io.adven27.telegram.bots.mamot

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.*
import io.adven27.telegram.bots.Bot
import io.adven27.telegram.bots.mamot.commands.*
import io.adven27.telegram.bots.mamot.legacy.HandlersChainListener
import io.adven27.telegram.bots.mamot.legacy.MessageCommand
import io.adven27.telegram.bots.mamot.legacy.UpdateHandler
import mu.KLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "bot.mamot", name = ["enabled"], havingValue = "true", matchIfMissing = false)
class Mamot(private val mamotConfig: MamotConfig) : Bot {

    override fun start() {
        val handlers = updateHandlers()
        val bot = TelegramBot(mamotConfig.token)
        bot.setUpdatesListener(
            HandlersChainListener(
                bot,
                object : UpdateHandler {
                    override fun handle(bot: TelegramBot, update: Update): Boolean {
                        return if (update.message() != null) printHelp(handlers, bot, update.message().chat())
                        else repostFromChannel(bot, update)
                    }
                },
                *handlers
            )
        )
    }

    private fun updateHandlers(): Array<UpdateHandler> {
        val localizationService = LocalizationService()
        val dao = DAO()
        val weatherPrinter = WeatherPrinter(localizationService, dao)
        val weather: Weather = SimpleWeather(weatherPrinter, WeatherResource())

        return arrayOf(
            WeatherCommand(weather),
            TicTacToeCommand(),
            AdviceCommand(MessageFromURL(AdviceResource(), AdvicePrinter())),
            QuoteCommand(MessageFromURL(QuoteResource(), QuotePrinter())),
            SupCommand(dao),
            Game2048Command(
                PGSQLGameRepo(mamotConfig.database),
                LeaderBoardImpl(PGSQLGameLeaderBoardRepo(mamotConfig.database))
            ),
            UncleBobCommand(PreviewPrinter()),
        )
    }

    private fun printHelp(handlers: Array<UpdateHandler>, b: TelegramBot, chat: Chat): Boolean {
        b.execute(SendSticker(chat, Sticker.HELP.id()))
        b.execute(SendMessage(chat, helpMessage(handlers)))
        return true
    }

    private fun helpMessage(handlers: Array<UpdateHandler>): String? =
        handlers.filterIsInstance<MessageCommand>().joinToString("\n") { it.description() }

    private fun repostFromChannel(b: TelegramBot, u: Update): Boolean {
        val post = if (u.channelPost() == null) u.editedChannelPost() else u.channelPost()
        if (post != null) {
            when {
                post.document() != null -> b.execute(SendDocument(mamotConfig.repostTo, post.document().fileId()))
                post.audio() != null -> b.execute(SendAudio(mamotConfig.repostTo, post.audio().fileId()))
                post.video() != null -> b.execute(SendVideo(mamotConfig.repostTo, post.video().fileId()))
                post.photo() != null -> b.execute(SendPhoto(mamotConfig.repostTo, post.photo()[0].fileId()))
                !post.text().isNullOrBlank() -> b.execute(SendMessage(mamotConfig.repostTo, post.text()))
            }
            return true
        }
        return false
    }

    companion object : KLogging()
}

@Component
@ConfigurationProperties(prefix = "bot.mamot")
data class MamotConfig(
    var enabled: Boolean = false,
    var token: String = "null",
    var repostTo: Long = 0L,
    var team: Long = 0L,
    var tasksNy: NyTask = NyTask(),
    var database: String = "null",
)

data class NyTask(val cron: String = "-", val caption: String = "", val photo: String = "")

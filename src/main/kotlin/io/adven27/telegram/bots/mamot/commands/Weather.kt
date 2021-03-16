package io.adven27.telegram.bots.mamot.commands

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.User
import com.pengrad.telegrambot.model.request.ParseMode.Markdown
import io.adven27.telegram.bots.mamot.commands.Emoji.*
import io.adven27.telegram.bots.mamot.legacy.MessageCommand
import io.adven27.telegram.bots.send
import mu.KLogging
import java.io.UnsupportedEncodingException
import java.lang.String.format
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class WeatherCommand(private val weather: Weather) : MessageCommand("/weather", "current weather") {
    override fun execute(bot: TelegramBot, user: User, chat: Chat, params: String?) {
        bot.send(chat.id(), print()) { it.disableWebPagePreview(true).parseMode(Markdown) }
    }

    private fun print(): String = try {
        weather.printCurrentFor(39.888599, 59.2187, "ru", "metric").toString() + "\n\n" +
                weather.printForecastFor(39.888599, 59.2187, "ru", "metric")
    } catch (e: Exception) {
        logger.error("Error", e)
        "Связь с атмосферой потеряна..."
    }

    companion object : KLogging()
}

interface Weather {
    fun printForecastFor(city: String, language: String, units: String): String?
    fun printForecastFor(longitude: Double, latitude: Double, language: String, units: String): String?
    fun printCurrentFor(city: String, language: String, units: String): String?
    fun printCurrentFor(longitude: Double, latitude: Double, language: String, units: String): String?
}

class SimpleWeather(private val weatherPrinter: WeatherPrinter, private val weatherResource: WeatherResource) : Weather {
    override fun printForecastFor(city: String, language: String, units: String): String =
        weatherPrinter.printForecast(language, weatherResource.fetchForecastBy(city, language, units))

    override fun printForecastFor(longitude: Double, latitude: Double, language: String, units: String): String =
        weatherPrinter.printForecast(
            language,
            weatherResource.fetchForecastBy(longitude, latitude, language, units)
        )

    override fun printCurrentFor(city: String, language: String, units: String): String =
        weatherPrinter.printCurrent(language, weatherResource.fetchCurrentBy(city, language, units))

    override fun printCurrentFor(longitude: Double, latitude: Double, language: String, units: String): String =
        weatherPrinter.printMamologda(
            language,
            weatherResource.fetchCurrentBy(longitude, latitude, language, units)
        )
}

class WeatherResource : JsonResource() {
    fun fetchForecastBy(city: String, language: String, units: String): org.json.JSONObject {
        return getObjectFrom(buildForecastUrlBy(city, language, units))
    }

    fun fetchForecastBy(longitude: Double, latitude: Double, language: String, units: String): org.json.JSONObject {
        return getObjectFrom(buildForecastUrlBy(longitude, latitude, language, units))
    }

    fun fetchCurrentBy(city: String, language: String, units: String): org.json.JSONObject {
        return getObjectFrom(buildCurrentWeatherUrlBy(city, language, units))
    }

    fun fetchCurrentBy(longitude: Double, latitude: Double, language: String, units: String): org.json.JSONObject {
        return getObjectFrom(buildCurrentWeatherUrlBy(longitude, latitude, language, units))
    }

    private fun buildRequestUrl(type: String, query: String, lang: String, units: String): String {
        return "$BASEURL$type?$query" + REQUEST_PARAMS.replace("@language@", lang)
            .replace("@units@", units) + APIIDEND
    }

    private fun buildForecastUrlBy(longitude: Double, latitude: Double, language: String, units: String): String {
        return buildRequestUrl(FORECASTPATH, getLocationQuery(longitude, latitude), language, units)
    }

    private fun buildForecastUrlBy(city: String, language: String, units: String): String {
        return buildRequestUrl(FORECASTPATH, getCityQuery(city), language, units)
    }

    private fun buildCurrentWeatherUrlBy(city: String, language: String, units: String): String {
        return buildRequestUrl(CURRENTPATH, getCityQuery(city), language, units)
    }

    private fun buildCurrentWeatherUrlBy(longitude: Double, latitude: Double, language: String, units: String): String {
        return buildRequestUrl(CURRENTPATH, getLocationQuery(longitude, latitude), language, units)
    }

    private fun getLocationQuery(longitude: Double, latitude: Double): String {
        return try {
            "lat=" + URLEncoder.encode(
                latitude.toString() + "",
                "UTF-8"
            ) + "&lon=" + URLEncoder.encode(longitude.toString() + "", "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    private fun getCityQuery(city: String): String {
        return try {
            "id=" + URLEncoder.encode(city.toInt().toString() + "", "UTF-8")
        } catch (e: NumberFormatException) {
            try {
                "q=" + URLEncoder.encode(city, "UTF-8")
            } catch (e1: UnsupportedEncodingException) {
                throw RuntimeException(e)
            }
        } catch (e: NullPointerException) {
            try {
                "q=" + URLEncoder.encode(city, "UTF-8")
            } catch (e1: UnsupportedEncodingException) {
                throw RuntimeException(e)
            }
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        private const val OPENWEATHERAPIKEY = "70f61f3bdf754f840006e26a969b7e83"
        private const val BASEURL = "http://api.openweathermap.org/data/2.5/"
        private const val FORECASTPATH = "forecast/daily"
        private const val CURRENTPATH = "weather"
        private const val APIIDEND = "&APPID=$OPENWEATHERAPIKEY"
        private const val REQUEST_PARAMS = "&cnt=7&units=@units@&lang=@language@"
    }
}

class WeatherPrinter(private val localisation: LocalizationService, private val dao: DAO) {
    fun printForecast(language: String, js: org.json.JSONObject): String {
        return if (isOkResp(js)) format(
            localisation.getString("weatherForecast", language),
            convertListOfForecastToString(js, language, true)
        ) else localisation.getString("cityNotFound", language)
    }

    fun printCurrent(language: String, js: org.json.JSONObject): String {
        return if (isOkResp(js)) {
            val city: String = js.getString("name") + " (" + js.getJSONObject("sys").getString("country") + ")"
            format(localisation.getString("weatherCurrent", language), city, convertCurrentWeatherToString(js))
        } else {
            localisation.getString("cityNotFound", language)
        }
    }

    private fun isOkResp(json: org.json.JSONObject): Boolean {
        return json.getInt("cod") == 200
    }

    private fun convertListOfForecastToString(js: org.json.JSONObject, language: String, addDate: Boolean): String {
        var responseToUser = ""
        for (i in 0 until js.getJSONArray("list").length()) {
            val internalJSON: org.json.JSONObject = js.getJSONArray("list").getJSONObject(i)
            responseToUser += convertInternalInformationToString(internalJSON, language, addDate)
        }
        return responseToUser
    }

    private fun convertInternalInformationToString(
        js: org.json.JSONObject,
        language: String,
        addDate: Boolean
    ): String {
        val date = Instant.ofEpochSecond(js.getLong("dt")).atZone(ZoneId.systemDefault()).toLocalDate()
        val tMin: String = js.getJSONObject("temp").get("min").toString()
        val tMax: String = js.getJSONObject("temp").get("max").toString()
        val weather: org.json.JSONObject = js.getJSONArray("weather").getJSONObject(0)
        val emoji: Emoji? = getEmojiForWeather(weather)
        val weatherDesc: String = weather.getString("description")
        return if (addDate) format(
            localisation.getString("forecastWeatherPartMetric", language),
            emoji?.toString() ?: "",
            DateTimeFormatter.ofPattern("EEE dd.MM").format(date),
            tMax,
            tMin,
            weatherDesc
        ) else format(
            localisation.getString("alertWeatherPartMetric", language),
            emoji?.toString() ?: weatherDesc,
            tMax,
            tMin
        )
    }

    //KTODO Fix hard code
    fun printMamologda(language: String, json: org.json.JSONObject): String = if (isOkResp(json)) String.format(
        "Сегодня в Мамологде %s",
        convertCurrentWeatherToString(json)
    ) else localisation.getString("cityNotFound", language)

    private fun convertCurrentWeatherToString(js: org.json.JSONObject): String {
        val temp: String = js.getJSONObject("main").get("temp").toString()
        val cloudiness: String = js.getJSONObject("clouds").get("all").toString() + "%"
        val wind: String = js.getJSONObject("wind").get("speed").toString() + "м/с"
        val weather: org.json.JSONObject = js.getJSONArray("weather").getJSONObject(0)
        val emoji: Emoji? = getEmojiForWeather(weather)
        val weatherDesc: String = weather.getString("description")
        return format(
            " %s %s *%sC* ветер *%s* мамоблачность *%s*... %s",
            weatherDesc, emoji?.toString() ?: "", temp, wind, cloudiness,
            dao.getEndWord(Random().nextInt(4))
        )
    }

    private fun getEmojiForWeather(weather: org.json.JSONObject): Emoji? = when (weather.getString("icon")) {
        "01n", "01d" -> SUN_WITH_FACE
        "02n", "02d" -> Emoji.SUN_BEHIND_CLOUD
        "03n", "03d", "04n", "04d" -> CLOUD
        "09n", "09d", "10n", "10d" -> UMBRELLA_WITH_RAIN_DROPS
        "11n", "11d" -> HIGH_VOLTAGE_SIGN
        "13n", "13d" -> SNOWFLAKE
        "50n", "50d" -> FOGGY
        else -> null
    }
}

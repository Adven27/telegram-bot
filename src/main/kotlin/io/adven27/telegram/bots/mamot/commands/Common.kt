package io.adven27.telegram.bots.mamot.commands

import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.io.FeedException
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.entity.BufferedHttpEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Enumerate of emojis with unicode chars
 * @date 02 of July of 2015
 */
enum class Emoji(private val firstChar: Char?, second: Char) {
    // Emoticones group
    GRINNING_FACE_WITH_SMILING_EYES('\uD83D', '\uDE01'), FACE_WITH_TEARS_OF_JOY(
        '\uD83D',
        '\uDE02'
    ),
    SMILING_FACE_WITH_OPEN_MOUTH('\uD83D', '\uDE03'), SMILING_FACE_WITH_OPEN_MOUTH_AND_SMILING_EYES(
        '\uD83D',
        '\uDE04'
    ),
    SMILING_FACE_WITH_OPEN_MOUTH_AND_COLD_SWEAT(
        '\uD83D',
        '\uDE05'
    ),
    SMILING_FACE_WITH_OPEN_MOUTH_AND_TIGHTLY_CLOSED_EYES('\uD83D', '\uDE06'), WINKING_FACE(
        '\uD83D',
        '\uDE09'
    ),
    SMILING_FACE_WITH_SMILING_EYES('\uD83D', '\uDE0A'), FACE_SAVOURING_DELICIOUS_FOOD(
        '\uD83D',
        '\uDE0B'
    ),
    RELIEVED_FACE('\uD83D', '\uDE0C'), SMILING_FACE_WITH_HEART_SHAPED_EYES('\uD83D', '\uDE0D'), SMIRKING_FACE(
        '\uD83D',
        '\uDE0F'
    ),
    UNAMUSED_FACE('\uD83D', '\uDE12'), FACE_WITH_COLD_SWEAT('\uD83D', '\uDE13'), PENSIVE_FACE(
        '\uD83D',
        '\uDE14'
    ),
    CONFOUNDED_FACE('\uD83D', '\uDE16'), FACE_THROWING_A_KISS('\uD83D', '\uDE18'), KISSING_FACE_WITH_CLOSED_EYES(
        '\uD83D',
        '\uDE1A'
    ),
    FACE_WITH_STUCK_OUT_TONGUE_AND_WINKING_EYE('\uD83D', '\uDE1C'), FACE_WITH_STUCK_OUT_TONGUE_AND_TIGHTLY_CLOSED_EYES(
        '\uD83D',
        '\uDE1D'
    ),
    DISAPPOINTED_FACE('\uD83D', '\uDE1E'), ANGRY_FACE('\uD83D', '\uDE20'), POUTING_FACE(
        '\uD83D',
        '\uDE21'
    ),
    CRYING_FACE('\uD83D', '\uDE22'), PERSEVERING_FACE('\uD83D', '\uDE23'), FACE_WITH_LOOK_OF_TRIUMPH(
        '\uD83D',
        '\uDE24'
    ),
    DISAPPOINTED_BUT_RELIEVED_FACE('\uD83D', '\uDE25'), FEARFUL_FACE('\uD83D', '\uDE28'), WEARY_FACE(
        '\uD83D',
        '\uDE29'
    ),
    SLEEPY_FACE('\uD83D', '\uDE2A'), TIRED_FACE('\uD83D', '\uDE2B'), LOUDLY_CRYING_FACE(
        '\uD83D',
        '\uDE2D'
    ),
    FACE_WITH_OPEN_MOUTH_AND_COLD_SWEAT('\uD83D', '\uDE30'), FACE_SCREAMING_IN_FEAR(
        '\uD83D',
        '\uDE31'
    ),
    ASTONISHED_FACE('\uD83D', '\uDE32'), FLUSHED_FACE('\uD83D', '\uDE33'), DIZZY_FACE(
        '\uD83D',
        '\uDE35'
    ),
    FACE_WITH_MEDICAL_MASK('\uD83D', '\uDE37'), GRINNING_CAT_FACE_WITH_SMILING_EYES(
        '\uD83D',
        '\uDE38'
    ),
    CAT_FACE_WITH_TEARS_OF_JOY('\uD83D', '\uDE39'), SMILING_CAT_FACE_WITH_OPEN_MOUTH(
        '\uD83D',
        '\uDE3A'
    ),
    SMILING_CAT_FACE_WITH_HEART_SHAPED_EYES('\uD83D', '\uDE3B'), CAT_FACE_WITH_WRY_SMILE(
        '\uD83D',
        '\uDE3C'
    ),
    KISSING_CAT_FACE_WITH_CLOSED_EYES('\uD83D', '\uDE3D'), POUTING_CAT_FACE('\uD83D', '\uDE3E'), CRYING_CAT_FACE(
        '\uD83D',
        '\uDE3F'
    ),
    WEARY_CAT_FACE('\uD83D', '\uDE40'), FACE_WITH_NO_GOOD_GESTURE('\uD83D', '\uDE45'), FACE_WITH_OK_GESTURE(
        '\uD83D',
        '\uDE46'
    ),
    PERSON_BOWING_DEEPLY('\uD83D', '\uDE47'), SEE_NO_EVIL_MONKEY('\uD83D', '\uDE48'), HEAR_NO_EVIL_MONKEY(
        '\uD83D',
        '\uDE49'
    ),
    SPEAK_NO_EVIL_MONKEY('\uD83D', '\uDE4A'), HAPPY_PERSON_RAISING_ONE_HAND(
        '\uD83D',
        '\uDE4B'
    ),
    PERSON_RAISING_BOTH_HANDS_IN_CELEBRATION('\uD83D', '\uDE4C'), PERSON_FROWNING(
        '\uD83D',
        '\uDE4D'
    ),
    PERSON_WITH_POUTING_FACE('\uD83D', '\uDE4E'), PERSON_WITH_FOLDED_HANDS('\uD83D', '\uDE4F'),  // Dingbats group
    BLACK_SCISSORS(null, '\u2702'), WHITE_HEAVY_CHECK_MARK(null, '\u2705'), AIRPLANE(null, '\u2708'), ENVELOPE(
        null,
        '\u2709'
    ),
    RAISED_FIST(null, '\u270A'), RAISED_HAND(null, '\u270B'), VICTORY_HAND(null, '\u270C'), PENCIL(
        null,
        '\u270F'
    ),
    BLACK_NIB(null, '\u2712'), HEAVY_CHECK_MARK(null, '\u2714'), HEAVY_MULTIPLICATION_X(null, '\u2716'), SPARKLES(
        null,
        '\u2728'
    ),
    EIGHT_SPOKED_ASTERISK(null, '\u2733'), EIGHT_POINTED_BLACK_STAR(null, '\u2734'), SNOWFLAKE(null, '\u2744'), SPARKLE(
        null,
        '\u2747'
    ),
    CROSS_MARK(null, '\u274C'), NEGATIVE_SQUARED_CROSS_MARK(null, '\u274E'), BLACK_QUESTION_MARK_ORNAMENT(
        null,
        '\u2753'
    ),
    WHITE_QUESTION_MARK_ORNAMENT(null, '\u2754'), WHITE_EXCLAMATION_MARK_ORNAMENT(
        null,
        '\u2755'
    ),
    HEAVY_EXCLAMATION_MARK_SYMBOL(null, '\u2757'), HEAVY_BLACK_HEART(null, '\u2764'), HEAVY_PLUS_SIGN(
        null,
        '\u2795'
    ),
    HEAVY_MINUS_SIGN(null, '\u2796'), HEAVY_DIVISION_SIGN(null, '\u2797'), BLACK_RIGHTWARDS_ARROW(
        null,
        '\u27A1'
    ),
    CURLY_LOOP(null, '\u27B0'),  // Transport and map symbols Group
    ROCKET('\uD83D', '\uDE80'), RAILWAY_CAR('\uD83D', '\uDE83'), HIGH_SPEED_TRAIN(
        '\uD83D',
        '\uDE84'
    ),
    HIGH_SPEED_TRAIN_WITH_BULLET_NOSE('\uD83D', '\uDE85'), METRO('\uD83D', '\uDE87'), STATION('\uD83D', '\uDE89'), BUS(
        '\uD83D',
        '\uDE8C'
    ),
    BUS_STOP('\uD83D', '\uDE8F'), AMBULANCE('\uD83D', '\uDE91'), FIRE_ENGINE('\uD83D', '\uDE92'), POLICE_CAR(
        '\uD83D',
        '\uDE93'
    ),
    TAXI('\uD83D', '\uDE95'), AUTOMOBILE('\uD83D', '\uDE97'), RECREATIONAL_VEHICLE('\uD83D', '\uDE99'), DELIVERY_TRUCK(
        '\uD83D',
        '\uDE9A'
    ),
    SHIP('\uD83D', '\uDEA2'), SPEEDBOAT('\uD83D', '\uDEA4'), HORIZONTAL_TRAFFIC_LIGHT(
        '\uD83D',
        '\uDEA5'
    ),
    CONSTRUCTION_SIGN('\uD83D', '\uDEA7'), POLICE_CARS_REVOLVING_LIGHT('\uD83D', '\uDEA8'), TRIANGULAR_FLAG_ON_POST(
        '\uD83D',
        '\uDEA9'
    ),
    DOOR('\uD83D', '\uDEAA'), NO_ENTRY_SIGN('\uD83D', '\uDEAB'), SMOKING_SYMBOL('\uD83D', '\uDEAC'), NO_SMOKING_SYMBOL(
        '\uD83D',
        '\uDEAD'
    ),
    BICYCLE('\uD83D', '\uDEB2'), PEDESTRIAN('\uD83D', '\uDEB6'), MENS_SYMBOL(
        '\uD83D',
        '\uDEB9'
    ),
    WOMENS_SYMBOL('\uD83D', '\uDEBA'), RESTROOM('\uD83D', '\uDEBB'), BABY_SYMBOL('\uD83D', '\uDEBC'), TOILET(
        '\uD83D',
        '\uDEBD'
    ),
    WATER_CLOSET('\uD83D', '\uDEBE'), BATH('\uD83D', '\uDEC0'),  // Weather
    UMBRELLA_WITH_RAIN_DROPS(null, '\u2614'), HIGH_VOLTAGE_SIGN(null, '\u26A1'), SNOWMAN_WITHOUT_SNOW(
        null,
        '\u26C4'
    ),
    SUN_BEHIND_CLOUD(null, '\u26C5'), CLOSED_UMBRELLA('\uD83C', '\uDF02'), SUN_WITH_FACE('\uD83C', '\uDF1E'), FOGGY(
        '\uD83C',
        '\uDF01'
    ),
    CLOUD(null, '\u2601'), RIGHT_ARROW(null, '\u25B6'), RIGHT_DOUBLE_ARROW(null, '\u23E9'), LEFT_ARROW(
        null,
        '\u25C0'
    ),
    LEFT_DOUBLE_ARROW(null, '\u23EA'), UP_DOUBLE_ARROW(null, '\u23EB'), DOWN_DOUBLE_ARROW(
        null,
        '\u23EC'
    ),
    RIGHT_UP_ARROW(null, '\u2934'), RIGHT_DOWN_ARROW(null, '\u2935'), ANCHOR(null, '\u2693'), SAILBOAT(
        '\u26F5',
        '\uFE0F'
    ),
    ZERO('\u0030', '\u20E3'), ONE('\u0031', '\u20E3'), TWO('\u0032', '\u20E3'), THREE(
        '\u0033',
        '\u20E3'
    ),
    FOUR('\u0034', '\u20E3'), FIVE('\u0035', '\u20E3'), SIX('\u0036', '\u20E3'), SEVEN('\u0037', '\u20E3'), EIGHT(
        '\u0038',
        '\u20E3'
    ),
    NINE('\u0039', '\u20E3'), TEN('\uD83D', '\uDD1F'), EMPTY_CELL(null, '\u25FB'), CIRCLE_WHITE(
        null,
        '\u25FB'
    ),
    CIRCLE_BLACK(null, '\u25FB'), EMPTY_CELL_WHITE(null, '\u26AA'),  // Others
    LEFT_RIGHT_ARROW(null, '\u2194'), ALARM_CLOCK(null, '\u23F0'), SOON_WITH_RIGHTWARDS_ARROW_ABOVE(
        '\uD83D',
        '\uDD1C'
    ),
    EARTH_GLOBE_EUROPE_AFRICA('\uD83C', '\uDF0D'), GLOBE_WITH_MERIDIANS('\uD83C', '\uDF10'), STRAIGHT_RULER(
        '\uD83D',
        '\uDCCF'
    ),
    INFORMATION_SOURCE(null, '\u2139'), BLACK_RIGHT_POINTING_DOUBLE_TRIANGLE(
        null,
        '\u23E9'
    ),
    BLACK_RIGHT_POINTING_TRIANGLE(null, '\u25B6'), BACK_WITH_LEFTWARDS_ARROW_ABOVE('\uD83D', '\uDD19'), WRENCH(
        '\uD83D',
        '\uDD27'
    ),
    DIGIT_THREE(null, '\u0033'), CLIPBOARD('\uD83D', '\uDCCB'), THUMBS_UP_SIGN(
        '\uD83D',
        '\uDC4D'
    ),
    WHITE_RIGHT_POINTING_BACKHAND_INDEX('\uD83D', '\uDC49'), TEAR_OFF_CALENDAR(
        '\uD83D',
        '\uDCC6'
    ),
    LARGE_ORANGE_DIAMOND('\uD83D', '\uDD36'), HUNDRED_POINTS_SYMBOL('\uD83D', '\uDCAF'), ROUND_PUSHPIN(
        '\uD83D',
        '\uDCCD'
    ),
    WAVING_HAND_SIGN('\uD83D', '\uDC4B');

    private val secondChar: Char?
    override fun toString(): String {
        val sb = StringBuilder()
        if (firstChar != null) {
            sb.append(firstChar)
        }
        if (secondChar != null) {
            sb.append(secondChar)
        }
        return sb.toString()
    }

    init {
        secondChar = second
    }
}

open class JsonResource {
    protected fun getObjectFrom(url: String): org.json.JSONObject = org.json.JSONObject(from(url))
    protected fun getArrayFrom(url: String): org.json.JSONArray = org.json.JSONArray(from(url))

    private fun from(url: String): String {
        logger.info("Fetching from $url")
        val client: CloseableHttpClient =
            HttpClientBuilder.create().setSSLHostnameVerifier(NoopHostnameVerifier()).build()
        val buf = BufferedHttpEntity(client.execute(HttpGet(url)).entity)
        return EntityUtils.toString(buf, "UTF-8")
    }

    companion object : KLogging()
}

class LocalizationService {
    private val english: ResourceBundle
    private val russian: ResourceBundle

    private class CustomClassLoader(parent: ClassLoader?) : ClassLoader(parent) {
        override fun getResourceAsStream(name: String): InputStream? {
            val utf8in = parent.getResourceAsStream(name)
            if (utf8in != null) {
                try {
                    val utf8Bytes = ByteArray(utf8in.available())
                    utf8in.read(utf8Bytes, 0, utf8Bytes.size)
                    val iso8859Bytes = String(utf8Bytes, Charset.defaultCharset()).toByteArray(charset("ISO-8859-1"))
                    return ByteArrayInputStream(iso8859Bytes)
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    try {
                        utf8in.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            return null
        }
    }

    /**
     * Get a string in default language (en)
     *
     * @param key key of the resource to fetch
     * @return fetched string or error message otherwise
     */
    fun getString(key: String?): String {
        return getString(key, "en")
    }

    /**
     * Get a string in default language
     *
     * @param key key of the resource to fetch
     * @return fetched string or error message otherwise
     */
    fun getString(key: String?, language: String): String {
        return try {
            when (language.toLowerCase()) {
                "en" -> english.getString(key)
                "ru" -> russian.getString(key)
                else -> english.getString(key)
            }
        } catch (e: MissingResourceException) {
            "String not found"
        }
    }

    init {
        val loader = CustomClassLoader(Thread.currentThread().contextClassLoader)
        english = ResourceBundle.getBundle("localisation.strings", Locale("en", "US"), loader)
        russian = ResourceBundle.getBundle("localisation.strings", Locale("ru", "RU"), loader)
    }
}

class DAO {
    fun getEndWord(index: Int): String {
        return endWords()[index % endWords().size]
    }

    private val quotes: List<String>
        get() {
            var list: List<String> = ArrayList()
            val path = javaClass.classLoader.getResource(QUOTES_FILE).path.substring(1)
            try {
                Files.newBufferedReader(Paths.get(path)).use { br ->
                    list = br.lines()
                        .collect(Collectors.toList())
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return list
        }
    val quote: String
        get() {
            val i = Random().nextInt(200) + 1
            val quotes = quotes
            val qi = if (i % 2 == 0) i - 2 else i - 1
            val ai = qi + 1
            return """
                   ${quotes[qi]}
                   
                   ${quotes[ai]}
                   """.trimIndent()
        }
    val complement: String
        get() {
            val words1 = mapOf(0 to "так", 1 to "очень", 2 to "офигенски", 3 to "просто")
            val words2 = mapOf(0 to "круто", 1 to "потрясно", 2 to "вкусно", 3 to "улетно")
            val words3 = mapOf(
                0 to "выглядишь",
                1 to "пахнешь",
                2 to "кодишь",
                3 to "говнокодишь",
                4 to "делаешь вид что работаешь",
            )
            val words4 = mapOf(0 to "снова", 1 to "сегодня", 2 to "всегда", 3 to "пупсик")
            val random = Random()
            return "ты " + words1[random.nextInt(4)] + " " +
                    words2[random.nextInt(4)] + " " +
                    words3[random.nextInt(5)] + " " +
                    words4[random.nextInt(4)]
        }

    companion object {
        const val QUOTES_FILE = "quotes/quotes"
        private fun endWords() = listOf("наверное", "может быть", "мне кажется", "уверен!")
    }
}

enum class Sticker(private val id: String) {
    HI("BQADAgAD9gIAAi8P8AbW5LZuTmYRNwI"), ASS("BQADAgADugIAAi8P8AbTPDXTPJyALwI"),
    DRINK("BQADAgADGwMAAi8P8AaO8R3qFJ6cpgI"), EAT("BQADAgAD0gIAAi8P8AbA6c5fV6KeAAEC"),
    RUN("BQADAgADvAIAAi8P8AZyeCjjha4HpwI"), DANCE("BQADAgADHwMAAi8P8Aax9-Ibl9ozBwI"),
    THINK("BQADAgAD4wIAAi8P8AZPctLFKjIGjwI"), ASK("BQADAgAD4QIAAi8P8AbPKiGkzj2RIQI"),
    LOL("BQADAgADJAUAAi8P8AbOf9c5yMdOIQI"), HELP("BQADAgAD9AIAAi8P8AaNtW5R8yIbWAI"),
    ALONE("BQADAgADAwMAAi8P8AYvq92xk41GJgI"), BLA("BQADAgADHQMAAi8P8AZUoPRpcJ7uiAI");

    fun id(): String {
        return id
    }

    companion object {
        fun random(): Sticker {
            return values()[Random().nextInt(values().size)]
        }
    }
}

interface URLResource {
    fun fetch(): Map<String, String>
}

interface MessagePrinter {
    fun print(data: Map<String, String>): String
}

class MessageFromURL(private val resource: URLResource, private val printer: MessagePrinter) {
    fun print(): String = printer.print(resource.fetch())
}

open class HttpResource : Resource {
    override fun from(url: String): String {
        logger.info("Fetching from {}", url)
        val response = OkHttpClient.Builder().build().newCall(Request.Builder().url(url).build()).execute()
        return response.body()!!.string()
    }

    companion object : KLogging()
}

interface Resource {
    fun from(url: String): String
}

interface EntryPrinter {
    fun print(entry: Entry): String
}

class PreviewPrinter : EntryPrinter {
    override fun print(entry: Entry): String {
        return entry.title + "\n" + entry.link
    }
}

class Entry(val id: String, val title: String, val publishedDate: Date?, val content: String, val link: String)

class AtomFeed(override val url: URL) : Feed {

    constructor(path: String) : this(getURL(path))

    private fun loadRawFeed(): List<SyndEntry> = try {
        SyndFeedInput().build(XmlReader(url)).entries
    } catch (e: FeedException) {
        throw RetrieveFeedException("Error during retrieving feed entry.", e)
    } catch (e: IOException) {
        throw RetrieveFeedException("Error during retrieving feed entry.", e)
    }

    override fun get(): Entry? = with(loadRawFeed()) {
        if (isNotEmpty()) toEntry(this[0]) else null
    }

    override operator fun get(number: Int): List<Entry> {
        require(number >= 1) { "Number of requested entries should be greater than zero." }
        var total = number
        val raw: List<SyndEntry> = loadRawFeed()
        if (number - raw.size > 0) {
            total = raw.size
        }
        return getNumberOfLatestInReverseOrder(total)
    }

    override operator fun iterator(): Iterator<Entry> {
        return object : Iterator<Entry> {
            private val reverse: ReverseIterator<SyndEntry> = ReverseIterator(loadRawFeed())
            override fun hasNext(): Boolean {
                return reverse.hasNext()
            }

            override fun next(): Entry {
                return toEntry(reverse.next())
            }
        }
    }

    private fun getNumberOfLatestInReverseOrder(total: Int): List<Entry> {
        val entries: MutableList<Entry> = ArrayList(total)
        val i: Iterator<SyndEntry> = ReverseIterator(loadRawFeed().subList(0, total))
        while (i.hasNext()) {
            entries.add(toEntry(i.next()))
        }
        return entries
    }

    private fun toEntry(rawEntry: SyndEntry): Entry {
        return Entry(
            rawEntry.uri,
            rawEntry.title,
            rawEntry.publishedDate,
            rawEntry.contents[SINGLE].value,
            rawEntry.link
        )
    }

    private class ReverseIterator<T>(items: List<T>) : Iterator<T> {
        private val iterator: ListIterator<T> = items.listIterator(items.size)
        override fun hasNext(): Boolean {
            return iterator.hasPrevious()
        }

        override fun next(): T {
            return iterator.previous()
        }
    }

    class RetrieveFeedException(message: String?, e: Exception?) :
        RuntimeException(message, e)

    companion object {
        private const val SINGLE = 0
        private fun getURL(path: String): URL {
            return try {
                URL(path)
            } catch (e: MalformedURLException) {
                throw RetrieveFeedException("Error during retrieving feed entry.", e)
            }
        }
    }
}

interface Feed : Iterable<Entry?> {
    val url: URL?

    fun get(): Entry?
    operator fun get(number: Int): List<Entry?>?
}

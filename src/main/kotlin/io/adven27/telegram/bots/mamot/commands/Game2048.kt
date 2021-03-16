package io.adven27.telegram.bots.mamot.commands

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.Chat.Type.Private
import com.pengrad.telegrambot.model.User
import io.adven27.telegram.bots.*
import io.adven27.telegram.bots.mamot.commands.LeaderBoard.Companion.BOARD_SIZE
import io.adven27.telegram.bots.mamot.commands.LeaderBoardRepo.Record
import io.adven27.telegram.bots.mamot.db.PGSQLRepo
import io.adven27.telegram.bots.mamot.db.Repo
import io.adven27.telegram.bots.mamot.legacy.CallbackCommand
import io.adven27.telegram.bots.mamot.legacy.Game2048
import mu.KLogging
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URISyntaxException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import java.util.stream.Collectors

class Game2048Command(private val repo: Repo, private val leaderBoard: LeaderBoard) :
    CallbackCommand("/game2048", "Game 2048") {
    private var viewLeaderBoard = false
    override fun execute(bot: TelegramBot, user: User, chat: Chat, params: String?) {
        val userName = getUserName(user)
        if (userGames[userName] != null) {
            bot.send(chat, screen(chat)) { it.replyMarkup(inlineKeyboard) }
            return
        }
        val g = Game2048()
        userGames[userName] = g
        repo.insert(userName, g.toJSON())
        bot.send(chat, screen(chat)) { it.replyMarkup(inlineKeyboard) }
    }

    private fun getUserName(user: User): String = user.lastName()

    private val inlineKeyboard = keyboardBuilder
            .rowSigned(UP, DOWN, LEFT, RIGHT)
            .rowSigned(LEADER_BOARD, RESTART)
            .build()

    private fun drawTile(tile: Game2048.Tile): String {
        return when (tile.value) {
            2 -> Emoji.ZERO.toString()
            4 -> Emoji.ONE.toString()
            8 -> Emoji.TWO.toString()
            16 -> Emoji.THREE.toString()
            32 -> Emoji.FOUR.toString()
            64 -> Emoji.FIVE.toString()
            128 -> Emoji.SIX.toString()
            256 -> Emoji.SEVEN.toString()
            512 -> Emoji.EIGHT.toString()
            1024 -> Emoji.NINE.toString()
            2048 -> Emoji.TEN.toString()
            else -> Emoji.EMPTY_CELL.toString()
        }
    }

    private fun screen(chat: Chat): String {
        var msg = ""
        val games: Map<String, Game2048> = if (chat.type() == Private) userGames.filter { it.key == chat.lastName() }
        else userGames
        val gs: LinkedList<Game2048> = LinkedList<Game2048>()
        for ((key, g) in games) {
            gs.add(g)
            msg += key + " " + g.score
            repo.update(key, g.toJSON())
            if (g.isLose) {
                msg += LOSE_MSG
            } else if (g.isWin) {
                msg += WON_MSG
            }
            msg += "\n"
        }
        if (viewLeaderBoard) {
            msg = "\tTop\n"
            val recordStream: List<Record> = leaderBoard.all as List<Record>
            var pos = 1
            for (record in recordStream) {
                msg += """$pos. ${record.user()} ${record.score()}
"""
                pos++
            }
        }
        val maxInRow = 4
        var cur = 0
        val rows: MutableList<List<Game2048>> = ArrayList<List<Game2048>>()
        var row: MutableList<Game2048> = ArrayList<Game2048>()
        val size = gs.size
        for (i in 0 until size) {
            if (cur < maxInRow) {
                cur++
                row.add(gs.remove())
            } else {
                rows.add(row)
                row = ArrayList<Game2048>()
                row.add(gs.remove())
                cur = 1
            }
        }
        rows.add(row)
        for (r in rows) {
            for (y in 0..3) {
                for (game in r) {
                    for (x in 0..3) {
                        msg += drawTile(game.tiles[x + y * 4]!!)
                    }
                    msg += BORDER
                }
                msg += "\n"
            }
            msg += "\n"
        }
        return msg
    }

    override fun callback(bot: TelegramBot, cb: CallbackQuery): Boolean {
        val message = cb.message()
        if (userGames.isEmpty()) {
            repo.selectAll().forEach { (name, game) -> userGames[name] = Game2048(game) }
        }
        val data = cb.unsignedData()
        val from = cb.from()
        doAction(data, from)
        bot.edit(message.chat(), message.messageId(), screen(message.chat())) { it.replyMarkup(inlineKeyboard) }
        return true
    }

    private fun doAction(action: String, from: User) {
        val userName = getUserName(from)
        val g: Game2048? = userGames[userName]
        if (g == null) {
            userGames[userName] = Game2048()
        } else when (action) {
            LEFT -> g.left()
            RIGHT -> g.right()
            UP -> g.up()
            DOWN -> g.down()
            RESTART -> {
                leaderBoard.update(userName, g)
                g.resetGame()
            }
            LEADER_BOARD -> viewLeaderBoard = !viewLeaderBoard
            else -> {
            }
        }
    }

    companion object {
        private const val DOWN = "\uD83D\uDD3D"
        private const val UP = "\uD83D\uDD3C"
        private val LEFT = Emoji.LEFT_ARROW.toString()
        private val RIGHT: String = Emoji.RIGHT_ARROW.toString()
        private const val RESTART = "\uD83D\uDD04"
        private const val LEADER_BOARD = "\uD83D\uDCF6"
        private const val LOSE_MSG = " \uD83D\uDC80"
        private const val WON_MSG = " \uD83C\uDF89\uD83C\uDF89\uD83C\uDF89"
        private const val BORDER = "\uD83D\uDDB1"
        private val userGames: MutableMap<String, Game2048> = HashMap<String, Game2048>()
    }

    init {
        repo.selectAll().forEach { (name, game) -> userGames[name] = Game2048(game) }
        println("userGames = $userGames")
    }
}

class Game20482 {
    var tiles: Array<Tile?> = arrayOfNulls(4 * 4)
    var isWin = false
    var isLose = false
    var score: Long = 0

    constructor(json: String) {
        setGame(json)
    }

    constructor() {
        resetGame()
    }

    constructor(score: Long) {
        this.score = score
        isLose = false
        isWin = isLose
        tiles = arrayOfNulls(4 * 4)
    }

    fun resetGame() {
        score = 0
        isWin = false
        isLose = false
        tiles = arrayOfNulls(4 * 4)
        for (i in tiles.indices) {
            tiles[i] = Tile()
        }
        addTile()
        addTile()
    }

    private fun setGame(json: String) {
        val jsonObject = JSONObject(json)
        score = jsonObject.getInt("score").toLong()
        isWin = jsonObject.getBoolean("win")
        isLose = jsonObject.getBoolean("lose")
        tiles = arrayOfNulls(4 * 4)
        for (i in tiles.indices) {
            tiles[i] = Tile(jsonObject.getJSONArray("tiles").getJSONObject(i).getInt("value"))
        }
    }

    fun left() {
        var needAddTile = false
        for (i in 0..3) {
            val line = getLine(i)
            val merged = mergeLine(moveLine(line))
            setLine(i, merged)
            if (!needAddTile && !compare(line, merged)) {
                needAddTile = true
            }
        }
        if (needAddTile) {
            addTile()
        }
        isLose = !canMove()
    }

    fun right() {
        tiles = rotate(180)
        left()
        tiles = rotate(180)
    }

    fun up() {
        tiles = rotate(270)
        left()
        tiles = rotate(90)
    }

    fun down() {
        tiles = rotate(90)
        left()
        tiles = rotate(270)
    }

    private fun tileAt(x: Int, y: Int): Tile? {
        return tiles[x + y * 4]
    }

    private fun addTile() {
        val list = availableSpace()
        if (availableSpace().isNotEmpty()) {
            val index = (Math.random() * list.size).toInt() % list.size
            val emptyTime = list[index]
            emptyTime.value = if (Math.random() < 0.9) 2 else 4
        }
    }

    private fun availableSpace(): List<Tile> {
        val list: MutableList<Tile> = ArrayList(16)
        for (t in tiles) {
            if (t!!.empty()) {
                list.add(t)
            }
        }
        return list
    }

    private val isFull: Boolean
        get() = availableSpace().isEmpty()

    fun canMove(): Boolean {
        if (!isFull) {
            return true
        }
        for (x in 0..3) {
            for (y in 0..3) {
                val t = tileAt(x, y)
                if (x < 3 && t!!.value == tileAt(x + 1, y)!!.value
                    || y < 3 && t!!.value == tileAt(x, y + 1)!!.value
                ) {
                    return true
                }
            }
        }
        return false
    }

    private fun compare(line1: Array<Tile?>, line2: Array<Tile?>): Boolean {
        if (line1.contentEquals(line2)) {
            return true
        } else if (line1.size != line2.size) {
            return false
        }
        for (i in line1.indices) {
            if (line1[i]!!.value != line2[i]!!.value) {
                return false
            }
        }
        return true
    }

    private fun rotate(angle: Int): Array<Tile?> {
        val newTiles: Array<Tile?> = arrayOfNulls(4 * 4)
        var offsetX = 3
        var offsetY = 3
        if (angle == 90) {
            offsetY = 0
        } else if (angle == 270) {
            offsetX = 0
        }
        val rad = Math.toRadians(angle.toDouble())
        val cos = Math.cos(rad).toInt()
        val sin = Math.sin(rad).toInt()
        for (x in 0..3) {
            for (y in 0..3) {
                val newX = x * cos - y * sin + offsetX
                val newY = x * sin + y * cos + offsetY
                newTiles[newX + newY * 4] = tileAt(x, y)
            }
        }
        return newTiles
    }

    private fun moveLine(oldLine: Array<Tile?>): Array<Tile?> {
        val l = LinkedList<Tile?>()
        for (i in 0..3) {
            if (!oldLine[i]!!.empty()) l.addLast(oldLine[i])
        }
        return if (l.size == 0) {
            oldLine
        } else {
            val newLine = arrayOfNulls<Tile>(4)
            ensureSize(l, 4)
            for (i in 0..3) {
                newLine[i] = l.removeFirst()
            }
            newLine
        }
    }

    private fun mergeLine(oldLine: Array<Tile?>): Array<Tile?> {
        val list = LinkedList<Tile?>()
        var i = 0
        while (i < 4 && !oldLine[i]!!.empty()) {
            var num = oldLine[i]!!.value
            if (i < 3 && oldLine[i]!!.value == oldLine[i + 1]!!.value) {
                num *= 2
                score += num.toLong()
                val ourTarget = 2048
                if (num == ourTarget) {
                    isWin = true
                }
                i++
            }
            list.add(Tile(num))
            i++
        }
        return if (list.size == 0) {
            oldLine
        } else {
            ensureSize(list, 4)
            list.toTypedArray()
        }
    }

    private fun getLine(index: Int): Array<Tile?> {
        val result = arrayOfNulls<Tile>(4)
        for (i in 0..3) {
            result[i] = tileAt(i, index)
        }
        return result
    }

    private fun setLine(index: Int, re: Array<Tile?>) {
        System.arraycopy(re, 0, tiles, index * 4, 4)
    }

    fun toJSON(): String {
        val jsonObject = JSONObject()
        jsonObject.put("tiles", JSONArray(tiles))
        jsonObject.put("win", isWin)
        jsonObject.put("lose", isLose)
        jsonObject.put("score", score)
        return jsonObject.toString()
    }

    class Tile @JvmOverloads constructor(var value: Int = 0) {
        fun empty(): Boolean = value == 0
    }

    companion object {
        private fun ensureSize(l: MutableList<Tile?>, s: Int) {
            while (l.size != s) {
                l.add(Tile())
            }
        }
    }
}

interface LeaderBoard {
    fun update(username: String, game: Game2048): Boolean
    val all: List<Any>

    companion object {
        const val BOARD_SIZE = 5
    }
}

interface LeaderBoardRepo {
    fun selectAll(): List<Record>
    fun replace(id: Int, username: String?, score: Long)
    fun insert(username: String?, score: Long)
    class Record(val id: Int, val user: String, val score: Long) {
        fun id(): Int {
            return id
        }

        fun user(): String {
            return user
        }

        fun score(): Long {
            return score
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val record = other as Record
            return score == record.score &&
                    user == record.user
        }

        override fun hashCode(): Int {
            return Objects.hash(user, score)
        }
    }
}

class PGSQLGameLeaderBoardRepo @JvmOverloads constructor(
    private val dbUrl: String,
    private val table: String = "games_leaderboard"
) : LeaderBoardRepo {
    private fun connect(): Connection? {
        var conn: Connection? = null
        try {
            val uri = URI(dbUrl)
            val username = uri.userInfo.split(":").toTypedArray()[0]
            val password = uri.userInfo.split(":").toTypedArray()[1]
            val url = String.format("jdbc:postgresql://%s:%d%s?sslmode=require", uri.host, uri.port, uri.path)
            conn = DriverManager.getConnection(url, username, password)
        } catch (e: SQLException) {
            println(e.message)
        } catch (e: URISyntaxException) {
            println(e.message)
        }
        return conn
    }

    override fun selectAll(): List<Record> {
        val result: MutableList<Record> = ArrayList()
        try {
            connect().use { c ->
                c!!.createStatement().executeQuery("SELECT id, username, score FROM $table").use { rs ->
                    while (rs.next()) {
                        result.add(Record(rs.getInt("id"), rs.getString("username"), rs.getLong("score")))
                    }
                }
            }
        } catch (e: SQLException) {
            println(e.message)
        }
        return result
    }

    override fun replace(id: Int, username: String?, score: Long) {
        //TODO TRANSACTION
        insert(username, score)
        delete(id)
    }

    override fun insert(username: String?, score: Long) {
        try {
            connect().use { conn ->
                conn!!.prepareStatement("INSERT INTO $table(username, score) VALUES(?,?)").use { ps ->
                    ps.setString(1, username)
                    ps.setLong(2, score)
                    ps.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    private fun delete(id: Int) {
        val sql = "DELETE FROM $table WHERE id = ?"
        try {
            connect().use { c ->
                c!!.prepareStatement(sql).use { ps ->
                    ps.setInt(1, id)
                    ps.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    fun createTable() {
        try {
            connect().use { c ->
                c!!.createStatement()
                    .execute("CREATE TABLE IF NOT EXISTS $table (id SERIAL PRIMARY KEY, username TEXT NOT NULL, score INT NOT NULL);")
            }
        } catch (e: SQLException) {
            logger.error("Failed to create table: ", e)
        }
    }

    fun dropTable() {
        try {
            connect().use { conn -> conn!!.createStatement().execute("DROP TABLE $table;") }
        } catch (e: SQLException) {
            logger.error("Failed to drop table: ", e)
        }
    }

    init {
        createTable()
    }

    companion object : KLogging()
}

class PGSQLGameRepo(databaseUrl: String) : PGSQLRepo(databaseUrl, "games", "game")

class LeaderBoardImpl(private val boardRepo: LeaderBoardRepo?) : LeaderBoard {
    override fun update(username: String, game: Game2048): Boolean {
        var res = false
        val records: List<Record?> = boardRepo!!.selectAll()
        if (records.size < BOARD_SIZE) {
            boardRepo.insert(username, game.score)
            res = true
        } else {
            val lastOne = records.stream().min { r1: Record?, r2: Record? ->
                if (r1!!.score() > r2!!.score()) return@min 1
                if (r2.score() > r1.score()) return@min -1
                0
            }.get()
            if (game.score > lastOne.score()) {
                boardRepo.replace(lastOne.id(), username, game.score)
                res = true
            }
        }
        return res
    }

    override val all: List<Record>
        get() = boardRepo!!.selectAll().stream().sorted { r1, r2 ->
            if (r1.score() > r2.score()) return@sorted -1
            if (r2.score() > r1.score()) return@sorted 1
            0
        }.collect(Collectors.toList())
}

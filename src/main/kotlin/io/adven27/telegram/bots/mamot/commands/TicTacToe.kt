package io.adven27.telegram.bots.mamot.commands

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.User
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import io.adven27.telegram.bots.*
import io.adven27.telegram.bots.mamot.legacy.CallbackCommand
import java.util.*

class TicTacToeCommand : CallbackCommand("/ttt", "Try to beat me, skin bastard...") {
    private val game: TicTacToe = TicTacToe()

    override fun execute(bot: TelegramBot, user: User, chat: Chat, params: String?) {
        bot.send(chat, game.toString()) { it.replyMarkup(keyboard) }
    }

    override fun callback(bot: TelegramBot, cb: CallbackQuery): Boolean {
        when {
            "reset" == cb.unsignedData() -> game.reset()
            game.result() === TicTacToe.Result.NONE -> game.move(cb.unsignedData().toInt())
            else -> {
                bot.answer(cb, "Game Over")
                return true
            }
        }
        bot.edit(cb.chatId(), cb.messageId(), game.toString()) { it.replyMarkup(keyboard) }
        return true
    }

    private val keyboard: InlineKeyboardMarkup = keyboardBuilder
        .rowSigned("0", "1", "2")
        .rowSigned("3", "4", "5")
        .rowSigned("6", "7", "8")
        .rowSigned("reset").build()
}

class TicTacToe {
    private var result = Result.NONE
    private var board = arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
    private var iter = 0
    private var round = 0

    fun availableMoves(): List<Int> = listOf(*avail(board))

    private fun board(): List<Int> = listOf(*board)

    private fun avail(reboard: Array<Int>): Array<Int> =
        reboard.filter { i: Int -> i != HUPLAYER && i != AIPLAYER }.toTypedArray()

    fun move(move: Int) {
        move(move, HUPLAYER)
    }

    fun reset() {
        round = 0
        board = arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
        result = Result.NONE
    }

    private fun move(move: Int, player: Int) {
        if (board[move] != HUPLAYER && board[move] != AIPLAYER) {
            round++
            board[move] = player
            log("HUMAN")
            if (winning(board, player)) {
                result = Result.HU
            } else if (round > 8) {
                result = Result.TIE
            } else {
                round++
                val index = minimax(board, AIPLAYER).index
                board[index] = AIPLAYER
                log("AI")
                if (winning(board, AIPLAYER)) {
                    result = Result.AI
                } else if (round == 0) {
                    result = Result.TIE
                }
            }
        }
    }

    private fun log(player: String) {
        println(
            "---$player---\n" + board.map { this.print(it) }
                .reduceIndexed { i: Int, s1: String, s2: String -> this.reduce(i, s1, s2) }
        )
    }

    private fun minimax(reboard: Array<Int>, player: Int): Move {
        iter++
        val array = avail(reboard)
        when {
            winning(reboard, HUPLAYER) -> return Move(-10)
            winning(reboard, AIPLAYER) -> return Move(10)
            array.isEmpty() -> return Move(0)
            else -> {
                val moves: MutableList<Move> = ArrayList()
                for (anArray in array) {
                    val move = Move(0)
                    move.index = reboard[anArray]
                    reboard[anArray] = player
                    if (player == AIPLAYER) {
                        val g = minimax(reboard, HUPLAYER)
                        move.score = g.score
                    } else {
                        val g = minimax(reboard, AIPLAYER)
                        move.score = g.score
                    }
                    reboard[anArray] = move.index
                    moves.add(move)
                }
                var bestMove = 0
                if (player == AIPLAYER) {
                    var bestScore = -10000
                    for (i in moves.indices) {
                        if (moves[i].score > bestScore) {
                            bestScore = moves[i].score
                            bestMove = i
                        }
                    }
                } else {
                    var bestScore = 10000
                    for (i in moves.indices) {
                        if (moves[i].score < bestScore) {
                            bestScore = moves[i].score
                            bestMove = i
                        }
                    }
                }
                return moves[bestMove]
            }
        }
    }

    private class Move internal constructor(var score: Int) {
        var index = 0
    }

    fun keepPlaying(): Boolean = result == Result.NONE

    fun result(): Result = result

    private fun winning(board: Array<Int>, player: Int): Boolean =
        board[0] == player && board[1] == player && board[2] == player ||
                board[3] == player && board[4] == player && board[5] == player ||
                board[6] == player && board[7] == player && board[8] == player ||
                board[0] == player && board[3] == player && board[6] == player ||
                board[1] == player && board[4] == player && board[7] == player ||
                board[2] == player && board[5] == player && board[8] == player ||
                board[0] == player && board[4] == player && board[8] == player ||
                board[2] == player && board[4] == player && board[6] == player

    override fun toString(): String {
        val result = when (result()) {
            Result.AI -> MSG_AI_WON
            Result.HU -> MSG_AI_LOSE
            Result.TIE -> MSG_TIE
            Result.NONE -> MSG_YOUR_TURN
        }
        return """
     $result
     
     """.trimIndent() + board()
            .map { this.print(it) }
            .reduceIndexed { i: Int, s1: String, s2: String -> this.reduce(i, s1, s2) }
    }

    private fun print(i: Int): String = when (i) {
        HUPLAYER -> Emoji.EMPTY_CELL_WHITE.toString()
        AIPLAYER -> Emoji.CROSS_MARK.toString()
        else -> Emoji.EMPTY_CELL.toString()
    }

    private fun reduce(i: Int, s1: String, s2: String): String = if (i % 3 == 0) {
        "$s1\n$s2"
    } else s1 + s2

    enum class Result { AI, HU, TIE, NONE }

    companion object {
        const val MSG_AI_WON = "Проиграл, кожаный ублюдок!"
        const val MSG_AI_LOSE = "Не может быть!!! Кожаный ублюдок выйграл... :("
        const val MSG_TIE = "Ничья, кожаный ублюдок!"
        const val MSG_YOUR_TURN = "Твой ход, кожаный ублюдок!"
        const val HUPLAYER = -1
        const val AIPLAYER = -2
    }
}

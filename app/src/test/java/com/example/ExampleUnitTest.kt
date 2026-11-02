package com.example

import com.example.domain.engine.AiDifficulty
import com.example.domain.engine.BoardRuleMode
import com.example.domain.engine.GameStatus
import com.example.domain.engine.Mark
import com.example.domain.engine.MoveRecord
import com.example.domain.engine.TicTacToeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `detects all 8 winning lines for X and O`() {
        for (line in TicTacToeEngine.WINNING_LINES) {
            for (mark in listOf(Mark.X, Mark.O)) {
                val board = MutableList<Mark?>(9) { null }
                line.forEach { idx -> board[idx] = mark }
                val win = TicTacToeEngine.checkWin(board)
                assertNotNull("Expected win for $mark on $line", win)
                assertEquals(mark, win?.winner)
                assertEquals(line, win?.indices)
            }
        }
    }

    @Test
    fun `detects draw condition accurately`() {
        // X O X
        // X O O
        // O X X
        val drawBoard = listOf<Mark?>(
            Mark.X, Mark.O, Mark.X,
            Mark.X, Mark.O, Mark.O,
            Mark.O, Mark.X, Mark.X
        )
        val (status, winLine) = TicTacToeEngine.evaluateStatus(drawBoard)
        assertEquals(GameStatus.DRAW, status)
        assertNull(winLine)
    }

    @Test
    fun `prevents invalid moves on occupied or out of bounds cells`() {
        val board = listOf<Mark?>(
            Mark.X, null, null,
            null, Mark.O, null,
            null, null, null
        )
        assertFalse(TicTacToeEngine.isValidMove(board, 0))
        assertFalse(TicTacToeEngine.isValidMove(board, 4))
        assertFalse(TicTacToeEngine.isValidMove(board, -1))
        assertFalse(TicTacToeEngine.isValidMove(board, 9))
        assertTrue(TicTacToeEngine.isValidMove(board, 1))
    }

    @Test
    fun `infinity 3-mark mode vanishes oldest mark on 4th placement`() {
        var board = List<Mark?>(9) { null }
        val history = mutableListOf<MoveRecord>()

        // X at 0, O at 3, X at 1, O at 4, X at 5, O at 8 -> X now has 3 marks (0, 1, 5)
        val moves = listOf(
            0 to Mark.X,
            3 to Mark.O,
            1 to Mark.X,
            4 to Mark.O,
            5 to Mark.X,
            8 to Mark.O
        )
        for ((idx, m) in moves) {
            val (nextBoard, vanished) = TicTacToeEngine.applyMove(
                currentBoard = board,
                moveHistory = history,
                cellIndex = idx,
                mark = m,
                ruleMode = BoardRuleMode.INFINITY_THREE
            )
            assertNull(vanished)
            board = nextBoard
            history.add(MoveRecord(idx, m, TicTacToeEngine.coordinateForIndex(idx), history.size + 1))
        }

        // Now X has 3 marks at 0, 1, 5. Oldest is 0.
        val oldestForX = TicTacToeEngine.getOldestActiveCellForMark(
            board = board,
            moveHistory = history,
            mark = Mark.X,
            ruleMode = BoardRuleMode.INFINITY_THREE
        )
        assertEquals(0, oldestForX)

        // When X places 4th mark at 6, index 0 vanishes
        val (afterFourthX, vanishedIdx) = TicTacToeEngine.applyMove(
            currentBoard = board,
            moveHistory = history,
            cellIndex = 6,
            mark = Mark.X,
            ruleMode = BoardRuleMode.INFINITY_THREE
        )
        assertEquals(0, vanishedIdx)
        assertNull(afterFourthX[0])
        assertEquals(Mark.X, afterFourthX[6])
        assertEquals(3, afterFourthX.count { it == Mark.X })
    }

    @Test
    fun `minimax hard mode takes immediate win and blocks immediate opponent threat`() {
        // O has 0 and 1, empty 2 -> O should play 2 to win immediately
        val winningBoard = listOf<Mark?>(
            Mark.O, Mark.O, null,
            Mark.X, Mark.X, null,
            null, null, null
        )
        val winDecision = TicTacToeEngine.findBestMove(
            board = winningBoard,
            aiMark = Mark.O,
            difficulty = AiDifficulty.HARD
        )
        assertEquals(2, winDecision.cellIndex)

        // X threatens 0 and 4 (diagonal to 8), O must block at 8
        val blockBoard = listOf<Mark?>(
            Mark.X, null, null,
            null, Mark.X, null,
            null, Mark.O, null
        )
        val blockDecision = TicTacToeEngine.findBestMove(
            board = blockBoard,
            aiMark = Mark.O,
            difficulty = AiDifficulty.HARD
        )
        assertEquals(8, blockDecision.cellIndex)
    }
}

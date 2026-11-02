package com.example.domain.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class Mark(val symbol: String, val label: String) {
    X("X", "Player X"),
    O("O", "Player O");

    fun opponent(): Mark = if (this == X) O else X
}

enum class GameMode(val id: String, val title: String, val subtitle: String) {
    PVP("PVP", "Pass & Play", "Two players on one device"),
    PVC("PVC", "Minimax Engine", "Local deterministic tactician"),
    GEMINI("GEMINI", "Gemini AI", "Live neural grandmaster with commentary");

    companion object {
        fun fromId(id: String): GameMode = entries.firstOrNull { it.id == id } ?: PVC
    }
}

enum class AiDifficulty(
    val id: String,
    val title: String,
    val tag: String,
    val description: String
) {
    EASY("EASY", "Apprentice", "DEPTH 2", "Forgiving tactical play for relaxed matches"),
    MEDIUM("MEDIUM", "Tactician", "DEPTH 5", "Blocks immediate threats & punishes blunders"),
    HARD("HARD", "Grandmaster", "MINIMAX OPTIMAL", "Unbeatable full-tree Minimax search");

    companion object {
        fun fromId(id: String): AiDifficulty = entries.firstOrNull { it.id == id } ?: HARD
    }
}

enum class BoardRuleMode(val id: String, val title: String, val badge: String, val description: String) {
    CLASSIC(
        id = "CLASSIC",
        title = "Classic 3×3",
        badge = "STANDARD",
        description = "Traditional rules: 9 cells, first to align 3 wins or draw."
    ),
    INFINITY_THREE(
        id = "INFINITY_THREE",
        title = "Infinity 3-Mark",
        badge = "NO DRAWS",
        description = "Each player holds max 3 marks. Placing a 4th vanishes your oldest mark."
    );

    companion object {
        fun fromId(id: String): BoardRuleMode = entries.firstOrNull { it.id == id } ?: CLASSIC
    }
}

data class MoveRecord(
    val cellIndex: Int,
    val mark: Mark,
    val coordinate: String,
    val moveNumber: Int,
    val vanishedCellIndex: Int? = null
)

data class WinningLine(
    val indices: List<Int>,
    val winner: Mark
)

enum class GameStatus {
    IN_PROGRESS,
    WON_X,
    WON_O,
    DRAW
}

data class TacticalMoveEvaluation(
    val cellIndex: Int,
    val coordinate: String,
    val score: Int,
    val tacticalReason: String
)

object TicTacToeEngine {

    val WINNING_LINES: List<List<Int>> = listOf(
        listOf(0, 1, 2), // Row A
        listOf(3, 4, 5), // Row B
        listOf(6, 7, 8), // Row C
        listOf(0, 3, 6), // Col 1
        listOf(1, 4, 7), // Col 2
        listOf(2, 5, 8), // Col 3
        listOf(0, 4, 8), // Diagonal \
        listOf(2, 4, 6)  // Diagonal /
    )

    private val COORDINATES = arrayOf(
        "A1", "A2", "A3",
        "B1", "B2", "B3",
        "C1", "C2", "C3"
    )

    private val CORNERS = listOf(0, 2, 6, 8)
    private val EDGES = listOf(1, 3, 5, 7)
    const val CENTER = 4

    fun coordinateForIndex(index: Int): String =
        if (index in 0..8) COORDINATES[index] else "?"

    fun indexForCoordinate(coord: String): Int {
        val normalized = coord.trim().uppercase()
        return COORDINATES.indexOf(normalized)
    }

    fun isValidMove(board: List<Mark?>, index: Int): Boolean {
        return index in 0..8 && board.getOrNull(index) == null
    }

    fun getAvailableMoves(board: List<Mark?>): List<Int> {
        return board.indices.filter { board[it] == null }
    }

    fun checkWin(board: List<Mark?>): WinningLine? {
        for (line in WINNING_LINES) {
            val a = board[line[0]]
            val b = board[line[1]]
            val c = board[line[2]]
            if (a != null && a == b && b == c) {
                return WinningLine(indices = line, winner = a)
            }
        }
        return null
    }

    fun isBoardFull(board: List<Mark?>): Boolean = board.none { it == null }

    fun evaluateStatus(board: List<Mark?>): Pair<GameStatus, WinningLine?> {
        val win = checkWin(board)
        if (win != null) {
            val status = if (win.winner == Mark.X) GameStatus.WON_X else GameStatus.WON_O
            return status to win
        }
        return if (isBoardFull(board)) {
            GameStatus.DRAW to null
        } else {
            GameStatus.IN_PROGRESS to null
        }
    }

    /**
     * In INFINITY_THREE mode, if the given player already has 3 active marks on the board,
     * returns the cell index of their oldest active mark that will vanish on their next move.
     */
    fun getOldestActiveCellForMark(
        board: List<Mark?>,
        moveHistory: List<MoveRecord>,
        mark: Mark,
        ruleMode: BoardRuleMode
    ): Int? {
        if (ruleMode != BoardRuleMode.INFINITY_THREE) return null
        val activeCellsForMark = board.indices.filter { board[it] == mark }
        if (activeCellsForMark.size < 3) return null
        for (record in moveHistory) {
            if (record.mark == mark && board.getOrNull(record.cellIndex) == mark) {
                return record.cellIndex
            }
        }
        return activeCellsForMark.firstOrNull()
    }

    /**
     * Applies a move at [cellIndex] for [mark], respecting [ruleMode].
     * Returns the updated 9-cell board and the cell index that vanished (if any).
     */
    fun applyMove(
        currentBoard: List<Mark?>,
        moveHistory: List<MoveRecord>,
        cellIndex: Int,
        mark: Mark,
        ruleMode: BoardRuleMode
    ): Pair<List<Mark?>, Int?> {
        require(isValidMove(currentBoard, cellIndex)) {
            "Illegal move at index $cellIndex"
        }
        val vanishedIndex = getOldestActiveCellForMark(currentBoard, moveHistory, mark, ruleMode)
        val nextBoard = currentBoard.toMutableList()
        if (vanishedIndex != null) {
            nextBoard[vanishedIndex] = null
        }
        nextBoard[cellIndex] = mark
        return nextBoard.toList() to vanishedIndex
    }

    /**
     * Finds the optimal or difficulty-adjusted move for [aiMark] using Minimax with Alpha-Beta Pruning.
     */
    fun findBestMove(
        board: List<Mark?>,
        aiMark: Mark,
        difficulty: AiDifficulty,
        ruleMode: BoardRuleMode = BoardRuleMode.CLASSIC,
        moveHistory: List<MoveRecord> = emptyList(),
        random: Random = Random.Default
    ): TacticalMoveEvaluation {
        val available = getAvailableMoves(board)
        require(available.isNotEmpty()) { "No available moves on board" }

        val humanMark = aiMark.opponent()

        // 1. Check immediate winning move (always check in MEDIUM and HARD, 65% in EASY)
        val immediateWin = findImmediateWinningCell(board, moveHistory, aiMark, ruleMode)
        if (immediateWin != null && (difficulty != AiDifficulty.EASY || random.nextFloat() < 0.65f)) {
            val coord = coordinateForIndex(immediateWin)
            return TacticalMoveEvaluation(
                cellIndex = immediateWin,
                coordinate = coord,
                score = 100,
                tacticalReason = "Executing decisive winning alignment at $coord."
            )
        }

        // 2. Check immediate opponent block (always in MEDIUM and HARD, 45% in EASY)
        val immediateBlock = findImmediateWinningCell(board, moveHistory, humanMark, ruleMode)
        if (immediateBlock != null && (difficulty != AiDifficulty.EASY || random.nextFloat() < 0.45f)) {
            val coord = coordinateForIndex(immediateBlock)
            return TacticalMoveEvaluation(
                cellIndex = immediateBlock,
                coordinate = coord,
                score = 80,
                tacticalReason = "Neutralizing opponent's winning threat at $coord."
            )
        }

        // 3. Difficulty branching
        return when (difficulty) {
            AiDifficulty.EASY -> {
                val chosen = available.random(random)
                val coord = coordinateForIndex(chosen)
                TacticalMoveEvaluation(
                    cellIndex = chosen,
                    coordinate = coord,
                    score = 10,
                    tacticalReason = "Developing positional presence at $coord."
                )
            }

            AiDifficulty.MEDIUM -> {
                // 55% optimal minimax, 45% positional preference
                if (random.nextFloat() < 0.55f) {
                    computeMinimaxMove(board, aiMark, ruleMode, moveHistory)
                } else {
                    val chosen = when {
                        CENTER in available -> CENTER
                        available.any { it in CORNERS } -> available.filter { it in CORNERS }.random(random)
                        else -> available.random(random)
                    }
                    val coord = coordinateForIndex(chosen)
                    TacticalMoveEvaluation(
                        cellIndex = chosen,
                        coordinate = coord,
                        score = 50,
                        tacticalReason = if (chosen == CENTER) {
                            "Controlling central pivot B2."
                        } else {
                            "Contesting tactical lane at $coord."
                        }
                    )
                }
            }

            AiDifficulty.HARD -> {
                computeMinimaxMove(board, aiMark, ruleMode, moveHistory)
            }
        }
    }

    private fun findImmediateWinningCell(
        board: List<Mark?>,
        moveHistory: List<MoveRecord>,
        mark: Mark,
        ruleMode: BoardRuleMode
    ): Int? {
        for (cell in getAvailableMoves(board)) {
            val (nextBoard, _) = applyMove(board, moveHistory, cell, mark, ruleMode)
            if (checkWin(nextBoard)?.winner == mark) {
                return cell
            }
        }
        return null
    }

    private fun computeMinimaxMove(
        board: List<Mark?>,
        aiMark: Mark,
        ruleMode: BoardRuleMode,
        moveHistory: List<MoveRecord>
    ): TacticalMoveEvaluation {
        val available = getAvailableMoves(board)

        // Opening book optimization for empty or 1-move board
        val occupiedCount = board.count { it != null }
        if (occupiedCount == 0) {
            return TacticalMoveEvaluation(
                cellIndex = CENTER,
                coordinate = coordinateForIndex(CENTER),
                score = 0,
                tacticalReason = "Opening at central nexus B2 for maximum diagonal & orthogonal control."
            )
        }
        if (occupiedCount == 1 && board[CENTER] == null) {
            return TacticalMoveEvaluation(
                cellIndex = CENTER,
                coordinate = coordinateForIndex(CENTER),
                score = 0,
                tacticalReason = "Seizing undefended center B2 to neutralize corner opening."
            )
        }

        var bestScore = Int.MIN_VALUE
        var bestCell = available.first()

        // Order moves center -> corners -> edges so alpha-beta prunes faster and prefers strong squares on ties
        val orderedMoves = available.sortedBy { cell ->
            when (cell) {
                CENTER -> 0
                in CORNERS -> 1
                else -> 2
            }
        }

        val maxDepth = if (ruleMode == BoardRuleMode.INFINITY_THREE) 6 else 9

        for (cell in orderedMoves) {
            val (nextBoard, vanished) = applyMove(board, moveHistory, cell, aiMark, ruleMode)
            val nextHistory = moveHistory + MoveRecord(
                cellIndex = cell,
                mark = aiMark,
                coordinate = coordinateForIndex(cell),
                moveNumber = moveHistory.size + 1,
                vanishedCellIndex = vanished
            )
            val score = minimax(
                board = nextBoard,
                history = nextHistory,
                depth = 1,
                maxDepth = maxDepth,
                isMaximizing = false,
                aiMark = aiMark,
                ruleMode = ruleMode,
                alpha = Int.MIN_VALUE,
                beta = Int.MAX_VALUE
            )
            if (score > bestScore) {
                bestScore = score
                bestCell = cell
            }
        }

        val coord = coordinateForIndex(bestCell)
        val reason = when {
            bestScore >= 10 -> "Minimax forced win path locked via $coord."
            bestCell == CENTER -> "Commanding central nexus $coord to dictate tempo."
            bestCell in CORNERS -> "Anchoring strategic corner $coord to create dual fork pressure."
            else -> "Holding equilibrium along flank $coord."
        }

        return TacticalMoveEvaluation(
            cellIndex = bestCell,
            coordinate = coord,
            score = bestScore,
            tacticalReason = reason
        )
    }

    private fun minimax(
        board: List<Mark?>,
        history: List<MoveRecord>,
        depth: Int,
        maxDepth: Int,
        isMaximizing: Boolean,
        aiMark: Mark,
        ruleMode: BoardRuleMode,
        alpha: Int,
        beta: Int
    ): Int {
        val win = checkWin(board)
        if (win != null) {
            return if (win.winner == aiMark) (20 - depth) else (depth - 20)
        }
        val available = getAvailableMoves(board)
        if (available.isEmpty() || depth >= maxDepth) {
            return evaluateHeuristic(board, aiMark)
        }

        var currentAlpha = alpha
        var currentBeta = beta

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (cell in available) {
                val (nextBoard, vanished) = applyMove(board, history, cell, aiMark, ruleMode)
                val nextHistory = history + MoveRecord(
                    cellIndex = cell,
                    mark = aiMark,
                    coordinate = coordinateForIndex(cell),
                    moveNumber = history.size + 1,
                    vanishedCellIndex = vanished
                )
                val eval = minimax(
                    board = nextBoard,
                    history = nextHistory,
                    depth = depth + 1,
                    maxDepth = maxDepth,
                    isMaximizing = false,
                    aiMark = aiMark,
                    ruleMode = ruleMode,
                    alpha = currentAlpha,
                    beta = currentBeta
                )
                maxEval = max(maxEval, eval)
                currentAlpha = max(currentAlpha, eval)
                if (currentBeta <= currentAlpha) break
            }
            return maxEval
        } else {
            val humanMark = aiMark.opponent()
            var minEval = Int.MAX_VALUE
            for (cell in available) {
                val (nextBoard, vanished) = applyMove(board, history, cell, humanMark, ruleMode)
                val nextHistory = history + MoveRecord(
                    cellIndex = cell,
                    mark = humanMark,
                    coordinate = coordinateForIndex(cell),
                    moveNumber = history.size + 1,
                    vanishedCellIndex = vanished
                )
                val eval = minimax(
                    board = nextBoard,
                    history = nextHistory,
                    depth = depth + 1,
                    maxDepth = maxDepth,
                    isMaximizing = true,
                    aiMark = aiMark,
                    ruleMode = ruleMode,
                    alpha = currentAlpha,
                    beta = currentBeta
                )
                minEval = min(minEval, eval)
                currentBeta = min(currentBeta, eval)
                if (currentBeta <= currentAlpha) break
            }
            return minEval
        }
    }

    private fun evaluateHeuristic(board: List<Mark?>, aiMark: Mark): Int {
        val humanMark = aiMark.opponent()
        var score = 0
        for (line in WINNING_LINES) {
            val marks = line.map { board[it] }
            val aiCount = marks.count { it == aiMark }
            val humanCount = marks.count { it == humanMark }
            if (aiCount > 0 && humanCount == 0) {
                score += if (aiCount == 2) 3 else 1
            } else if (humanCount > 0 && aiCount == 0) {
                score -= if (humanCount == 2) 3 else 1
            }
        }
        return score
    }

    fun formatBoardForPrompt(board: List<Mark?>): String {
        val rows = listOf(
            "Row A: A1=${board[0]?.symbol ?: "."}, A2=${board[1]?.symbol ?: "."}, A3=${board[2]?.symbol ?: "."}",
            "Row B: B1=${board[3]?.symbol ?: "."}, B2=${board[4]?.symbol ?: "."}, B3=${board[5]?.symbol ?: "."}",
            "Row C: C1=${board[6]?.symbol ?: "."}, C2=${board[7]?.symbol ?: "."}, C3=${board[8]?.symbol ?: "."}"
        )
        return rows.joinToString("\n")
    }
}

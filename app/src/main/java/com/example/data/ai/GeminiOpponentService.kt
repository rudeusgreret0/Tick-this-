package com.example.data.ai

import android.util.Log
import com.example.domain.engine.AiDifficulty
import com.example.domain.engine.BoardRuleMode
import com.example.domain.engine.Mark
import com.example.domain.engine.MoveRecord
import com.example.domain.engine.TicTacToeEngine
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiOpponentService"

data class GeminiMoveDecision(
    val cellIndex: Int,
    val coordinate: String,
    val tacticalCommentary: String,
    val confidence: Int,
    val usedLocalFallback: Boolean,
    val errorNotice: String? = null
)

interface AiOpponentProvider {
    suspend fun requestMove(
        board: List<Mark?>,
        aiMark: Mark,
        difficulty: AiDifficulty,
        ruleMode: BoardRuleMode,
        moveHistory: List<MoveRecord>
    ): GeminiMoveDecision
}

class GeminiOpponentService : AiOpponentProvider {

    private val moveResponseSchema = Schema.obj(
        properties = mapOf(
            "cellIndex" to Schema.integer(
                description = "The 0-based board cell index (0 to 8) where AI places its mark"
            ),
            "coordinate" to Schema.string(
                description = "The grid coordinate corresponding to cellIndex (A1, A2, A3, B1, B2, B3, C1, C2, C3)"
            ),
            "tacticalCommentary" to Schema.string(
                description = "Concise, articulate 1-sentence tactical insight explaining this move"
            ),
            "confidence" to Schema.integer(
                description = "Confidence score from 1 to 100"
            )
        )
    )

    private val config = generationConfig {
        responseMimeType = "application/json"
        responseSchema = moveResponseSchema
    }

    // Lazily initialize Firebase AI Logic model per mandatory skill rules
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    override suspend fun requestMove(
        board: List<Mark?>,
        aiMark: Mark,
        difficulty: AiDifficulty,
        ruleMode: BoardRuleMode,
        moveHistory: List<MoveRecord>
    ): GeminiMoveDecision {
        val availableIndices = TicTacToeEngine.getAvailableMoves(board)
        require(availableIndices.isNotEmpty()) { "Board has no available moves" }

        val availableCoordinates = availableIndices.joinToString(", ") { idx ->
            "$idx (${TicTacToeEngine.coordinateForIndex(idx)})"
        }

        val oldestVanishIndex = TicTacToeEngine.getOldestActiveCellForMark(
            board = board,
            moveHistory = moveHistory,
            mark = aiMark,
            ruleMode = ruleMode
        )
        val vanishNote = if (oldestVanishIndex != null) {
            "IMPORTANT RULE (Infinity 3-Mark Mode): You already have 3 marks on the board. When you place this new mark, your oldest mark at index $oldestVanishIndex (${TicTacToeEngine.coordinateForIndex(oldestVanishIndex)}) will vanish immediately."
        } else {
            "Rule Mode: ${ruleMode.title}."
        }

        val personaInstruction = when (difficulty) {
            AiDifficulty.EASY ->
                "Play like a friendly, encouraging club apprentice. Occasionally leave tactical openings for the human player while making natural moves."
            AiDifficulty.MEDIUM ->
                "Play like a sharp tactical contender. Block immediate three-in-a-row threats and contest the center and corners."
            AiDifficulty.HARD ->
                "Play like an unbeatable grandmaster tactician. Always take immediate winning moves, block any opponent two-in-a-row threat, create double-attack forks, and command center B2 (index 4) or strategic corners."
        }

        val prompt = buildString {
            appendLine("You are the Gemini AI Tactician playing Tic Tac Toe as '${aiMark.symbol}' against Human ('${aiMark.opponent().symbol}').")
            appendLine(personaInstruction)
            appendLine(vanishNote)
            appendLine("Current 3x3 Board State ('.' means empty):")
            appendLine(TicTacToeEngine.formatBoardForPrompt(board))
            appendLine("Index mapping: 0=A1, 1=A2, 2=A3, 3=B1, 4=B2 (Center), 5=B3, 6=C1, 7=C2, 8=C3.")
            appendLine("LEGAL EMPTY CELLS (You MUST choose ONE of these indices): $availableCoordinates")
            appendLine("Select a valid cellIndex from [$availableCoordinates] and provide a brief, sophisticated 1-sentence tacticalCommentary.")
        }

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val jsonString = response.text
                ?: throw IllegalStateException("Gemini returned an empty response.")

            val json = JSONObject(jsonString)
            val rawIndex = json.optInt("cellIndex", -1)
            val rawCoord = json.optString("coordinate", "")
            val commentary = json.optString(
                "tacticalCommentary",
                "Calculated positional response on the grid."
            )
            val confidence = json.optInt("confidence", 88).coerceIn(1, 100)

            // Resolve index either directly or from coordinate if needed
            val resolvedIndex = when {
                TicTacToeEngine.isValidMove(board, rawIndex) -> rawIndex
                TicTacToeEngine.indexForCoordinate(rawCoord)
                    .let { it in 0..8 && TicTacToeEngine.isValidMove(board, it) } ->
                    TicTacToeEngine.indexForCoordinate(rawCoord)
                else -> -1
            }

            if (resolvedIndex != -1) {
                GeminiMoveDecision(
                    cellIndex = resolvedIndex,
                    coordinate = TicTacToeEngine.coordinateForIndex(resolvedIndex),
                    tacticalCommentary = commentary,
                    confidence = confidence,
                    usedLocalFallback = false,
                    errorNotice = null
                )
            } else {
                Log.e(TAG, "Gemini proposed illegal move index=$rawIndex coord=$rawCoord; engaging Minimax fallback.")
                buildFallbackDecision(
                    board = board,
                    aiMark = aiMark,
                    difficulty = difficulty,
                    ruleMode = ruleMode,
                    moveHistory = moveHistory,
                    errorNotice = "Gemini proposed occupied square ($rawCoord); validated via local Minimax engine."
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini AI move timed out after 60s", e)
            buildFallbackDecision(
                board = board,
                aiMark = aiMark,
                difficulty = difficulty,
                ruleMode = ruleMode,
                moveHistory = moveHistory,
                errorNotice = "Gemini request timed out after 60s. Local Minimax engine executed fallback move."
            )
        } catch (e: CancellationException) {
            // MANDATORY: Always re-throw CancellationException to preserve coroutine cancellation
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            val reason = e.localizedMessage ?: "Gemini connection unavailable."
            buildFallbackDecision(
                board = board,
                aiMark = aiMark,
                difficulty = difficulty,
                ruleMode = ruleMode,
                moveHistory = moveHistory,
                errorNotice = "Gemini API notice: $reason — Local Minimax fallback active."
            )
        }
    }

    private fun buildFallbackDecision(
        board: List<Mark?>,
        aiMark: Mark,
        difficulty: AiDifficulty,
        ruleMode: BoardRuleMode,
        moveHistory: List<MoveRecord>,
        errorNotice: String
    ): GeminiMoveDecision {
        val eval = TicTacToeEngine.findBestMove(
            board = board,
            aiMark = aiMark,
            difficulty = difficulty,
            ruleMode = ruleMode,
            moveHistory = moveHistory
        )
        return GeminiMoveDecision(
            cellIndex = eval.cellIndex,
            coordinate = eval.coordinate,
            tacticalCommentary = "${eval.tacticalReason} (Local Engine)",
            confidence = 96,
            usedLocalFallback = true,
            errorNotice = errorNotice
        )
    }
}

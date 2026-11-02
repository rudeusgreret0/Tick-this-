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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "GeminiOpponentService"

data class ApiKeyTestOutcome(
    val status: ApiKeyHealthStatus,
    val userMessage: String,
    val technicalDetail: String? = null
)

data class GeminiMoveDecision(
    val cellIndex: Int,
    val coordinate: String,
    val tacticalCommentary: String,
    val confidence: Int,
    val usedLocalFallback: Boolean,
    val providerLabel: String = "Gemini AI",
    val compactNotice: String? = null,
    val technicalDiagnostics: String? = null,
    val canSwitchKey: Boolean = false
)

interface AiOpponentProvider {
    suspend fun requestMove(
        board: List<Mark?>,
        aiMark: Mark,
        difficulty: AiDifficulty,
        ruleMode: BoardRuleMode,
        moveHistory: List<MoveRecord>
    ): GeminiMoveDecision

    suspend fun testApiKey(rawKey: String): ApiKeyTestOutcome {
        return ApiKeyTestOutcome(
            status = ApiKeyHealthStatus.VALID,
            userMessage = "API key verified."
        )
    }
}

class GeminiOpponentService(
    private val keyManagerRepository: GeminiKeyManagerRepository? = null
) : AiOpponentProvider {

    private val moveResponseSchema = Schema.obj(
        properties = mapOf(
            "cellIndex" to Schema.integer(
                description = "The 0-based board cell index (0 to 8) where AI places its mark"
            ),
            "coordinate" to Schema.string(
                description = "The grid coordinate corresponding to cellIndex (A1..C3)"
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
    private val firebaseModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    override suspend fun testApiKey(rawKey: String): ApiKeyTestOutcome = withContext(Dispatchers.IO) {
        val cleanKey = rawKey.trim()
        if (cleanKey.length < 10) {
            return@withContext ApiKeyTestOutcome(
                status = ApiKeyHealthStatus.INVALID,
                userMessage = "Key format is too short. Paste a valid Gemini API key.",
                technicalDetail = "Validation failed: key length < 10 characters."
            )
        }

        val candidateModels = listOf("gemini-2.5-flash", "gemini-2.0-flash")
        var lastErrorOutcome: ApiKeyTestOutcome? = null

        for (modelName in candidateModels) {
            try {
                val (httpCode, responseBody) = executeDirectRestCall(
                    apiKey = cleanKey,
                    modelName = modelName,
                    prompt = "Reply with JSON: {\"status\":\"ok\"}",
                    timeoutMs = 12_000
                )
                if (httpCode in 200..299) {
                    return@withContext ApiKeyTestOutcome(
                        status = ApiKeyHealthStatus.VALID,
                        userMessage = "Connected! Verified with $modelName.",
                        technicalDetail = "HTTP $httpCode OK ($modelName)"
                    )
                } else {
                    val classified = classifyHttpError(httpCode, responseBody)
                    lastErrorOutcome = classified
                    // If key is invalid or quota exceeded, no need to try other model names
                    if (classified.status == ApiKeyHealthStatus.INVALID ||
                        classified.status == ApiKeyHealthStatus.QUOTA_EXCEEDED
                    ) {
                        return@withContext classified
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "API key connection test failed on model $modelName", e)
                lastErrorOutcome = ApiKeyTestOutcome(
                    status = ApiKeyHealthStatus.UNAVAILABLE,
                    userMessage = "Connection failed. Check your network and try again.",
                    technicalDetail = "${e.javaClass.simpleName}: ${e.localizedMessage ?: "Network error"}"
                )
            }
        }

        lastErrorOutcome ?: ApiKeyTestOutcome(
            status = ApiKeyHealthStatus.UNAVAILABLE,
            userMessage = "Could not verify key at this time."
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

        val prompt = buildTacticalPrompt(board, aiMark, difficulty, ruleMode, moveHistory, availableIndices)

        // Check if the user has selected an active personal API key in the Gemini API Manager
        val activePersonalKeyPair = keyManagerRepository?.getDecryptedActiveKey()
        if (activePersonalKeyPair != null) {
            val (savedKeyMeta, decryptedKey) = activePersonalKeyPair
            return requestMoveWithPersonalKey(
                savedKeyMeta = savedKeyMeta,
                decryptedKey = decryptedKey,
                prompt = prompt,
                board = board,
                aiMark = aiMark,
                difficulty = difficulty,
                ruleMode = ruleMode,
                moveHistory = moveHistory
            )
        }

        // Otherwise use Server-Side Firebase AI Logic proxy
        return requestMoveWithFirebaseAiLogic(
            prompt = prompt,
            board = board,
            aiMark = aiMark,
            difficulty = difficulty,
            ruleMode = ruleMode,
            moveHistory = moveHistory
        )
    }

    private suspend fun requestMoveWithPersonalKey(
        savedKeyMeta: SavedGeminiApiKey,
        decryptedKey: String,
        prompt: String,
        board: List<Mark?>,
        aiMark: Mark,
        difficulty: AiDifficulty,
        ruleMode: BoardRuleMode,
        moveHistory: List<MoveRecord>
    ): GeminiMoveDecision = withContext(Dispatchers.IO) {
        val candidateModels = listOf("gemini-2.5-flash", "gemini-2.0-flash")
        var lastError: ApiKeyTestOutcome? = null

        for (modelName in candidateModels) {
            try {
                val (httpCode, body) = executeDirectRestCall(
                    apiKey = decryptedKey,
                    modelName = modelName,
                    prompt = prompt,
                    timeoutMs = 15_000
                )
                if (httpCode in 200..299) {
                    val extractedJson = extractCandidateTextFromRestResponse(body)
                    if (!extractedJson.isNullOrBlank()) {
                        val parsed = parseAndValidateMoveJson(
                            jsonString = extractedJson,
                            board = board,
                            providerLabel = "Gemini (${savedKeyMeta.label})"
                        )
                        if (parsed != null) {
                            keyManagerRepository?.updateKeyStatus(
                                keyId = savedKeyMeta.id,
                                status = ApiKeyHealthStatus.VALID,
                                statusMessage = "Active & responding ($modelName)"
                            )
                            return@withContext parsed
                        }
                    }
                } else {
                    val classified = classifyHttpError(httpCode, body)
                    lastError = classified
                    keyManagerRepository?.updateKeyStatus(
                        keyId = savedKeyMeta.id,
                        status = classified.status,
                        statusMessage = classified.userMessage
                    )
                    if (classified.status == ApiKeyHealthStatus.INVALID ||
                        classified.status == ApiKeyHealthStatus.QUOTA_EXCEEDED
                    ) {
                        break
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Personal key move request failed ($modelName)", e)
                lastError = ApiKeyTestOutcome(
                    status = ApiKeyHealthStatus.UNAVAILABLE,
                    userMessage = "Network interrupted • Local Master AI stepped in.",
                    technicalDetail = "${e.javaClass.simpleName}: ${e.localizedMessage}"
                )
            }
        }

        val outcome = lastError ?: ApiKeyTestOutcome(
            status = ApiKeyHealthStatus.UNAVAILABLE,
            userMessage = "Gemini unavailable • Local Master AI stepped in."
        )
        buildFallbackDecision(
            board = board,
            aiMark = aiMark,
            difficulty = difficulty,
            ruleMode = ruleMode,
            moveHistory = moveHistory,
            compactNotice = "${savedKeyMeta.label}: ${outcome.userMessage}",
            technicalDiagnostics = outcome.technicalDetail,
            canSwitchKey = true
        )
    }

    private suspend fun requestMoveWithFirebaseAiLogic(
        prompt: String,
        board: List<Mark?>,
        aiMark: Mark,
        difficulty: AiDifficulty,
        ruleMode: BoardRuleMode,
        moveHistory: List<MoveRecord>
    ): GeminiMoveDecision {
        return try {
            val response = withTimeout(60_000L) {
                firebaseModel.generateContent(prompt)
            }
            val jsonString = response.text
                ?: throw IllegalStateException("Empty response from Gemini model.")

            val parsed = parseAndValidateMoveJson(
                jsonString = jsonString,
                board = board,
                providerLabel = "Gemini AI"
            )
            if (parsed != null) {
                parsed
            } else {
                buildFallbackDecision(
                    board = board,
                    aiMark = aiMark,
                    difficulty = difficulty,
                    ruleMode = ruleMode,
                    moveHistory = moveHistory,
                    compactNotice = "Move validated by Local Master AI.",
                    technicalDiagnostics = "Model proposed an occupied cell; local Minimax resolved legal move.",
                    canSwitchKey = false
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
                compactNotice = "Gemini timed out • Local Master AI played.",
                technicalDiagnostics = "TimeoutCancellationException after 60,000ms.",
                canSwitchKey = true
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            val rawMsg = e.localizedMessage ?: "Connection unavailable"
            val isQuota = rawMsg.contains("quota", ignoreCase = true) ||
                rawMsg.contains("resource_exhausted", ignoreCase = true) ||
                rawMsg.contains("429")
            val compact = if (isQuota) {
                "Free tier quota reached • Local Master AI active."
            } else {
                "Gemini offline • Local Master AI active."
            }
            buildFallbackDecision(
                board = board,
                aiMark = aiMark,
                difficulty = difficulty,
                ruleMode = ruleMode,
                moveHistory = moveHistory,
                compactNotice = compact,
                technicalDiagnostics = rawMsg,
                canSwitchKey = true
            )
        }
    }

    private fun parseAndValidateMoveJson(
        jsonString: String,
        board: List<Mark?>,
        providerLabel: String
    ): GeminiMoveDecision? {
        return try {
            val cleanJson = jsonString.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val json = JSONObject(cleanJson)
            val rawIndex = json.optInt("cellIndex", -1)
            val rawCoord = json.optString("coordinate", "")
            val commentary = json.optString(
                "tacticalCommentary",
                "Balanced positional play on the grid."
            )
            val confidence = json.optInt("confidence", 90).coerceIn(1, 100)

            val resolvedIndex = when {
                TicTacToeEngine.isValidMove(board, rawIndex) -> rawIndex
                TicTacToeEngine.indexForCoordinate(rawCoord)
                    .let { it in 0..8 && TicTacToeEngine.isValidMove(board, it) } ->
                    TicTacToeEngine.indexForCoordinate(rawCoord)
                else -> -1
            }

            if (resolvedIndex != -1) {
                val coord = TicTacToeEngine.coordinateForIndex(resolvedIndex)
                GeminiMoveDecision(
                    cellIndex = resolvedIndex,
                    coordinate = coord,
                    tacticalCommentary = commentary,
                    confidence = confidence,
                    usedLocalFallback = false,
                    providerLabel = providerLabel,
                    compactNotice = null,
                    technicalDiagnostics = null,
                    canSwitchKey = false
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun executeDirectRestCall(
        apiKey: String,
        modelName: String,
        prompt: String,
        timeoutMs: Int
    ): Pair<Int, String> {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-goog-api-key", apiKey)
        }

        val payload = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.3)
                }
            )
        }

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val responseBody = if (stream != null) {
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        } else {
            ""
        }
        return code to responseBody
    }

    private fun extractCandidateTextFromRestResponse(rawBody: String): String? {
        return try {
            val root = JSONObject(rawBody)
            val candidates = root.optJSONArray("candidates") ?: return null
            val first = candidates.optJSONObject(0) ?: return null
            val content = first.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            parts.optJSONObject(0)?.optString("text")
        } catch (_: Exception) {
            null
        }
    }

    private fun classifyHttpError(httpCode: Int, responseBody: String): ApiKeyTestOutcome {
        val lower = responseBody.lowercase()
        return when {
            httpCode == 429 || lower.contains("resource_exhausted") || lower.contains("quota") -> {
                ApiKeyTestOutcome(
                    status = ApiKeyHealthStatus.QUOTA_EXCEEDED,
                    userMessage = "Quota limit reached (HTTP 429). Switch to another key or use Local AI.",
                    technicalDetail = "HTTP $httpCode: Quota / rate limit exceeded."
                )
            }
            httpCode == 400 || httpCode == 401 || httpCode == 403 || lower.contains("api_key_invalid") -> {
                ApiKeyTestOutcome(
                    status = ApiKeyHealthStatus.INVALID,
                    userMessage = "Invalid or unauthorized API key (HTTP $httpCode).",
                    technicalDetail = "HTTP $httpCode: Key rejected by Google Generative Language API."
                )
            }
            else -> {
                ApiKeyTestOutcome(
                    status = ApiKeyHealthStatus.UNAVAILABLE,
                    userMessage = "Gemini service unavailable (HTTP $httpCode).",
                    technicalDetail = "HTTP $httpCode response from server."
                )
            }
        }
    }

    private fun buildTacticalPrompt(
        board: List<Mark?>,
        aiMark: Mark,
        difficulty: AiDifficulty,
        ruleMode: BoardRuleMode,
        moveHistory: List<MoveRecord>,
        availableIndices: List<Int>
    ): String {
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
            "Infinity 3-Mark Rule: Your oldest mark at index $oldestVanishIndex (${TicTacToeEngine.coordinateForIndex(oldestVanishIndex)}) will vanish when you place this move."
        } else {
            "Rule Mode: ${ruleMode.title}."
        }
        return buildString {
            appendLine("You are playing Tic Tac Toe as '${aiMark.symbol}' against '${aiMark.opponent().symbol}'. Difficulty: ${difficulty.title}.")
            appendLine(vanishNote)
            appendLine("Board ('.' = empty):")
            appendLine(TicTacToeEngine.formatBoardForPrompt(board))
            appendLine("LEGAL EMPTY CELLS (Choose ONE index): $availableCoordinates")
            appendLine("Return JSON with keys: cellIndex (integer), coordinate (string), tacticalCommentary (short 1-sentence reason), confidence (integer 1..100).")
        }
    }

    private fun buildFallbackDecision(
        board: List<Mark?>,
        aiMark: Mark,
        difficulty: AiDifficulty,
        ruleMode: BoardRuleMode,
        moveHistory: List<MoveRecord>,
        compactNotice: String,
        technicalDiagnostics: String?,
        canSwitchKey: Boolean
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
            tacticalCommentary = eval.tacticalReason,
            confidence = 96,
            usedLocalFallback = true,
            providerLabel = "Local Master AI",
            compactNotice = compactNotice,
            technicalDiagnostics = technicalDiagnostics,
            canSwitchKey = canSwitchKey
        )
    }
}

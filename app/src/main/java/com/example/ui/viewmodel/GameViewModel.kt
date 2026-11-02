package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundAndHapticsManager
import com.example.data.ai.AiOpponentProvider
import com.example.data.ai.GeminiOpponentService
import com.example.data.local.MatchRecordEntity
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.MatchRepository
import com.example.domain.engine.AiDifficulty
import com.example.domain.engine.BoardRuleMode
import com.example.domain.engine.GameMode
import com.example.domain.engine.GameStatus
import com.example.domain.engine.Mark
import com.example.domain.engine.MoveRecord
import com.example.domain.engine.TacticalMoveEvaluation
import com.example.domain.engine.TicTacToeEngine
import com.example.domain.engine.WinningLine
import com.example.ui.theme.StudioPalette
import com.example.ui.theme.ThemeModeOverride
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class UndoSnapshot(
    val board: List<Mark?>,
    val currentTurn: Mark,
    val moveHistory: List<MoveRecord>,
    val aiCommentary: String?,
    val aiConfidence: Int?
)

data class StatisticsSummary(
    val totalMatches: Int = 0,
    val xWins: Int = 0,
    val oWins: Int = 0,
    val draws: Int = 0,
    val playerOneWins: Int = 0,
    val winRatePercent: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val fastestWinSeconds: Int? = null,
    val averageMoves: Float = 0f,
    val geminiMatches: Int = 0,
    val minimaxMatches: Int = 0,
    val pvpMatches: Int = 0
)

data class GameSessionUiState(
    val board: List<Mark?> = List(9) { null },
    val currentTurn: Mark = Mark.X,
    val startingMark: Mark = Mark.X,
    val humanMark: Mark = Mark.X,
    val gameMode: GameMode = GameMode.GEMINI,
    val difficulty: AiDifficulty = AiDifficulty.HARD,
    val ruleMode: BoardRuleMode = BoardRuleMode.CLASSIC,
    val playerXName: String = "Player 1",
    val playerOName: String = "Gemini AI",
    val status: GameStatus = GameStatus.IN_PROGRESS,
    val winningLine: WinningLine? = null,
    val moveHistory: List<MoveRecord> = emptyList(),
    val undoStack: List<UndoSnapshot> = emptyList(),
    val hintEvaluation: TacticalMoveEvaluation? = null,
    val nextVanishCellIndex: Int? = null,
    val isAiThinking: Boolean = false,
    val aiCommentary: String = "Awaiting opening move on the tactical grid.",
    val aiConfidence: Int? = null,
    val aiErrorBanner: String? = null,
    val usedFallbackInMatch: Boolean = false,
    val sessionScoreX: Int = 0,
    val sessionScoreO: Int = 0,
    val sessionDraws: Int = 0,
    val sessionStreak: Int = 0,
    val elapsedSeconds: Int = 0,
    val inspectedMatch: MatchRecordEntity? = null,
    val replayMoveStep: Int = 0
) {
    val canUndo: Boolean
        get() = undoStack.isNotEmpty() && !isAiThinking && status == GameStatus.IN_PROGRESS

    val isHumanTurn: Boolean
        get() = when (gameMode) {
            GameMode.PVP -> true
            GameMode.PVC, GameMode.GEMINI -> currentTurn == humanMark
        }
}

class GameViewModel(
    private val matchRepository: MatchRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val soundManager: SoundAndHapticsManager,
    private val aiOpponentProvider: AiOpponentProvider = GeminiOpponentService(),
    private val enableMatchTimer: Boolean = true,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    private val activeScope: kotlinx.coroutines.CoroutineScope = coroutineScope ?: viewModelScope

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = activeScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val matchHistory: StateFlow<List<MatchRecordEntity>> = matchRepository.allMatches
        .stateIn(
            scope = activeScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val statistics: StateFlow<StatisticsSummary> = matchHistory.map { matches ->
        computeStatistics(matches)
    }.stateIn(
        scope = activeScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsSummary()
    )

    private val _session = MutableStateFlow(GameSessionUiState())
    val session: StateFlow<GameSessionUiState> = _session.asStateFlow()

    private var aiJob: Job? = null
    private var timerJob: Job? = null
    private var prefsInitialized = false

    init {
        activeScope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                soundManager.soundEnabled = prefs.soundEnabled
                soundManager.soundVolume = prefs.soundVolume
                soundManager.musicEnabled = prefs.musicEnabled
                soundManager.hapticsEnabled = prefs.hapticsEnabled

                if (!prefsInitialized) {
                    prefsInitialized = true
                    configureAndStartMatch(
                        gameMode = prefs.selectedGameMode,
                        difficulty = prefs.selectedDifficulty,
                        ruleMode = prefs.selectedRuleMode,
                        humanMark = prefs.preferredMark,
                        xGoesFirst = prefs.xGoesFirst,
                        playerOneName = prefs.playerOneName,
                        playerTwoName = prefs.playerTwoName,
                        resetSessionScores = true
                    )
                } else {
                    // Keep display names in sync when updated in settings
                    _session.update { state ->
                        val (xName, oName) = resolvePlayerNames(
                            mode = state.gameMode,
                            difficulty = state.difficulty,
                            humanMark = state.humanMark,
                            p1 = prefs.playerOneName,
                            p2 = prefs.playerTwoName
                        )
                        state.copy(playerXName = xName, playerOName = oName)
                    }
                }
            }
        }
    }

    fun configureAndStartMatch(
        gameMode: GameMode = _session.value.gameMode,
        difficulty: AiDifficulty = _session.value.difficulty,
        ruleMode: BoardRuleMode = _session.value.ruleMode,
        humanMark: Mark = _session.value.humanMark,
        xGoesFirst: Boolean = preferences.value.xGoesFirst,
        playerOneName: String = preferences.value.playerOneName,
        playerTwoName: String = preferences.value.playerTwoName,
        resetSessionScores: Boolean = false
    ) {
        aiJob?.cancel()
        val firstTurn = if (xGoesFirst) Mark.X else Mark.O
        val (xName, oName) = resolvePlayerNames(
            mode = gameMode,
            difficulty = difficulty,
            humanMark = humanMark,
            p1 = playerOneName,
            p2 = playerTwoName
        )

        val defaultCommentary = when (gameMode) {
            GameMode.PVP -> "Pass & Play duel ready. ${if (firstTurn == Mark.X) xName else oName} opens."
            GameMode.PVC -> "Minimax Engine (${difficulty.title}) ready. Select any square."
            GameMode.GEMINI -> "Gemini AI Tactician (${difficulty.title}) connected via Firebase AI Logic."
        }

        _session.update { prev ->
            GameSessionUiState(
                board = List(9) { null },
                currentTurn = firstTurn,
                startingMark = firstTurn,
                humanMark = humanMark,
                gameMode = gameMode,
                difficulty = difficulty,
                ruleMode = ruleMode,
                playerXName = xName,
                playerOName = oName,
                status = GameStatus.IN_PROGRESS,
                winningLine = null,
                moveHistory = emptyList(),
                undoStack = emptyList(),
                hintEvaluation = null,
                nextVanishCellIndex = null,
                isAiThinking = false,
                aiCommentary = defaultCommentary,
                aiConfidence = null,
                aiErrorBanner = null,
                usedFallbackInMatch = false,
                sessionScoreX = if (resetSessionScores) 0 else prev.sessionScoreX,
                sessionScoreO = if (resetSessionScores) 0 else prev.sessionScoreO,
                sessionDraws = if (resetSessionScores) 0 else prev.sessionDraws,
                sessionStreak = if (resetSessionScores) 0 else prev.sessionStreak,
                elapsedSeconds = 0,
                inspectedMatch = prev.inspectedMatch,
                replayMoveStep = prev.replayMoveStep
            )
        }

        activeScope.launch {
            preferencesRepository.updateGameMode(gameMode)
            preferencesRepository.updateDifficulty(difficulty)
            preferencesRepository.updateRuleMode(ruleMode)
            preferencesRepository.updatePreferredMark(humanMark)
            preferencesRepository.updateXGoesFirst(xGoesFirst)
        }

        startMatchTimer()
        triggerAiMoveIfNeeded()
    }

    fun onCellTapped(cellIndex: Int) {
        val state = _session.value
        if (state.status != GameStatus.IN_PROGRESS || state.isAiThinking) return
        if (!state.isHumanTurn) return
        if (!TicTacToeEngine.isValidMove(state.board, cellIndex)) return

        executeMove(cellIndex = cellIndex, isAiMove = false)
    }

    private fun executeMove(
        cellIndex: Int,
        isAiMove: Boolean,
        aiComment: String? = null,
        aiConfidenceScore: Int? = null,
        usedFallback: Boolean = false,
        errorBanner: String? = null
    ) {
        val current = _session.value
        if (!TicTacToeEngine.isValidMove(current.board, cellIndex)) return

        val activeMark = current.currentTurn
        if (activeMark == Mark.X) {
            soundManager.playMarkX()
        } else {
            soundManager.playMarkO()
        }

        val snapshot = UndoSnapshot(
            board = current.board,
            currentTurn = current.currentTurn,
            moveHistory = current.moveHistory,
            aiCommentary = current.aiCommentary,
            aiConfidence = current.aiConfidence
        )

        val (updatedBoard, vanishedIndex) = TicTacToeEngine.applyMove(
            currentBoard = current.board,
            moveHistory = current.moveHistory,
            cellIndex = cellIndex,
            mark = activeMark,
            ruleMode = current.ruleMode
        )

        val newMoveRecord = MoveRecord(
            cellIndex = cellIndex,
            mark = activeMark,
            coordinate = TicTacToeEngine.coordinateForIndex(cellIndex),
            moveNumber = current.moveHistory.size + 1,
            vanishedCellIndex = vanishedIndex
        )
        val updatedHistory = current.moveHistory + newMoveRecord
        val (newStatus, winningLine) = TicTacToeEngine.evaluateStatus(updatedBoard)
        val nextTurn = activeMark.opponent()

        val nextVanish = if (newStatus == GameStatus.IN_PROGRESS) {
            TicTacToeEngine.getOldestActiveCellForMark(
                board = updatedBoard,
                moveHistory = updatedHistory,
                mark = nextTurn,
                ruleMode = current.ruleMode
            )
        } else null

        val updatedScoreX = if (newStatus == GameStatus.WON_X) current.sessionScoreX + 1 else current.sessionScoreX
        val updatedScoreO = if (newStatus == GameStatus.WON_O) current.sessionScoreO + 1 else current.sessionScoreO
        val updatedDraws = if (newStatus == GameStatus.DRAW) current.sessionDraws + 1 else current.sessionDraws

        val humanWon = (newStatus == GameStatus.WON_X && current.humanMark == Mark.X) ||
            (newStatus == GameStatus.WON_O && current.humanMark == Mark.O) ||
            (current.gameMode == GameMode.PVP && (newStatus == GameStatus.WON_X || newStatus == GameStatus.WON_O))

        val updatedStreak = when {
            newStatus == GameStatus.IN_PROGRESS -> current.sessionStreak
            humanWon -> current.sessionStreak + 1
            else -> 0
        }

        val moveSummary = aiComment ?: buildHumanMoveCommentary(
            mark = activeMark,
            coord = newMoveRecord.coordinate,
            vanishedIndex = vanishedIndex,
            newStatus = newStatus
        )

        _session.update {
            it.copy(
                board = updatedBoard,
                currentTurn = nextTurn,
                status = newStatus,
                winningLine = winningLine,
                moveHistory = updatedHistory,
                undoStack = it.undoStack + snapshot,
                hintEvaluation = null,
                nextVanishCellIndex = nextVanish,
                isAiThinking = false,
                aiCommentary = moveSummary,
                aiConfidence = aiConfidenceScore ?: it.aiConfidence,
                aiErrorBanner = errorBanner ?: if (!isAiMove) it.aiErrorBanner else null,
                usedFallbackInMatch = it.usedFallbackInMatch || usedFallback,
                sessionScoreX = updatedScoreX,
                sessionScoreO = updatedScoreO,
                sessionDraws = updatedDraws,
                sessionStreak = updatedStreak
            )
        }

        if (newStatus != GameStatus.IN_PROGRESS) {
            timerJob?.cancel()
            playEndOfMatchAudio(newStatus, current.gameMode, current.humanMark)
            persistCompletedMatch(
                status = newStatus,
                winningLine = winningLine,
                history = updatedHistory,
                durationSeconds = _session.value.elapsedSeconds.coerceAtLeast(1),
                usedFallback = _session.value.usedFallbackInMatch
            )
        } else {
            triggerAiMoveIfNeeded()
        }
    }

    private fun triggerAiMoveIfNeeded() {
        val state = _session.value
        if (state.status != GameStatus.IN_PROGRESS) return
        if (state.gameMode == GameMode.PVP) return
        if (state.currentTurn == state.humanMark) return

        aiJob?.cancel()
        aiJob = activeScope.launch {
            _session.update {
                it.copy(
                    isAiThinking = true,
                    aiCommentary = if (state.gameMode == GameMode.GEMINI) {
                        "Gemini AI Tactician is analyzing grid vectors..."
                    } else {
                        "Computing Minimax search tree (${state.difficulty.tag})..."
                    }
                )
            }

            if (state.gameMode == GameMode.PVC) {
                // Brief tactile pacing so the human eye perceives the move before response
                delay(320L)
                val current = _session.value
                if (current.status != GameStatus.IN_PROGRESS) return@launch
                val eval = TicTacToeEngine.findBestMove(
                    board = current.board,
                    aiMark = current.currentTurn,
                    difficulty = current.difficulty,
                    ruleMode = current.ruleMode,
                    moveHistory = current.moveHistory
                )
                executeMove(
                    cellIndex = eval.cellIndex,
                    isAiMove = true,
                    aiComment = eval.tacticalReason,
                    aiConfidenceScore = when (current.difficulty) {
                        AiDifficulty.EASY -> 68
                        AiDifficulty.MEDIUM -> 85
                        AiDifficulty.HARD -> 99
                    }
                )
            } else if (state.gameMode == GameMode.GEMINI) {
                val current = _session.value
                val decision = aiOpponentProvider.requestMove(
                    board = current.board,
                    aiMark = current.currentTurn,
                    difficulty = current.difficulty,
                    ruleMode = current.ruleMode,
                    moveHistory = current.moveHistory
                )
                executeMove(
                    cellIndex = decision.cellIndex,
                    isAiMove = true,
                    aiComment = decision.tacticalCommentary,
                    aiConfidenceScore = decision.confidence,
                    usedFallback = decision.usedLocalFallback,
                    errorBanner = decision.errorNotice
                )
            }
        }
    }

    fun undoLastTurn() {
        val state = _session.value
        if (!state.canUndo) return
        soundManager.playUndoOrHint()
        aiJob?.cancel()

        // In PvC / Gemini mode, if at least 2 snapshots exist, pop back to the human's previous turn
        val stepsToPop = if (state.gameMode != GameMode.PVP && state.undoStack.size >= 2) 2 else 1
        val targetIndex = (state.undoStack.size - stepsToPop).coerceAtLeast(0)
        val restored = state.undoStack[targetIndex]
        val remainingStack = state.undoStack.take(targetIndex)

        val nextVanish = TicTacToeEngine.getOldestActiveCellForMark(
            board = restored.board,
            moveHistory = restored.moveHistory,
            mark = restored.currentTurn,
            ruleMode = state.ruleMode
        )

        _session.update {
            it.copy(
                board = restored.board,
                currentTurn = restored.currentTurn,
                status = GameStatus.IN_PROGRESS,
                winningLine = null,
                moveHistory = restored.moveHistory,
                undoStack = remainingStack,
                hintEvaluation = null,
                nextVanishCellIndex = nextVanish,
                isAiThinking = false,
                aiCommentary = "Rewound $stepsToPop move(s). ${restored.currentTurn.symbol} to play."
            )
        }
    }

    fun requestTacticalHint() {
        val state = _session.value
        if (state.status != GameStatus.IN_PROGRESS || state.isAiThinking) return
        soundManager.playUndoOrHint()
        val evaluation = TicTacToeEngine.findBestMove(
            board = state.board,
            aiMark = state.currentTurn,
            difficulty = AiDifficulty.HARD,
            ruleMode = state.ruleMode,
            moveHistory = state.moveHistory
        )
        _session.update {
            it.copy(
                hintEvaluation = evaluation,
                aiCommentary = "Advisor Hint (${evaluation.coordinate}): ${evaluation.tacticalReason}"
            )
        }
    }

    fun restartMatch() {
        soundManager.playButtonTap()
        val current = _session.value
        configureAndStartMatch(
            gameMode = current.gameMode,
            difficulty = current.difficulty,
            ruleMode = current.ruleMode,
            humanMark = current.humanMark,
            xGoesFirst = current.startingMark == Mark.X
        )
    }

    fun rematchWithAlternatingStarter() {
        soundManager.playButtonTap()
        val current = _session.value
        val nextStarterIsX = current.startingMark != Mark.X
        configureAndStartMatch(
            gameMode = current.gameMode,
            difficulty = current.difficulty,
            ruleMode = current.ruleMode,
            humanMark = current.humanMark,
            xGoesFirst = nextStarterIsX
        )
    }

    fun swapMarksAndRestart() {
        soundManager.playButtonTap()
        val current = _session.value
        val nextHumanMark = current.humanMark.opponent()
        configureAndStartMatch(
            gameMode = current.gameMode,
            difficulty = current.difficulty,
            ruleMode = current.ruleMode,
            humanMark = nextHumanMark,
            xGoesFirst = true
        )
    }

    fun dismissAiErrorBanner() {
        _session.update { it.copy(aiErrorBanner = null) }
    }

    fun inspectMatchReplay(match: MatchRecordEntity?) {
        soundManager.playButtonTap()
        val totalMoves = match?.moveSequenceNotation
            ?.split(";")
            ?.count { it.isNotBlank() } ?: 0
        _session.update {
            it.copy(
                inspectedMatch = match,
                replayMoveStep = totalMoves
            )
        }
    }

    fun setReplayMoveStep(step: Int) {
        val match = _session.value.inspectedMatch ?: return
        val maxSteps = match.moveSequenceNotation.split(";").count { it.isNotBlank() }
        _session.update {
            it.copy(replayMoveStep = step.coerceIn(0, maxSteps))
        }
    }

    fun deleteMatchRecord(id: Long) {
        soundManager.playButtonTap()
        activeScope.launch {
            matchRepository.deleteMatch(id)
            if (_session.value.inspectedMatch?.id == id) {
                _session.update { it.copy(inspectedMatch = null) }
            }
        }
    }

    fun clearAllMatchHistory() {
        soundManager.playButtonTap()
        activeScope.launch {
            matchRepository.clearAllMatches()
            _session.update {
                it.copy(
                    sessionScoreX = 0,
                    sessionScoreO = 0,
                    sessionDraws = 0,
                    sessionStreak = 0,
                    inspectedMatch = null
                )
            }
        }
    }

    // Settings updates
    fun updatePlayerNames(playerOne: String, playerTwo: String) {
        activeScope.launch {
            preferencesRepository.updatePlayerNames(playerOne, playerTwo)
        }
    }

    fun updatePreferredMark(mark: Mark) {
        soundManager.playButtonTap()
        activeScope.launch {
            preferencesRepository.updatePreferredMark(mark)
        }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        soundManager.soundEnabled = enabled
        if (enabled) soundManager.playButtonTap()
        activeScope.launch {
            preferencesRepository.updateSoundEnabled(enabled)
        }
    }

    fun updateSoundVolume(volume: Float) {
        soundManager.soundVolume = volume
        activeScope.launch {
            preferencesRepository.updateSoundVolume(volume)
        }
    }

    fun updateMusicEnabled(enabled: Boolean) {
        soundManager.musicEnabled = enabled
        activeScope.launch {
            preferencesRepository.updateMusicEnabled(enabled)
        }
    }

    fun updateHapticsEnabled(enabled: Boolean) {
        soundManager.hapticsEnabled = enabled
        if (enabled) soundManager.playButtonTap()
        activeScope.launch {
            preferencesRepository.updateHapticsEnabled(enabled)
        }
    }

    fun updateAnimationsEnabled(enabled: Boolean) {
        soundManager.playButtonTap()
        activeScope.launch {
            preferencesRepository.updateAnimationsEnabled(enabled)
        }
    }

    fun updateShowCoordinates(enabled: Boolean) {
        soundManager.playButtonTap()
        activeScope.launch {
            preferencesRepository.updateShowCoordinates(enabled)
        }
    }

    fun updateStudioPalette(palette: StudioPalette) {
        soundManager.playButtonTap()
        activeScope.launch {
            preferencesRepository.updateStudioPalette(palette)
        }
    }

    fun updateThemeModeOverride(mode: ThemeModeOverride) {
        soundManager.playButtonTap()
        activeScope.launch {
            preferencesRepository.updateThemeModeOverride(mode)
        }
    }

    private fun startMatchTimer() {
        timerJob?.cancel()
        if (!enableMatchTimer) return
        timerJob = activeScope.launch {
            while (isActive && _session.value.status == GameStatus.IN_PROGRESS && _session.value.elapsedSeconds < 3600) {
                delay(1000L)
                if (_session.value.status == GameStatus.IN_PROGRESS) {
                    _session.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }

    private fun playEndOfMatchAudio(status: GameStatus, mode: GameMode, humanMark: Mark) {
        when (status) {
            GameStatus.DRAW -> soundManager.playDraw()
            GameStatus.WON_X -> {
                if (mode == GameMode.PVP || humanMark == Mark.X) {
                    soundManager.playVictory()
                } else {
                    soundManager.playDefeat()
                }
            }
            GameStatus.WON_O -> {
                if (mode == GameMode.PVP || humanMark == Mark.O) {
                    soundManager.playVictory()
                } else {
                    soundManager.playDefeat()
                }
            }
            GameStatus.IN_PROGRESS -> Unit
        }
    }

    private fun persistCompletedMatch(
        status: GameStatus,
        winningLine: WinningLine?,
        history: List<MoveRecord>,
        durationSeconds: Int,
        usedFallback: Boolean
    ) {
        val current = _session.value
        val winnerCode = when (status) {
            GameStatus.WON_X -> "X"
            GameStatus.WON_O -> "O"
            else -> "DRAW"
        }
        val notation = history.joinToString(";") { move ->
            val vanishSuffix = move.vanishedCellIndex?.let { "-v$it" } ?: ""
            "${move.mark.symbol}:${move.coordinate}$vanishSuffix"
        }
        val winCsv = winningLine?.indices?.joinToString(",") ?: ""

        viewModelScope.launch {
            matchRepository.recordMatch(
                MatchRecordEntity(
                    timestamp = System.currentTimeMillis(),
                    gameMode = current.gameMode.id,
                    difficulty = current.difficulty.id,
                    ruleMode = current.ruleMode.id,
                    playerXName = current.playerXName,
                    playerOName = current.playerOName,
                    humanMark = current.humanMark.symbol,
                    winner = winnerCode,
                    moveCount = history.size,
                    durationSeconds = durationSeconds,
                    moveSequenceNotation = notation,
                    winningCellsCsv = winCsv,
                    usedLocalFallback = usedFallback
                )
            )
        }
    }

    private fun resolvePlayerNames(
        mode: GameMode,
        difficulty: AiDifficulty,
        humanMark: Mark,
        p1: String,
        p2: String
    ): Pair<String, String> {
        val cleanP1 = p1.ifBlank { "Player 1" }
        val cleanP2 = p2.ifBlank { "Player 2" }
        return when (mode) {
            GameMode.PVP -> cleanP1 to cleanP2
            GameMode.PVC -> {
                val aiLabel = "Minimax (${difficulty.title})"
                if (humanMark == Mark.X) cleanP1 to aiLabel else aiLabel to cleanP1
            }
            GameMode.GEMINI -> {
                val aiLabel = "Gemini AI (${difficulty.title})"
                if (humanMark == Mark.X) cleanP1 to aiLabel else aiLabel to cleanP1
            }
        }
    }

    private fun buildHumanMoveCommentary(
        mark: Mark,
        coord: String,
        vanishedIndex: Int?,
        newStatus: GameStatus
    ): String {
        return when (newStatus) {
            GameStatus.WON_X, GameStatus.WON_O -> "${mark.symbol} completes the winning alignment via $coord!"
            GameStatus.DRAW -> "Final square $coord locked — tactical equilibrium (Draw)."
            GameStatus.IN_PROGRESS -> {
                if (vanishedIndex != null) {
                    val vanishedCoord = TicTacToeEngine.coordinateForIndex(vanishedIndex)
                    "${mark.symbol} claimed $coord (oldest mark at $vanishedCoord vanished)."
                } else {
                    "${mark.symbol} anchored $coord."
                }
            }
        }
    }

    private fun computeStatistics(matches: List<MatchRecordEntity>): StatisticsSummary {
        if (matches.isEmpty()) return StatisticsSummary()
        val total = matches.size
        val xWins = matches.count { it.winner == "X" }
        val oWins = matches.count { it.winner == "O" }
        val draws = matches.count { it.winner == "DRAW" }

        val playerOneWins = matches.count { match ->
            (match.winner == "X" && match.humanMark == "X") ||
                (match.winner == "O" && match.humanMark == "O")
        }

        val winRate = ((playerOneWins.toFloat() / total.toFloat()) * 100f).toInt()

        // Chronological order for streak computation
        val chronological = matches.sortedBy { it.timestamp }
        var curStreak = 0
        var maxStreak = 0
        for (m in chronological) {
            val won = (m.winner == "X" && m.humanMark == "X") ||
                (m.winner == "O" && m.humanMark == "O")
            if (won) {
                curStreak++
                if (curStreak > maxStreak) maxStreak = curStreak
            } else {
                curStreak = 0
            }
        }

        val fastestWin = matches
            .filter { (it.winner == "X" && it.humanMark == "X") || (it.winner == "O" && it.humanMark == "O") }
            .minOfOrNull { it.durationSeconds }

        val avgMoves = matches.map { it.moveCount }.average().toFloat()

        return StatisticsSummary(
            totalMatches = total,
            xWins = xWins,
            oWins = oWins,
            draws = draws,
            playerOneWins = playerOneWins,
            winRatePercent = winRate,
            currentStreak = curStreak,
            bestStreak = maxStreak,
            fastestWinSeconds = fastestWin,
            averageMoves = avgMoves,
            geminiMatches = matches.count { it.gameMode == GameMode.GEMINI.id },
            minimaxMatches = matches.count { it.gameMode == GameMode.PVC.id },
            pvpMatches = matches.count { it.gameMode == GameMode.PVP.id }
        )
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.stopAmbientMusic()
    }

    class Factory(
        private val matchRepository: MatchRepository,
        private val preferencesRepository: UserPreferencesRepository,
        private val soundManager: SoundAndHapticsManager,
        private val aiOpponentProvider: AiOpponentProvider = GeminiOpponentService()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameViewModel(
                matchRepository = matchRepository,
                preferencesRepository = preferencesRepository,
                soundManager = soundManager,
                aiOpponentProvider = aiOpponentProvider
            ) as T
        }
    }
}

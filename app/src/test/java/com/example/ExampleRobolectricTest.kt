package com.example

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.audio.SoundAndHapticsManager
import com.example.data.ai.AiOpponentProvider
import com.example.data.ai.GeminiMoveDecision
import com.example.data.local.MatchDao
import com.example.data.local.MatchRecordEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.MatchRepository
import com.example.domain.engine.AiDifficulty
import com.example.domain.engine.BoardRuleMode
import com.example.domain.engine.GameMode
import com.example.domain.engine.GameStatus
import com.example.domain.engine.Mark
import com.example.domain.engine.MoveRecord
import com.example.domain.engine.TicTacToeEngine
import com.example.ui.theme.StudioPalette
import com.example.ui.viewmodel.GameViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ExampleRobolectricTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class FakeMatchDao : MatchDao {
        private val records = MutableStateFlow<List<MatchRecordEntity>>(emptyList())
        private var nextId = 1L

        override fun getAllMatches(): Flow<List<MatchRecordEntity>> = records

        override suspend fun insertMatch(match: MatchRecordEntity): Long {
            val id = if (match.id == 0L) nextId++ else match.id
            val stored = match.copy(id = id)
            records.value = listOf(stored) + records.value.filterNot { it.id == id }
            return id
        }

        override suspend fun deleteMatchById(id: Long) {
            records.value = records.value.filterNot { it.id == id }
        }

        override suspend fun clearAllMatches() {
            records.value = emptyList()
        }
    }

    @Test
    fun `pvp match victory persists to repository and supports undo and rematch`() = runTest(UnconfinedTestDispatcher()) {
        val fakeDao = FakeMatchDao()
        val matchRepository = MatchRepository(fakeDao)
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tempFolder.newFile("test_prefs_pvp.preferences_pb") }
        )
        val preferencesRepository = UserPreferencesRepository(testDataStore)
        val soundManager = SoundAndHapticsManager(null).apply {
            soundEnabled = false
            hapticsEnabled = false
        }

        val viewModel = GameViewModel(
            matchRepository = matchRepository,
            preferencesRepository = preferencesRepository,
            soundManager = soundManager,
            enableMatchTimer = false,
            coroutineScope = backgroundScope
        )
        viewModel.configureAndStartMatch(
            gameMode = GameMode.PVP,
            difficulty = AiDifficulty.HARD,
            ruleMode = BoardRuleMode.CLASSIC,
            humanMark = Mark.X,
            xGoesFirst = true
        )

        // X plays 0, O plays 3, X plays 1
        viewModel.onCellTapped(0)
        viewModel.onCellTapped(3)
        viewModel.onCellTapped(1)
        assertEquals(3, viewModel.session.value.moveHistory.size)

        // Test Undo in PvP (rewinds 1 move so X's move at 1 is undone)
        viewModel.undoLastTurn()
        assertEquals(2, viewModel.session.value.moveHistory.size)
        assertEquals(Mark.X, viewModel.session.value.currentTurn)

        // Replay X at 1, O at 4, X at 2 -> X wins top row [0, 1, 2]
        viewModel.onCellTapped(1)
        viewModel.onCellTapped(4)
        viewModel.onCellTapped(2)

        val finalState = viewModel.session.value
        assertEquals(GameStatus.WON_X, finalState.status)
        assertEquals(1, finalState.sessionScoreX)
        assertNotNull(finalState.winningLine)

        // Verify match record saved in repository
        val savedMatches = matchRepository.allMatches.first()
        assertEquals(1, savedMatches.size)
        assertEquals("X", savedMatches.first().winner)
        assertEquals(5, savedMatches.first().moveCount)

        // Test Rematch
        viewModel.rematchWithAlternatingStarter()
        assertEquals(GameStatus.IN_PROGRESS, viewModel.session.value.status)
        assertTrue(viewModel.session.value.board.all { it == null })
    }

    @Test
    fun `gemini ai mode handles fallback gracefully when api error notice occurs`() = runTest(UnconfinedTestDispatcher()) {
        val fakeDao = FakeMatchDao()
        val matchRepository = MatchRepository(fakeDao)
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tempFolder.newFile("test_prefs_gemini.preferences_pb") }
        )
        val preferencesRepository = UserPreferencesRepository(testDataStore)
        val soundManager = SoundAndHapticsManager(null).apply {
            soundEnabled = false
            hapticsEnabled = false
        }

        val fallbackProvider = object : AiOpponentProvider {
            override suspend fun requestMove(
                board: List<Mark?>,
                aiMark: Mark,
                difficulty: AiDifficulty,
                ruleMode: BoardRuleMode,
                moveHistory: List<MoveRecord>
            ): GeminiMoveDecision {
                val best = TicTacToeEngine.findBestMove(board, aiMark, difficulty, ruleMode, moveHistory)
                return GeminiMoveDecision(
                    cellIndex = best.cellIndex,
                    coordinate = best.coordinate,
                    tacticalCommentary = "${best.tacticalReason} (Local Engine)",
                    confidence = 95,
                    usedLocalFallback = true,
                    errorNotice = "Network offline — Local Minimax fallback active."
                )
            }
        }

        val viewModel = GameViewModel(
            matchRepository = matchRepository,
            preferencesRepository = preferencesRepository,
            soundManager = soundManager,
            aiOpponentProvider = fallbackProvider,
            enableMatchTimer = false,
            coroutineScope = backgroundScope
        )

        viewModel.configureAndStartMatch(
            gameMode = GameMode.GEMINI,
            difficulty = AiDifficulty.HARD,
            ruleMode = BoardRuleMode.CLASSIC,
            humanMark = Mark.X,
            xGoesFirst = true
        )

        // Human X taps corner 0; Gemini fallback responds immediately at center 4
        viewModel.onCellTapped(0)

        val state = viewModel.session.value
        assertEquals(Mark.X, state.board[0])
        assertEquals(Mark.O, state.board[4])
        assertTrue(state.usedFallbackInMatch)
        assertNotNull(state.aiErrorBanner)
    }

    @Test
    fun `preferences repository persists studio palette and player names`() = runTest(UnconfinedTestDispatcher()) {
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tempFolder.newFile("test_prefs_settings.preferences_pb") }
        )
        val preferencesRepository = UserPreferencesRepository(testDataStore)

        preferencesRepository.updateStudioPalette(StudioPalette.KYOTO_HINOKI)
        preferencesRepository.updatePlayerNames("Aria", "Kenji")

        val prefs = preferencesRepository.preferencesFlow.first()
        assertEquals(StudioPalette.KYOTO_HINOKI, prefs.studioPalette)
        assertEquals("Aria", prefs.playerOneName)
        assertEquals("Kenji", prefs.playerTwoName)
    }
}

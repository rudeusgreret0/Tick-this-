package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.SoundAndHapticsManager
import com.example.data.ai.GeminiKeyManagerRepository
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.MatchRepository
import com.example.security.configureAppCheck
import com.example.ui.screens.AnimatedSplashScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsAndHistoryScreen
import com.example.ui.theme.LocalStudioColors
import com.example.ui.theme.TicTacToeTheme
import com.example.ui.viewmodel.GameViewModel

enum class StudioDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    ARENA("arena", "Lobby", Icons.Filled.Home, Icons.Outlined.Home),
    BOARD("board", "Play", Icons.Filled.GridOn, Icons.Outlined.GridOn),
    RECORDS("records", "Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    STUDIO("studio", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        // STEP 2: Enable Edge-to-Edge layout
        enableEdgeToEdge()

        setContent {
            val appContext = LocalContext.current.applicationContext
            val database = remember { AppDatabase.getInstance(appContext) }
            val matchRepository = remember { MatchRepository(database.matchDao()) }
            val preferencesRepository = remember { UserPreferencesRepository(appContext) }
            val keyManagerRepository = remember { GeminiKeyManagerRepository(appContext) }
            val soundManager = remember { SoundAndHapticsManager(appContext) }

            val viewModel: GameViewModel = viewModel(
                factory = GameViewModel.Factory(
                    matchRepository = matchRepository,
                    preferencesRepository = preferencesRepository,
                    soundManager = soundManager,
                    keyManagerRepository = keyManagerRepository
                )
            )

            TicTacToeApp(viewModel = viewModel)
        }
    }
}

@Composable
fun TicTacToeApp(
    viewModel: GameViewModel,
    skipSplashForTesting: Boolean = false
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    val matchHistory by viewModel.matchHistory.collectAsStateWithLifecycle()
    val apiConfigState by viewModel.apiConfigState.collectAsStateWithLifecycle()
    val apiKeyManagerUiState by viewModel.apiKeyManagerUiState.collectAsStateWithLifecycle()

    var showSplash by rememberSaveable { mutableStateOf(!skipSplashForTesting) }
    var currentDestination by rememberSaveable { mutableStateOf(StudioDestination.ARENA) }

    // Ensure hardware Back button navigates back to Lobby from secondary tabs
    BackHandler(enabled = !showSplash && currentDestination != StudioDestination.ARENA) {
        currentDestination = StudioDestination.ARENA
    }

    TicTacToeTheme(
        palette = preferences.studioPalette,
        themeModeOverride = preferences.themeModeOverride
    ) {
        val studioColors = LocalStudioColors.current

        Crossfade(
            targetState = showSplash,
            label = "splash_transition"
        ) { isSplashVisible ->
            if (isSplashVisible) {
                AnimatedSplashScreen(
                    onSplashFinished = { showSplash = false }
                )
            } else {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(studioColors.backgroundPrimary)
                ) {
                    val useNavigationRail = maxWidth >= 640.dp

                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                        containerColor = studioColors.backgroundPrimary,
                        bottomBar = {
                            if (!useNavigationRail) {
                                NavigationBar(
                                    containerColor = studioColors.surfaceElevated,
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    StudioDestination.entries.forEach { dest ->
                                        val selected = currentDestination == dest
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { currentDestination = dest },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = dest.label
                                                )
                                            },
                                            label = { Text(dest.label) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = studioColors.textPrimary,
                                                selectedTextColor = studioColors.textPrimary,
                                                indicatorColor = studioColors.accentGoldContainer,
                                                unselectedIconColor = studioColors.textMuted,
                                                unselectedTextColor = studioColors.textMuted
                                            ),
                                            modifier = Modifier.testTag("nav_tab_${dest.route}")
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (useNavigationRail) {
                                NavigationRail(
                                    containerColor = studioColors.surfaceElevated
                                ) {
                                    StudioDestination.entries.forEach { dest ->
                                        val selected = currentDestination == dest
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { currentDestination = dest },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = dest.label
                                                )
                                            },
                                            label = { Text(dest.label) },
                                            modifier = Modifier.testTag("nav_rail_${dest.route}")
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                when (currentDestination) {
                                    StudioDestination.ARENA -> {
                                        HomeScreen(
                                            preferences = preferences,
                                            statistics = statistics,
                                            onSelectMode = { mode ->
                                                viewModel.configureAndStartMatch(gameMode = mode)
                                            },
                                            onSelectDifficulty = { diff ->
                                                viewModel.configureAndStartMatch(difficulty = diff)
                                            },
                                            onSelectRuleMode = { rule ->
                                                viewModel.configureAndStartMatch(ruleMode = rule)
                                            },
                                            onSelectMark = { mark ->
                                                viewModel.configureAndStartMatch(humanMark = mark)
                                            },
                                            onCyclePalette = { palette ->
                                                viewModel.updateStudioPalette(palette)
                                            },
                                            onSavePlayerNames = { p1, p2 ->
                                                viewModel.updatePlayerNames(p1, p2)
                                            },
                                            onLaunchMatch = { mode, diff, rule, mark ->
                                                viewModel.configureAndStartMatch(
                                                    gameMode = mode,
                                                    difficulty = diff,
                                                    ruleMode = rule,
                                                    humanMark = mark
                                                )
                                                currentDestination = StudioDestination.BOARD
                                            }
                                        )
                                    }

                                    StudioDestination.BOARD -> {
                                        GameScreen(
                                            session = session,
                                            preferences = preferences,
                                            onCellTap = { viewModel.onCellTapped(it) },
                                            onUndo = { viewModel.undoLastTurn() },
                                            onHint = { viewModel.requestTacticalHint() },
                                            onRestart = { viewModel.restartMatch() },
                                            onRematch = { viewModel.rematchWithAlternatingStarter() },
                                            onSwapMarks = { viewModel.swapMarksAndRestart() },
                                            onDismissErrorBanner = { viewModel.dismissAiErrorBanner() },
                                            onNavigateToArenaSetup = {
                                                currentDestination = StudioDestination.ARENA
                                            },
                                            onOpenApiKeyManager = {
                                                currentDestination = StudioDestination.STUDIO
                                            }
                                        )
                                    }

                                    StudioDestination.RECORDS -> {
                                        StatsAndHistoryScreen(
                                            statistics = statistics,
                                            matches = matchHistory,
                                            inspectedMatch = session.inspectedMatch,
                                            replayMoveStep = session.replayMoveStep,
                                            onInspectMatch = { viewModel.inspectMatchReplay(it) },
                                            onSetReplayStep = { viewModel.setReplayMoveStep(it) },
                                            onDeleteMatch = { viewModel.deleteMatchRecord(it) },
                                            onClearAllMatches = { viewModel.clearAllMatchHistory() }
                                        )
                                    }

                                    StudioDestination.STUDIO -> {
                                        SettingsScreen(
                                            preferences = preferences,
                                            apiConfigState = apiConfigState,
                                            apiKeyManagerUiState = apiKeyManagerUiState,
                                            onAddApiKey = { label, rawApiKey, makeActive, validateFirst ->
                                                viewModel.addGeminiApiKey(
                                                    label = label,
                                                    rawApiKey = rawApiKey,
                                                    makeActive = makeActive,
                                                    validateFirst = validateFirst
                                                )
                                            },
                                            onTestUnsavedApiKey = { rawApiKey ->
                                                viewModel.testUnsavedApiKeyConnection(rawApiKey)
                                            },
                                            onTestSavedApiKey = { keyId ->
                                                viewModel.testSavedApiKeyConnection(keyId)
                                            },
                                            onReplaceSavedApiKey = { keyId, newLabel, newRawApiKey, validateIfNewKeyProvided ->
                                                viewModel.replaceSavedApiKey(
                                                    keyId = keyId,
                                                    newLabel = newLabel,
                                                    newRawApiKey = newRawApiKey,
                                                    validateIfNewKeyProvided = validateIfNewKeyProvided
                                                )
                                            },
                                            onDeleteSavedApiKey = { keyId ->
                                                viewModel.deleteSavedApiKey(keyId)
                                            },
                                            onSelectActiveApiKey = { keyId ->
                                                viewModel.selectActiveApiKey(keyId)
                                            },
                                            onClearApiKeyFeedback = {
                                                viewModel.clearApiKeyManagerFeedback()
                                            },
                                            onSelectPalette = { viewModel.updateStudioPalette(it) },
                                            onSelectThemeMode = { viewModel.updateThemeModeOverride(it) },
                                            onToggleSound = { viewModel.updateSoundEnabled(it) },
                                            onVolumeChange = { viewModel.updateSoundVolume(it) },
                                            onToggleMusic = { viewModel.updateMusicEnabled(it) },
                                            onToggleHaptics = { viewModel.updateHapticsEnabled(it) },
                                            onToggleAnimations = { viewModel.updateAnimationsEnabled(it) },
                                            onToggleCoordinates = { viewModel.updateShowCoordinates(it) },
                                            onSavePlayerNames = { p1, p2 ->
                                                viewModel.updatePlayerNames(p1, p2)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

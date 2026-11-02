package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.UserPreferences
import com.example.domain.engine.BoardRuleMode
import com.example.domain.engine.GameMode
import com.example.domain.engine.GameStatus
import com.example.domain.engine.Mark
import com.example.ui.components.TacticalGameBoard
import com.example.ui.theme.LocalStudioColors
import com.example.ui.viewmodel.GameSessionUiState

@Composable
fun GameScreen(
    session: GameSessionUiState,
    preferences: UserPreferences,
    onCellTap: (Int) -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onRestart: () -> Unit,
    onRematch: () -> Unit,
    onSwapMarks: () -> Unit,
    onDismissErrorBanner: () -> Unit,
    onNavigateToArenaSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val studioColors = LocalStudioColors.current
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(studioColors.backgroundPrimary)
            .testTag("game_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Top Match Metadata Bar
            MatchMetaHeader(
                session = session,
                onNavigateToArenaSetup = onNavigateToArenaSetup
            )

            // 2. Dual Player Scoreboard
            ScoreboardRow(session = session)

            // 3. Gemini AI / Minimax Tactical Telemetry Card
            TacticalTelemetryBanner(
                session = session,
                onDismissError = onDismissErrorBanner
            )

            // 4. Centerpiece Architectural Game Board
            TacticalGameBoard(
                board = session.board,
                winningLine = session.winningLine,
                lastMoveCellIndex = session.moveHistory.lastOrNull()?.cellIndex,
                hintCellIndex = session.hintEvaluation?.cellIndex,
                nextVanishCellIndex = session.nextVanishCellIndex,
                showCoordinates = preferences.showCoordinates,
                animationsEnabled = preferences.animationsEnabled,
                enabled = session.status == GameStatus.IN_PROGRESS && !session.isAiThinking && session.isHumanTurn,
                onCellClick = onCellTap,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 390.dp)
            )

            // 5. Post-Match Celebration Banner OR Tactical Controls
            AnimatedVisibility(
                visible = session.status != GameStatus.IN_PROGRESS,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MatchOutcomeCard(
                    session = session,
                    onRematch = onRematch,
                    onSwapSides = onSwapMarks,
                    onConfigure = onNavigateToArenaSetup
                )
            }

            // 6. Tactical Action Deck (Undo, Hint, Swap, Restart)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TacticalActionButton(
                    label = "Undo",
                    icon = Icons.AutoMirrored.Filled.Undo,
                    enabled = session.canUndo,
                    onClick = onUndo,
                    testTag = "undo_button",
                    modifier = Modifier.weight(1f)
                )
                TacticalActionButton(
                    label = "Hint",
                    icon = Icons.Default.Lightbulb,
                    enabled = session.status == GameStatus.IN_PROGRESS && !session.isAiThinking,
                    onClick = onHint,
                    testTag = "hint_button",
                    modifier = Modifier.weight(1f)
                )
                TacticalActionButton(
                    label = "Swap X/O",
                    icon = Icons.Default.SwapHoriz,
                    enabled = !session.isAiThinking,
                    onClick = onSwapMarks,
                    testTag = "swap_sides_button",
                    modifier = Modifier.weight(1f)
                )
                TacticalActionButton(
                    label = "Reset",
                    icon = Icons.Default.Refresh,
                    enabled = !session.isAiThinking,
                    onClick = onRestart,
                    testTag = "restart_button",
                    modifier = Modifier.weight(1f)
                )
            }

            // 7. Live Move Notation Strip
            if (session.moveHistory.isNotEmpty()) {
                MoveSequenceTicker(session = session)
            }
        }
    }
}

@Composable
private fun MatchMetaHeader(
    session: GameSessionUiState,
    onNavigateToArenaSetup: () -> Unit
) {
    val studioColors = LocalStudioColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = studioColors.surfaceCard,
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle)
            ) {
                Text(
                    text = "${session.gameMode.title.uppercase()} • ${
                        if (session.gameMode == GameMode.PVP) "2P DUEL" else session.difficulty.tag
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = studioColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            if (session.ruleMode == BoardRuleMode.INFINITY_THREE) {
                Surface(
                    color = studioColors.accentGoldContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "INFINITY 3-MARK",
                        style = MaterialTheme.typography.labelSmall,
                        color = studioColors.accentGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val minutes = session.elapsedSeconds / 60
            val seconds = session.elapsedSeconds % 60
            Text(
                text = "%02d:%02d".format(minutes, seconds),
                style = MaterialTheme.typography.labelMedium,
                color = studioColors.textMuted
            )

            Surface(
                onClick = onNavigateToArenaSetup,
                shape = RoundedCornerShape(50),
                color = studioColors.surfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle),
                modifier = Modifier.testTag("change_setup_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Configure match setup",
                        tint = studioColors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Setup",
                        style = MaterialTheme.typography.labelSmall,
                        color = studioColors.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreboardRow(session: GameSessionUiState) {
    val studioColors = LocalStudioColors.current
    val xActive = session.status == GameStatus.IN_PROGRESS && session.currentTurn == Mark.X
    val oActive = session.status == GameStatus.IN_PROGRESS && session.currentTurn == Mark.O

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Player X Card
        PlayerScoreCard(
            mark = Mark.X,
            name = session.playerXName,
            score = session.sessionScoreX,
            isActiveTurn = xActive,
            isWinner = session.status == GameStatus.WON_X,
            accentColor = studioColors.markXPrimary,
            containerColor = studioColors.markXContainer,
            modifier = Modifier
                .weight(1f)
                .testTag("score_card_X")
        )

        // Center Draws & Streak Pill
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = studioColors.surfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DRAWS",
                    style = MaterialTheme.typography.labelSmall,
                    color = studioColors.textMuted
                )
                Text(
                    text = "${session.sessionDraws}",
                    style = MaterialTheme.typography.titleLarge,
                    color = studioColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("draws_score_text")
                )
                if (session.sessionStreak > 0) {
                    Text(
                        text = "🔥 ${session.sessionStreak}",
                        style = MaterialTheme.typography.labelSmall,
                        color = studioColors.accentGold
                    )
                }
            }
        }

        // Player O Card
        PlayerScoreCard(
            mark = Mark.O,
            name = session.playerOName,
            score = session.sessionScoreO,
            isActiveTurn = oActive,
            isWinner = session.status == GameStatus.WON_O,
            accentColor = studioColors.markOPrimary,
            containerColor = studioColors.markOContainer,
            modifier = Modifier
                .weight(1f)
                .testTag("score_card_O")
        )
    }
}

@Composable
private fun PlayerScoreCard(
    mark: Mark,
    name: String,
    score: Int,
    isActiveTurn: Boolean,
    isWinner: Boolean,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    val studioColors = LocalStudioColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (isActiveTurn || isWinner) studioColors.surfaceElevated else studioColors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isActiveTurn || isWinner) 2.dp else 1.dp,
            color = if (isActiveTurn || isWinner) accentColor else studioColors.borderSubtle
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(containerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mark.symbol,
                            style = MaterialTheme.typography.labelLarge,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    if (isActiveTurn) {
                        Text(
                            text = "TURN",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (isWinner) {
                        Text(
                            text = "WINNER",
                            style = MaterialTheme.typography.labelSmall,
                            color = studioColors.accentGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = studioColors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineMedium,
                color = studioColors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TacticalTelemetryBanner(
    session: GameSessionUiState,
    onDismissError: () -> Unit
) {
    val studioColors = LocalStudioColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("telemetry_banner"),
            shape = RoundedCornerShape(16.dp),
            color = studioColors.surfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (session.isAiThinking) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .testTag("ai_thinking_indicator"),
                        strokeWidth = 2.2.dp,
                        color = studioColors.accentGold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = studioColors.accentGold,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = session.aiCommentary,
                    style = MaterialTheme.typography.bodySmall,
                    color = studioColors.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (session.aiConfidence != null && !session.isAiThinking) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = studioColors.accentGoldContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "${session.aiConfidence}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = studioColors.accentGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Transparent AI Error / Fallback Notice if applicable
        if (session.aiErrorBanner != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_error_banner"),
                shape = RoundedCornerShape(14.dp),
                color = studioColors.accentGoldContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.statusWarning)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = studioColors.statusWarning,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = session.aiErrorBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = studioColors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismissError,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss AI status notice",
                            tint = studioColors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchOutcomeCard(
    session: GameSessionUiState,
    onRematch: () -> Unit,
    onSwapSides: () -> Unit,
    onConfigure: () -> Unit
) {
    val studioColors = LocalStudioColors.current
    val (headline, subtitle, badgeColor) = when (session.status) {
        GameStatus.WON_X -> Triple(
            "${session.playerXName} (X) Triumphs!",
            "Decisive alignment in ${session.moveHistory.size} moves (${session.elapsedSeconds}s).",
            studioColors.markXPrimary
        )
        GameStatus.WON_O -> Triple(
            "${session.playerOName} (O) Triumphs!",
            "Decisive alignment in ${session.moveHistory.size} moves (${session.elapsedSeconds}s).",
            studioColors.markOPrimary
        )
        GameStatus.DRAW -> Triple(
            "Tactical Stalemate — Draw",
            "Every lane neutralized across ${session.moveHistory.size} moves.",
            studioColors.accentGold
        )
        GameStatus.IN_PROGRESS -> Triple("", "", studioColors.accentGold)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("match_outcome_card"),
        shape = RoundedCornerShape(22.dp),
        color = studioColors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(2.dp, badgeColor)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MATCH CONCLUDED",
                style = MaterialTheme.typography.labelMedium,
                color = badgeColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineSmall,
                color = studioColors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = studioColors.textSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRematch,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("rematch_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = badgeColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rematch", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSwapSides,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("outcome_swap_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Swap X/O")
                }

                OutlinedButton(
                    onClick = onConfigure,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Modes")
                }
            }
        }
    }
}

@Composable
private fun TacticalActionButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val studioColors = LocalStudioColors.current
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(50.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) studioColors.surfaceElevated else studioColors.surfaceCard.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) studioColors.textPrimary else studioColors.textMuted.copy(alpha = 0.45f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) studioColors.textPrimary else studioColors.textMuted.copy(alpha = 0.45f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MoveSequenceTicker(session: GameSessionUiState) {
    val studioColors = LocalStudioColors.current
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(session.moveHistory) { move ->
            val markColor = if (move.mark == Mark.X) studioColors.markXPrimary else studioColors.markOPrimary
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = studioColors.surfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${move.moveNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = studioColors.textMuted
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(markColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${move.mark.symbol}→${move.coordinate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = studioColors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

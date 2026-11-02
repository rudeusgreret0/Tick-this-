package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onOpenApiKeyManager: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val studioColors = LocalStudioColors.current
    val scrollState = rememberScrollState()
    var showDiagnosticsModal by rememberSaveable { mutableStateOf(false) }

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
                .widthIn(max = 520.dp)
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Sleek Top Pill Bar (Opponent Indicator + Setup Button)
            TopGamePillBar(
                session = session,
                onNavigateToArenaSetup = onNavigateToArenaSetup
            )

            // 2. Player vs Player Scoreboard Capsules
            ScoreboardRow(session = session)

            // 3. Compact Status Whisper & Non-Intrusive Quota/Fallback Pill
            CompactGameStatusBar(
                session = session,
                onDismissError = onDismissErrorBanner,
                onOpenApiKeyManager = onOpenApiKeyManager,
                onShowDiagnostics = { showDiagnosticsModal = true }
            )

            // 4. Centerpiece Game Board (Main Visual Attraction)
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
                    .widthIn(max = 380.dp)
            )

            // 5. Victory / Defeat / Draw Celebration Sheet
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

            // 6. Clean Bottom Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameActionPill(
                    label = "Undo",
                    icon = Icons.AutoMirrored.Filled.Undo,
                    enabled = session.canUndo,
                    onClick = onUndo,
                    testTag = "undo_button",
                    modifier = Modifier.weight(1f)
                )
                GameActionPill(
                    label = "Hint",
                    icon = Icons.Default.Lightbulb,
                    enabled = session.status == GameStatus.IN_PROGRESS && !session.isAiThinking,
                    onClick = onHint,
                    testTag = "hint_button",
                    modifier = Modifier.weight(1f)
                )
                GameActionPill(
                    label = "Swap",
                    icon = Icons.Default.SwapHoriz,
                    enabled = !session.isAiThinking,
                    onClick = onSwapMarks,
                    testTag = "swap_sides_button",
                    modifier = Modifier.weight(1f)
                )
                GameActionPill(
                    label = "Reset",
                    icon = Icons.Default.Refresh,
                    enabled = !session.isAiThinking,
                    onClick = onRestart,
                    testTag = "restart_button",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showDiagnosticsModal) {
        AlertDialog(
            onDismissRequest = { showDiagnosticsModal = false },
            title = { Text("AI Connection Diagnostics") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Active Engine: ${session.activeAiProviderLabel}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = session.aiErrorBanner ?: "Operating normally.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!session.aiTechnicalDiagnostics.isNullOrBlank()) {
                        Surface(
                            color = studioColors.surfaceCard,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = session.aiTechnicalDiagnostics,
                                style = MaterialTheme.typography.labelSmall,
                                color = studioColors.textSecondary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiagnosticsModal = false
                        onOpenApiKeyManager()
                    }
                ) {
                    Text("Open Gemini API Manager")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiagnosticsModal = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun TopGamePillBar(
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
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = studioColors.surfaceElevated,
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (session.usedFallbackInMatch) studioColors.markOPrimary
                                else studioColors.accentGold
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = session.activeAiProviderLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = studioColors.textPrimary
                    )
                }
            }

            if (session.ruleMode == BoardRuleMode.INFINITY_THREE) {
                Surface(
                    color = studioColors.accentGoldContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "3-Mark Infinity",
                        style = MaterialTheme.typography.labelSmall,
                        color = studioColors.accentGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Surface(
            onClick = onNavigateToArenaSetup,
            shape = RoundedCornerShape(50),
            color = studioColors.surfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle),
            modifier = Modifier.testTag("change_setup_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Change game mode",
                    tint = studioColors.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Mode",
                    style = MaterialTheme.typography.labelLarge,
                    color = studioColors.textPrimary
                )
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerScoreCapsule(
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

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = studioColors.surfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TIE",
                    style = MaterialTheme.typography.labelSmall,
                    color = studioColors.textMuted
                )
                Text(
                    text = "${session.sessionDraws}",
                    style = MaterialTheme.typography.titleMedium,
                    color = studioColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("draws_score_text")
                )
            }
        }

        PlayerScoreCapsule(
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
private fun PlayerScoreCapsule(
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mark.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        color = studioColors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            isWinner -> "Winner"
                            isActiveTurn -> "Playing now"
                            else -> "Waiting"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActiveTurn || isWinner) accentColor else studioColors.textMuted
                    )
                }
            }

            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineSmall,
                color = studioColors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CompactGameStatusBar(
    session: GameSessionUiState,
    onDismissError: () -> Unit,
    onOpenApiKeyManager: () -> Unit,
    onShowDiagnostics: () -> Unit
) {
    val studioColors = LocalStudioColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Concise 1-line turn / AI insight whisper
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("telemetry_banner"),
            shape = RoundedCornerShape(14.dp),
            color = studioColors.surfaceCard
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (session.isAiThinking) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(15.dp)
                            .testTag("ai_thinking_indicator"),
                        strokeWidth = 2.dp,
                        color = studioColors.accentGold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else if (session.gameMode != GameMode.PVP) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = studioColors.accentGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = session.aiCommentary,
                    style = MaterialTheme.typography.bodySmall,
                    color = studioColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        // Compact, single-line dismissible notification when quota/API issue occurs
        if (session.aiErrorBanner != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_error_banner"),
                shape = RoundedCornerShape(12.dp),
                color = studioColors.accentGoldContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.accentGold.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.aiErrorBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = studioColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (session.canSwitchKeyFromBanner) {
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onOpenApiKeyManager)
                                .testTag("banner_switch_key_button"),
                            color = studioColors.surfaceElevated,
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = studioColors.accentGold,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Keys",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = studioColors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onShowDiagnostics,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Technical details",
                            tint = studioColors.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismissError,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss notification",
                            tint = studioColors.textSecondary,
                            modifier = Modifier.size(15.dp)
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
            "${session.playerXName} Wins!",
            "Completed in ${session.moveHistory.size} moves (${session.elapsedSeconds}s)",
            studioColors.markXPrimary
        )
        GameStatus.WON_O -> Triple(
            "${session.playerOName} Wins!",
            "Completed in ${session.moveHistory.size} moves (${session.elapsedSeconds}s)",
            studioColors.markOPrimary
        )
        GameStatus.DRAW -> Triple(
            "It's a Draw!",
            "Balanced match across ${session.moveHistory.size} moves",
            studioColors.accentGold
        )
        GameStatus.IN_PROGRESS -> Triple("", "", studioColors.accentGold)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("match_outcome_card"),
        shape = RoundedCornerShape(24.dp),
        color = studioColors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(2.dp, badgeColor)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineSmall,
                color = studioColors.textPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = studioColors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    Text("Play Again", fontWeight = FontWeight.Bold)
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
private fun GameActionPill(
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
            .height(48.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) studioColors.surfaceElevated else studioColors.surfaceCard.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
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

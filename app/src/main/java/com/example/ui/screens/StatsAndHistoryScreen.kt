package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.MatchRecordEntity
import com.example.domain.engine.BoardRuleMode
import com.example.domain.engine.GameMode
import com.example.domain.engine.Mark
import com.example.domain.engine.MoveRecord
import com.example.domain.engine.TicTacToeEngine
import com.example.ui.theme.LocalStudioColors
import com.example.ui.viewmodel.StatisticsSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsAndHistoryScreen(
    statistics: StatisticsSummary,
    matches: List<MatchRecordEntity>,
    inspectedMatch: MatchRecordEntity?,
    replayMoveStep: Int,
    onInspectMatch: (MatchRecordEntity?) -> Unit,
    onSetReplayStep: (Int) -> Unit,
    onDeleteMatch: (Long) -> Unit,
    onClearAllMatches: () -> Unit,
    modifier: Modifier = Modifier
) {
    val studioColors = LocalStudioColors.current
    var modeFilter by rememberSaveable { mutableStateOf("ALL") }
    var showClearConfirm by rememberSaveable { mutableStateOf(false) }

    val filteredMatches = if (modeFilter == "ALL") {
        matches
    } else {
        matches.filter { it.gameMode == modeFilter }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(studioColors.backgroundPrimary)
            .testTag("stats_history_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "STUDIO ARCHIVES",
                            style = MaterialTheme.typography.labelMedium,
                            color = studioColors.accentGold
                        )
                        Text(
                            text = "Statistics & Match Ledger",
                            style = MaterialTheme.typography.headlineMedium,
                            color = studioColors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (matches.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear all match history",
                                tint = studioColors.markXPrimary
                            )
                        }
                    }
                }
            }

            // 2. Analytics Summary Card
            item {
                StatisticsOverviewCard(statistics = statistics)
            }

            // 3. Filter Bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "RECORDED MATCHES (${filteredMatches.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = studioColors.textMuted
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val filters = listOf(
                            "ALL" to "All Matches",
                            GameMode.GEMINI.id to "Gemini AI",
                            GameMode.PVC.id to "Minimax",
                            GameMode.PVP.id to "Pass & Play"
                        )
                        items(filters) { (key, label) ->
                            FilterChip(
                                selected = modeFilter == key,
                                onClick = { modeFilter = key },
                                label = { Text(label) },
                                modifier = Modifier.testTag("history_filter_$key")
                            )
                        }
                    }
                }
            }

            // 4. Match History Items or Empty State
            if (filteredMatches.isEmpty()) {
                item {
                    EmptyHistoryCard()
                }
            } else {
                items(filteredMatches, key = { it.id }) { record ->
                    MatchHistoryRowCard(
                        record = record,
                        onClick = { onInspectMatch(record) },
                        onDelete = { onDeleteMatch(record.id) }
                    )
                }
            }
        }
    }

    if (inspectedMatch != null) {
        MatchReplayDialog(
            match = inspectedMatch,
            currentStep = replayMoveStep,
            onStepChange = onSetReplayStep,
            onDismiss = { onInspectMatch(null) }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Match Archive?") },
            text = {
                Text("This permanently removes all recorded matches and resets career telemetry.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllMatches()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = studioColors.markXPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_clear_history_button")
                ) {
                    Text("Clear Archive")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatisticsOverviewCard(statistics: StatisticsSummary) {
    val studioColors = LocalStudioColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("statistics_overview_card"),
        shape = RoundedCornerShape(22.dp),
        color = studioColors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    label = "TOTAL GAMES",
                    value = "${statistics.totalMatches}",
                    accent = studioColors.textPrimary
                )
                MetricColumn(
                    label = "WIN RATE",
                    value = "${statistics.winRatePercent}%",
                    accent = studioColors.accentGold
                )
                MetricColumn(
                    label = "BEST STREAK",
                    value = "${statistics.bestStreak}",
                    accent = studioColors.markOPrimary
                )
                MetricColumn(
                    label = "AVG MOVES",
                    value = if (statistics.totalMatches > 0) "%.1f".format(statistics.averageMoves) else "—",
                    accent = studioColors.markXPrimary
                )
            }

            // Win / Draw / Loss Distribution Bar
            val total = statistics.totalMatches.coerceAtLeast(1)
            val xRatio = (statistics.xWins.toFloat() / total).coerceIn(0f, 1f)
            val drawRatio = (statistics.draws.toFloat() / total).coerceIn(0f, 1f)
            val oRatio = (statistics.oWins.toFloat() / total).coerceIn(0f, 1f)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "X Wins: ${statistics.xWins}",
                        style = MaterialTheme.typography.labelSmall,
                        color = studioColors.markXPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Draws: ${statistics.draws}",
                        style = MaterialTheme.typography.labelSmall,
                        color = studioColors.accentGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "O Wins: ${statistics.oWins}",
                        style = MaterialTheme.typography.labelSmall,
                        color = studioColors.markOPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(studioColors.surfaceBoard)
                ) {
                    if (statistics.totalMatches > 0) {
                        if (xRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(xRatio.coerceAtLeast(0.05f))
                                    .fillMaxHeight()
                                    .background(studioColors.markXPrimary)
                            )
                        }
                        if (drawRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(drawRatio.coerceAtLeast(0.05f))
                                    .fillMaxHeight()
                                    .background(studioColors.accentGold)
                            )
                        }
                        if (oRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(oRatio.coerceAtLeast(0.05f))
                                    .fillMaxHeight()
                                    .background(studioColors.markOPrimary)
                            )
                        }
                    }
                }
            }

            // Mode breakdown row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(studioColors.surfaceCard)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Gemini AI: ${statistics.geminiMatches}",
                    style = MaterialTheme.typography.labelSmall,
                    color = studioColors.textSecondary
                )
                Text(
                    text = "Minimax: ${statistics.minimaxMatches}",
                    style = MaterialTheme.typography.labelSmall,
                    color = studioColors.textSecondary
                )
                Text(
                    text = "Pass & Play: ${statistics.pvpMatches}",
                    style = MaterialTheme.typography.labelSmall,
                    color = studioColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, accent: Color) {
    val studioColors = LocalStudioColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = studioColors.textMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = accent,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyHistoryCard() {
    val studioColors = LocalStudioColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_history_card"),
        shape = RoundedCornerShape(20.dp),
        color = studioColors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = studioColors.accentGold,
                modifier = Modifier.size(38.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No Recorded Matches Yet",
                style = MaterialTheme.typography.titleMedium,
                color = studioColors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Complete a match against Gemini AI, Minimax, or a local friend to inspect move-by-move replays here.",
                style = MaterialTheme.typography.bodySmall,
                color = studioColors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MatchHistoryRowCard(
    record: MatchRecordEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val studioColors = LocalStudioColors.current
    val badgeColor = when (record.winner) {
        "X" -> studioColors.markXPrimary
        "O" -> studioColors.markOPrimary
        else -> studioColors.accentGold
    }
    val outcomeLabel = when (record.winner) {
        "X" -> "${record.playerXName} (X) Won"
        "O" -> "${record.playerOName} (O) Won"
        else -> "Tactical Draw"
    }
    val formatter = SimpleDateFormat("MMM d • HH:mm", Locale.getDefault())
    val formattedDate = formatter.format(Date(record.timestamp))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("match_history_item_${record.id}"),
        shape = RoundedCornerShape(18.dp),
        color = studioColors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (record.winner == "DRAW") "=" else record.winner,
                    style = MaterialTheme.typography.titleLarge,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = outcomeLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = studioColors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${record.moveCount} moves • ${record.durationSeconds}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = studioColors.textMuted
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${record.playerXName} vs ${record.playerOName} • $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = studioColors.textSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.PlayCircleOutline,
                contentDescription = "Replay match",
                tint = studioColors.accentGold,
                modifier = Modifier.size(22.dp)
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete match record",
                    tint = studioColors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MatchReplayDialog(
    match: MatchRecordEntity,
    currentStep: Int,
    onStepChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val studioColors = LocalStudioColors.current
    val tokens = match.moveSequenceNotation.split(";").filter { it.isNotBlank() }
    val totalSteps = tokens.size
    val ruleMode = BoardRuleMode.fromId(match.ruleMode)

    // Reconstruct board up to currentStep
    var board = List<Mark?>(9) { null }
    val history = mutableListOf<MoveRecord>()
    for (i in 0 until currentStep.coerceIn(0, totalSteps)) {
        val raw = tokens[i]
        val mainPart = raw.substringBefore("-v")
        val markSymbol = mainPart.substringBefore(":")
        val coord = mainPart.substringAfter(":")
        val mark = if (markSymbol == "O") Mark.O else Mark.X
        val cellIdx = TicTacToeEngine.indexForCoordinate(coord)
        if (cellIdx in 0..8) {
            val (nextBoard, vanished) = TicTacToeEngine.applyMove(
                currentBoard = board,
                moveHistory = history,
                cellIndex = cellIdx,
                mark = mark,
                ruleMode = ruleMode
            )
            board = nextBoard
            history.add(
                MoveRecord(
                    cellIndex = cellIdx,
                    mark = mark,
                    coordinate = coord,
                    moveNumber = i + 1,
                    vanishedCellIndex = vanished
                )
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "TACTICAL REPLAY INSPECTOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = studioColors.accentGold
                )
                Text(
                    text = "${match.playerXName} vs ${match.playerOName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mini 3x3 Board
                Column(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(studioColors.surfaceBoard)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (r in 0 until 3) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (c in 0 until 3) {
                                val idx = r * 3 + c
                                val cellMark = board[idx]
                                val isLastStepCell = history.lastOrNull()?.cellIndex == idx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(studioColors.cellSurface)
                                        .border(
                                            width = if (isLastStepCell) 2.dp else 1.dp,
                                            color = if (isLastStepCell) studioColors.accentGold else studioColors.borderSubtle,
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (cellMark != null) {
                                        Text(
                                            text = cellMark.symbol,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = if (cellMark == Mark.X) studioColors.markXPrimary else studioColors.markOPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = TicTacToeEngine.coordinateForIndex(idx),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = studioColors.textMuted.copy(alpha = 0.45f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Step Scrubber Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { onStepChange(currentStep - 1) },
                        enabled = currentStep > 0
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous move"
                        )
                    }
                    Text(
                        text = "Move $currentStep / $totalSteps",
                        style = MaterialTheme.typography.labelLarge
                    )
                    IconButton(
                        onClick = { onStepChange(currentStep + 1) },
                        enabled = currentStep < totalSteps
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next move"
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

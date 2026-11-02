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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.UserPreferences
import com.example.domain.engine.AiDifficulty
import com.example.domain.engine.BoardRuleMode
import com.example.domain.engine.GameMode
import com.example.domain.engine.Mark
import com.example.ui.components.StudioEmblemCanvas
import com.example.ui.theme.LocalStudioColors
import com.example.ui.theme.StudioPalette
import com.example.ui.viewmodel.StatisticsSummary

@Composable
fun HomeScreen(
    preferences: UserPreferences,
    statistics: StatisticsSummary,
    onSelectMode: (GameMode) -> Unit,
    onSelectDifficulty: (AiDifficulty) -> Unit,
    onSelectRuleMode: (BoardRuleMode) -> Unit,
    onSelectMark: (Mark) -> Unit,
    onCyclePalette: (StudioPalette) -> Unit,
    onSavePlayerNames: (String, String) -> Unit,
    onLaunchMatch: (GameMode, AiDifficulty, BoardRuleMode, Mark) -> Unit,
    modifier: Modifier = Modifier
) {
    val studioColors = LocalStudioColors.current
    var selectedMode by remember(preferences.selectedGameMode) {
        mutableStateOf(preferences.selectedGameMode)
    }
    var selectedDifficulty by remember(preferences.selectedDifficulty) {
        mutableStateOf(preferences.selectedDifficulty)
    }
    var selectedRuleMode by remember(preferences.selectedRuleMode) {
        mutableStateOf(preferences.selectedRuleMode)
    }
    var selectedMark by remember(preferences.preferredMark) {
        mutableStateOf(preferences.preferredMark)
    }
    var showProfileDialog by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(studioColors.backgroundPrimary)
            .testTag("home_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 540.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Bar: Player Pill + Theme Switcher
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { showProfileDialog = true }
                            .testTag("edit_profile_chip"),
                        color = studioColors.surfaceElevated,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(studioColors.markXContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = preferences.playerOneName.take(1).uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = studioColors.markXPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = preferences.playerOneName,
                                style = MaterialTheme.typography.titleSmall,
                                color = studioColors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit player names",
                                tint = studioColors.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                val palettes = StudioPalette.entries
                                val nextIdx = (palettes.indexOf(preferences.studioPalette) + 1) % palettes.size
                                onCyclePalette(palettes[nextIdx])
                            }
                            .testTag("cycle_palette_chip"),
                        color = studioColors.surfaceElevated,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Switch theme",
                                tint = studioColors.accentGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = preferences.studioPalette.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = studioColors.textPrimary
                            )
                        }
                    }
                }
            }

            // 2. Game Title Showcase Banner
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = Color.Black.copy(alpha = 0.08f)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    color = studioColors.surfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StudioEmblemCanvas(modifier = Modifier.size(84.dp))

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Tic Tac Toe",
                            style = MaterialTheme.typography.displaySmall,
                            color = studioColors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Pure strategy • Crafted for touch",
                            style = MaterialTheme.typography.bodyMedium,
                            color = studioColors.textSecondary
                        )

                        if (statistics.totalMatches > 0) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(studioColors.surfaceCard)
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = studioColors.accentGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${statistics.playerOneWins} Wins • ${statistics.winRatePercent}% Win Rate",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = studioColors.textPrimary
                                )
                                if (statistics.bestStreak > 1) {
                                    Text(
                                        text = "🔥 ${statistics.bestStreak} Streak",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = studioColors.accentGold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                onLaunchMatch(
                                    selectedMode,
                                    selectedDifficulty,
                                    selectedRuleMode,
                                    selectedMark
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("launch_match_button"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = studioColors.markXPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Play Match",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. Game Mode Selection
            item {
                Text(
                    text = "Game Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = studioColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GameModeTile(
                        mode = GameMode.GEMINI,
                        title = "Play vs Gemini AI",
                        subtitle = "Dynamic neural opponent with live move insights",
                        icon = Icons.Default.AutoAwesome,
                        selected = selectedMode == GameMode.GEMINI,
                        accentColor = studioColors.accentGold,
                        onClick = {
                            selectedMode = GameMode.GEMINI
                            onSelectMode(GameMode.GEMINI)
                        }
                    )
                    GameModeTile(
                        mode = GameMode.PVC,
                        title = "Play vs Computer",
                        subtitle = "Fast offline Minimax tactician",
                        icon = Icons.Default.Memory,
                        selected = selectedMode == GameMode.PVC,
                        accentColor = studioColors.markOPrimary,
                        onClick = {
                            selectedMode = GameMode.PVC
                            onSelectMode(GameMode.PVC)
                        }
                    )
                    GameModeTile(
                        mode = GameMode.PVP,
                        title = "Pass & Play (2 Players)",
                        subtitle = "${preferences.playerOneName} vs ${preferences.playerTwoName} on one screen",
                        icon = Icons.Default.People,
                        selected = selectedMode == GameMode.PVP,
                        accentColor = studioColors.markXPrimary,
                        onClick = {
                            selectedMode = GameMode.PVP
                            onSelectMode(GameMode.PVP)
                        }
                    )
                }
            }

            // 4. Difficulty Selection (only when playing vs AI)
            if (selectedMode != GameMode.PVP) {
                item {
                    Text(
                        text = "AI Difficulty",
                        style = MaterialTheme.typography.titleMedium,
                        color = studioColors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(studioColors.surfaceCard)
                            .padding(5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AiDifficulty.entries.forEach { diff ->
                            val isSelected = selectedDifficulty == diff
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedDifficulty = diff
                                        onSelectDifficulty(diff)
                                    }
                                    .testTag("difficulty_${diff.id}"),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) studioColors.surfaceElevated else Color.Transparent,
                                shadowElevation = if (isSelected) 2.dp else 0.dp
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = diff.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isSelected) studioColors.textPrimary else studioColors.textSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Rules & Your Piece
            item {
                Text(
                    text = "Rules & Piece",
                    style = MaterialTheme.typography.titleMedium,
                    color = studioColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BoardRuleMode.entries.forEach { rule ->
                        val isSelected = selectedRuleMode == rule
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    selectedRuleMode = rule
                                    onSelectRuleMode(rule)
                                }
                                .testTag("rule_${rule.id}"),
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) studioColors.surfaceElevated else studioColors.surfaceCard,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) studioColors.markOPrimary else studioColors.borderSubtle
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (rule == BoardRuleMode.INFINITY_THREE) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = studioColors.accentGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = rule.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = studioColors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (rule == BoardRuleMode.CLASSIC) {
                                        "Standard 3-in-a-row"
                                    } else {
                                        "Max 3 marks each • No draws"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = studioColors.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PieceSelectionPill(
                        mark = Mark.X,
                        selected = selectedMark == Mark.X,
                        color = studioColors.markXPrimary,
                        containerColor = studioColors.markXContainer,
                        onClick = {
                            selectedMark = Mark.X
                            onSelectMark(Mark.X)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PieceSelectionPill(
                        mark = Mark.O,
                        selected = selectedMark == Mark.O,
                        color = studioColors.markOPrimary,
                        containerColor = studioColors.markOContainer,
                        onClick = {
                            selectedMark = Mark.O
                            onSelectMark(Mark.O)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showProfileDialog) {
        PlayerProfileDialog(
            initialPlayerOne = preferences.playerOneName,
            initialPlayerTwo = preferences.playerTwoName,
            onDismiss = { showProfileDialog = false },
            onSave = { p1, p2 ->
                onSavePlayerNames(p1, p2)
                showProfileDialog = false
            }
        )
    }
}

@Composable
private fun GameModeTile(
    mode: GameMode,
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val studioColors = LocalStudioColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("mode_card_${mode.id}"),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) studioColors.surfaceElevated else studioColors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) accentColor else studioColors.borderSubtle
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = studioColors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = studioColors.textSecondary
                )
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (selected) 7.dp else 1.5.dp,
                        color = if (selected) accentColor else studioColors.gridLine,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun PieceSelectionPill(
    mark: Mark,
    selected: Boolean,
    color: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val studioColors = LocalStudioColors.current
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("mark_choice_${mark.symbol}"),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) containerColor else studioColors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) color else studioColors.borderSubtle
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = mark.symbol,
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Play as ${mark.symbol}",
                style = MaterialTheme.typography.titleSmall,
                color = studioColors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PlayerProfileDialog(
    initialPlayerOne: String,
    initialPlayerTwo: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var p1 by rememberSaveable { mutableStateOf(initialPlayerOne) }
    var p2 by rememberSaveable { mutableStateOf(initialPlayerTwo) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Player Names",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = p1,
                    onValueChange = { p1 = it },
                    label = { Text("Player 1") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_one_input")
                )
                OutlinedTextField(
                    value = p2,
                    onValueChange = { p2 = it },
                    label = { Text("Player 2 (Pass & Play)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_two_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(p1, p2) },
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

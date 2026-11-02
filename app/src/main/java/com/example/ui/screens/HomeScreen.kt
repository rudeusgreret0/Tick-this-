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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Studio Header Banner
            item {
                StudioHeroCard(
                    playerName = preferences.playerOneName,
                    palette = preferences.studioPalette,
                    statistics = statistics,
                    onEditProfile = { showProfileDialog = true },
                    onCyclePalette = {
                        val palettes = StudioPalette.entries
                        val nextIdx = (palettes.indexOf(preferences.studioPalette) + 1) % palettes.size
                        onCyclePalette(palettes[nextIdx])
                    }
                )
            }

            // 2. Game Mode Selection
            item {
                SectionHeader(
                    eyebrow = "01 • OPPONENT SYSTEM",
                    title = "Select Match Discipline"
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeSelectionCard(
                        mode = GameMode.GEMINI,
                        icon = Icons.Default.AutoAwesome,
                        badge = "FIREBASE AI LOGIC",
                        selected = selectedMode == GameMode.GEMINI,
                        accentColor = studioColors.accentGold,
                        onClick = {
                            selectedMode = GameMode.GEMINI
                            onSelectMode(GameMode.GEMINI)
                        }
                    )
                    ModeSelectionCard(
                        mode = GameMode.PVC,
                        icon = Icons.Default.Memory,
                        badge = "ALPHA-BETA ENGINE",
                        selected = selectedMode == GameMode.PVC,
                        accentColor = studioColors.markOPrimary,
                        onClick = {
                            selectedMode = GameMode.PVC
                            onSelectMode(GameMode.PVC)
                        }
                    )
                    ModeSelectionCard(
                        mode = GameMode.PVP,
                        icon = Icons.Default.People,
                        badge = "LOCAL TABLETOP",
                        selected = selectedMode == GameMode.PVP,
                        accentColor = studioColors.markXPrimary,
                        onClick = {
                            selectedMode = GameMode.PVP
                            onSelectMode(GameMode.PVP)
                        }
                    )
                }
            }

            // 3. Difficulty Selection (when vs AI)
            if (selectedMode != GameMode.PVP) {
                item {
                    SectionHeader(
                        eyebrow = "02 • TACTICAL DEPTH",
                        title = "AI Intelligence Tier"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AiDifficulty.entries.forEach { diff ->
                            val isSelected = selectedDifficulty == diff
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        selectedDifficulty = diff
                                        onSelectDifficulty(diff)
                                    }
                                    .testTag("difficulty_${diff.id}"),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) studioColors.surfaceElevated else studioColors.surfaceCard,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) studioColors.accentGold else studioColors.borderSubtle
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = diff.tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) studioColors.accentGold else studioColors.textMuted
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = diff.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = studioColors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Rule Variant & Mark Identity Selection
            item {
                SectionHeader(
                    eyebrow = if (selectedMode != GameMode.PVP) "03 • BOARD RULES & ALLEGIANCE" else "02 • BOARD RULES & ALLEGIANCE",
                    title = "Rule Variant & Piece"
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BoardRuleMode.entries.forEach { rule ->
                        val isSelected = selectedRuleMode == rule
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedRuleMode = rule
                                    onSelectRuleMode(rule)
                                }
                                .testTag("rule_${rule.id}"),
                            shape = RoundedCornerShape(16.dp),
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
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = rule.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) studioColors.markOPrimary else studioColors.textMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = rule.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = studioColors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = rule.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = studioColors.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mark X vs O Allegiance Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MarkChoiceCard(
                        mark = Mark.X,
                        subtitle = if (selectedMode == GameMode.PVP) preferences.playerOneName else "First Strike • Vermilion",
                        selected = selectedMark == Mark.X,
                        color = studioColors.markXPrimary,
                        containerColor = studioColors.markXContainer,
                        onClick = {
                            selectedMark = Mark.X
                            onSelectMark(Mark.X)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MarkChoiceCard(
                        mark = Mark.O,
                        subtitle = if (selectedMode == GameMode.PVP) preferences.playerTwoName else "Counter Play • Viridian",
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

            // 5. Primary Launch Action
            item {
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
                        .height(58.dp)
                        .testTag("launch_match_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = studioColors.textPrimary,
                        contentColor = studioColors.backgroundPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Enter Tactical Board",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
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
private fun StudioHeroCard(
    playerName: String,
    palette: StudioPalette,
    statistics: StatisticsSummary,
    onEditProfile: () -> Unit,
    onCyclePalette: () -> Unit
) {
    val studioColors = LocalStudioColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = studioColors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = studioColors.accentGoldContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "ATELIER XO • STUDIO",
                        style = MaterialTheme.typography.labelMedium,
                        color = studioColors.accentGold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onCyclePalette)
                            .testTag("cycle_palette_chip"),
                        color = studioColors.surfaceCard,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Switch studio theme palette",
                                tint = studioColors.accentGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = palette.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = studioColors.textPrimary
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onEditProfile)
                            .testTag("edit_profile_chip"),
                        color = studioColors.surfaceCard,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit player profile",
                                tint = studioColors.textSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = playerName,
                                style = MaterialTheme.typography.labelSmall,
                                color = studioColors.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tic Tac Toe",
                        style = MaterialTheme.typography.displaySmall,
                        color = studioColors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Architectural strategy crafted with deterministic Minimax & live Gemini AI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = studioColors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                StudioEmblemCanvas(
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Career Telemetry Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(studioColors.surfaceCard)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickStatItem(label = "MATCHES", value = "${statistics.totalMatches}")
                QuickStatItem(label = "WIN RATE", value = "${statistics.winRatePercent}%")
                QuickStatItem(label = "BEST STREAK", value = "${statistics.bestStreak}")
                QuickStatItem(
                    label = "FASTEST WIN",
                    value = statistics.fastestWinSeconds?.let { "${it}s" } ?: "—"
                )
            }
        }
    }
}

@Composable
private fun QuickStatItem(label: String, value: String) {
    val studioColors = LocalStudioColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = studioColors.textMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = studioColors.textPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionHeader(eyebrow: String, title: String) {
    val studioColors = LocalStudioColors.current
    Column {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelMedium,
            color = studioColors.accentGold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = studioColors.textPrimary
        )
    }
}

@Composable
private fun ModeSelectionCard(
    mode: GameMode,
    icon: ImageVector,
    badge: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val studioColors = LocalStudioColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("mode_card_${mode.id}"),
        shape = RoundedCornerShape(18.dp),
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
                    .size(46.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = mode.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = studioColors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = mode.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = studioColors.textSecondary
                )
            }
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (selected) 6.dp else 1.5.dp,
                        color = if (selected) accentColor else studioColors.gridLine,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun MarkChoiceCard(
    mark: Mark,
    subtitle: String,
    selected: Boolean,
    color: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val studioColors = LocalStudioColors.current
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("mark_choice_${mark.symbol}"),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) containerColor else studioColors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) color else studioColors.borderSubtle
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mark.symbol,
                style = MaterialTheme.typography.headlineLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Play as ${mark.symbol}",
                    style = MaterialTheme.typography.titleSmall,
                    color = studioColors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = studioColors.textSecondary,
                    maxLines = 1
                )
            }
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
                text = "Player Roster & Callsigns",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = p1,
                    onValueChange = { p1 = it },
                    label = { Text("Primary Player (Player 1)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_one_input")
                )
                OutlinedTextField(
                    value = p2,
                    onValueChange = { p2 = it },
                    label = { Text("Challenger (Pass & Play Player 2)") },
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
                Text("Save Roster")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

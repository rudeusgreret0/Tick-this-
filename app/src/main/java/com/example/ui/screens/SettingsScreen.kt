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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.example.data.local.UserPreferences
import com.example.ui.theme.AtelierIvoryDark
import com.example.ui.theme.AtelierIvoryLight
import com.example.ui.theme.KyotoHinokiLight
import com.example.ui.theme.LocalStudioColors
import com.example.ui.theme.NordicSlateLight
import com.example.ui.theme.StudioPalette
import com.example.ui.theme.ThemeModeOverride

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onSelectPalette: (StudioPalette) -> Unit,
    onSelectThemeMode: (ThemeModeOverride) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMusic: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleAnimations: (Boolean) -> Unit,
    onToggleCoordinates: (Boolean) -> Unit,
    onSavePlayerNames: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val studioColors = LocalStudioColors.current
    var p1Name by rememberSaveable(preferences.playerOneName) {
        mutableStateOf(preferences.playerOneName)
    }
    var p2Name by rememberSaveable(preferences.playerTwoName) {
        mutableStateOf(preferences.playerTwoName)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(studioColors.backgroundPrimary)
            .testTag("settings_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "STUDIO ATELIER",
                        style = MaterialTheme.typography.labelMedium,
                        color = studioColors.accentGold
                    )
                    Text(
                        text = "Design, Audio & Roster",
                        style = MaterialTheme.typography.headlineMedium,
                        color = studioColors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 1. Curated Studio Color Palettes
            item {
                SettingsGroupCard(title = "01 • STUDIO COLOR DIRECTION") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        StudioPalette.entries.forEach { palette ->
                            val previewColors = when (palette) {
                                StudioPalette.ATELIER_IVORY -> AtelierIvoryLight
                                StudioPalette.NORDIC_SLATE -> NordicSlateLight
                                StudioPalette.KYOTO_HINOKI -> KyotoHinokiLight
                                StudioPalette.OBSIDIAN_STUDIO -> AtelierIvoryDark
                            }
                            val selected = preferences.studioPalette == palette
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectPalette(palette) }
                                    .testTag("palette_option_${palette.id}"),
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) studioColors.surfaceElevated else studioColors.surfaceCard,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) studioColors.accentGold else studioColors.borderSubtle
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Swatch trio
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        SwatchCircle(previewColors.backgroundPrimary, previewColors.borderSubtle)
                                        SwatchCircle(previewColors.markXPrimary, Color.Transparent)
                                        SwatchCircle(previewColors.markOPrimary, Color.Transparent)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = palette.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = studioColors.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = palette.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = studioColors.textSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Light / Dark / System Mode Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeModeOverride.entries.forEach { mode ->
                                val isSelected = preferences.themeModeOverride == mode
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectThemeMode(mode) }
                                        .testTag("theme_mode_${mode.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) studioColors.accentGoldContainer else studioColors.surfaceCard,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) studioColors.accentGold else studioColors.borderSubtle
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSelected) studioColors.accentGold else studioColors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Audio & Tactile Feel
            item {
                SettingsGroupCard(title = "02 • ACOUSTICS & HAPTICS") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingSwitchRow(
                            title = "Acoustic Sound Effects",
                            subtitle = "Synthesized marimba & ceramic chimes on X and O moves",
                            checked = preferences.soundEnabled,
                            onCheckedChange = onToggleSound,
                            testTag = "toggle_sound"
                        )

                        if (preferences.soundEnabled) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Master Acoustic Volume",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = studioColors.textSecondary
                                    )
                                    Text(
                                        text = "${(preferences.soundVolume * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = studioColors.accentGold
                                    )
                                }
                                Slider(
                                    value = preferences.soundVolume,
                                    onValueChange = onVolumeChange,
                                    modifier = Modifier.testTag("volume_slider")
                                )
                            }
                        }

                        SettingSwitchRow(
                            title = "Ambient Studio Harmonic Drone",
                            subtitle = "Gentle low-volume acoustic chord progression during play",
                            checked = preferences.musicEnabled,
                            onCheckedChange = onToggleMusic,
                            testTag = "toggle_music"
                        )

                        SettingSwitchRow(
                            title = "Tactile Haptic Feedback",
                            subtitle = "Precision physical impulse on moves and victories",
                            checked = preferences.hapticsEnabled,
                            onCheckedChange = onToggleHaptics,
                            testTag = "toggle_haptics"
                        )
                    }
                }
            }

            // 3. Motion & Board Presentation
            item {
                SettingsGroupCard(title = "03 • BOARD PRESENTATION & MOTION") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingSwitchRow(
                            title = "Choreographed Stroke Animations",
                            subtitle = "Smooth drawing strokes for X, O, and winning strike lines",
                            checked = preferences.animationsEnabled,
                            onCheckedChange = onToggleAnimations,
                            testTag = "toggle_animations"
                        )

                        SettingSwitchRow(
                            title = "Architectural Grid Coordinates (A1–C3)",
                            subtitle = "Display subtle coordinate notation inside each cell",
                            checked = preferences.showCoordinates,
                            onCheckedChange = onToggleCoordinates,
                            testTag = "toggle_coordinates"
                        )
                    }
                }
            }

            // 4. Player Roster
            item {
                SettingsGroupCard(title = "04 • PLAYER ROSTER") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = p1Name,
                            onValueChange = { p1Name = it },
                            label = { Text("Player 1 Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_player_one_input")
                        )
                        OutlinedTextField(
                            value = p2Name,
                            onValueChange = { p2Name = it },
                            label = { Text("Player 2 Name (Pass & Play)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_player_two_input")
                        )
                        Button(
                            onClick = { onSavePlayerNames(p1Name, p2Name) },
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("settings_save_names_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Update Callsigns")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
    val studioColors = LocalStudioColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = studioColors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, studioColors.borderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = studioColors.accentGold
            )
            content()
        }
    }
}

@Composable
private fun SwatchCircle(fill: Color, stroke: Color) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, stroke, CircleShape)
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val studioColors = LocalStudioColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = studioColors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = studioColors.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

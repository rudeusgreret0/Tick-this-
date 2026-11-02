package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ai.ApiKeyHealthStatus
import com.example.data.ai.GeminiApiConfigState
import com.example.data.ai.SavedGeminiApiKey
import com.example.data.local.UserPreferences
import com.example.ui.theme.AtelierIvoryDark
import com.example.ui.theme.AtelierIvoryLight
import com.example.ui.theme.KyotoHinokiLight
import com.example.ui.theme.LocalStudioColors
import com.example.ui.theme.NordicSlateLight
import com.example.ui.theme.StudioPalette
import com.example.ui.theme.ThemeModeOverride
import com.example.ui.viewmodel.ApiKeyManagerUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    apiConfigState: GeminiApiConfigState,
    apiKeyManagerUiState: ApiKeyManagerUiState,
    onAddApiKey: (label: String, rawApiKey: String, makeActive: Boolean, validateFirst: Boolean) -> Unit,
    onTestUnsavedApiKey: (rawApiKey: String) -> Unit,
    onTestSavedApiKey: (keyId: String) -> Unit,
    onReplaceSavedApiKey: (keyId: String, newLabel: String, newRawApiKey: String, validateIfNewKeyProvided: Boolean) -> Unit,
    onDeleteSavedApiKey: (keyId: String) -> Unit,
    onSelectActiveApiKey: (keyId: String?) -> Unit,
    onClearApiKeyFeedback: () -> Unit,
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

    // Gemini API Manager form states
    var showAddKeyForm by rememberSaveable { mutableStateOf(false) }
    var newKeyLabel by rememberSaveable { mutableStateOf("Primary Key") }
    var newKeyRawValue by rememberSaveable { mutableStateOf("") }
    var showNewKeyPlainText by rememberSaveable { mutableStateOf(false) }

    // Replace/Edit modal state
    var keyBeingReplaced by rememberSaveable { mutableStateOf<String?>(null) }
    var replaceLabelInput by rememberSaveable { mutableStateOf("") }
    var replaceRawKeyInput by rememberSaveable { mutableStateOf("") }
    var showReplacePlainText by rememberSaveable { mutableStateOf(false) }

    // Delete confirmation modal state
    var keyPendingDeletion by rememberSaveable { mutableStateOf<String?>(null) }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Studio & AI Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = studioColors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage Gemini AI keys, visual themes, audio, and players",
                        style = MaterialTheme.typography.bodySmall,
                        color = studioColors.textSecondary
                    )
                }
            }

            // 1. GEMINI API MANAGER SECTION
            item {
                SettingsGroupCard(
                    title = "Gemini API Manager",
                    subtitle = "Add, test, switch, replace, or remove personal Gemini API keys"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Active Configuration Indicator Banner
                        val activeKey = apiConfigState.activeKey
                        val isUsingCustomKey = activeKey != null
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("active_api_config_banner"),
                            shape = RoundedCornerShape(14.dp),
                            color = studioColors.surfaceCard,
                            border = BorderStroke(
                                1.dp,
                                if (isUsingCustomKey) studioColors.markOPrimary.copy(alpha = 0.5f)
                                else studioColors.accentGold.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isUsingCustomKey) Icons.Default.Key else Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = if (isUsingCustomKey) studioColors.markOPrimary else studioColors.accentGold,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (activeKey != null) {
                                            "Active Key: ${activeKey.label} (${activeKey.maskedPreview})"
                                        } else {
                                            "Active Provider: Studio Cloud Proxy (Firebase AI Logic)"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = studioColors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (activeKey != null) {
                                            "Status: ${activeKey.status.displayLabel} • Automatic failover to backup keys & Local Master AI"
                                        } else {
                                            "Uses default studio quota • Add a personal key below if studio quota is reached"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = studioColors.textSecondary
                                    )
                                }
                            }
                        }

                        // Feedback Banner (Test / Save / Switch status)
                        if (!apiKeyManagerUiState.feedbackMessage.isNullOrBlank()) {
                            val isError = apiKeyManagerUiState.isFeedbackError
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("api_manager_feedback_banner"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isError) {
                                    studioColors.markXContainer.copy(alpha = 0.65f)
                                } else {
                                    studioColors.markOContainer.copy(alpha = 0.65f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isError) studioColors.markXPrimary else studioColors.markOPrimary
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isError) studioColors.markXPrimary else studioColors.markOPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = apiKeyManagerUiState.feedbackMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = studioColors.textPrimary
                                        )
                                    }
                                    TextButton(onClick = onClearApiKeyFeedback) {
                                        Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        // Option to use Default Cloud Proxy vs Saved Personal Keys
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectActiveApiKey(null) }
                                .testTag("select_cloud_proxy_option"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (!isUsingCustomKey) studioColors.surfaceElevated else studioColors.surfaceCard,
                            border = BorderStroke(
                                width = if (!isUsingCustomKey) 1.5.dp else 1.dp,
                                color = if (!isUsingCustomKey) studioColors.accentGold else studioColors.borderSubtle
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = !isUsingCustomKey,
                                    onClick = { onSelectActiveApiKey(null) }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Built-in Studio Cloud Proxy",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = studioColors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Default Firebase AI Logic connection",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = studioColors.textSecondary
                                    )
                                }
                            }
                        }

                        // Saved Personal API Keys List
                        if (apiConfigState.savedKeys.isNotEmpty()) {
                            Text(
                                text = "Saved Personal Keys (${apiConfigState.savedKeys.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = studioColors.textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )

                            apiConfigState.savedKeys.forEach { savedKey ->
                                val isSelected = apiConfigState.activeKeyId == savedKey.id
                                val isTestingThisKey = apiKeyManagerUiState.isTestingConnection &&
                                    apiKeyManagerUiState.testingKeyId == savedKey.id

                                SavedApiKeyCard(
                                    savedKey = savedKey,
                                    isSelected = isSelected,
                                    isTesting = isTestingThisKey,
                                    onSelect = { onSelectActiveApiKey(savedKey.id) },
                                    onTest = { onTestSavedApiKey(savedKey.id) },
                                    onEditReplace = {
                                        keyBeingReplaced = savedKey.id
                                        replaceLabelInput = savedKey.label
                                        replaceRawKeyInput = ""
                                        showReplacePlainText = false
                                    },
                                    onDelete = {
                                        keyPendingDeletion = savedKey.id
                                    }
                                )
                            }
                        }

                        // Add New API Key Toggle or Expanded Form
                        if (!showAddKeyForm) {
                            Button(
                                onClick = { showAddKeyForm = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_add_api_key_form_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = studioColors.accentGold,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add New Gemini API Key", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_api_key_form_card"),
                                shape = RoundedCornerShape(16.dp),
                                color = studioColors.surfaceCard,
                                border = BorderStroke(1.dp, studioColors.accentGold.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Add Personal Gemini API Key",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = studioColors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )

                                    // Preset label chips
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Primary Key", "Backup Key", "Personal Key").forEach { preset ->
                                            AssistChip(
                                                onClick = { newKeyLabel = preset },
                                                label = {
                                                    Text(
                                                        text = preset,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = newKeyLabel,
                                        onValueChange = { newKeyLabel = it },
                                        label = { Text("Key Name / Label") },
                                        placeholder = { Text("e.g., Primary Key, Backup Key") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("new_api_key_label_input")
                                    )

                                    OutlinedTextField(
                                        value = newKeyRawValue,
                                        onValueChange = { newKeyRawValue = it },
                                        label = { Text("Gemini API Key") },
                                        placeholder = { Text("Paste your Gemini API key") },
                                        singleLine = true,
                                        visualTransformation = if (showNewKeyPlainText) {
                                            VisualTransformation.None
                                        } else {
                                            PasswordVisualTransformation()
                                        },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { showNewKeyPlainText = !showNewKeyPlainText },
                                                modifier = Modifier.testTag("toggle_new_key_visibility")
                                            ) {
                                                Icon(
                                                    imageVector = if (showNewKeyPlainText) {
                                                        Icons.Default.VisibilityOff
                                                    } else {
                                                        Icons.Default.Visibility
                                                    },
                                                    contentDescription = if (showNewKeyPlainText) {
                                                        "Hide API key"
                                                    } else {
                                                        "Show API key"
                                                    }
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("new_api_key_value_input")
                                    )

                                    val isTestingUnsaved = apiKeyManagerUiState.isTestingConnection &&
                                        apiKeyManagerUiState.testingKeyId == null

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onTestUnsavedApiKey(newKeyRawValue) },
                                            enabled = newKeyRawValue.isNotBlank() && !isTestingUnsaved,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("test_unsaved_api_key_button"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            if (isTestingUnsaved) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(15.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Testing...")
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.NetworkCheck,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Test Key")
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                val trimmed = newKeyRawValue.trim()
                                                if (trimmed.isNotEmpty()) {
                                                    onAddApiKey(
                                                        newKeyLabel.ifBlank { "Personal Key" },
                                                        trimmed,
                                                        true,
                                                        true
                                                    )
                                                    newKeyRawValue = ""
                                                    showNewKeyPlainText = false
                                                    showAddKeyForm = false
                                                }
                                            },
                                            enabled = newKeyRawValue.isNotBlank(),
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .testTag("save_new_api_key_button"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = studioColors.markOPrimary,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text("Save & Activate", fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    TextButton(
                                        onClick = {
                                            showAddKeyForm = false
                                            newKeyRawValue = ""
                                        },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        }

                        // Security Note
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(studioColors.surfaceCard)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = studioColors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Keys are encrypted locally with Android Keystore (AES-256-GCM) and masked after saving. Avoid saving production keys on shared devices.",
                                style = MaterialTheme.typography.labelSmall,
                                color = studioColors.textSecondary
                            )
                        }
                    }
                }
            }

            // 2. Curated Studio Color Palettes
            item {
                SettingsGroupCard(
                    title = "Visual Theme & Palette",
                    subtitle = "Choose your board surface and lighting mood"
                ) {
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
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) studioColors.accentGold else studioColors.borderSubtle
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
                                    border = BorderStroke(
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

            // 3. Audio & Haptics
            item {
                SettingsGroupCard(
                    title = "Sound & Haptics",
                    subtitle = "Acoustic chimes and physical touch feedback"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingSwitchRow(
                            title = "Sound Effects",
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
                                        text = "Volume",
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
                            title = "Ambient Studio Tone",
                            subtitle = "Gentle low-volume harmonic chord on new matches",
                            checked = preferences.musicEnabled,
                            onCheckedChange = onToggleMusic,
                            testTag = "toggle_music"
                        )

                        SettingSwitchRow(
                            title = "Haptic Feedback",
                            subtitle = "Subtle tactile impulse on moves and victories",
                            checked = preferences.hapticsEnabled,
                            onCheckedChange = onToggleHaptics,
                            testTag = "toggle_haptics"
                        )
                    }
                }
            }

            // 4. Board & Motion
            item {
                SettingsGroupCard(
                    title = "Board & Animation",
                    subtitle = "Customize stroke motion and grid labels"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingSwitchRow(
                            title = "Smooth Stroke Animations",
                            subtitle = "Animated drawing for X, O, and winning lines",
                            checked = preferences.animationsEnabled,
                            onCheckedChange = onToggleAnimations,
                            testTag = "toggle_animations"
                        )

                        SettingSwitchRow(
                            title = "Grid Coordinates (A1–C3)",
                            subtitle = "Show subtle coordinate notation inside empty cells",
                            checked = preferences.showCoordinates,
                            onCheckedChange = onToggleCoordinates,
                            testTag = "toggle_coordinates"
                        )
                    }
                }
            }

            // 5. Player Names
            item {
                SettingsGroupCard(
                    title = "Player Profiles",
                    subtitle = "Customize names shown on the scoreboard"
                ) {
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
                            Text("Save Names")
                        }
                    }
                }
            }
        }
    }

    // Replace / Edit Existing API Key Dialog
    val replaceTarget = apiConfigState.savedKeys.firstOrNull { it.id == keyBeingReplaced }
    if (replaceTarget != null) {
        AlertDialog(
            onDismissRequest = { keyBeingReplaced = null },
            title = {
                Text(
                    text = "Replace or Rename API Key",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Current key: ${replaceTarget.maskedPreview}. Other saved keys will not be affected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = studioColors.textSecondary
                    )
                    OutlinedTextField(
                        value = replaceLabelInput,
                        onValueChange = { replaceLabelInput = it },
                        label = { Text("Key Label") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("replace_api_key_label_input")
                    )
                    OutlinedTextField(
                        value = replaceRawKeyInput,
                        onValueChange = { replaceRawKeyInput = it },
                        label = { Text("New API Key (leave blank to keep existing key)") },
                        placeholder = { Text(replaceTarget.maskedPreview) },
                        singleLine = true,
                        visualTransformation = if (showReplacePlainText) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { showReplacePlainText = !showReplacePlainText }) {
                                Icon(
                                    imageVector = if (showReplacePlainText) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = "Toggle key visibility"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("replace_api_key_value_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReplaceSavedApiKey(
                            replaceTarget.id,
                            replaceLabelInput.ifBlank { replaceTarget.label },
                            replaceRawKeyInput.trim(),
                            replaceRawKeyInput.isNotBlank()
                        )
                        keyBeingReplaced = null
                    },
                    modifier = Modifier.testTag("confirm_replace_api_key_button")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { keyBeingReplaced = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete API Key Confirmation Dialog
    val deleteTarget = apiConfigState.savedKeys.firstOrNull { it.id == keyPendingDeletion }
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { keyPendingDeletion = null },
            title = {
                Text(
                    text = "Remove '${deleteTarget.label}'?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This removes ${deleteTarget.label} (${deleteTarget.maskedPreview}) from encrypted device storage. Your other saved keys will remain intact.",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSavedApiKey(deleteTarget.id)
                        keyPendingDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = studioColors.markXPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_api_key_button")
                ) {
                    Text("Remove Key")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { keyPendingDeletion = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SavedApiKeyCard(
    savedKey: SavedGeminiApiKey,
    isSelected: Boolean,
    isTesting: Boolean,
    onSelect: () -> Unit,
    onTest: () -> Unit,
    onEditReplace: () -> Unit,
    onDelete: () -> Unit
) {
    val studioColors = LocalStudioColors.current
    val statusColor = when (savedKey.status) {
        ApiKeyHealthStatus.VALID -> studioColors.markOPrimary
        ApiKeyHealthStatus.QUOTA_EXCEEDED -> studioColors.accentGold
        ApiKeyHealthStatus.INVALID -> studioColors.markXPrimary
        ApiKeyHealthStatus.UNAVAILABLE -> studioColors.accentGold
        ApiKeyHealthStatus.UNTESTED -> studioColors.textMuted
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSelect() }
            .testTag("saved_api_key_card_${savedKey.id}"),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) studioColors.surfaceElevated else studioColors.surfaceCard,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) studioColors.markOPrimary else studioColors.borderSubtle
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = onSelect,
                        modifier = Modifier.testTag("select_api_key_radio_${savedKey.id}")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = savedKey.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = studioColors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isSelected) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = studioColors.markOContainer
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = studioColors.markOPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = savedKey.maskedPreview,
                            style = MaterialTheme.typography.bodySmall,
                            color = studioColors.textSecondary,
                            modifier = Modifier.testTag("masked_key_preview_${savedKey.id}")
                        )
                    }
                }

                // Status Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = savedKey.status.displayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (!savedKey.statusMessage.isNullOrBlank()) {
                Text(
                    text = savedKey.statusMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = studioColors.textSecondary,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            // Action bar: Test Connection, Replace, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onTest,
                    enabled = !isTesting,
                    modifier = Modifier.testTag("test_saved_key_${savedKey.id}")
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Testing", style = MaterialTheme.typography.labelSmall)
                    } else {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = "Test API key",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test", style = MaterialTheme.typography.labelSmall)
                    }
                }

                TextButton(
                    onClick = onEditReplace,
                    modifier = Modifier.testTag("replace_saved_key_${savedKey.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Replace API key",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Replace", style = MaterialTheme.typography.labelSmall)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_saved_key_${savedKey.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove API key",
                        tint = studioColors.markXPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    val studioColors = LocalStudioColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = studioColors.surfaceElevated,
        border = BorderStroke(1.dp, studioColors.borderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = studioColors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = studioColors.textSecondary
                    )
                }
            }
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

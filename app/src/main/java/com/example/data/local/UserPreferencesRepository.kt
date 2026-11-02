package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.engine.AiDifficulty
import com.example.domain.engine.BoardRuleMode
import com.example.domain.engine.GameMode
import com.example.domain.engine.Mark
import com.example.ui.theme.StudioPalette
import com.example.ui.theme.ThemeModeOverride
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "studio_settings")

data class UserPreferences(
    val playerOneName: String = "Player 1",
    val playerTwoName: String = "Player 2",
    val preferredMark: Mark = Mark.X,
    val xGoesFirst: Boolean = true,
    val selectedGameMode: GameMode = GameMode.GEMINI,
    val selectedDifficulty: AiDifficulty = AiDifficulty.HARD,
    val selectedRuleMode: BoardRuleMode = BoardRuleMode.CLASSIC,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.8f,
    val musicEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val animationsEnabled: Boolean = true,
    val showCoordinates: Boolean = false,
    val studioPalette: StudioPalette = StudioPalette.ATELIER_IVORY,
    val themeModeOverride: ThemeModeOverride = ThemeModeOverride.LIGHT
)

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.dataStore)

    private object Keys {
        val PLAYER_ONE_NAME = stringPreferencesKey("player_one_name")
        val PLAYER_TWO_NAME = stringPreferencesKey("player_two_name")
        val PREFERRED_MARK = stringPreferencesKey("preferred_mark")
        val X_GOES_FIRST = booleanPreferencesKey("x_goes_first")
        val SELECTED_GAME_MODE = stringPreferencesKey("selected_game_mode")
        val SELECTED_DIFFICULTY = stringPreferencesKey("selected_difficulty")
        val SELECTED_RULE_MODE = stringPreferencesKey("selected_rule_mode")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val SOUND_VOLUME = floatPreferencesKey("sound_volume")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val SHOW_COORDINATES = booleanPreferencesKey("show_coordinates")
        val STUDIO_PALETTE = stringPreferencesKey("studio_palette")
        val THEME_MODE_OVERRIDE = stringPreferencesKey("theme_mode_override")
    }

    val preferencesFlow: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            playerOneName = prefs[Keys.PLAYER_ONE_NAME]?.ifBlank { "Player 1" } ?: "Player 1",
            playerTwoName = prefs[Keys.PLAYER_TWO_NAME]?.ifBlank { "Player 2" } ?: "Player 2",
            preferredMark = if (prefs[Keys.PREFERRED_MARK] == "O") Mark.O else Mark.X,
            xGoesFirst = prefs[Keys.X_GOES_FIRST] ?: true,
            selectedGameMode = GameMode.fromId(prefs[Keys.SELECTED_GAME_MODE] ?: GameMode.GEMINI.id),
            selectedDifficulty = AiDifficulty.fromId(prefs[Keys.SELECTED_DIFFICULTY] ?: AiDifficulty.HARD.id),
            selectedRuleMode = BoardRuleMode.fromId(prefs[Keys.SELECTED_RULE_MODE] ?: BoardRuleMode.CLASSIC.id),
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            soundVolume = (prefs[Keys.SOUND_VOLUME] ?: 0.8f).coerceIn(0f, 1f),
            musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: false,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            animationsEnabled = prefs[Keys.ANIMATIONS_ENABLED] ?: true,
            showCoordinates = prefs[Keys.SHOW_COORDINATES] ?: false,
            studioPalette = StudioPalette.fromId(prefs[Keys.STUDIO_PALETTE] ?: StudioPalette.ATELIER_IVORY.id),
            themeModeOverride = ThemeModeOverride.fromId(prefs[Keys.THEME_MODE_OVERRIDE] ?: ThemeModeOverride.LIGHT.id)
        )
    }

    suspend fun updatePlayerNames(playerOne: String, playerTwo: String) {
        dataStore.edit { prefs ->
            prefs[Keys.PLAYER_ONE_NAME] = playerOne.trim().ifBlank { "Player 1" }.take(20)
            prefs[Keys.PLAYER_TWO_NAME] = playerTwo.trim().ifBlank { "Player 2" }.take(20)
        }
    }

    suspend fun updatePreferredMark(mark: Mark) {
        dataStore.edit { it[Keys.PREFERRED_MARK] = mark.symbol }
    }

    suspend fun updateXGoesFirst(xFirst: Boolean) {
        dataStore.edit { it[Keys.X_GOES_FIRST] = xFirst }
    }

    suspend fun updateGameMode(mode: GameMode) {
        dataStore.edit { it[Keys.SELECTED_GAME_MODE] = mode.id }
    }

    suspend fun updateDifficulty(difficulty: AiDifficulty) {
        dataStore.edit { it[Keys.SELECTED_DIFFICULTY] = difficulty.id }
    }

    suspend fun updateRuleMode(ruleMode: BoardRuleMode) {
        dataStore.edit { it[Keys.SELECTED_RULE_MODE] = ruleMode.id }
    }

    suspend fun updateSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun updateSoundVolume(volume: Float) {
        dataStore.edit { it[Keys.SOUND_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun updateMusicEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.MUSIC_ENABLED] = enabled }
    }

    suspend fun updateHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun updateAnimationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ANIMATIONS_ENABLED] = enabled }
    }

    suspend fun updateShowCoordinates(enabled: Boolean) {
        dataStore.edit { it[Keys.SHOW_COORDINATES] = enabled }
    }

    suspend fun updateStudioPalette(palette: StudioPalette) {
        dataStore.edit { it[Keys.STUDIO_PALETTE] = palette.id }
    }

    suspend fun updateThemeModeOverride(mode: ThemeModeOverride) {
        dataStore.edit { it[Keys.THEME_MODE_OVERRIDE] = mode.id }
    }
}

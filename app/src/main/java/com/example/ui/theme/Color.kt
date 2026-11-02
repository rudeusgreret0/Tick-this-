package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class StudioPalette(
    val id: String,
    val displayName: String,
    val subtitle: String
) {
    ATELIER_IVORY(
        id = "ATELIER_IVORY",
        displayName = "Atelier Ivory",
        subtitle = "Warm alabaster stone, vermilion & viridian"
    ),
    NORDIC_SLATE(
        id = "NORDIC_SLATE",
        displayName = "Nordic Porcelain",
        subtitle = "Cool architectural limestone, ultramarine & amber"
    ),
    KYOTO_HINOKI(
        id = "KYOTO_HINOKI",
        displayName = "Kyoto Hinoki",
        subtitle = "Cypress wood grain, torii crimson & moss"
    ),
    OBSIDIAN_STUDIO(
        id = "OBSIDIAN_STUDIO",
        displayName = "Obsidian Studio",
        subtitle = "Matte basalt slate, ember coral & mint jade"
    );

    companion object {
        fun fromId(id: String): StudioPalette =
            entries.firstOrNull { it.id == id } ?: ATELIER_IVORY
    }
}

enum class ThemeModeOverride(val id: String, val label: String) {
    LIGHT("LIGHT", "Light Studio"),
    DARK("DARK", "Dark Studio"),
    SYSTEM("SYSTEM", "Match System");

    companion object {
        fun fromId(id: String): ThemeModeOverride =
            entries.firstOrNull { it.id == id } ?: LIGHT
    }
}

@Immutable
data class StudioColors(
    val backgroundPrimary: Color,
    val backgroundSecondary: Color,
    val surfaceElevated: Color,
    val surfaceCard: Color,
    val surfaceBoard: Color,
    val cellSurface: Color,
    val cellHoverSurface: Color,
    val gridLine: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val markXPrimary: Color,
    val markXContainer: Color,
    val markOPrimary: Color,
    val markOContainer: Color,
    val accentGold: Color,
    val accentGoldContainer: Color,
    val winStrikeColor: Color,
    val statusSuccess: Color,
    val statusWarning: Color,
    val isDark: Boolean
)

val AtelierIvoryLight = StudioColors(
    backgroundPrimary = Color(0xFFFAF6F0),
    backgroundSecondary = Color(0xFFF2ECE1),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceCard = Color(0xFFF5EFE6),
    surfaceBoard = Color(0xFFEDE5D8),
    cellSurface = Color(0xFFFDFBF7),
    cellHoverSurface = Color(0xFFF4EDE2),
    gridLine = Color(0xFFD6CBB9),
    borderSubtle = Color(0xFFE2D9C8),
    textPrimary = Color(0xFF1A1E26),
    textSecondary = Color(0xFF4A5260),
    textMuted = Color(0xFF7D8492),
    markXPrimary = Color(0xFFD95338),      // Sculpted Terracotta Vermilion
    markXContainer = Color(0xFFFAE6E1),
    markOPrimary = Color(0xFF1E7E75),      // Deep Viridian Teal
    markOContainer = Color(0xFFDFF1EF),
    accentGold = Color(0xFFB37D28),        // Brushed Architectural Brass
    accentGoldContainer = Color(0xFFF7EBD4),
    winStrikeColor = Color(0xFFB37D28),
    statusSuccess = Color(0xFF267A53),
    statusWarning = Color(0xFFC26A1B),
    isDark = false
)

val AtelierIvoryDark = StudioColors(
    backgroundPrimary = Color(0xFF13161D),
    backgroundSecondary = Color(0xFF191D26),
    surfaceElevated = Color(0xFF212632),
    surfaceCard = Color(0xFF1C212B),
    surfaceBoard = Color(0xFF171B24),
    cellSurface = Color(0xFF222836),
    cellHoverSurface = Color(0xFF2B3242),
    gridLine = Color(0xFF343C4F),
    borderSubtle = Color(0xFF2C3344),
    textPrimary = Color(0xFFF4EFE6),
    textSecondary = Color(0xFFB6BCC8),
    textMuted = Color(0xFF798193),
    markXPrimary = Color(0xFFEB6A50),
    markXContainer = Color(0xFF3B2221),
    markOPrimary = Color(0xFF3CC2B4),
    markOContainer = Color(0xFF193638),
    accentGold = Color(0xFFE0AC52),
    accentGoldContainer = Color(0xFF382F1E),
    winStrikeColor = Color(0xFFE0AC52),
    statusSuccess = Color(0xFF46B883),
    statusWarning = Color(0xFFE59849),
    isDark = true
)

val NordicSlateLight = StudioColors(
    backgroundPrimary = Color(0xFFF4F6F9),
    backgroundSecondary = Color(0xFFE9EDF2),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceCard = Color(0xFFEEF2F6),
    surfaceBoard = Color(0xFFE2E8F0),
    cellSurface = Color(0xFFFAFBFC),
    cellHoverSurface = Color(0xFFEDF2F7),
    gridLine = Color(0xFFCBD5E1),
    borderSubtle = Color(0xFFD8E0EA),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF64748B),
    markXPrimary = Color(0xFF2554C7),      // Cobalt Ultramarine
    markXContainer = Color(0xFFE0E8FA),
    markOPrimary = Color(0xFFC86D18),      // Burnished Amber Ochre
    markOContainer = Color(0xFFFAEBDC),
    accentGold = Color(0xFF334155),
    accentGoldContainer = Color(0xFFE2E8F0),
    winStrikeColor = Color(0xFF0F172A),
    statusSuccess = Color(0xFF15803D),
    statusWarning = Color(0xFFB45309),
    isDark = false
)

val KyotoHinokiLight = StudioColors(
    backgroundPrimary = Color(0xFFF7F2E7),
    backgroundSecondary = Color(0xFFEDE4D3),
    surfaceElevated = Color(0xFFFCF9F2),
    surfaceCard = Color(0xFFF1E9D8),
    surfaceBoard = Color(0xFFE5D9C3),
    cellSurface = Color(0xFFFBF7EE),
    cellHoverSurface = Color(0xFFF0E6D2),
    gridLine = Color(0xFFCBBBA0),
    borderSubtle = Color(0xFFD9CBB3),
    textPrimary = Color(0xFF231F1A),
    textSecondary = Color(0xFF574E42),
    textMuted = Color(0xFF857968),
    markXPrimary = Color(0xFFC43B28),      // Torii Vermilion
    markXContainer = Color(0xFFF7E2DD),
    markOPrimary = Color(0xFF366B48),      // Matcha Cypress Green
    markOContainer = Color(0xFFDCECE1),
    accentGold = Color(0xFF9C6B27),
    accentGoldContainer = Color(0xFFF2E4CE),
    winStrikeColor = Color(0xFF9C6B27),
    statusSuccess = Color(0xFF366B48),
    statusWarning = Color(0xFFB8621B),
    isDark = false
)

val LocalStudioColors = staticCompositionLocalOf { AtelierIvoryLight }

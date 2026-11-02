package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

fun resolveStudioColors(
    palette: StudioPalette,
    themeModeOverride: ThemeModeOverride,
    systemDark: Boolean
): StudioColors {
    val useDark = when (themeModeOverride) {
        ThemeModeOverride.LIGHT -> palette == StudioPalette.OBSIDIAN_STUDIO
        ThemeModeOverride.DARK -> true
        ThemeModeOverride.SYSTEM -> systemDark || palette == StudioPalette.OBSIDIAN_STUDIO
    }
    return if (useDark) {
        AtelierIvoryDark
    } else {
        when (palette) {
            StudioPalette.ATELIER_IVORY -> AtelierIvoryLight
            StudioPalette.NORDIC_SLATE -> NordicSlateLight
            StudioPalette.KYOTO_HINOKI -> KyotoHinokiLight
            StudioPalette.OBSIDIAN_STUDIO -> AtelierIvoryDark
        }
    }
}

@Composable
fun TicTacToeTheme(
    palette: StudioPalette = StudioPalette.ATELIER_IVORY,
    themeModeOverride: ThemeModeOverride = ThemeModeOverride.LIGHT,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val studioColors = resolveStudioColors(palette, themeModeOverride, systemDark)

    val colorScheme = if (studioColors.isDark) {
        darkColorScheme(
            primary = studioColors.markXPrimary,
            onPrimary = Color.White,
            primaryContainer = studioColors.markXContainer,
            onPrimaryContainer = studioColors.textPrimary,
            secondary = studioColors.markOPrimary,
            onSecondary = Color.White,
            secondaryContainer = studioColors.markOContainer,
            onSecondaryContainer = studioColors.textPrimary,
            tertiary = studioColors.accentGold,
            onTertiary = Color(0xFF1A150C),
            tertiaryContainer = studioColors.accentGoldContainer,
            onTertiaryContainer = studioColors.textPrimary,
            background = studioColors.backgroundPrimary,
            onBackground = studioColors.textPrimary,
            surface = studioColors.surfaceElevated,
            onSurface = studioColors.textPrimary,
            surfaceVariant = studioColors.surfaceCard,
            onSurfaceVariant = studioColors.textSecondary,
            outline = studioColors.borderSubtle,
            outlineVariant = studioColors.gridLine
        )
    } else {
        lightColorScheme(
            primary = studioColors.markXPrimary,
            onPrimary = Color.White,
            primaryContainer = studioColors.markXContainer,
            onPrimaryContainer = studioColors.textPrimary,
            secondary = studioColors.markOPrimary,
            onSecondary = Color.White,
            secondaryContainer = studioColors.markOContainer,
            onSecondaryContainer = studioColors.textPrimary,
            tertiary = studioColors.accentGold,
            onTertiary = Color.White,
            tertiaryContainer = studioColors.accentGoldContainer,
            onTertiaryContainer = studioColors.textPrimary,
            background = studioColors.backgroundPrimary,
            onBackground = studioColors.textPrimary,
            surface = studioColors.surfaceElevated,
            onSurface = studioColors.textPrimary,
            surfaceVariant = studioColors.surfaceCard,
            onSurfaceVariant = studioColors.textSecondary,
            outline = studioColors.borderSubtle,
            outlineVariant = studioColors.gridLine
        )
    }

    CompositionLocalProvider(LocalStudioColors provides studioColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = StudioShapes,
            content = content
        )
    }
}

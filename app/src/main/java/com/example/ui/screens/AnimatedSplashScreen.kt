package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StudioEmblemCanvas
import com.example.ui.theme.LocalStudioColors
import kotlinx.coroutines.delay

@Composable
fun AnimatedSplashScreen(
    onSplashFinished: () -> Unit
) {
    val studioColors = LocalStudioColors.current
    val emblemScale = remember { Animatable(0.82f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        emblemScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
        )
        delay(650L)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(studioColors.backgroundPrimary)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSplashFinished
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .alpha(contentAlpha.value)
        ) {
            StudioEmblemCanvas(
                modifier = Modifier
                    .size(116.dp)
                    .scale(emblemScale.value)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                color = studioColors.accentGoldContainer,
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "ATELIER XO • STUDIO EDITION",
                    style = MaterialTheme.typography.labelMedium,
                    color = studioColors.accentGold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            Text(
                text = "Tic Tac Toe",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.0).sp
                ),
                color = studioColors.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tactical Precision • Minimax & Gemini AI",
                style = MaterialTheme.typography.bodyMedium,
                color = studioColors.textSecondary
            )

            Spacer(modifier = Modifier.height(36.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "X",
                    style = MaterialTheme.typography.titleLarge,
                    color = studioColors.markXPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "•",
                    style = MaterialTheme.typography.titleMedium,
                    color = studioColors.gridLine
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "O",
                    style = MaterialTheme.typography.titleLarge,
                    color = studioColors.markOPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.Mark
import com.example.domain.engine.TicTacToeEngine
import com.example.domain.engine.WinningLine
import com.example.ui.theme.LocalStudioColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedMarkCanvas(
    mark: Mark,
    isWinningCell: Boolean,
    isAboutToVanish: Boolean,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val studioColors = LocalStudioColors.current
    val drawProgress = remember(mark) { Animatable(if (animationsEnabled) 0f else 1f) }

    LaunchedEffect(mark, animationsEnabled) {
        if (animationsEnabled) {
            drawProgress.snapTo(0f)
            drawProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
        } else {
            drawProgress.snapTo(1f)
        }
    }

    val primaryColor = when (mark) {
        Mark.X -> studioColors.markXPrimary
        Mark.O -> studioColors.markOPrimary
    }
    val effectiveAlpha = if (isAboutToVanish) 0.42f else 1f

    Canvas(modifier = modifier.fillMaxSize()) {
        val progress = drawProgress.value
        val strokeWidth = (size.minDimension * 0.145f).coerceAtLeast(8f)
        val inset = size.minDimension * 0.24f
        val left = inset
        val top = inset
        val right = size.width - inset
        val bottom = size.height - inset

        // Subtle tactile shadow offset
        val shadowColor = Color.Black.copy(alpha = 0.08f * effectiveAlpha)
        val shadowOffset = Offset(0f, strokeWidth * 0.22f)

        if (mark == Mark.X) {
            val firstStrokeProgress = (progress * 2f).coerceIn(0f, 1f)
            val secondStrokeProgress = ((progress - 0.5f) * 2f).coerceIn(0f, 1f)

            if (firstStrokeProgress > 0f) {
                val endX = left + (right - left) * firstStrokeProgress
                val endY = top + (bottom - top) * firstStrokeProgress
                drawLine(
                    color = shadowColor,
                    start = Offset(left, top) + shadowOffset,
                    end = Offset(endX, endY) + shadowOffset,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = primaryColor.copy(alpha = effectiveAlpha),
                    start = Offset(left, top),
                    end = Offset(endX, endY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            if (secondStrokeProgress > 0f) {
                val endX = right - (right - left) * secondStrokeProgress
                val endY = top + (bottom - top) * secondStrokeProgress
                drawLine(
                    color = shadowColor,
                    start = Offset(right, top) + shadowOffset,
                    end = Offset(endX, endY) + shadowOffset,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = primaryColor.copy(alpha = effectiveAlpha),
                    start = Offset(right, top),
                    end = Offset(endX, endY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        } else {
            val diameter = (right - left).coerceAtLeast(1f)
            val sweepAngle = 360f * progress
            drawArc(
                color = shadowColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(left, top) + shadowOffset,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = primaryColor.copy(alpha = effectiveAlpha),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(left, top),
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        if (isWinningCell && progress >= 0.95f) {
            drawCircle(
                color = studioColors.accentGold.copy(alpha = 0.14f),
                radius = size.minDimension * 0.42f,
                center = center
            )
        }
    }
}

@Composable
fun TacticalGameBoard(
    board: List<Mark?>,
    winningLine: WinningLine?,
    lastMoveCellIndex: Int?,
    hintCellIndex: Int?,
    nextVanishCellIndex: Int?,
    showCoordinates: Boolean,
    animationsEnabled: Boolean,
    enabled: Boolean,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val studioColors = LocalStudioColors.current
    val strikeProgress = remember(winningLine) {
        Animatable(if (winningLine != null && !animationsEnabled) 1f else 0f)
    }

    LaunchedEffect(winningLine, animationsEnabled) {
        if (winningLine != null) {
            if (animationsEnabled) {
                strikeProgress.snapTo(0f)
                strikeProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 260f)
                )
            } else {
                strikeProgress.snapTo(1f)
            }
        } else {
            strikeProgress.snapTo(0f)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "hint_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.16f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(studioColors.surfaceBoard)
            .border(1.5.dp, studioColors.borderSubtle, RoundedCornerShape(28.dp))
            .padding(14.dp)
            .testTag("game_board_container")
    ) {
        val spacing: Dp = 10.dp

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            for (row in 0 until 3) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    for (col in 0 until 3) {
                        val cellIndex = row * 3 + col
                        val mark = board.getOrNull(cellIndex)
                        val coord = TicTacToeEngine.coordinateForIndex(cellIndex)
                        val isWinningCell = winningLine?.indices?.contains(cellIndex) == true
                        val isLastMove = lastMoveCellIndex == cellIndex
                        val isHint = hintCellIndex == cellIndex && mark == null
                        val isAboutToVanish = nextVanishCellIndex == cellIndex && mark != null

                        val cellBackground = when {
                            isWinningCell && mark == Mark.X -> studioColors.markXContainer
                            isWinningCell && mark == Mark.O -> studioColors.markOContainer
                            isHint -> studioColors.accentGoldContainer
                            else -> studioColors.cellSurface
                        }

                        val cellBorderColor = when {
                            isWinningCell -> studioColors.accentGold
                            isHint -> studioColors.accentGold.copy(alpha = pulseAlpha)
                            isAboutToVanish -> studioColors.statusWarning.copy(alpha = 0.75f)
                            isLastMove -> studioColors.gridLine
                            else -> studioColors.borderSubtle.copy(alpha = 0.65f)
                        }

                        val cellDesc = buildString {
                            append("Cell $coord, ")
                            when (mark) {
                                Mark.X -> append("occupied by X")
                                Mark.O -> append("occupied by O")
                                null -> append("empty")
                            }
                            if (isAboutToVanish) append(", vanishes on next turn")
                            if (isHint) append(", recommended tactical move")
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(18.dp))
                                .background(cellBackground)
                                .border(
                                    width = if (isWinningCell || isHint || isAboutToVanish) 2.dp else 1.dp,
                                    color = cellBorderColor,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable(
                                    enabled = enabled && mark == null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true),
                                    onClick = { onCellClick(cellIndex) }
                                )
                                .semantics { contentDescription = cellDesc }
                                .testTag("board_cell_$cellIndex")
                        ) {
                            if (showCoordinates) {
                                Text(
                                    text = coord,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isHint) {
                                        studioColors.accentGold
                                    } else {
                                        studioColors.textMuted.copy(alpha = 0.55f)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 8.dp, top = 6.dp)
                                )
                            }

                            if (isAboutToVanish) {
                                Text(
                                    text = "FADING",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = studioColors.statusWarning,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 5.dp)
                                )
                            } else if (isHint) {
                                Text(
                                    text = "ADVISOR",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = studioColors.accentGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 6.dp)
                                )
                            }

                            if (isLastMove && mark != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (mark == Mark.X) studioColors.markXPrimary
                                            else studioColors.markOPrimary
                                        )
                                )
                            }

                            if (mark != null) {
                                AnimatedMarkCanvas(
                                    mark = mark,
                                    isWinningCell = isWinningCell,
                                    isAboutToVanish = isAboutToVanish,
                                    animationsEnabled = animationsEnabled
                                )
                            }
                        }
                    }
                }
            }
        }

        // Winning Strike Line Overlay & Subtle Victory Geometric Accents
        if (winningLine != null && winningLine.indices.size == 3) {
            val strikeColor = when (winningLine.winner) {
                Mark.X -> studioColors.markXPrimary
                Mark.O -> studioColors.markOPrimary
            }
            val goldAccent = studioColors.accentGold

            Canvas(modifier = Modifier.fillMaxSize()) {
                val progress = strikeProgress.value
                if (progress <= 0.01f) return@Canvas

                val firstIdx = winningLine.indices.first()
                val lastIdx = winningLine.indices.last()

                fun centerOfCell(idx: Int): Offset {
                    val r = idx / 3
                    val c = idx % 3
                    val cellW = size.width / 3f
                    val cellH = size.height / 3f
                    return Offset(
                        x = c * cellW + cellW / 2f,
                        y = r * cellH + cellH / 2f
                    )
                }

                val startCenter = centerOfCell(firstIdx)
                val endCenter = centerOfCell(lastIdx)
                val currentEnd = Offset(
                    x = startCenter.x + (endCenter.x - startCenter.x) * progress,
                    y = startCenter.y + (endCenter.y - startCenter.y) * progress
                )

                val lineStroke = (size.minDimension * 0.036f).coerceAtLeast(10f)

                // Outer warm halo
                drawLine(
                    color = goldAccent.copy(alpha = 0.35f),
                    start = startCenter,
                    end = currentEnd,
                    strokeWidth = lineStroke * 1.9f,
                    cap = StrokeCap.Round
                )

                // Core strike beam
                drawLine(
                    color = strikeColor,
                    start = startCenter,
                    end = currentEnd,
                    strokeWidth = lineStroke,
                    cap = StrokeCap.Round
                )

                // Subtle celebratory radial sparks around the 3 winning centers
                if (progress > 0.6f) {
                    val sparkPhase = ((progress - 0.6f) / 0.4f).coerceIn(0f, 1f)
                    for (cellIdx in winningLine.indices) {
                        val c = centerOfCell(cellIdx)
                        val baseRadius = size.minDimension * 0.11f * sparkPhase
                        for (angleDeg in 0 until 360 step 60) {
                            val rad = Math.toRadians(angleDeg.toDouble())
                            val p1 = Offset(
                                x = c.x + (baseRadius * 0.7f * cos(rad)).toFloat(),
                                y = c.y + (baseRadius * 0.7f * sin(rad)).toFloat()
                            )
                            val p2 = Offset(
                                x = c.x + (baseRadius * 1.15f * cos(rad)).toFloat(),
                                y = c.y + (baseRadius * 1.15f * sin(rad)).toFloat()
                            )
                            drawLine(
                                color = goldAccent.copy(alpha = (1f - sparkPhase * 0.5f)),
                                start = p1,
                                end = p2,
                                strokeWidth = 4f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom architectural studio emblem drawn with Compose Canvas for the Splash & Arena Header.
 */
@Composable
fun StudioEmblemCanvas(
    modifier: Modifier = Modifier
) {
    val studioColors = LocalStudioColors.current
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = size.minDimension

        // Soft rounded stone plaque
        drawRoundRect(
            color = studioColors.surfaceBoard,
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(minDim * 0.22f, minDim * 0.22f)
        )

        val pad = minDim * 0.18f
        val innerSize = minDim - pad * 2f
        val third = innerSize / 3f
        val gridStroke = (minDim * 0.025f).coerceAtLeast(3f)

        // Grid lines
        for (i in 1..2) {
            val offset = pad + third * i
            drawLine(
                color = studioColors.gridLine,
                start = Offset(offset, pad),
                end = Offset(offset, h - pad),
                strokeWidth = gridStroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = studioColors.gridLine,
                start = Offset(pad, offset),
                end = Offset(w - pad, offset),
                strokeWidth = gridStroke,
                cap = StrokeCap.Round
            )
        }

        // Draw sculpted X in top-left cell and center cell
        fun drawMiniX(row: Int, col: Int) {
            val cx = pad + col * third + third / 2f
            val cy = pad + row * third + third / 2f
            val r = third * 0.26f
            val s = (minDim * 0.048f).coerceAtLeast(4f)
            drawLine(
                color = studioColors.markXPrimary,
                start = Offset(cx - r, cy - r),
                end = Offset(cx + r, cy + r),
                strokeWidth = s,
                cap = StrokeCap.Round
            )
            drawLine(
                color = studioColors.markXPrimary,
                start = Offset(cx + r, cy - r),
                end = Offset(cx - r, cy + r),
                strokeWidth = s,
                cap = StrokeCap.Round
            )
        }

        // Draw sculpted O in top-right and bottom-center
        fun drawMiniO(row: Int, col: Int) {
            val cx = pad + col * third + third / 2f
            val cy = pad + row * third + third / 2f
            val r = third * 0.26f
            val s = (minDim * 0.048f).coerceAtLeast(4f)
            drawCircle(
                color = studioColors.markOPrimary,
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = s)
            )
        }

        drawMiniX(0, 0)
        drawMiniX(1, 1)
        drawMiniX(2, 2)
        drawMiniO(0, 2)
        drawMiniO(2, 0)

        // Diagonal brass strike
        drawLine(
            color = studioColors.accentGold.copy(alpha = 0.85f),
            start = Offset(pad + third * 0.3f, pad + third * 0.3f),
            end = Offset(w - pad - third * 0.3f, h - pad - third * 0.3f),
            strokeWidth = (minDim * 0.028f).coerceAtLeast(3f),
            cap = StrokeCap.Round
        )
    }
}

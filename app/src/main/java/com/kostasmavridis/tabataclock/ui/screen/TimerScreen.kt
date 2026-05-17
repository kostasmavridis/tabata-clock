package com.kostasmavridis.tabataclock.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kostasmavridis.tabataclock.model.TabataPhase
import com.kostasmavridis.tabataclock.ui.theme.PhaseColors
import com.kostasmavridis.tabataclock.viewmodel.TabataViewModel

@Composable
fun TimerScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: TabataViewModel = hiltViewModel()
) {
    val state    by viewModel.timerState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // ── Phase colours ────────────────────────────────────────────────────────────────────────
    val targetTop = when (state.phase) {
        TabataPhase.PREPARE -> PhaseColors.Prepare
        TabataPhase.WORK    -> PhaseColors.Work
        TabataPhase.REST    -> PhaseColors.Rest
        TabataPhase.DONE    -> PhaseColors.Done
    }
    val targetBot = when (state.phase) {
        TabataPhase.PREPARE -> PhaseColors.PrepareDark
        TabataPhase.WORK    -> PhaseColors.WorkDark
        TabataPhase.REST    -> PhaseColors.RestDark
        TabataPhase.DONE    -> PhaseColors.DoneDark
    }
    val topColor by animateColorAsState(targetTop, tween(500), label = "top")
    val botColor by animateColorAsState(targetBot, tween(500), label = "bot")

    // ── Progress arc ──────────────────────────────────────────────────────────────────────
    val arcProgress by animateFloatAsState(
        targetValue    = state.phaseProgress,
        animationSpec  = tween(900, easing = LinearEasing),
        label          = "arc"
    )

    // ── Pulse glow when running ─────────────────────────────────────────────────────────────
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue   = 0.15f,
        targetValue    = 0.45f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val glowAlpha = if (state.isRunning) pulseAlpha else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(listOf(topColor, botColor))
            )
    ) {
        // Settings icon
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White.copy(alpha = 0.8f)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Phase label with letter-spacing
            Text(
                text  = state.phase.label.uppercase(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    letterSpacing = 6.sp,
                    fontWeight    = FontWeight.Bold
                ),
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(Modifier.height(16.dp))

            // ── Countdown circle ───────────────────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .drawWithCache {
                        val stroke     = 16.dp.toPx()
                        val glowStroke = 32.dp.toPx()
                        val inset      = stroke / 2f
                        val arcSize    = Size(size.width - stroke, size.height - stroke)
                        val topLeft    = Offset(inset, inset)
                        onDrawBehind {
                            // Glow ring (pulse when running)
                            drawArc(
                                color      = Color.White.copy(alpha = glowAlpha),
                                startAngle = -90f,
                                sweepAngle = 360f * arcProgress,
                                useCenter  = false,
                                topLeft    = topLeft,
                                size       = arcSize,
                                style      = Stroke(width = glowStroke, cap = StrokeCap.Round)
                            )
                            // Track
                            drawArc(
                                color      = Color.White.copy(alpha = 0.12f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter  = false,
                                topLeft    = topLeft,
                                size       = arcSize,
                                style      = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            // Progress
                            drawArc(
                                color      = Color.White.copy(alpha = 0.90f),
                                startAngle = -90f,
                                sweepAngle = 360f * arcProgress,
                                useCenter  = false,
                                topLeft    = topLeft,
                                size       = arcSize,
                                style      = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.10f),
                                    Color.Black.copy(alpha = 0.35f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = "%02d".format(state.secondsLeft),
                        fontSize   = 88.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Round pips
            RoundPips(
                total        = settings.rounds,
                currentRound = state.currentRound,
                active       = state.phase != TabataPhase.PREPARE && state.phase != TabataPhase.DONE
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text  = "Round ${state.currentRound} / ${settings.rounds}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
            if (settings.sets > 1) {
                Text(
                    text  = "Set ${state.currentSet} / ${settings.sets}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(Modifier.height(44.dp))

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Reset
                IconButton(
                    onClick  = { viewModel.reset() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector        = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint               = Color.White,
                        modifier           = Modifier.size(26.dp)
                    )
                }

                // Play / Pause FAB
                FloatingActionButton(
                    onClick = {
                        when {
                            state.isRunning -> viewModel.pause()
                            state.isPaused  -> viewModel.resume()
                            else            -> viewModel.start()
                        }
                    },
                    modifier       = Modifier.size(80.dp),
                    shape          = CircleShape,
                    containerColor = Color.White,
                    elevation      = FloatingActionButtonDefaults.elevation(8.dp, 12.dp)
                ) {
                    Icon(
                        imageVector        = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isRunning) "Pause" else "Play",
                        tint               = topColor,
                        modifier           = Modifier.size(42.dp)
                    )
                }
            }
        }
    }
}

/** Small coloured dot per round — filled for completed, outlined for upcoming. */
@Composable
private fun RoundPips(
    total:        Int,
    currentRound: Int,
    active:       Boolean
) {
    val dotSize    = if (total <= 12) 10.dp else 7.dp
    val dotSpacing = if (total <= 12) 6.dp  else 4.dp
    Row(
        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        for (i in 1..minOf(total, 20)) {
            val isDone    = active && i < currentRound
            val isCurrent = active && i == currentRound
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrent -> Color.White
                            isDone    -> Color.White.copy(alpha = 0.55f)
                            else      -> Color.White.copy(alpha = 0.18f)
                        }
                    )
            )
        }
    }
}

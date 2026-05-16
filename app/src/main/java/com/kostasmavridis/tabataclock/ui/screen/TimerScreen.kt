package com.kostasmavridis.tabataclock.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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

    // ── Animated background colour ─────────────────────────────────────────
    val targetBgColor = when (state.phase) {
        TabataPhase.PREPARE -> PhaseColors.Prepare
        TabataPhase.WORK    -> PhaseColors.Work
        TabataPhase.REST    -> PhaseColors.Rest
        TabataPhase.DONE    -> PhaseColors.Done
    }
    val bgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 400),
        label = "bgColor"
    )

    // ── Animated progress arc ──────────────────────────────────────────────
    val arcProgress by animateFloatAsState(
        targetValue = state.phaseProgress,
        animationSpec = tween(durationMillis = 800),
        label = "arcProgress"
    )
    // Arc colour is a lighter tint of the background
    val arcColor by animateColorAsState(
        targetValue = Color.White.copy(alpha = 0.85f),
        animationSpec = tween(durationMillis = 400),
        label = "arcColor"
    )
    val arcTrackColor = Color.White.copy(alpha = 0.15f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Settings icon — top right
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Phase label
            Text(
                text = state.phase.label,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))

            // ── Countdown circle with progress arc overlay ─────────────────
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .drawWithCache {
                        val strokeWidth = 14.dp.toPx()
                        val inset = strokeWidth / 2f
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(inset, inset)
                        onDrawBehind {
                            // Track (full circle)
                            drawArc(
                                color = arcTrackColor,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            // Progress arc
                            drawArc(
                                color = arcColor,
                                startAngle = -90f,
                                sweepAngle = 360f * arcProgress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Inner filled circle
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "%02d".format(state.secondsLeft),
                        fontSize = 96.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Round / Set info
            Text(
                text = "Round ${state.currentRound} / ${settings.rounds}",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            if (settings.sets > 1) {
                Text(
                    text = "Set ${state.currentSet} / ${settings.sets}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(Modifier.height(48.dp))

            // Controls row
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset
                IconButton(
                    onClick = { viewModel.reset() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play / Pause — large FAB
                FloatingActionButton(
                    onClick = {
                        when {
                            state.isRunning -> viewModel.pause()
                            state.isPaused  -> viewModel.resume()
                            else            -> viewModel.start()
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    containerColor = Color.White
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isRunning) "Pause" else "Play",
                        tint = bgColor,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}

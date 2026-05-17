package com.kostasmavridis.tabataclock.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kostasmavridis.tabataclock.model.TabataPhase
import com.kostasmavridis.tabataclock.ui.theme.PhaseColors
import com.kostasmavridis.tabataclock.viewmodel.TabataViewModel
import kotlinx.coroutines.launch

@Composable
fun TimerScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: TabataViewModel = hiltViewModel()
) {
    val state    by viewModel.timerState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context  = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope    = rememberCoroutineScope()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.start()
        } else {
            viewModel.start()
            scope.launch {
                snackbarHostState.showSnackbar(
                    message  = "Timer running — notifications blocked. " +
                               "Enable in Settings to see phase updates when screen is off.",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    fun onStartTapped() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.start()
        }
    }

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

    val arcProgress by animateFloatAsState(
        targetValue   = state.phaseProgress,
        animationSpec = tween(900, easing = LinearEasing),
        label         = "arc"
    )

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue  = 0.15f,
        targetValue   = 0.45f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val glowAlpha = if (state.isRunning) pulseAlpha else 0f

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        modifier       = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(listOf(topColor, botColor)))
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Settings icon always top-end regardless of orientation
            IconButton(
                onClick  = onNavigateToSettings,
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

            if (isLandscape) {
                TimerContentLandscape(
                    state         = state,
                    settings      = settings,
                    topColor      = topColor,
                    arcProgress   = arcProgress,
                    glowAlpha     = glowAlpha,
                    onStartTapped = ::onStartTapped,
                    onPause       = { viewModel.pause() },
                    onResume      = { viewModel.resume() },
                    onReset       = { viewModel.reset() }
                )
            } else {
                TimerContentPortrait(
                    state         = state,
                    settings      = settings,
                    topColor      = topColor,
                    arcProgress   = arcProgress,
                    glowAlpha     = glowAlpha,
                    onStartTapped = ::onStartTapped,
                    onPause       = { viewModel.pause() },
                    onResume      = { viewModel.resume() },
                    onReset       = { viewModel.reset() }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Portrait layout
// ---------------------------------------------------------------------------

@Composable
private fun TimerContentPortrait(
    state        : TabataViewModel.TimerState,
    settings     : com.kostasmavridis.tabataclock.model.TabataSettings,
    topColor     : Color,
    arcProgress  : Float,
    glowAlpha    : Float,
    onStartTapped: () -> Unit,
    onPause      : () -> Unit,
    onResume     : () -> Unit,
    onReset      : () -> Unit
) {
    // fillMaxSize Box provides the BoxScope needed to centre the Column.
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier            = Modifier.padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhaseLabel(state.phase)
            Spacer(Modifier.height(16.dp))
            TimerArc(
                arcSize     = 260.dp,
                digitSize   = 88,
                arcProgress = arcProgress,
                glowAlpha   = glowAlpha,
                secondsLeft = state.secondsLeft
            )
            Spacer(Modifier.height(20.dp))
            RoundPips(
                total        = settings.rounds,
                currentRound = state.currentRound,
                active       = state.phase != TabataPhase.PREPARE && state.phase != TabataPhase.DONE
            )
            Spacer(Modifier.height(10.dp))
            RoundSetLabels(state = state, settings = settings)
            Spacer(Modifier.height(44.dp))
            TimerControls(
                isRunning     = state.isRunning,
                isPaused      = state.isPaused,
                topColor      = topColor,
                onStartTapped = onStartTapped,
                onPause       = onPause,
                onResume      = onResume,
                onReset       = onReset
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Landscape layout
// ---------------------------------------------------------------------------

@Composable
private fun TimerContentLandscape(
    state        : TabataViewModel.TimerState,
    settings     : com.kostasmavridis.tabataclock.model.TabataSettings,
    topColor     : Color,
    arcProgress  : Float,
    glowAlpha    : Float,
    onStartTapped: () -> Unit,
    onPause      : () -> Unit,
    onResume     : () -> Unit,
    onReset      : () -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // Left: arc + countdown number
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier.weight(1f)
        ) {
            TimerArc(
                arcSize     = 200.dp,
                digitSize   = 64,
                arcProgress = arcProgress,
                glowAlpha   = glowAlpha,
                secondsLeft = state.secondsLeft
            )
        }

        // Right: phase label, pips, labels, controls
        Column(
            modifier            = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PhaseLabel(state.phase)
            Spacer(Modifier.height(12.dp))
            RoundPips(
                total        = settings.rounds,
                currentRound = state.currentRound,
                active       = state.phase != TabataPhase.PREPARE && state.phase != TabataPhase.DONE
            )
            Spacer(Modifier.height(8.dp))
            RoundSetLabels(state = state, settings = settings)
            Spacer(Modifier.height(24.dp))
            TimerControls(
                isRunning     = state.isRunning,
                isPaused      = state.isPaused,
                topColor      = topColor,
                onStartTapped = onStartTapped,
                onPause       = onPause,
                onResume      = onResume,
                onReset       = onReset
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Shared sub-composables
// ---------------------------------------------------------------------------

@Composable
private fun PhaseLabel(phase: TabataPhase) {
    Text(
        text  = phase.label.uppercase(),
        style = MaterialTheme.typography.headlineSmall.copy(
            letterSpacing = 6.sp,
            fontWeight    = FontWeight.Bold
        ),
        color = Color.White.copy(alpha = 0.85f)
    )
}

@Composable
private fun TimerArc(
    arcSize     : Dp,
    digitSize   : Int,
    arcProgress : Float,
    glowAlpha   : Float,
    secondsLeft : Int
) {
    val innerSize = arcSize - 60.dp
    Box(
        modifier = Modifier
            .size(arcSize)
            .drawWithCache {
                val stroke     = 16.dp.toPx()
                val glowStroke = 32.dp.toPx()
                val inset      = stroke / 2f
                val arcSz      = Size(size.width - stroke, size.height - stroke)
                val topLeft    = Offset(inset, inset)
                onDrawBehind {
                    drawArc(
                        color      = Color.White.copy(alpha = glowAlpha),
                        startAngle = -90f,
                        sweepAngle = 360f * arcProgress,
                        useCenter  = false,
                        topLeft    = topLeft,
                        size       = arcSz,
                        style      = Stroke(width = glowStroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color      = Color.White.copy(alpha = 0.12f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter  = false,
                        topLeft    = topLeft,
                        size       = arcSz,
                        style      = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color      = Color.White.copy(alpha = 0.90f),
                        startAngle = -90f,
                        sweepAngle = 360f * arcProgress,
                        useCenter  = false,
                        topLeft    = topLeft,
                        size       = arcSz,
                        style      = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(innerSize)
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
                text       = "%02d".format(secondsLeft),
                fontSize   = digitSize.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )
        }
    }
}

@Composable
private fun RoundSetLabels(
    state    : TabataViewModel.TimerState,
    settings : com.kostasmavridis.tabataclock.model.TabataSettings
) {
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
}

@Composable
private fun TimerControls(
    isRunning     : Boolean,
    isPaused      : Boolean,
    topColor      : Color,
    onStartTapped : () -> Unit,
    onPause       : () -> Unit,
    onResume      : () -> Unit,
    onReset       : () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        IconButton(
            onClick  = onReset,
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

        FloatingActionButton(
            onClick = {
                when {
                    isRunning -> onPause()
                    isPaused  -> onResume()
                    else      -> onStartTapped()
                }
            },
            modifier       = Modifier.size(80.dp),
            shape          = CircleShape,
            containerColor = Color.White,
            elevation      = FloatingActionButtonDefaults.elevation(8.dp, 12.dp)
        ) {
            Icon(
                imageVector        = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isRunning) "Pause" else "Play",
                tint               = topColor,
                modifier           = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
private fun RoundPips(
    total        : Int,
    currentRound : Int,
    active       : Boolean
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

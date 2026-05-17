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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kostasmavridis.tabataclock.model.TabataPhase
import com.kostasmavridis.tabataclock.model.accentColor
import com.kostasmavridis.tabataclock.model.gradientColors
import com.kostasmavridis.tabataclock.viewmodel.TabataViewModel
import kotlinx.coroutines.launch

private val BgBase = Color(0xFF06060F)

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
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Reinitialise SoundPool every time this screen re-enters Resumed state.
    // Covers both app-foreground and back-navigation from SettingsScreen.
    LifecycleResumeEffect(Unit) {
        viewModel.onScreenResumed()
        onPauseOrDispose { /* nothing to clean up */ }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.start()
        else {
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

    val accent by animateColorAsState(
        targetValue   = state.phase.accentColor(),
        animationSpec = tween(500),
        label         = "accent"
    )
    val (blobVivid, _) = state.phase.gradientColors()
    val blobColor by animateColorAsState(
        targetValue   = blobVivid,
        animationSpec = tween(600),
        label         = "blob"
    )
    val arcProgress by animateFloatAsState(
        targetValue   = state.phaseProgress,
        animationSpec = tween(900, easing = LinearEasing),
        label         = "arc"
    )
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue  = 0.25f,
        targetValue   = 0.60f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val glowAlpha = if (state.isRunning) pulseAlpha else 0.25f

    var flashAlpha by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state.phase) { flashAlpha = 0.55f }
    val animatedFlash by animateFloatAsState(
        targetValue      = flashAlpha,
        animationSpec    = tween(450, easing = LinearOutSlowInEasing),
        finishedListener = { flashAlpha = 0f },
        label            = "flash"
    )

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        modifier       = Modifier
            .fillMaxSize()
            .background(BgBase)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            BlobBackground(blobColor = blobColor)

            if (isLandscape) {
                TimerContentLandscape(
                    state = state, settings = settings, accent = accent,
                    arcProgress = arcProgress, glowAlpha = glowAlpha,
                    onStartTapped = ::onStartTapped,
                    onPause  = { viewModel.pause() },
                    onResume = { viewModel.resume() },
                    onReset  = { viewModel.reset() },
                    onSkip   = { viewModel.skip() }
                )
            } else {
                TimerContentPortrait(
                    state = state, settings = settings, accent = accent,
                    arcProgress = arcProgress, glowAlpha = glowAlpha,
                    onStartTapped = ::onStartTapped,
                    onPause  = { viewModel.pause() },
                    onResume = { viewModel.resume() },
                    onReset  = { viewModel.reset() },
                    onSkip   = { viewModel.skip() }
                )
            }

            if (animatedFlash > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accent.copy(alpha = animatedFlash * 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            if (state.phase == TabataPhase.DONE) {
                DoneOverlay(settings = settings, onRestart = { viewModel.reset() })
            }

            TopBar(
                accent               = accent,
                onNavigateToSettings = onNavigateToSettings,
                modifier             = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Blob background — extracted so SettingsScreen can reuse it
// ---------------------------------------------------------------------------

@Composable
internal fun BlobBackground(
    blobColor : Color,
    modifier  : Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blobColor.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.20f),
                        radius = size.width * 0.70f
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.20f),
                    radius = size.width * 0.70f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blobColor.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.82f),
                        radius = size.width * 0.55f
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.82f),
                    radius = size.width * 0.55f
                )
            }
    )
}

// ---------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------

@Composable
private fun TopBar(
    accent               : Color,
    onNavigateToSettings : () -> Unit,
    modifier             : Modifier = Modifier
) {
    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row {
            Text(
                text          = "TABATA",
                fontWeight    = FontWeight.ExtraBold,
                fontSize      = 18.sp,
                letterSpacing = 2.sp,
                color         = Color.White
            )
            Text(
                text          = "CLOCK",
                fontWeight    = FontWeight.ExtraBold,
                fontSize      = 18.sp,
                letterSpacing = 2.sp,
                color         = accent
            )
        }
        TextButton(
            onClick  = onNavigateToSettings,
            shape    = RoundedCornerShape(50),
            colors   = ButtonDefaults.textButtonColors(
                contentColor = Color.White.copy(alpha = 0.75f)
            ),
            modifier = Modifier
                .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(50))
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint     = Color.White.copy(alpha = 0.70f)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text          = "SETTINGS",
                fontSize      = 11.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Done overlay
// ---------------------------------------------------------------------------

@Composable
private fun DoneOverlay(
    settings  : com.kostasmavridis.tabataclock.model.TabataSettings,
    onRestart : () -> Unit
) {
    val totalWorkSecs = settings.workSecs * settings.rounds * settings.sets
    val minutes = totalWorkSecs / 60
    val seconds = totalWorkSecs % 60
    val accent  = TabataPhase.DONE.accentColor()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF9B59B6).copy(alpha = 0.30f),
                        BgBase.copy(alpha = 0.92f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("\uD83D\uDD25", fontSize = 64.sp)
            Text(
                "Session Complete",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp
                ),
                color = accent
            )
            Text(
                "${settings.rounds * settings.sets} rounds · ${minutes}m ${seconds}s work",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = onRestart,
                shape    = CircleShape,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = accent, contentColor = Color.White
                ),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Text(
                    "Start Again", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 16.dp)
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
    accent       : Color,
    arcProgress  : Float,
    glowAlpha    : Float,
    onStartTapped: () -> Unit,
    onPause      : () -> Unit,
    onResume     : () -> Unit,
    onReset      : () -> Unit,
    onSkip       : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top    = 68.dp,
                bottom = WindowInsets.navigationBars
                    .asPaddingValues().calculateBottomPadding().coerceAtLeast(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PhaseLabel(phase = state.phase, accent = accent)
            Spacer(Modifier.height(16.dp))
            TimerArc(
                arcSize = 260.dp, digitSize = 88,
                arcProgress = arcProgress, glowAlpha = glowAlpha,
                accent = accent, secondsLeft = state.secondsLeft, phase = state.phase
            )
            Spacer(Modifier.height(24.dp))
            RoundPips(
                total = settings.rounds, currentRound = state.currentRound,
                accent = accent,
                active = state.phase != TabataPhase.PREPARE && state.phase != TabataPhase.DONE
            )
            Spacer(Modifier.height(16.dp))
            MetaChips(state = state, settings = settings, accent = accent)
            Spacer(Modifier.height(40.dp))
            TimerControls(
                isRunning = state.isRunning, isPaused = state.isPaused, accent = accent,
                onStartTapped = onStartTapped, onPause = onPause,
                onResume = onResume, onReset = onReset, onSkip = onSkip
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
    accent       : Color,
    arcProgress  : Float,
    glowAlpha    : Float,
    onStartTapped: () -> Unit,
    onPause      : () -> Unit,
    onResume     : () -> Unit,
    onReset      : () -> Unit,
    onSkip       : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            TimerArc(
                arcSize = 200.dp, digitSize = 64,
                arcProgress = arcProgress, glowAlpha = glowAlpha,
                accent = accent, secondsLeft = state.secondsLeft, phase = state.phase
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PhaseLabel(phase = state.phase, accent = accent)
            Spacer(Modifier.height(12.dp))
            RoundPips(
                total = settings.rounds, currentRound = state.currentRound,
                accent = accent,
                active = state.phase != TabataPhase.PREPARE && state.phase != TabataPhase.DONE
            )
            Spacer(Modifier.height(10.dp))
            MetaChips(state = state, settings = settings, accent = accent)
            Spacer(Modifier.height(24.dp))
            TimerControls(
                isRunning = state.isRunning, isPaused = state.isPaused, accent = accent,
                onStartTapped = onStartTapped, onPause = onPause,
                onResume = onResume, onReset = onReset, onSkip = onSkip
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Shared sub-composables
// ---------------------------------------------------------------------------

@Composable
private fun PhaseLabel(phase: TabataPhase, accent: Color) {
    Text(
        text  = phase.label.uppercase(),
        style = MaterialTheme.typography.headlineSmall.copy(
            letterSpacing = 6.sp, fontWeight = FontWeight.Bold
        ),
        color = accent
    )
}

@Composable
private fun TimerArc(
    arcSize     : Dp,
    digitSize   : Int,
    arcProgress : Float,
    glowAlpha   : Float,
    accent      : Color,
    secondsLeft : Int,
    phase       : TabataPhase
) {
    val innerSize = arcSize - 60.dp
    Box(
        modifier = Modifier
            .size(arcSize)
            .drawWithCache {
                val stroke     = 14.dp.toPx()
                val glowStroke = 48.dp.toPx()
                val inset      = stroke / 2f
                val arcSz      = Size(size.width - stroke, size.height - stroke)
                val topLeft    = Offset(inset, inset)
                onDrawBehind {
                    drawArc(
                        color = Color.White.copy(alpha = 0.07f),
                        startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        topLeft = topLeft, size = arcSz,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = accent.copy(alpha = glowAlpha * 0.55f),
                        startAngle = -90f, sweepAngle = 360f * arcProgress, useCenter = false,
                        topLeft = topLeft, size = arcSz,
                        style = Stroke(width = glowStroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = accent,
                        startAngle = -90f, sweepAngle = 360f * arcProgress, useCenter = false,
                        topLeft = topLeft, size = arcSz,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
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
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.Black.copy(alpha = 0.30f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "%02d".format(secondsLeft),
                    fontSize = digitSize.sp, fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                if (phase != TabataPhase.DONE) {
                    Text(
                        phase.label.uppercase(),
                        fontSize = (digitSize * 0.17f).sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 3.sp,
                        color = Color.White.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaChips(
    state    : TabataViewModel.TimerState,
    settings : com.kostasmavridis.tabataclock.model.TabataSettings,
    accent   : Color
) {
    val totalRounds = settings.rounds * settings.sets
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        MetaChip("Round", state.currentRound, settings.rounds, accent)
        if (settings.sets > 1) MetaChip("Set", state.currentSet, settings.sets, accent)
        MetaChip("Total", state.totalRoundsCompleted, totalRounds, accent)
    }
}

@Composable
private fun MetaChip(label: String, value: Int, total: Int, accent: Color) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$label ", fontSize = 13.sp, color = Color.White.copy(alpha = 0.55f))
            Text("$value",  fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text("/$total", fontSize = 13.sp, color = Color.White.copy(alpha = 0.40f))
        }
    }
}

@Composable
private fun TimerControls(
    isRunning     : Boolean,
    isPaused      : Boolean,
    accent        : Color,
    onStartTapped : () -> Unit,
    onPause       : () -> Unit,
    onResume      : () -> Unit,
    onReset       : () -> Unit,
    onSkip        : () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        SmallControlButton(onReset, "Reset", Icons.Default.Refresh)

        FloatingActionButton(
            onClick = {
                when {
                    isRunning -> onPause()
                    isPaused  -> onResume()
                    else      -> onStartTapped()
                }
            },
            modifier = Modifier
                .size(80.dp)
                .drawBehind {
                    val r = size.minDimension / 2f
                    drawCircle(color = accent.copy(alpha = 0.35f), radius = r + 18.dp.toPx())
                    drawCircle(color = accent.copy(alpha = 0.18f), radius = r + 30.dp.toPx())
                }
                .drawWithCache {
                    onDrawBehind {
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    accent,
                                    accent.copy(
                                        red   = (accent.red   * 0.65f).coerceIn(0f, 1f),
                                        green = (accent.green * 0.65f).coerceIn(0f, 1f),
                                        blue  = (accent.blue  * 0.65f).coerceIn(0f, 1f)
                                    )
                                ),
                                start = Offset(0f, 0f),
                                end   = Offset(size.width, size.height)
                            )
                        )
                    }
                },
            shape          = CircleShape,
            containerColor = Color.Transparent,
            elevation      = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
        ) {
            Icon(
                if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isRunning) "Pause" else "Play",
                tint = Color.White, modifier = Modifier.size(42.dp)
            )
        }

        SmallControlButton(onSkip, "Skip", Icons.Default.SkipNext)
    }
}

@Composable
private fun SmallControlButton(
    onClick            : () -> Unit,
    contentDescription : String,
    icon               : androidx.compose.ui.graphics.vector.ImageVector
) {
    IconButton(
        onClick  = onClick,
        modifier = Modifier
            .size(52.dp)
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .background(Color.White.copy(alpha = 0.08f), CircleShape)
    ) {
        Icon(
            icon, contentDescription = contentDescription,
            tint = Color.White.copy(alpha = 0.80f), modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun RoundPips(
    total        : Int,
    currentRound : Int,
    accent       : Color,
    active       : Boolean
) {
    val maxVisible   = 20
    val showOverflow = total > maxVisible
    val displayCount = if (showOverflow) maxVisible else total
    val baseDotSize  = if (total <= 12) 10.dp else 7.dp
    val dotSpacing   = if (total <= 12) 6.dp  else 4.dp
    Row(
        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        for (i in 1..displayCount) {
            val isDone    = active && i < currentRound
            val isCurrent = active && i == currentRound
            val scale by animateFloatAsState(
                targetValue   = if (isCurrent) 1.45f else 1.0f,
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                label         = "pipScale$i"
            )
            val dotSize = baseDotSize * scale
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .then(
                        if (isCurrent) Modifier.drawBehind {
                            drawCircle(accent.copy(alpha = 0.55f), size.minDimension / 2f + 6.dp.toPx())
                            drawCircle(accent.copy(alpha = 0.25f), size.minDimension / 2f + 12.dp.toPx())
                        } else Modifier
                    )
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrent -> accent
                            isDone    -> Color.White.copy(alpha = 0.55f)
                            else      -> Color.White.copy(alpha = 0.15f)
                        }
                    )
            )
        }
        if (showOverflow) {
            Text("\u2026", color = Color.White.copy(alpha = 0.45f),
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

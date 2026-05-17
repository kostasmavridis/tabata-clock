package com.kostasmavridis.tabataclock.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kostasmavridis.tabataclock.BuildConfig
import com.kostasmavridis.tabataclock.debug.debugActions
import com.kostasmavridis.tabataclock.ui.theme.PhaseColors
import com.kostasmavridis.tabataclock.viewmodel.TabataViewModel

// Same base as TimerScreen
private val BgBase = Color(0xFF06060F)

// Neutral blue-grey blobs — not phase-specific since settings is phase-neutral
private val SettingsBlobColor = Color(0xFF2A3A6E)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: TabataViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase)
    ) {
        // Dual blob background — same visual language as TimerScreen
        BlobBackground(blobColor = SettingsBlobColor)

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                SettingsTopBar(onBack = onBack)
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingsSectionHeader("Durations")

                SettingsCard {
                    DurationSetting(
                        label    = "Prepare",
                        icon     = Icons.Default.HourglassTop,
                        iconTint = PhaseColors.Prepare,
                        value    = settings.prepareSecs,
                        range    = 5..60,
                        unit     = "s",
                        onChange = { viewModel.updateSettings(settings.copy(prepareSecs = it)) }
                    )
                    SettingsDivider()
                    DurationSetting(
                        label    = "Work",
                        icon     = Icons.Default.Whatshot,
                        iconTint = PhaseColors.Work,
                        value    = settings.workSecs,
                        range    = 10..120,
                        unit     = "s",
                        onChange = { viewModel.updateSettings(settings.copy(workSecs = it)) }
                    )
                    SettingsDivider()
                    DurationSetting(
                        label    = "Rest",
                        icon     = Icons.Default.AcUnit,
                        iconTint = PhaseColors.Rest,
                        value    = settings.restSecs,
                        range    = 5..60,
                        unit     = "s",
                        onChange = { viewModel.updateSettings(settings.copy(restSecs = it)) }
                    )
                }

                SettingsSectionHeader("Structure")

                SettingsCard {
                    StepSetting(
                        label    = "Rounds",
                        icon     = Icons.Default.Repeat,
                        iconTint = Color(0xFFFFB300),
                        value    = settings.rounds,
                        range    = 1..20,
                        onChange = { viewModel.updateSettings(settings.copy(rounds = it)) }
                    )
                    SettingsDivider()
                    StepSetting(
                        label    = "Sets",
                        icon     = Icons.Default.LayersClear,
                        iconTint = Color(0xFF7C4DFF),
                        value    = settings.sets,
                        range    = 1..10,
                        onChange = { viewModel.updateSettings(settings.copy(sets = it)) }
                    )
                }

                if (BuildConfig.DEBUG) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    SettingsSectionHeader("Debug")
                    SettingsCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.BugReport, null,
                                    tint     = Color(0xFFFF5252),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        "Export logs",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Text(
                                        "Share logcat via share sheet",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.45f)
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = { debugActions.exportLogs(context) },
                                colors  = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFFF5252).copy(alpha = 0.18f),
                                    contentColor   = Color(0xFFFF5252)
                                )
                            ) { Text("Share") }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Top bar — matches TimerScreen pattern (logo left, pill badge right)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = {
            Row {
                Text(
                    "TABATA",
                    fontWeight    = FontWeight.ExtraBold,
                    fontSize      = 18.sp,
                    letterSpacing = 2.sp,
                    color         = Color.White
                )
                Text(
                    "CLOCK",
                    fontWeight    = FontWeight.ExtraBold,
                    fontSize      = 18.sp,
                    letterSpacing = 2.sp,
                    // Use the neutral blob colour as a static accent on Settings
                    color         = SettingsBlobColor.copy(alpha = 1f)
                        .let { Color(0xFF4F73D9) }  // brighter readable variant
                )
            }
        },
        navigationIcon = {
            TextButton(
                onClick  = onBack,
                shape    = RoundedCornerShape(50),
                colors   = ButtonDefaults.textButtonColors(
                    contentColor = Color.White.copy(alpha = 0.75f)
                ),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(50))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, null,
                    modifier = Modifier.size(14.dp),
                    tint     = Color.White.copy(alpha = 0.70f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "BACK",
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor    = Color.Transparent,
            titleContentColor = Color.White
        )
    )
}

// ---------------------------------------------------------------------------
// Cards — glassmorphism style (border + translucent bg)
// ---------------------------------------------------------------------------

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text.uppercase(),
        style    = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 2.sp,
            color         = Color.White.copy(alpha = 0.45f)
        ),
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            )
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        content = content
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(vertical = 2.dp),
        color     = Color.White.copy(alpha = 0.06f),
        thickness = 1.dp
    )
}

// ---------------------------------------------------------------------------
// Settings rows (unchanged behaviour)
// ---------------------------------------------------------------------------

@Composable
private fun DurationSetting(
    label    : String,
    icon     : ImageVector,
    iconTint : Color,
    value    : Int,
    range    : IntRange,
    unit     : String,
    onChange : (Int) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
                Text(label, style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.85f))
            }
            Text(
                "$value$unit",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = iconTint
            )
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value         = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange    = range.first.toFloat()..range.last.toFloat(),
            steps         = range.last - range.first - 1,
            colors        = SliderDefaults.colors(
                thumbColor         = iconTint,
                activeTrackColor   = iconTint.copy(alpha = 0.85f),
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
private fun StepSetting(
    label    : String,
    icon     : ImageVector,
    iconTint : Color,
    value    : Int,
    range    : IntRange,
    onChange : (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.85f))
        }
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilledIconButton(
                onClick  = { onChange(value - 1) },
                enabled  = value > range.first,
                modifier = Modifier.size(36.dp),
                colors   = IconButtonDefaults.filledIconButtonColors(
                    containerColor = iconTint.copy(alpha = 0.18f),
                    contentColor   = iconTint
                )
            ) {
                Icon(Icons.Default.Remove, "Decrease", modifier = Modifier.size(18.dp))
            }
            Text(
                value.toString(),
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                modifier   = Modifier.widthIn(min = 36.dp)
            )
            FilledIconButton(
                onClick  = { onChange(value + 1) },
                enabled  = value < range.last,
                modifier = Modifier.size(36.dp),
                colors   = IconButtonDefaults.filledIconButtonColors(
                    containerColor = iconTint.copy(alpha = 0.18f),
                    contentColor   = iconTint
                )
            ) {
                Icon(Icons.Default.Add, "Increase", modifier = Modifier.size(18.dp))
            }
        }
    }
}

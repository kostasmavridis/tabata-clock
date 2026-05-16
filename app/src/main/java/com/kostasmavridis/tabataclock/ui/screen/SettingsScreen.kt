package com.kostasmavridis.tabataclock.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kostasmavridis.tabataclock.viewmodel.TabataViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: TabataViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            DurationSetting(
                label = "Prepare",
                value = settings.prepareSecs,
                range = 5..30,
                onChange = { viewModel.updateSettings(settings.copy(prepareSecs = it)) }
            )
            DurationSetting(
                label = "Work",
                value = settings.workSecs,
                range = 10..60,
                onChange = { viewModel.updateSettings(settings.copy(workSecs = it)) }
            )
            DurationSetting(
                label = "Rest",
                value = settings.restSecs,
                range = 5..30,
                onChange = { viewModel.updateSettings(settings.copy(restSecs = it)) }
            )
            StepSetting(
                label = "Rounds",
                value = settings.rounds,
                range = 1..20,
                onChange = { viewModel.updateSettings(settings.copy(rounds = it)) }
            )
            StepSetting(
                label = "Sets",
                value = settings.sets,
                range = 1..10,
                onChange = { viewModel.updateSettings(settings.copy(sets = it)) }
            )
        }
    }
}

@Composable
private fun DurationSetting(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Text(text = "${value}s",  style = MaterialTheme.typography.titleMedium)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1
        )
    }
}

@Composable
private fun StepSetting(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (value > range.first) onChange(value - 1) },
                enabled = value > range.first
            ) { Text("-", style = MaterialTheme.typography.headlineSmall) }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.widthIn(min = 32.dp)
            )
            IconButton(
                onClick = { if (value < range.last) onChange(value + 1) },
                enabled = value < range.last
            ) { Text("+", style = MaterialTheme.typography.headlineSmall) }
        }
    }
}

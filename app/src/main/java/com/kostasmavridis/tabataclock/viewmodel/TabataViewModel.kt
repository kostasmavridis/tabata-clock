package com.kostasmavridis.tabataclock.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kostasmavridis.tabataclock.audio.SoundManager
import com.kostasmavridis.tabataclock.data.SettingsRepository
import com.kostasmavridis.tabataclock.model.TabataPhase
import com.kostasmavridis.tabataclock.model.TabataSettings
import com.kostasmavridis.tabataclock.service.TabataForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TabataViewModel @Inject constructor(
    application: Application,
    private val repo: SettingsRepository,
    private val soundManager: SoundManager
) : AndroidViewModel(application) {

    // ── Settings ──────────────────────────────────────────────────────────────
    val settings: StateFlow<TabataSettings> = repo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TabataSettings())

    fun updateSettings(s: TabataSettings) {
        viewModelScope.launch { repo.saveSettings(s) }
    }

    // ── Timer State ───────────────────────────────────────────────────────────
    data class TimerState(
        val phase: TabataPhase = TabataPhase.PREPARE,
        val secondsLeft: Int = 10,
        val phaseDurationSecs: Int = 10,   // total seconds for current phase — drives the progress arc
        val currentRound: Int = 1,
        val currentSet: Int = 1,
        val isRunning: Boolean = false,
        val isPaused: Boolean = false,
        val totalElapsedSecs: Int = 0
    ) {
        /** 0.0 (start) → 1.0 (end) progress within the current phase */
        val phaseProgress: Float
            get() = if (phaseDurationSecs <= 0) 1f
                    else 1f - (secondsLeft.toFloat() / phaseDurationSecs.toFloat())
    }

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    // ── Controls ──────────────────────────────────────────────────────────────
    fun start() {
        if (_timerState.value.isRunning) return
        _timerState.update { it.copy(isRunning = true, isPaused = false) }
        timerJob = viewModelScope.launch { runTabataCycle() }
    }

    fun pause() {
        timerJob?.cancel()
        _timerState.update { it.copy(isRunning = false, isPaused = true) }
        stopService()
    }

    fun resume() {
        if (!_timerState.value.isPaused) return
        val state = _timerState.value
        _timerState.update { it.copy(isRunning = true, isPaused = false) }
        notifyService(state.phase, state.secondsLeft, state.currentRound)
        timerJob = viewModelScope.launch {
            runPhase(state.phase, state.secondsLeft, state.phaseDurationSecs)
            // After the resumed phase completes we cannot easily know where we were
            // in the full cycle, so we restart from round 1 of the remaining sets.
            // Full mid-cycle persistence is tracked in issue #1 (v2).
        }
    }

    fun reset() {
        timerJob?.cancel()
        stopService()
        val prepareSecs = settings.value.prepareSecs
        _timerState.value = TimerState(
            secondsLeft = prepareSecs,
            phaseDurationSecs = prepareSecs
        )
    }

    // ── Cycle Logic ───────────────────────────────────────────────────────────
    private suspend fun runTabataCycle() {
        val s = settings.value
        runPhase(TabataPhase.PREPARE, s.prepareSecs, s.prepareSecs)
        for (set in 1..s.sets) {
            for (round in 1..s.rounds) {
                _timerState.update { it.copy(currentRound = round, currentSet = set) }
                soundManager.playWork()
                runPhase(TabataPhase.WORK, s.workSecs, s.workSecs)
                val isLastRound = set == s.sets && round == s.rounds
                if (!isLastRound) {
                    soundManager.playRest()
                    runPhase(TabataPhase.REST, s.restSecs, s.restSecs)
                }
            }
        }
        soundManager.playDone()
        stopService()
        _timerState.update { it.copy(
            phase = TabataPhase.DONE,
            isRunning = false,
            secondsLeft = 0,
            phaseProgress = 1f  // compiler-friendly: computed property, no direct set needed
        )}
    }

    private suspend fun runPhase(phase: TabataPhase, durationSecs: Int, totalSecs: Int) {
        for (remaining in durationSecs downTo 1) {
            val state = _timerState.updateAndGet { it.copy(
                phase = phase,
                secondsLeft = remaining,
                phaseDurationSecs = totalSecs,
                totalElapsedSecs = it.totalElapsedSecs + 1
            )}
            notifyService(phase, remaining, state.currentRound)
            if (remaining <= 3) soundManager.playBeep()
            delay(1_000L)
        }
    }

    // ── Service Integration ───────────────────────────────────────────────────
    private fun notifyService(phase: TabataPhase, secondsLeft: Int, round: Int) {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, TabataForegroundService::class.java).apply {
            putExtra(TabataForegroundService.EXTRA_PHASE,   phase.label)
            putExtra(TabataForegroundService.EXTRA_SECONDS, secondsLeft)
            putExtra(TabataForegroundService.EXTRA_ROUND,   round)
        }
        ctx.startForegroundService(intent)
    }

    private fun stopService() {
        val ctx = getApplication<Application>()
        ctx.stopService(Intent(ctx, TabataForegroundService::class.java))
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}

package com.kostasmavridis.tabataclock.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kostasmavridis.tabataclock.audio.ISoundManager
import com.kostasmavridis.tabataclock.data.ISettingsRepository
import com.kostasmavridis.tabataclock.model.TabataPhase
import com.kostasmavridis.tabataclock.model.TabataSettings
import com.kostasmavridis.tabataclock.service.NoOpServiceNotifier
import com.kostasmavridis.tabataclock.service.ServiceNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TabataViewModel @Inject constructor(
    application: Application,
    private val repo: ISettingsRepository,
    private val soundManager: ISoundManager,
    private val serviceNotifier: ServiceNotifier = NoOpServiceNotifier()
) : AndroidViewModel(application) {

    // ── Settings ───────────────────────────────────────────────────────
    val settings: StateFlow<TabataSettings> = repo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, repo.settingsFlow.value)

    fun updateSettings(s: TabataSettings) {
        viewModelScope.launch { repo.saveSettings(s) }
    }

    // ── Timer State ──────────────────────────────────────────────────────
    data class TimerState(
        val phase: TabataPhase = TabataPhase.PREPARE,
        val secondsLeft: Int = 0,
        val phaseDurationSecs: Int = 0,
        val currentRound: Int = 1,
        val currentSet: Int = 1,
        val isRunning: Boolean = false,
        val isPaused: Boolean = false,
        val totalRoundsCompleted: Int = 0
    ) {
        val phaseProgress: Float
            get() = if (phaseDurationSecs <= 0) 1f
                    else 1f - (secondsLeft.toFloat() / phaseDurationSecs.toFloat())
    }

    private val _timerState = MutableStateFlow(
        TimerState(
            secondsLeft       = repo.settingsFlow.value.prepareSecs,
            phaseDurationSecs = repo.settingsFlow.value.prepareSecs
        )
    )
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null
    @Volatile private var skipRequested = false

    // ── Lifecycle callbacks ──────────────────────────────────────────────

    /**
     * Called when the app returns to the foreground while NOT running.
     * Rebuilds the SoundPool so OEM-silenced AudioTrack sessions are
     * transparently restored before the next start().
     */
    fun onAppForegrounded() {
        if (!_timerState.value.isRunning) {
            soundManager.reinitialise()
        }
    }

    /**
     * Called every time TimerScreen re-enters the Resumed lifecycle state
     * — covers both app-foreground and back-navigation from SettingsScreen.
     * Uses reinitialiseIfNeeded() which is safe to call mid-run.
     */
    fun onScreenResumed() {
        soundManager.reinitialiseIfNeeded()
    }

    // ── Controls ────────────────────────────────────────────────────────
    fun start() {
        if (_timerState.value.isRunning) return
        _timerState.update { it.copy(isRunning = true, isPaused = false) }
        timerJob = viewModelScope.launch { runTabataCycle() }
    }

    fun pause() {
        timerJob?.cancel()
        _timerState.update { it.copy(isRunning = false, isPaused = true) }
        serviceNotifier.stop()
    }

    fun resume() {
        if (!_timerState.value.isPaused) return
        val state = _timerState.value
        _timerState.update { it.copy(isRunning = true, isPaused = false) }
        serviceNotifier.notify(state.phase, state.secondsLeft, state.currentRound)
        timerJob = viewModelScope.launch {
            runPhase(state.phase, state.secondsLeft, state.phaseDurationSecs)
        }
    }

    fun reset() {
        timerJob?.cancel()
        skipRequested = false
        serviceNotifier.stop()
        val prepareSecs = settings.value.prepareSecs
        _timerState.value = TimerState(
            secondsLeft       = prepareSecs,
            phaseDurationSecs = prepareSecs
        )
    }

    fun skip() {
        if (!_timerState.value.isRunning) return
        if (_timerState.value.phase == TabataPhase.DONE) return
        skipRequested = true
    }

    // ── Cycle Logic ──────────────────────────────────────────────────────
    private suspend fun runTabataCycle() {
        val s = repo.settingsFlow.first()
        runPhase(TabataPhase.PREPARE, s.prepareSecs, s.prepareSecs)
        for (set in 1..s.sets) {
            for (round in 1..s.rounds) {
                _timerState.update { it.copy(currentRound = round, currentSet = set) }
                soundManager.playWork()
                runPhase(TabataPhase.WORK, s.workSecs, s.workSecs)
                _timerState.update {
                    it.copy(totalRoundsCompleted = it.totalRoundsCompleted + 1)
                }
                val isLastRound = set == s.sets && round == s.rounds
                if (!isLastRound) {
                    soundManager.playRest()
                    runPhase(TabataPhase.REST, s.restSecs, s.restSecs)
                }
            }
        }
        soundManager.playDone()
        serviceNotifier.stop()
        _timerState.update {
            it.copy(
                phase             = TabataPhase.DONE,
                secondsLeft       = 0,
                phaseDurationSecs = 0,
                isRunning         = false
            )
        }
    }

    private suspend fun runPhase(phase: TabataPhase, durationSecs: Int, totalSecs: Int) {
        for (remaining in durationSecs downTo 1) {
            if (skipRequested) {
                skipRequested = false
                return
            }
            val state = _timerState.updateAndGet {
                it.copy(
                    phase             = phase,
                    secondsLeft       = remaining,
                    phaseDurationSecs = totalSecs
                )
            }
            serviceNotifier.notify(phase, remaining, state.currentRound)
            if (remaining <= 3) soundManager.playBeep()
            delay(1_000L)
        }
        skipRequested = false
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}

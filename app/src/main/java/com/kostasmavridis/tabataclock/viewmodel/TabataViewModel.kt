package com.kostasmavridis.tabataclock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kostasmavridis.tabataclock.audio.SoundManager
import com.kostasmavridis.tabataclock.data.SettingsRepository
import com.kostasmavridis.tabataclock.model.TabataPhase
import com.kostasmavridis.tabataclock.model.TabataSettings
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
    private val repo: SettingsRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    // ── Settings ─────────────────────────────────────────────────────────────
    val settings: StateFlow<TabataSettings> = repo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TabataSettings())

    fun updateSettings(s: TabataSettings) {
        viewModelScope.launch { repo.saveSettings(s) }
    }

    // ── Timer State ───────────────────────────────────────────────────────────
    data class TimerState(
        val phase: TabataPhase = TabataPhase.PREPARE,
        val secondsLeft: Int = 10,
        val currentRound: Int = 1,
        val currentSet: Int = 1,
        val isRunning: Boolean = false,
        val isPaused: Boolean = false,
        val totalElapsedSecs: Int = 0
    )

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
    }

    fun resume() {
        if (!_timerState.value.isPaused) return
        _timerState.update { it.copy(isRunning = true, isPaused = false) }
        // Re-run from current remaining seconds in current phase
        timerJob = viewModelScope.launch {
            resumePhase(_timerState.value.phase, _timerState.value.secondsLeft)
        }
    }

    fun reset() {
        timerJob?.cancel()
        val prepareSecs = settings.value.prepareSecs
        _timerState.value = TimerState(secondsLeft = prepareSecs)
    }

    // ── Cycle Logic ───────────────────────────────────────────────────────────
    private suspend fun runTabataCycle() {
        val s = settings.value
        runPhase(TabataPhase.PREPARE, s.prepareSecs)
        for (set in 1..s.sets) {
            for (round in 1..s.rounds) {
                _timerState.update { it.copy(currentRound = round, currentSet = set) }
                soundManager.playWork()
                runPhase(TabataPhase.WORK, s.workSecs)
                val isLastRound = set == s.sets && round == s.rounds
                if (!isLastRound) {
                    soundManager.playRest()
                    runPhase(TabataPhase.REST, s.restSecs)
                }
            }
        }
        soundManager.playDone()
        _timerState.update { it.copy(phase = TabataPhase.DONE, isRunning = false, secondsLeft = 0) }
    }

    private suspend fun runPhase(phase: TabataPhase, durationSecs: Int) {
        for (remaining in durationSecs downTo 1) {
            _timerState.update { it.copy(
                phase = phase,
                secondsLeft = remaining,
                totalElapsedSecs = it.totalElapsedSecs + 1
            )}
            if (remaining <= 3) soundManager.playBeep()
            delay(1_000L)
        }
    }

    private suspend fun resumePhase(phase: TabataPhase, remainingSecs: Int) {
        runPhase(phase, remainingSecs)
        // After resuming, continue from next phase in cycle
        // For simplicity, restart full cycle from current round state
        // Full resume-from-mid-cycle requires persisting phase index — deferred to v2
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}

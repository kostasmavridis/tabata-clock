package com.kostasmavridis.tabataclock

import app.cash.turbine.test
import com.kostasmavridis.tabataclock.model.TabataPhase
import com.kostasmavridis.tabataclock.model.TabataSettings
import com.kostasmavridis.tabataclock.viewmodel.TabataViewModel
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("TabataViewModel")
class TabataViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeSettingsRepository
    private lateinit var sound: FakeSoundManager
    private lateinit var vm: TabataViewModel

    // Short durations so the full cycle completes in milliseconds
    private val fastSettings = TabataSettings(
        prepareSecs = 3,
        workSecs    = 4,
        restSecs    = 2,
        rounds      = 2,
        sets        = 1
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo  = FakeSettingsRepository(fastSettings)
        sound = FakeSoundManager()
        vm    = TabataViewModel(
            application  = mockk(relaxed = true),
            repo         = repo,
            soundManager = sound
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ────────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Initial state")
    inner class InitialState {

        @Test
        @DisplayName("phase is PREPARE on creation")
        fun `initial phase is PREPARE`() {
            assertEquals(TabataPhase.PREPARE, vm.timerState.value.phase)
        }

        @Test
        @DisplayName("timer is not running on creation")
        fun `timer not running initially`() {
            assertFalse(vm.timerState.value.isRunning)
        }

        @Test
        @DisplayName("secondsLeft matches prepareSecs from settings")
        fun `secondsLeft matches prepareSecs`() {
            assertEquals(fastSettings.prepareSecs, vm.timerState.value.secondsLeft)
        }

        @Test
        @DisplayName("currentRound starts at 1")
        fun `currentRound starts at 1`() {
            assertEquals(1, vm.timerState.value.currentRound)
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("start()")
    inner class Start {

        @Test
        @DisplayName("isRunning becomes true immediately")
        fun `start sets isRunning true`() = runTest {
            vm.start()
            assertTrue(vm.timerState.value.isRunning)
        }

        @Test
        @DisplayName("calling start() twice does not double-start the timer")
        fun `double start is idempotent`() = runTest {
            vm.start()
            vm.start()
            assertTrue(vm.timerState.value.isRunning)
        }

        @Test
        @DisplayName("PREPARE phase counts down each second")
        fun `prepare phase counts down`() = runTest {
            vm.start()
            advanceTimeBy(1_100L)
            assertEquals(TabataPhase.PREPARE, vm.timerState.value.phase)
            assertEquals(fastSettings.prepareSecs - 1, vm.timerState.value.secondsLeft)
        }

        @Test
        @DisplayName("transitions to WORK phase after PREPARE completes")
        fun `transitions PREPARE to WORK`() = runTest {
            vm.start()
            advanceTimeBy(fastSettings.prepareSecs * 1_000L + 500L)
            assertEquals(TabataPhase.WORK, vm.timerState.value.phase)
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("pause() and resume()")
    inner class PauseResume {

        @Test
        @DisplayName("pause stops countdown and sets isPaused = true")
        fun `pause freezes timer`() = runTest {
            vm.start()
            advanceTimeBy(1_100L)
            vm.pause()
            assertFalse(vm.timerState.value.isRunning)
            assertTrue(vm.timerState.value.isPaused)
        }

        @Test
        @DisplayName("secondsLeft does not change after pause")
        fun `seconds frozen after pause`() = runTest {
            vm.start()
            advanceTimeBy(1_100L)
            vm.pause()
            val frozenSecs = vm.timerState.value.secondsLeft
            advanceTimeBy(3_000L)
            assertEquals(frozenSecs, vm.timerState.value.secondsLeft)
        }

        @Test
        @DisplayName("resume clears isPaused and sets isRunning = true")
        fun `resume restores running state`() = runTest {
            vm.start()
            advanceTimeBy(1_100L)
            vm.pause()
            vm.resume()
            assertTrue(vm.timerState.value.isRunning)
            assertFalse(vm.timerState.value.isPaused)
        }

        @Test
        @DisplayName("resume() on non-paused state is a no-op")
        fun `resume on non-paused is no-op`() = runTest {
            vm.start()
            vm.resume() // not paused — should be ignored
            assertTrue(vm.timerState.value.isRunning)
        }

        @Test
        @DisplayName("countdown continues from paused secondsLeft after resume")
        fun `resume continues countdown from paused position`() = runTest {
            vm.start()
            advanceTimeBy(1_100L) // 1 second elapsed
            vm.pause()
            val secsBeforeResume = vm.timerState.value.secondsLeft
            vm.resume()
            advanceTimeBy(1_100L) // 1 more second
            assertEquals(secsBeforeResume - 1, vm.timerState.value.secondsLeft)
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("reset()")
    inner class Reset {

        @Test
        @DisplayName("reset returns to PREPARE phase with correct secondsLeft")
        fun `reset restores initial state`() = runTest {
            vm.start()
            advanceTimeBy(fastSettings.prepareSecs * 1_000L + 500L)
            vm.reset()
            with(vm.timerState.value) {
                assertEquals(TabataPhase.PREPARE, phase)
                assertEquals(fastSettings.prepareSecs, secondsLeft)
                assertFalse(isRunning)
                assertFalse(isPaused)
            }
        }

        @Test
        @DisplayName("reset while paused also clears isPaused")
        fun `reset clears paused state`() = runTest {
            vm.start()
            vm.pause()
            vm.reset()
            assertFalse(vm.timerState.value.isPaused)
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Full cycle")
    inner class FullCycle {

        // 3s prepare + 4s work + 2s rest + 4s work = 13 seconds total (sets=1, rounds=2)
        private val totalMs =
            (fastSettings.prepareSecs + fastSettings.workSecs +
             fastSettings.restSecs    + fastSettings.workSecs) * 1_000L

        @Test
        @DisplayName("reaches DONE phase after all rounds complete")
        fun `cycle ends with DONE`() = runTest {
            vm.start()
            advanceTimeBy(totalMs + 500L)
            assertEquals(TabataPhase.DONE, vm.timerState.value.phase)
            assertFalse(vm.timerState.value.isRunning)
        }

        @Test
        @DisplayName("playWork() called once per round")
        fun `work sound plays once per round`() = runTest {
            vm.start()
            advanceTimeBy(totalMs + 500L)
            assertEquals(fastSettings.rounds, sound.workCount)
        }

        @Test
        @DisplayName("playRest() called rounds-1 times (no rest after last round)")
        fun `rest sound skipped on last round`() = runTest {
            vm.start()
            advanceTimeBy(totalMs + 500L)
            assertEquals(fastSettings.rounds - 1, sound.restCount)
        }

        @Test
        @DisplayName("playDone() called exactly once at completion")
        fun `done sound plays once`() = runTest {
            vm.start()
            advanceTimeBy(totalMs + 500L)
            assertEquals(1, sound.doneCount)
        }

        @Test
        @DisplayName("round counter increments correctly through cycle")
        fun `round counter advances`() = runTest {
            vm.timerState.test {
                vm.start()
                advanceTimeBy(fastSettings.prepareSecs * 1_000L + 500L)
                advanceTimeBy(fastSettings.workSecs    * 1_000L + 500L)
                val inRest = awaitItem()
                assertEquals(1, inRest.currentRound)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("playBeep() fires during last 3 seconds of a phase")
        fun `beep plays in last 3 seconds of prepare`() = runTest {
            // prepareSecs=3: all 3 ticks are within the last-3-seconds window
            vm.start()
            advanceTimeBy(fastSettings.prepareSecs * 1_000L + 500L)
            assertEquals(fastSettings.prepareSecs, sound.beepCount)
        }

        @Test
        @DisplayName("multi-set cycle reaches DONE and plays correct sounds")
        fun `two sets cycle completes correctly`() = runTest {
            val twoSets = fastSettings.copy(sets = 2)
            repo.flow.value = twoSets
            vm.reset()
            // sets=2, rounds=2, work=4, rest=2:
            // PREPARE(3) + [WORK(4)+REST(2)] * (sets*rounds - 1) + WORK(4)
            // = 3 + (4+2)*3 + 4 = 3 + 18 + 4 = 25s
            // Formula: prepareSecs + sets*rounds*workSecs + (sets*rounds-1)*restSecs
            val totalMs = (twoSets.prepareSecs +
                twoSets.sets * twoSets.rounds * twoSets.workSecs +
                (twoSets.sets * twoSets.rounds - 1) * twoSets.restSecs) * 1_000L
            vm.start()
            advanceTimeBy(totalMs + 500L)
            assertEquals(TabataPhase.DONE, vm.timerState.value.phase)
            assertFalse(vm.timerState.value.isRunning)
            // 2 sets * 2 rounds = 4 work sounds
            assertEquals(twoSets.sets * twoSets.rounds, sound.workCount)
            // done plays exactly once
            assertEquals(1, sound.doneCount)
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("phaseProgress")
    inner class PhaseProgress {

        @Test
        @DisplayName("starts at 0.0 when secondsLeft == phaseDurationSecs")
        fun `progress starts at zero`() {
            assertEquals(0f, vm.timerState.value.phaseProgress, 0.01f)
        }

        @Test
        @DisplayName("is 1.0 when workout reaches DONE")
        fun `progress is 1 at DONE`() = runTest {
            val totalMs = (fastSettings.prepareSecs + fastSettings.workSecs +
                           fastSettings.restSecs    + fastSettings.workSecs) * 1_000L
            vm.start()
            advanceTimeBy(totalMs + 500L)
            assertEquals(1f, vm.timerState.value.phaseProgress, 0.01f)
        }

        @Test
        @DisplayName("phaseProgress is 1.0 when phaseDurationSecs is 0")
        fun `progress is 1 when phaseDurationSecs is zero`() {
            // TimerState with phaseDurationSecs=0 should return 1f (guard branch)
            val state = TabataViewModel.TimerState(phaseDurationSecs = 0)
            assertEquals(1f, state.phaseProgress, 0.01f)
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Settings")
    inner class SettingsTests {

        @Test
        @DisplayName("updateSettings persists new values to repository")
        fun `updateSettings saves to repo`() = runTest {
            val newSettings = TabataSettings(workSecs = 30, restSecs = 15, rounds = 4)
            vm.updateSettings(newSettings)
            testScheduler.advanceUntilIdle()
            assertEquals(newSettings, repo.flow.value)
        }

        @ParameterizedTest(name = "workSecs={0}, restSecs={1}, rounds={2}")
        @CsvSource(
            "20, 10, 8",
            "30, 15, 4",
            "40, 20, 6"
        )
        @DisplayName("saved settings are reflected in the repository flow")
        fun `settings flow reflects saved values`(workSecs: Int, restSecs: Int, rounds: Int) = runTest {
            val s = TabataSettings(workSecs = workSecs, restSecs = restSecs, rounds = rounds)
            vm.updateSettings(s)
            testScheduler.advanceUntilIdle()
            with(repo.flow.value) {
                assertEquals(workSecs, this.workSecs)
                assertEquals(restSecs, this.restSecs)
                assertEquals(rounds,   this.rounds)
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("TabataSettings model")
    inner class TabataSettingsModel {

        @Test
        @DisplayName("default settings have correct values")
        fun `default settings are correct`() {
            val defaults = TabataSettings()
            assertEquals(10, defaults.prepareSecs)
            assertEquals(20, defaults.workSecs)
            assertEquals(10, defaults.restSecs)
            assertEquals(8,  defaults.rounds)
            assertEquals(1,  defaults.sets)
        }

        @Test
        @DisplayName("totalWorkoutSecs excludes trailing rest")
        fun `totalWorkoutSecs is correct for fastSettings`() {
            // sets=1, rounds=2, work=4, rest=2:
            // 1 * (2*4 + (2-1)*2) = 1 * (8 + 2) = 10s
            assertEquals(10, fastSettings.totalWorkoutSecs())
        }

        @ParameterizedTest(name = "rounds={0} → expected={1}s")
        @CsvSource(
            "1, 4",
            "2, 10",
            "3, 16"
        )
        @DisplayName("totalWorkoutSecs parametrized by round count")
        fun `totalWorkoutSecs parametrized`(rounds: Int, expected: Int) {
            // Formula: sets * (rounds * workSecs + (rounds-1) * restSecs)
            // rounds=1: 1*(1*4 + 0*2) = 4
            // rounds=2: 1*(2*4 + 1*2) = 10
            // rounds=3: 1*(3*4 + 2*2) = 16
            assertEquals(expected, fastSettings.copy(rounds = rounds).totalWorkoutSecs())
        }

        @Test
        @DisplayName("totalWorkoutSecs scales correctly with multiple sets")
        fun `totalWorkoutSecs with two sets`() {
            // sets=2, rounds=2, work=4, rest=2:
            // 2 * (2*4 + (2-1)*2) = 2 * (8 + 2) = 20s
            // Note: each SET ends without a trailing rest, not just the final round globally.
            val s = fastSettings.copy(sets = 2)
            assertEquals(20, s.totalWorkoutSecs())
        }
    }
}

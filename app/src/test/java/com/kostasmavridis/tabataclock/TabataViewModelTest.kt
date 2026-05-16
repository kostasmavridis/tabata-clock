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

    // Standard Tabata: 3s prepare, 4s work, 2s rest, 2 rounds, 1 set
    // (short durations so tests complete quickly)
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

    // ───────────────────────────────────────────────────────────────────────────────
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
        fun `timer is not running initially`() {
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

    // ───────────────────────────────────────────────────────────────────────────────
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
            val jobBefore = vm.timerState.value.isRunning
            vm.start() // should be ignored
            assertTrue(jobBefore)
            assertTrue(vm.timerState.value.isRunning)
        }

        @Test
        @DisplayName("PREPARE phase counts down each second")
        fun `prepare phase counts down`() = runTest {
            vm.start()
            advanceTimeBy(1_100L)
            assertEquals(TabataPhase.PREPARE, vm.timerState.value.phase)
            // after 1 second, remaining should be prepareSecs - 1 = 2
            assertEquals(fastSettings.prepareSecs - 1, vm.timerState.value.secondsLeft)
        }

        @Test
        @DisplayName("transitions to WORK phase after PREPARE completes")
        fun `transitions PREPARE to WORK`() = runTest {
            vm.start()
            // advance past full prepare phase
            advanceTimeBy((fastSettings.prepareSecs * 1_000L) + 500L)
            assertEquals(TabataPhase.WORK, vm.timerState.value.phase)
        }
    }

    // ───────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("pause() and resume()")
    inner class PauseResume {

        @Test
        @DisplayName("pause stops countdown and sets isPaused = true")
        fun `pause freezes timer`() = runTest {
            vm.start()
            advanceTimeBy(1_100L)
            vm.pause()
            val stateAfterPause = vm.timerState.value
            assertFalse(stateAfterPause.isRunning)
            assertTrue(stateAfterPause.isPaused)
        }

        @Test
        @DisplayName("secondsLeft does not change after pause")
        fun `seconds frozen after pause`() = runTest {
            vm.start()
            advanceTimeBy(1_100L)
            vm.pause()
            val frozenSecs = vm.timerState.value.secondsLeft
            advanceTimeBy(3_000L) // time passes but timer is paused
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
            val runningSecs = vm.timerState.value.secondsLeft
            vm.resume() // not paused, should be ignored
            assertTrue(vm.timerState.value.isRunning)
            assertEquals(runningSecs, vm.timerState.value.secondsLeft)
        }
    }

    // ───────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("reset()")
    inner class Reset {

        @Test
        @DisplayName("reset returns to PREPARE phase with correct secondsLeft")
        fun `reset restores initial state`() = runTest {
            vm.start()
            advanceTimeBy((fastSettings.prepareSecs * 1_000L) + 500L) // mid-WORK
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

    // ───────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Full cycle")
    inner class FullCycle {

        // Total time: 3s prepare + (4s work + 2s rest) * 2 rounds - 2s rest (last)
        //           = 3 + 4 + 2 + 4 = 13 seconds
        private val totalMs = (
            fastSettings.prepareSecs +
            fastSettings.workSecs +
            fastSettings.restSecs +
            fastSettings.workSecs
        ) * 1_000L

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
                // skip prepare
                advanceTimeBy((fastSettings.prepareSecs * 1_000L) + 500L)
                // skip first work phase
                advanceTimeBy((fastSettings.workSecs * 1_000L) + 500L)
                // we should now be in REST of round 1
                val inRest = awaitItem()
                assertEquals(1, inRest.currentRound)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ───────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("phaseProgress")
    inner class PhaseProgress {

        @Test
        @DisplayName("starts at 0.0 (no time elapsed in phase)")
        fun `progress starts at zero`() {
            // secondsLeft == phaseDurationSecs → progress = 0.0
            assertEquals(0f, vm.timerState.value.phaseProgress, 0.01f)
        }

        @Test
        @DisplayName("reaches 1.0 when secondsLeft is 0")
        fun `progress is 1 when done`() = runTest {
            vm.start()
            advanceTimeBy(
                (fastSettings.prepareSecs + fastSettings.workSecs +
                 (fastSettings.restSecs + fastSettings.workSecs)) * 1_000L + 500L
            )
            assertEquals(1f, vm.timerState.value.phaseProgress, 0.01f)
        }
    }

    // ───────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Settings")
    inner class SettingsTests {

        @Test
        @DisplayName("updateSettings persists new values to repository")
        fun `updateSettings saves to repo`() = runTest {
            val newSettings = TabataSettings(workSecs = 30, restSecs = 15, rounds = 4)
            vm.updateSettings(newSettings)
            testScheduler.advanceUntilIdle()
            assertEquals(newSettings, repo.settingsFlow.value)
        }

        @ParameterizedTest(name = "workSecs={0}, restSecs={1}, rounds={2}")
        @CsvSource(
            "20, 10, 8",
            "30, 15, 4",
            "40, 20, 6"
        )
        @DisplayName("settings flow reflects saved values")
        fun `settings flow reflects saved values`(
            workSecs: Int,
            restSecs: Int,
            rounds: Int
        ) = runTest {
            val s = TabataSettings(workSecs = workSecs, restSecs = restSecs, rounds = rounds)
            vm.updateSettings(s)
            testScheduler.advanceUntilIdle()
            val saved = repo.settingsFlow.value
            assertEquals(workSecs, saved.workSecs)
            assertEquals(restSecs, saved.restSecs)
            assertEquals(rounds,   saved.rounds)
        }
    }

    // ───────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("TabataSettings model")
    inner class TabataSettingsModel {

        @Test
        @DisplayName("totalWorkoutSecs excludes trailing rest")
        fun `totalWorkoutSecs is correct`() {
            // 2 rounds: work + rest + work (no trailing rest)
            // = 4 + 2 + 4 = 10
            assertEquals(10, fastSettings.totalWorkoutSecs())
        }

        @ParameterizedTest(name = "rounds={0}, expected={1}")
        @CsvSource(
            "1, 4",   // just work, no rest
            "2, 10",  // work+rest+work
            "3, 16"   // work+rest+work+rest+work
        )
        @DisplayName("totalWorkoutSecs parametrized")
        fun `totalWorkoutSecs parametrized`(rounds: Int, expected: Int) {
            val s = fastSettings.copy(rounds = rounds)
            assertEquals(expected, s.totalWorkoutSecs())
        }
    }
}

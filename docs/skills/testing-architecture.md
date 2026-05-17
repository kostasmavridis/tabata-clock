---
name: testing-architecture
description: "JUnit 5 on Android, Fake* implementations, coroutine timer testing with StandardTestDispatcher, MockK, Turbine StateFlow assertions, and test data discipline for Tabata Clock's 26-test suite."
---

## Testing Architecture

All 26 existing tests must pass before any PR merges. New features must ship
with tests. This skill defines not just the tools but the philosophy behind
how tests are written in this project.

---

### The Four Rules

1. **Fake over mock** — write `Fake*` implementations in `src/test/` for simple
   interfaces. Do not mock what you can fake.
2. **No real time** — `Thread.sleep()` and real `delay()` are **banned** in tests.
   Use `StandardTestDispatcher` + `advanceTimeBy()` exclusively.
3. **Derive, don't hardcode** — time values come from test settings objects.
   Never use literals like `3_000L` or `20_000L` in test assertions or `advanceTimeBy` calls.
4. **Interface contract enables testability** — `TabataViewModel` has zero Android
   framework imports. This is what makes all of these tests possible without
   instrumentation or a device.

---

### JUnit 5 on Android

The project uses the `android-junit5` plugin by mannodermaus.

- The plugin version is **tightly coupled** to the `junit-jupiter` BOM version.
  Always check the mannodermaus compatibility matrix before upgrading either
  independently. The project has experienced binary-incompatibility failures here.
- Instrumentation tests and unit tests are separated:
  - **Unit tests** (`src/test/`) use pure JVM execution — fast, no emulator.
  - **Instrumentation tests** (`src/androidTest/`) require a device/emulator —
    not currently in the CI matrix; add only when testing platform behaviour
    that cannot be faked.
- Test classes use JUnit 5 APIs exclusively:

```kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.ExtendWith

// Do NOT mix with JUnit 4 (@RunWith, @org.junit.Test) — runners are incompatible
@ExtendWith(MainCoroutineExtension::class)
class TabataViewModelTest { /* ... */ }
```

---

### Fake Implementations

Fakes are real Kotlin classes that implement project interfaces.
They live in `src/test/java/.../fake/`.

Write a fake when:
- The interface is simple (< ~5 methods)
- You need to assert on calls made to the dependency
- The fake needs to emit state the system under test reacts to

```kotlin
// src/test/java/.../fake/FakeSoundPlayer.kt
class FakeSoundPlayer : SoundPlayer {
    val playedSounds = mutableListOf<SoundEvent>()

    override fun play(event: SoundEvent) {
        playedSounds += event
    }

    override fun release() {
        playedSounds.clear()
    }
}

// In tests:
val fake = FakeSoundPlayer()
// ... exercise the ViewModel ...
assertEquals(listOf(SoundEvent.WorkStart), fake.playedSounds)
```

Fakes are **not** test doubles constructed by a mocking framework.
They are production-quality implementations of the interface contract,
with added observability (e.g. a `playedSounds` list).

---

### Timer Testing with Coroutines

All timer behaviour is tested with `StandardTestDispatcher` and `advanceTimeBy()`.

```kotlin
@Test
fun `work phase ends after configured duration`() = runTest {
    val settings = TestDefaults.settings   // single canonical object
    val viewModel = TabataViewModel(
        settings   = settings,
        sound      = FakeSoundPlayer(),
        dispatcher = StandardTestDispatcher(testScheduler)
    )

    viewModel.start()
    advanceTimeBy(settings.workMillis)   // derived — not 20_000L

    assertEquals(Phase.REST, viewModel.uiState.value.phase)
}
```

Key mechanics:
- `StandardTestDispatcher` does not auto-advance time; coroutines only run
  when you call `advanceTimeBy()`, `runCurrent()`, or `advanceUntilIdle()`.
- `advanceTimeBy(n)` advances the virtual clock by `n` milliseconds without
  blocking a real thread.
- Inject the dispatcher into the ViewModel at construction. Never use
  `Dispatchers.Main` or `Dispatchers.Default` directly in the ViewModel body.
- Use `runTest` (from `kotlinx-coroutines-test`) as the test runner;
  it installs a `TestCoroutineScheduler` and cleans up after the block.

---

### MockK — When Fakes Are Insufficient

Use MockK when:
- The interface is complex and a fake would be unwieldy
- You need to verify **call order** across multiple methods
- You need to throw exceptions on specific calls to test error paths

```kotlin
// relaxed = true: all calls return defaults; no boilerplate stubbing required
val mockVibrator = mockk<HapticFeedback>(relaxed = true)

// Stub a specific call
every { mockVibrator.vibrate(VibrationPattern.LONG) } throws HapticException("unsupported")

// Verify interaction
verify(exactly = 1) { mockVibrator.vibrate(VibrationPattern.SHORT) }
```

- Prefer `relaxed = true` over bare `mockk<T>()` to minimise stubbing noise.
- Never mock data classes or simple value objects — use real instances.
- For coroutine-returning functions, use `coEvery` / `coVerify` from MockK.

---

### Turbine — Testing StateFlow & Flow

Turbine provides structured assertions on `StateFlow` and `Flow` emissions.

```kotlin
import app.cash.turbine.test

@Test
fun `phase transitions emit correct state sequence`() = runTest {
    viewModel.uiState.test {
        // First emission is the initial state
        assertEquals(Phase.IDLE, awaitItem().phase)

        viewModel.start()
        advanceTimeBy(TestDefaults.settings.workMillis)

        assertEquals(Phase.REST, awaitItem().phase)

        cancelAndIgnoreRemainingEvents()
    }
}
```

- `awaitItem()` suspends until the next emission, then asserts on it.
- `cancelAndIgnoreRemainingEvents()` ends the collection cleanly;
  use `awaitComplete()` only when the flow is expected to complete.
- Always cancel within the `test { }` block — leaving it open causes leaks.

---

### Test Data Discipline

Define a single canonical `TestDefaults` object in `src/test/`.
All tests derive their time values from it.

```kotlin
// src/test/java/.../TestDefaults.kt
object TestDefaults {
    val settings = TabataSettings(
        workSeconds = 20,
        restSeconds = 10,
        rounds      = 4,
        prepSeconds = 5
    )

    // All durations derived — changing settings updates every test automatically
    val workMillis: Long  get() = settings.workSeconds * 1_000L
    val restMillis: Long  get() = settings.restSeconds * 1_000L
    val prepMillis: Long  get() = settings.prepSeconds * 1_000L
    val roundMillis: Long get() = workMillis + restMillis
    val totalMillis: Long get() = prepMillis + (roundMillis * settings.rounds)
}
```

Why this matters: if you later change `workSeconds` from 20 to 40 to test a
different scenario, every `advanceTimeBy` call in every test that uses
`TestDefaults.workMillis` automatically uses the new value. Hardcoded
`20_000L` literals would silently diverge.

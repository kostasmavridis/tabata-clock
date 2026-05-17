---
name: testing-architecture
description: "JUnit 5, Fake implementations, coroutine timer testing, MockK, Turbine, and test data discipline for Tabata Clock's 26-test suite."
---

## Testing Architecture

All 26 existing tests must pass before any PR merges. New features must ship
with tests. This skill defines how tests are written in this project.

---

### The Core Philosophy

1. **Fake over mock** — write `Fake*` implementations in `src/test/` for simple
   interfaces. Do not mock what you can fake.
2. **No real time** — `Thread.sleep()` and real `delay()` are banned in tests.
3. **Derive, don't hardcode** — time values come from test settings objects,
   never from literals like `3_000L`.
4. **Interface contract enables testability** — the ViewModel has zero Android
   imports; this is what makes unit testing possible without instrumentation.

---

### JUnit 5 on Android

The project uses the `android-junit5` plugin by mannodermaus.

- The plugin version is coupled to the `junit-jupiter` BOM version.
  Always check the compatibility matrix before upgrading either independently.
- Test classes use `@ExtendWith` and `@Test` from `org.junit.jupiter.api`.
- Do **not** mix JUnit 4 (`@RunWith`, `@org.junit.Test`) with JUnit 5 in the
  same module — the runners are incompatible.

```kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class TabataViewModelTest {
    @Test
    fun `timer counts down correctly`() { /* ... */ }
}
```

---

### Fake Implementations

Fakes live in `src/test/java/.../fake/`. They are real implementations of
project interfaces, not mock frameworks.

```kotlin
// src/test/java/.../fake/FakeSoundPlayer.kt
class FakeSoundPlayer : SoundPlayer {
    val playedSounds = mutableListOf<SoundEvent>()

    override fun play(event: SoundEvent) {
        playedSounds += event
    }

    override fun release() { playedSounds.clear() }
}
```

Use fakes when:
- The interface is simple (< ~5 methods)
- You need to assert on calls made to the dependency
- The fake needs to emit state that the system under test reacts to

---

### Timer Testing with Coroutines

All timer behaviour is tested with `StandardTestDispatcher` and `advanceTimeBy()`.

```kotlin
@Test
fun `work phase ends after configured duration`() = runTest {
    val settings = TabataSettings(workSeconds = 20, restSeconds = 10, rounds = 4)
    val viewModel = TabataViewModel(
        settings = settings,
        soundPlayer = FakeSoundPlayer(),
        dispatcher = StandardTestDispatcher(testScheduler)
    )

    viewModel.start()
    advanceTimeBy(settings.workMillis)  // derived from settings, not 20_000L

    assertEquals(Phase.REST, viewModel.uiState.value.phase)
}
```

Key rules:
- Always inject the `TestCoroutineDispatcher` — never use `Dispatchers.Main` directly.
- Use `advanceTimeBy()` for time passage, `runCurrent()` for pending coroutines.
- Derive all durations from the settings object: `settings.workMillis`, not `20_000L`.

---

### MockK Usage

Use MockK when a fake would be impractical (complex interface, verify call order,
throw exceptions on specific calls).

```kotlin
val mockVibrator = mockk<HapticFeedback>(relaxed = true)
// relaxed = true: all calls return default values, no stubbing required
// use verify { } to assert interactions
verify { mockVibrator.vibrate(VibrationPattern.SHORT) }
```

- `relaxed = true` is preferred over `mockk<T>()` to avoid boilerplate stubbing.
- Never mock data classes or simple value objects — use real instances.

---

### Turbine — Testing StateFlow

Use Turbine to assert on `StateFlow` emissions in sequence.

```kotlin
import app.cash.turbine.test

@Test
fun `phase transitions emit correct states`() = runTest {
    viewModel.uiState.test {
        assertEquals(Phase.WORK, awaitItem().phase)
        viewModel.start()
        advanceTimeBy(settings.workMillis)
        assertEquals(Phase.REST, awaitItem().phase)
        cancelAndIgnoreRemainingEvents()
    }
}
```

---

### Test Data Discipline

- Define a canonical `TestDefaults` object in `src/test/`:

```kotlin
object TestDefaults {
    val settings = TabataSettings(
        workSeconds = 20,
        restSeconds = 10,
        rounds = 4,
        prepSeconds = 5
    )
    // All durations derived from this single object
    val workMillis = settings.workSeconds * 1_000L
    val restMillis = settings.restSeconds * 1_000L
}
```

- Changing `TestDefaults.settings` automatically updates all derived time values
  in every test. This prevents drift between test assumptions and test timings.

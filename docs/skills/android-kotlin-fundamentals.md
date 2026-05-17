---
name: android-kotlin-fundamentals
description: "Jetpack Compose, Coroutines/StateFlow, MVVM, Hilt DI, Android Services, and ADR authorship for the Tabata Clock Android app."
---

## Android & Kotlin Fundamentals

This skill covers the core application layer of Tabata Clock: UI, state management,
dependency injection, background services, and recording architecture decisions.

---

### Jetpack Compose UI

- Build composables that are stateless where possible; hoist state to the ViewModel.
- Every screen-level composable must have a `@Preview` that uses a fake/stub ViewModel
  state — never a real one.
- Recomposition is triggered by `StateFlow` collected via `collectAsStateWithLifecycle()`.
  Never use `collectAsState()` directly in production screens.
- Theme tokens (colours, typography, spacing) live in the `ui/theme` package.
  Do not hard-code colours or dimensions inline.

```kotlin
// Correct: stateless composable receiving derived state
@Composable
fun TimerDisplay(remainingSeconds: Int, phase: Phase) {
    Text(
        text = remainingSeconds.toClockString(),
        style = MaterialTheme.typography.displayLarge,
        color = phase.color()
    )
}
```

---

### Kotlin Coroutines & StateFlow

- **`StateFlow` over `LiveData` everywhere** — this is a project-wide rule, not a preference.
- Expose UI state as a single `data class` wrapped in `StateFlow<UiState>`.
  Derive computed values (e.g. `remainingSeconds`) as properties on the state class;
  do not store them as separate fields that can drift.
- Use `viewModelScope` for ViewModel-owned coroutines.
  Use `supervisorScope` when launching parallel work that must not cancel siblings on failure.
- Never use `GlobalScope`.

```kotlin
// Correct: derived value, cannot drift
data class TimerUiState(
    val totalSeconds: Int,
    val elapsedSeconds: Int
) {
    val remainingSeconds: Int get() = totalSeconds - elapsedSeconds
    val progressFraction: Float get() = elapsedSeconds.toFloat() / totalSeconds
}
```

---

### MVVM + Hilt DI

- `TabataViewModel` has **zero Android framework imports except `Application`**.
  Every platform service (audio, vibration, notifications, foreground service) is
  accessed through an interface injected by Hilt.
- Annotate ViewModels with `@HiltViewModel` and inject the interface, not the concrete
  Android class.
- KSP2 is the annotation processor. If a build log shows
  `KSP2 is enabled but the following processors do not support it`,
  add `ksp { arg("dagger.kspEnabled", "true") }` to `app/build.gradle.kts`.

```kotlin
@HiltViewModel
class TabataViewModel @Inject constructor(
    private val soundPlayer: SoundPlayer,   // interface
    private val vibrator: HapticFeedback,   // interface
    private val timerService: TimerControl  // interface
) : ViewModel() { /* ... */ }
```

---

### Android Services — Foreground Service

- Declare the correct `foregroundServiceType` in `AndroidManifest.xml`.
  Missing or wrong type causes a `MissingForegroundServiceTypeException` on API 34+.
- Access the service only through the `TimerControl` interface from the ViewModel.
- The service implementation lives in `service/` and is the only class allowed to
  import `android.app.Service`.
- Use `ServiceConnection` + a bound-service pattern, not a static singleton,
  so the connection lifecycle is tied to the Activity.

---

### Architecture Decision Records (ADR)

- Every significant architecture choice is documented in `docs/adr/`.
- File naming: `NNN-short-title.md` (e.g. `004-use-ksp-over-kapt.md`).
- Required sections: **Status**, **Context**, **Decision**, **Consequences**.
- When a decision is superseded, set its status to `Superseded by ADR-NNN` and
  create the new ADR — do not edit the old one in place.
- ADR creation is part of the same commit as the code change it documents.
  It is never a follow-up commit.

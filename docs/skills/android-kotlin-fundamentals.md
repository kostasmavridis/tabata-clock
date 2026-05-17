---
name: android-kotlin-fundamentals
description: "Jetpack Compose, Coroutines/StateFlow, MVVM, Hilt DI (KSP2), ForegroundService with correct serviceType, audio/vibration interfaces, and ADR authorship for the Tabata Clock Android app."
---

## Android & Kotlin Fundamentals

This skill covers the core application layer of Tabata Clock: UI, state management,
dependency injection, background services, and recording architecture decisions.
The project is Kotlin-first. Every design choice here is intentional and recorded
in `docs/adr/`.

---

### Jetpack Compose UI

- Build composables that are **stateless** where possible; hoist state to the ViewModel.
- Every screen-level composable must have a `@Preview` that uses a fake/stub state
  object — never a real ViewModel. Previews document intended appearance and catch
  recomposition regressions early.
- Recomposition is triggered by `StateFlow` collected via `collectAsStateWithLifecycle()`.
  Never use `collectAsState()` directly in production screens — it ignores the
  lifecycle and can cause work to continue when the app is backgrounded.
- Theme tokens (colours, typography, spacing) live in the `ui/theme` package.
  Hard-coded colours or dimensions inline in composables are a review failure.
- Understanding **recomposition scope** is essential: place state reads as deep in
  the tree as possible so only the affected subtree recomposes, not the whole screen.

```kotlin
// Correct: stateless composable, state read at the screen level only
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

- **`StateFlow` over `LiveData` everywhere.** `LiveData` is explicitly banned in this
  project — it is not Kotlin-idiomatic and does not compose with coroutine operators.
- Expose UI state as a single `data class` wrapped in `StateFlow<UiState>`.
  **Derive** computed values (e.g. `remainingSeconds`) as properties on the state
  class. A computed property on a data class cannot drift; a separate stored field can.
- Use `viewModelScope` for ViewModel-owned coroutines.
  Use `supervisorScope` when launching parallel work where one failure must not
  cancel siblings.
- Never use `GlobalScope`.
- Know the key `Flow` operators used in this project: `map`, `combine`, `stateIn`,
  `onEach`, `distinctUntilChanged`. Understand `SharingStarted.WhileSubscribed(5_000)`
  for `stateIn` — the 5-second timeout prevents re-fetching on config changes.
- For testing: always use `StandardTestDispatcher` + `advanceTimeBy()`. Real `delay()`
  and `Thread.sleep()` are banned in tests.

```kotlin
// Correct: derived value — cannot drift
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
  accessed through an interface injected by Hilt. This is the contract that makes
  the ViewModel unit-testable without instrumentation.
- Annotate ViewModels with `@HiltViewModel` and inject the interface, not the
  concrete Android class.
- Hilt runs on **KSP2**. The `ksp` configuration is used, not `kapt`. If a build log
  shows `KSP2 is enabled but the following processors do not support it`, add
  `ksp { arg("dagger.kspEnabled", "true") }` to `app/build.gradle.kts`.
- Scoped component hierarchies: prefer `@Singleton` for stateless services (e.g.
  audio player), `@ActivityRetainedScoped` for objects that must survive config
  changes but not the process.

```kotlin
@HiltViewModel
class TabataViewModel @Inject constructor(
    private val soundPlayer: SoundPlayer,    // interface — no android.media import
    private val vibrator: HapticFeedback,    // interface — no android.os import
    private val timerService: TimerControl   // interface — no android.app.Service import
) : ViewModel() { /* ... */ }
```

---

### Android Services — ForegroundService

- Declare the correct `foregroundServiceType` in `AndroidManifest.xml`.
  **Missing or wrong type causes `MissingForegroundServiceTypeException` at runtime
  on API 34+.** This has been a real failure in this project — it is not theoretical.
- Access the service exclusively through the `TimerControl` interface from the
  ViewModel. The ViewModel never imports `android.app.Service`.
- The service implementation lives in `service/` and is the only class permitted to
  import `android.app.Service`.
- Use a bound-service pattern with `ServiceConnection`; do not use a static singleton.
  The connection lifecycle must be tied to the Activity, not the Application.
- Audio and vibration are also accessed through interfaces (`SoundPlayer`,
  `HapticFeedback`). The concrete implementations (`AndroidSoundPlayer`,
  `AndroidHapticFeedback`) live in the `platform/` package and are bound by Hilt.

```xml
<!-- AndroidManifest.xml — correct for a media-playback timer -->
<service
    android:name=".service.TabataTimerService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="false" />
```

---

### Architecture Decision Records (ADR)

- Every significant architecture choice is documented in `docs/adr/`.
- File naming: `NNN-short-kebab-title.md` (e.g. `004-use-ksp-over-kapt.md`).
- Required sections: **Status**, **Context**, **Decision**, **Consequences**.
- When a decision is superseded, set its status to `Superseded by ADR-NNN` and
  create the new ADR. Do not edit the old one's body.
- ADR creation is part of the **same commit** as the code change it documents.
  A follow-up "add ADR" commit means the original commit was incomplete.
- Topics already deserving ADRs (or due when the decision is made):
  - Why MVVM over MVI
  - Why Hilt over manual DI
  - Why KSP over KAPT
  - Multi-module extraction (if/when it happens)
  - Kotlin Multiplatform (if/when evaluated)
  - Dark-mode theming strategy

```markdown
# 004. Use KSP over KAPT for annotation processing

**Date:** 2025-06-01  
**Status:** Accepted

## Context
KAPT compiles Kotlin to Java stubs before processing, causing a measurable
build-time penalty. KSP processes Kotlin source natively.

## Decision
All annotation processors (Hilt, Room if added) use KSP. KAPT is removed
and must not be re-introduced.

## Consequences
### Positive
- Faster incremental builds
- KSP2 enables parallel processing
### Negative / Trade-offs
- Processors must explicitly support KSP; check before adding new libraries
```

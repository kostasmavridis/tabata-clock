# ADR-001: MVVM as the Application Architecture Pattern

**Date:** 2026-05-17  
**Status:** Accepted  
**Deciders:** @kostasmavridis  
**Tags:** architecture, android, ui

---

## Context

Tabata Clock requires a UI that reacts to a continuously ticking timer running in a background coroutine. The core design challenge is: **who owns the timer state, and how does it reach the UI without tight coupling?**

The candidate patterns were:

| Pattern | State ownership | Background work | Testability |
|---|---|---|---|
| Activity / Fragment (no VM) | View layer | Difficult — lifecycle-bound | Low — Android instrumented tests only |
| MVP | Presenter | Moderate | Moderate — Presenter can be unit-tested |
| **MVVM** | `ViewModel` + `StateFlow` | Coroutines in `viewModelScope` | High — ViewModel is pure Kotlin |
| MVI | Store | Coroutines + reducers | High — but higher ceremony for a small app |

This is a single-screen app with one meaningful piece of state: the timer. There is no navigation graph beyond `MainActivity`, no multi-module dependency, and no server interaction. A lightweight but testable architecture is preferred over a heavily structured one.

## Decision

Use **MVVM** with:

- `TabataViewModel` (annotated `@HiltViewModel`) as the single source of truth for all timer state
- `TimerState` as a Kotlin `data class` inside the ViewModel — an immutable snapshot emitted via `MutableStateFlow`
- `StateFlow<TimerState>` collected by the Composable UI with `collectAsStateWithLifecycle()`
- All business logic (phase sequencing, round counting, sound cues) lives in the ViewModel — the UI is a pure rendering function of `TimerState`

```
UI (Compose)  ──reads──▶  StateFlow<TimerState>  ◀──emits──  ViewModel
                                                                   │
                                                          viewModelScope coroutine
                                                                   │
                                                    ISettingsRepository  ISoundManager
                                                    ServiceNotifier
```

## Consequences

### Positive
- The ViewModel survives configuration changes (screen rotation) — the timer keeps running
- `TimerState` is a pure `data class` with no Android dependencies — fully unit-testable with `kotlinx-coroutines-test` and Turbine
- UI code is a stateless rendering function — no timer logic leaks into Composables
- `viewModelScope` cancels automatically when the ViewModel is cleared — no manual coroutine lifecycle management

### Negative
- `AndroidViewModel` (instead of plain `ViewModel`) is required because `TabataViewModel` holds `Application` for `ServiceNotifier` — couples the ViewModel to Android slightly
- A single `TimerState` data class grows as features are added; may need to be split into sub-states if the app expands significantly

### Mitigations
- The `Application` dependency is injected by Hilt and mocked in tests via `NoOpServiceNotifier` — the Android coupling is isolated to the DI boundary
- `TimerState` sub-states can be introduced as nested data classes without changing the `StateFlow` contract

## Alternatives Considered

### MVI (Model-View-Intent)
Adds an `Intent` sealed class and a reducer function. Appropriate for complex multi-screen apps with many user actions. Unnecessary ceremony for a single-screen timer.

### No ViewModel (state in `Activity`)
Timer coroutines would be cancelled on screen rotation. Not viable for a background-capable timer app.

### MVP with a Presenter
Presenter must hold a View reference, requiring explicit `attach`/`detach` lifecycle management. Compose's declarative model makes the Presenter pattern redundant.

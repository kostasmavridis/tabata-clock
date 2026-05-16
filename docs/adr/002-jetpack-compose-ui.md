# ADR-002: Jetpack Compose as the UI Toolkit

**Date:** 2026-05-17  
**Status:** Accepted  
**Deciders:** @kostasmavridis  
**Tags:** architecture, ui, compose

---

## Context

Android UI can be built with two approaches:

| Approach | Description | Status |
|---|---|---|
| View system (XML layouts) | Imperative — mutate views in response to state changes | Legacy; still supported |
| **Jetpack Compose** | Declarative — UI is a pure function of state | Google-recommended since 2021 |

The project started fresh with no pre-existing XML layout investment. The UI consists of a single screen with:
- A circular progress arc for the current phase
- Phase label, seconds remaining, round/set counters
- Play / Pause / Reset buttons
- A settings panel (numeric inputs for durations)

This is a rendering problem: given a `TimerState`, produce pixels. The UI never needs to query state from views — it only reacts to `StateFlow` emissions.

## Decision

Use **Jetpack Compose** exclusively. No XML layouts. No `View`/`Fragment` system.

Key conventions adopted:
- All Composables are stateless — they receive `TimerState` and emit events via lambdas (e.g. `onPlay: () -> Unit`)
- State is hoisted to `TabataViewModel` — Composables never call `viewModel()` directly except at the screen entry point
- `collectAsStateWithLifecycle()` (from `lifecycle-runtime-compose`) is used instead of `collectAsState()` to respect the Activity lifecycle and avoid collecting emissions when the UI is not visible
- `Canvas` API is used for the circular phase progress arc — no third-party chart library dependency

## Consequences

### Positive
- No view mutation bugs — the UI is always a deterministic function of `TimerState`
- Compose previews enable instant visual iteration without running an emulator
- Tight integration with `StateFlow` / `collectAsStateWithLifecycle()` — no manual `Observer` boilerplate
- Single-screen app structure maps naturally to a single top-level Composable tree

### Negative
- Compose has a steeper initial learning curve than XML for developers unfamiliar with declarative UI
- Custom drawing (the phase arc) requires `Canvas` API knowledge
- Compose compiler is sensitive to `kotlin` / KSP version alignment (see ADR-007 for Dependabot grouping)

### Mitigations
- The Compose BOM pins all `androidx.compose.*` versions consistently — no version mismatches between compose libraries
- The `kotlin-ksp` Dependabot group ensures Kotlin and KSP are always bumped together

## Alternatives Considered

### XML Layouts + `ViewBinding`
Would require manual view mutation (`textView.text = ...`) on every `TimerState` emission. More boilerplate, more mutation bugs, no benefit for a greenfield single-screen app.

### XML Layouts + Data Binding
Adds XML expressions and generated binding classes. More moving parts than Compose for the same outcome.

### Third-party UI framework (e.g. Flutter)
Cross-platform capability is not a requirement. Introducing a non-native framework adds runtime overhead and breaks Android-specific integrations (foreground services, notifications).

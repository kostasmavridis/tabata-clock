---
name: compose-ui-ux-design
description: "Jetpack Compose UI patterns, theming, accessibility, animation, and UX conventions for the Tabata Clock workout timer screens."
---

## Compose UI/UX Design

This skill covers the user-facing layer of Tabata Clock: what the screens look
like, how they behave, and the constraints that keep the UI accessible,
performant, and consistent.

---

### Screen Inventory

| Screen | Route | Purpose |
|---|---|---|
| `TimerScreen` | `/timer` | Active workout display — phase, countdown, round progress |
| `SettingsScreen` | `/settings` | Configure work/rest durations, rounds, prep time |

Navigation is handled by `androidx.navigation:navigation-compose`. The
`NavHost` lives in `MainActivity`; screens are composable destinations.

---

### State Flow to UI

```
TabataViewModel
  └─ StateFlow<TimerUiState>
        └─ collectAsStateWithLifecycle()   ← in screen composable
              └─ passed as plain parameters to stateless child composables
```

- Screens collect state once at the top level and pass values down.
- Child composables are pure functions of their parameters — no ViewModel
  references below the screen level.

---

### Theming

- The app uses **Material 3** (`androidx.compose.material3`).
- Dynamic colour (Material You) is supported on API 31+; a static fallback
  palette is used on older APIs.
- All colour, shape, and typography references go through `MaterialTheme.*`.
  Hard-coded hex values in composables are a build review failure.
- Dark mode is supported. Test every new screen in both light and dark `@Preview`.

```kotlin
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, name = "Light")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark")
@Composable
fun TimerDisplayPreview() {
    TabataClockTheme {
        TimerDisplay(remainingSeconds = 17, phase = Phase.WORK)
    }
}
```

---

### Accessibility

- Every interactive element has a `contentDescription` or `semantics { }` block.
- The countdown timer uses `LiveRegion` semantics so screen readers announce
  phase changes without user interaction:

```kotlin
Text(
    text = remainingSeconds.toClockString(),
    modifier = Modifier.semantics {
        liveRegion = LiveRegionMode.Polite
    }
)
```

- Minimum touch target: 48×48 dp (enforced by Material 3 components by default).
- Do not disable system font scaling. Use `sp` for text sizes.

---

### Animation

- Phase transitions (WORK → REST → WORK) animate with `AnimatedContent`
  using a crossfade or slide spec.
- The circular progress indicator uses `animateFloatAsState` on `progressFraction`.
- Keep animations under 400 ms; longer durations feel sluggish on a workout timer.
- Respect `LocalReducedMotion` — skip or shorten animations when the
  system accessibility setting is active:

```kotlin
val reducedMotion = LocalReducedMotion.current
val animSpec = if (reducedMotion) snap() else tween(300)
```

---

### UX Conventions

- **No confirmation dialogs during a workout.** Pause is a single tap;
  stop/reset requires a long-press or swipe to prevent accidental cancellation.
- **Sound and haptics are tied to phase transitions**, not to every second.
  Fine-grained feedback annoys users during long intervals.
- **The screen must stay on** during an active workout.
  Use `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` via the `KeepScreenOn`
  composable effect — not a static manifest flag.
- Settings changes take effect at the **next workout start**, never mid-session.

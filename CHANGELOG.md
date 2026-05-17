# Changelog

All notable changes to Tabata Clock are documented here.  
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions follow [Semantic Versioning](https://semver.org/).

---

## [Unreleased]

### Planned
- Coordinated upgrade bundle: AGP 9 + Kotlin 2.3 + KSP2 + Gradle 9  
  _(deferred until Hilt confirms full KSP2 compatibility; Kotlin 2.3 drops KSP1 support)_

---

## [1.1.0] — 2026-05-17

### Added
- **SoundPool pending-play queue** — play requests issued before all four WAVs finish
  loading are queued in a `ConcurrentLinkedQueue` and drained via `onLoadCompleteListener`.
  Eliminates silent dropped beeps on cold start.
- **`POST_NOTIFICATIONS` permission flow** — requested at the moment the user taps Start
  (Android 13+). A `Snackbar` is shown if denied so the timer still runs without a
  notification, rather than crashing or silently failing.
- **Predictive back gesture** — `enableOnBackInvokedCallback="true"` in `AndroidManifest.xml`
  enables smooth system-back animations on Android 13+.
- **`ServiceNotifier` abstraction** — `IntentServiceNotifier` (production) and
  `NoOpServiceNotifier` (tests/default) replace direct `Context.startForegroundService()`
  calls in `TabataViewModel`, making the ViewModel fully unit-testable without a live
  Android `Service`.
- **`TabataSettings.validated()`** — factory method that clamps all fields to their valid
  ranges before a settings object is constructed; prevents impossible timer states from
  reaching the coroutine loop.
- **`phaseProgress` guard branch** — `phaseDurationSecs == 0` returns `1.0f` instead of
  throwing `ArithmeticException`; verified by a dedicated test.
- **Multi-set cycle test** — new `@Test` in the `Full cycle` suite confirms DONE is reached
  correctly after N × M rounds across multiple sets.
- **ADR-009 through ADR-011** — decision records covering `ServiceNotifier` abstraction,
  SoundPool load-queue strategy, and `POST_NOTIFICATIONS` UX pattern.
- **`CONTRIBUTING.md`** — contribution guide covering branch strategy, commit conventions,
  test requirements, and PR checklist.
- **GitHub Actions path filtering** — `build.yml` now only triggers on changes to `app/`,
  `scripts/`, `gradle/`, build config files, or the workflow itself; doc-only commits no
  longer spin up a full Android build.

### Fixed
- **`foregroundServiceType`** changed from `health` to `mediaPlayback`. Android 14 requires
  `health`-type services to hold `ACTIVITY_RECOGNITION`, `BODY_SENSORS`, or
  `HIGH_SAMPLING_RATE_SENSORS` — none of which a Tabata timer needs. `mediaPlayback` is
  semantically correct and avoids a `ForegroundServiceStartNotAllowedException` on API 34+.
- **Gradle wrapper JAR bootstrap** upgraded from Gradle 8.9 → **8.14.1** across
  `build.yml`, `codeql.yml`, and `gradle-wrapper.properties`.
- **`dependency-submission.yml`** updated to Gradle 8.14.1 to stay in sync with the
  build wrapper.

### Changed
- Kotlin upgraded from `2.0.0` → `2.1.21`.
- Hilt upgraded from `2.51.1` → `2.57.2`; KSP updated to `2.1.21-2.0.1`.
- Compose BOM verified at `2025.11.00` — current latest stable per Google Maven.
- AGP upgraded from `8.4.x` → `8.10.1`.
- All Lifecycle, Navigation, DataStore, Activity, and AndroidX Core libraries bumped
  to latest stable.
- `TabataViewModel` refactored to `AndroidViewModel` to safely hold `Application` context
  for foreground service lifecycle management.
- `runPhase()` now uses `updateAndGet` for atomic state updates, removing a potential
  race between write and the subsequent read of `_state.value`.
- `codeql.yml` already had `paths-ignore` for markdown/text; `build.yml` now has an
  equivalent `paths` allowlist.

---

## [1.0.0] — 2026-04-01

### Added
- Initial release of Tabata Clock.
- Phase-coloured full-screen UI: Deep Blue (prepare), Deep Red (work), Deep Green (rest),
  Near Black (done) with 400 ms `animateColorAsState` transitions.
- Animated white progress arc — 800 ms tween per phase tick.
- Countdown beep (880 Hz, 120 ms) on the last 3 seconds of each phase.
- Haptic vibration (150 ms one-shot) on every phase transition.
- Fully configurable: prepare time, work time, rest time, rounds per set, number of sets.
- Persistent settings via DataStore Preferences (survive process death).
- Foreground Service with live notification (phase / time remaining / round).
- Synthesised WAV sounds generated from Python stdlib — no binary assets in git.
- MVVM architecture with `TabataViewModel`, `StateFlow<TimerState>`, and coroutine timer
  loop (`runTabataCycle`).
- Hilt dependency injection with interface-backed fakes for full unit testability.
- 26 unit tests across 7 `@Nested` suites (JUnit 5 + Turbine + MockK +
  `kotlinx-coroutines-test`).
- Kover XML + HTML coverage reports generated on every CI run.
- GitHub Actions CI (`build.yml`): sounds → tests → coverage → debug APK.
- GitHub Actions release (`release.yml`): signed APK + GitHub Release on `v*` tags.
- CodeQL static analysis (`codeql.yml`): weekly + every push/PR to `main`.
- Architecture Decision Records (ADR-001 – ADR-008) in `docs/adr/`.

[Unreleased]: https://github.com/kostasmavridis/tabata-clock/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/kostasmavridis/tabata-clock/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/kostasmavridis/tabata-clock/releases/tag/v1.0.0

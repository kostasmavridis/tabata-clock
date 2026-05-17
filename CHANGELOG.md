# Changelog

All notable changes to Tabata Clock are documented here.  
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions follow [Semantic Versioning](https://semver.org/).

---

## [Unreleased]

### Security
- **Gradle binary checksum verification** added to all three workflows
  (`build.yml`, `release.yml`, `codeql.yml`). The SHA-256 of the
  downloaded `gradle-*-bin.zip` is verified against the hash published at
  https://gradle.org/release-checksums/ before `unzip` is executed,
  preventing MITM or CDN tampering from silently executing a malicious
  build tool.  
  ⚠️ **Action required after merge:** update `EXPECTED_SHA256` in all
  three bootstrap steps whenever `GRADLE_VERSION` is bumped. The correct
  hash for each release is published at https://gradle.org/releases/ under
  "checksums".
- **Signing credentials removed from Gradle `-P` project properties**
  (`release.yml`). `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`
  were previously passed as `-Pandroid.injected.signing.*` flags, which
  are visible to any co-tenant process that can read `/proc/*/cmdline` on
  the runner and appear in Gradle `--info` logs. They are now passed
  exclusively via `env:` and read by `build.gradle.kts` through
  `System.getenv()`.
- **Script injection guard for `github.ref`** (`release.yml`). All
  `${{ github.* }}` and `${{ steps.*.outputs.* }}` expressions inside
  `run:` blocks are now routed through `env:` variables so that a
  tag name containing shell metacharacters cannot execute arbitrary code.
  `GITHUB_OUTPUT` redirects are quoted (`>> "$GITHUB_OUTPUT"`).
- **`KEYSTORE_BASE64` secret routed through `env:`** in all three
  workflows. Previously the secret was interpolated inline into the shell
  string (`echo "${{ secrets.KEYSTORE_BASE64 }}"`), increasing exposure
  surface even though GitHub masks the value in logs.
- **Keystore file cleaned up after every build step** (`build.yml`,
  `release.yml`, `codeql.yml`). An `if: always()` step removes the
  decoded keystore from disk so that subsequent steps (third-party
  actions, build scan uploads) cannot read it.
- **`echo KEY_ALIAS` removed from `build.yml`**. The debug signing step
  previously printed `KEY_ALIAS=[value]` to the public CI log. The
  bracket wrapping can defeat GitHub's secret masking. The `keytool
  -list` call that validates the keystore is retained; only the `echo`
  is removed.
- **`SoundManager.reinitialise()` and `release()` made `@Synchronized`**.
  `SoundPool.release()` is called from the main thread while
  `onLoadCompleteListener` fires on a SoundPool internal thread. A
  `release()` call mid-drain of `pendingPlays` could cause a use-after-free
  on the native `SoundPool`. Both methods are now `@Synchronized`.
- **`TabataForegroundService` exception handler narrowed** from
  `catch (e: Exception)` (which silently swallows all unexpected runtime
  exceptions) to two explicit catches: `SecurityException` (missing
  `POST_NOTIFICATIONS` permission) and `IllegalStateException`
  (`ForegroundServiceStartNotAllowedException` extends
  `IllegalStateException` on Android 12+). Any other unexpected exception
  now propagates normally.

### Fixed
- **Silent audio after screen-off on Oppo/OnePlus devices.** The OS silently
  invalidates native AudioTrack sessions during extended screen-off periods;
  `SoundPool.play()` then returns 0 with no error while vibration continues to
  work normally (it uses a system `Vibrator` service). Fix: `SoundPool` is now
  unconditionally rebuilt on every `Activity.onResume()` via
  `ISoundManager.reinitialise()` → `SoundManager.buildPool()`. The rebuild is
  guarded in `TabataViewModel.onAppForegrounded()` so it is skipped while the
  timer is actively running, avoiding any mid-workout audio gap.
- **Pending-play dedup was ineffective.** The previous check compared
  `lambda.javaClass` for two separate lambdas capturing different `Int` soundIds
  — both resolve to the same anonymous class, so the guard never fired. Replaced
  with a `Collections.synchronizedSet<Int>` tracking pending soundIds directly.

### Planned
- Upgrade to Kotlin 2.3.x + KSP2 as a coordinated bundle, deferred until Hilt
  ships a `kotlin-metadata-jvm` version that supports metadata 2.3.0.
  (Hilt 2.57.2 caps at metadata 2.2.0; Kotlin 2.3.x writes 2.3.0.)

---

## [1.2.0] — 2026-05-17

### Fixed
- **`hiltViewModel` deprecation warning** — updated import from
  `androidx.hilt.navigation.compose` to `androidx.hilt.lifecycle.viewmodel.compose`
  in `TimerScreen.kt` and `SettingsScreen.kt`.
- **`ui-tooling` dependency resolution failure** — added
  `debugImplementation(platform(libs.androidx.compose.bom))` to `app/build.gradle.kts`
  so that debug-scoped Compose libraries have a BOM version anchor.
- **`compilerOptions` block placement** — moved from inside `android {}` to the
  top-level `kotlin {}` extension block for AGP 8.10.x / Kotlin 2.2.x compatibility.
- **Kotlin downgraded 2.3.21 → 2.2.21** — Hilt 2.57.2's bundled `kotlin-metadata-jvm`
  supports a maximum metadata version of 2.2.0; Kotlin 2.3.x writes version 2.3.0,
  causing `hiltJavaCompileDebug` to fail with `IllegalArgumentException`.

### Changed
- **Kotlin** `2.1.21` → `2.2.21`.
- **KSP** `2.1.21-2.0.1` → `2.2.21-2.0.5` (paired release for Kotlin 2.2.21).
- **Compose BOM** `2025.11.00` → `2026.05.00` (maps to Compose `1.11.1`).
- **`androidx.activity`** `1.10.1` → `1.13.0`.
- **JUnit 5 (Jupiter)** `5.13.1` → `5.13.4`.
- **`junit-platform-launcher`** `1.13.1` → `1.13.4` (kept in sync with Jupiter).
- **`actions/upload-artifact`** `v4.6.2` → `v7.0.1` in `build.yml` — eliminates
  `DEP0169 url.parse()` Node.js deprecation warning from artifact upload steps.
- **Target SDK** `35` → `36`.

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

[Unreleased]: https://github.com/kostasmavridis/tabata-clock/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/kostasmavridis/tabata-clock/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/kostasmavridis/tabata-clock/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/kostasmavridis/tabata-clock/releases/tag/v1.0.0

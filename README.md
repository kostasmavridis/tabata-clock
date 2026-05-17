<div align="center">

# ⏱ Tabata Clock

**A clean, minimal Tabata interval timer for Android**  
Built with Kotlin · Jetpack Compose · MVVM · Hilt · Coroutines

---

### CI & Quality
[![Android CI](https://github.com/kostasmavridis/tabata-clock/actions/workflows/build.yml/badge.svg)](https://github.com/kostasmavridis/tabata-clock/actions/workflows/build.yml)
[![CodeQL](https://github.com/kostasmavridis/tabata-clock/actions/workflows/codeql.yml/badge.svg)](https://github.com/kostasmavridis/tabata-clock/actions/workflows/codeql.yml)
[![Tests](https://img.shields.io/badge/Tests-26%20passing-4CAF50?logo=junit5&logoColor=white)](#testing-strategy)
[![Coverage](https://img.shields.io/badge/Coverage-Kover%20report-blueviolet?logo=kotlin&logoColor=white)](https://github.com/kostasmavridis/tabata-clock/actions/workflows/build.yml)

### Release & Distribution
[![Release](https://github.com/kostasmavridis/tabata-clock/actions/workflows/release.yml/badge.svg)](https://github.com/kostasmavridis/tabata-clock/actions/workflows/release.yml)
[![Latest Release](https://img.shields.io/github/v/release/kostasmavridis/tabata-clock?label=latest&color=0D47A1&logo=android)](https://github.com/kostasmavridis/tabata-clock/releases/latest)

### Stack
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose BOM](https://img.shields.io/badge/Compose%20BOM-2024.08.00-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Hilt](https://img.shields.io/badge/Hilt-2.51.1-FF6F00?logo=google&logoColor=white)](https://dagger.dev/hilt/)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-brightgreen?logo=android)](https://developer.android.com/about/versions/oreo)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-brightgreen?logo=android)](https://developer.android.com/about/versions/15)

### Community
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/kostasmavridis/tabata-clock/pulls)

</div>

---

## What is Tabata?

Tabata is a high-intensity interval training (HIIT) protocol developed by Dr. Izumi Tabata. Each set consists of **8 rounds** of **20 seconds work** followed by **10 seconds rest**, totalling 4 minutes per set. This app implements the full protocol with configurable durations so you can adapt it to any training style.

---

## Features

- 🟥 **Phase-coloured full-screen UI** — Deep Blue (prepare) → Deep Red (work) → Deep Green (rest), animated transitions
- ⭕ **Animated progress arc** — white ring sweeps clockwise around the countdown circle each phase
- 🔔 **Countdown beep** on the last 3 seconds of any phase
- 📳 **Haptic vibration** on every phase transition
- ⚙️ **Fully configurable** — prepare time, work time, rest time, rounds per set, number of sets
- 💾 **Persistent settings** — saved with DataStore, survive process death
- 🔒 **Foreground Service** — timer keeps running when the screen is off, with a live notification showing current phase, time remaining and round
- 🔔 **Smart notification permission** — `POST_NOTIFICATIONS` is requested at the moment you tap Start (Android 13+), with a snackbar fallback if denied so the timer always runs
- 🎵 **Synthesised sounds** — all four WAV files auto-generated from Python stdlib (no binary assets in git)
- 🔄 **Reliable first-beep** — SoundPool play requests are queued if sounds haven't finished loading yet; no silent dropped beeps on cold start
- 🔙 **Predictive back gesture** — `enableOnBackInvokedCallback` enabled for smooth Android 13+ back animations
- ✅ **Unit tested** — 26 tests with JUnit 5, Turbine, MockK and `kotlinx-coroutines-test`
- 📊 **Code coverage** — Kover XML report generated on every CI run
- 🤖 **GitHub Actions CI** — tests + coverage + debug APK on every push to `main` (skipped for doc-only changes)

---

## Screenshots

| Get Ready | Work | Rest | Settings |
|:---------:|:----:|:----:|:--------:|
| 🔵 Blue   | 🔴 Red | 🟢 Green | ⚙️ |

> Run the app on a device or emulator to see the animated arc and colour transitions in action.

---

## Tech Stack

| Layer | Technology | Notes |
|---|---|---|
| Language | **Kotlin 2.0** | `data class`, coroutines, extension functions |
| UI | **Jetpack Compose + Material 3** | Declarative, no XML layouts |
| Architecture | **MVVM + StateFlow** | Single source of truth in ViewModel |
| Timer Engine | **Coroutines** (`viewModelScope`) | Leak-safe; no `CountDownTimer` or `Handler` |
| Persistence | **DataStore Preferences** | Async, Flow-based; replaces SharedPreferences |
| DI | **Hilt 2.51** | Constructor injection via interfaces |
| Audio | **SoundPool** | Low-latency; play requests queued until all sounds loaded |
| Haptics | **VibrationEffect** | API 26+, one-shot 150ms pulse |
| Background | **Foreground Service** | `foregroundServiceType="mediaPlayback"` — no sensor permissions required |
| Permissions | **POST_NOTIFICATIONS** | Requested on first Start tap (Android 13+); snackbar if denied |
| Back gesture | **OnBackInvokedCallback** | `enableOnBackInvokedCallback="true"` — predictive back on Android 13+ |
| Navigation | **Navigation Compose** | `Timer` ↔ `Settings` destinations |
| Testing | **JUnit 5 + Turbine + MockK** | `StandardTestDispatcher` for virtual time |
| Coverage | **Kover 0.8** | XML + HTML reports |
| CI | **GitHub Actions** | Python sounds → tests → coverage → APK (path-filtered; skips doc-only commits) |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                             │
│  TimerScreen.kt          SettingsScreen.kt                  │
│  (Jetpack Compose)        (Jetpack Compose)                 │
└──────────────────────────┬──────────────────────────────────┘
                           │ collectAsStateWithLifecycle()
┌──────────────────────────▼──────────────────────────────────┐
│                    ViewModel Layer                          │
│  TabataViewModel                                            │
│  ├── StateFlow<TimerState>   (phase, secondsLeft, rounds…)  │
│  ├── StateFlow<TabataSettings>                              │
│  └── coroutine timer loop   (runTabataCycle)                │
└────────────┬─────────────────────────┬───────────────────── ┘
             │                         │
┌────────────▼──────────┐  ┌───────────▼────────────────────┐
│   ISettingsRepository │  │       ISoundManager            │
│   (DataStore)         │  │  (SoundPool + VibrationEffect) │
└───────────────────────┘  └────────────────────────────────┘
             │
┌────────────▼──────────────────────────────────────────────┐
│              ServiceNotifier (interface)                  │
│  ├── IntentServiceNotifier  — starts/updates/stops        │
│  │   TabataForegroundService via Intents (production)     │
│  └── NoOpServiceNotifier    — silent no-op (tests)        │
└───────────────────────────────────────────────────────────┘
             │
┌────────────▼──────────────────────────────────────────────┐
│              TabataForegroundService                      │
│  Persistent notification — phase / seconds / round        │
│  foregroundServiceType="mediaPlayback"                    │
│  Started by ViewModel on play, stopped on reset/done      │
└───────────────────────────────────────────────────────────┘
```

### Key design decisions

- **Interfaces over concretions** — `ISoundManager`, `ISettingsRepository`, and `ServiceNotifier` are all injected into `TabataViewModel`, meaning the ViewModel has zero dependency on Android framework classes. Fakes implement the interfaces directly in tests.
- **`ServiceNotifier` abstraction** — production code uses `IntentServiceNotifier` to start/update/stop `TabataForegroundService` via Intents. Tests use `NoOpServiceNotifier` as a silent default, keeping the ViewModel fully unit-testable without a running Service.
- **`AndroidViewModel` for service control** — `Application` context is needed to start/stop the `ForegroundService`. `AndroidViewModel` provides it safely without leaking an `Activity`.
- **`phaseProgress` as a computed property** — derived from `secondsLeft / phaseDurationSecs` inside `TimerState` data class; no extra state field, no risk of drift.
- **`updateAndGet`** — used in `runPhase()` to atomically update state and capture the new value in one call, avoiding a second `value` read.
- **`foregroundServiceType="mediaPlayback"`** — `health` type was incorrect; Android 14 enforces that `health` services hold a sensor permission (`ACTIVITY_RECOGNITION`, `BODY_SENSORS`, or `HIGH_SAMPLING_RATE_SENSORS`). A Tabata timer plays audio cues — `mediaPlayback` is semantically correct and requires no sensor permissions.
- **SoundPool pending-play queue** — `SoundPool` decodes audio asynchronously after `load()`. Play calls fired before all four sounds are ready are queued in a `ConcurrentLinkedQueue` and drained in `onLoadCompleteListener`, preventing silent dropped beeps on cold start.
- **No `gradle-wrapper.jar` in version control** — the GitHub Contents API silently corrupts binary files pushed through it. CI bootstraps the JAR by downloading the official Gradle distribution and running `gradle wrapper` on every build.

---

## Project Structure

```
tabata-clock/
├── .github/
│   └── workflows/
│       ├── build.yml              # CI: sounds → tests → coverage → APK (path-filtered)
│       └── release.yml            # Release: signed APK + GitHub Release
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/kostasmavridis/tabataclock/
│       │   │   ├── audio/
│       │   │   │   ├── ISoundManager.kt           # Interface
│       │   │   │   └── SoundManager.kt            # SoundPool + haptics impl (pending-play queue)
│       │   │   ├── data/
│       │   │   │   ├── ISettingsRepository.kt     # Interface
│       │   │   │   └── SettingsRepository.kt      # DataStore impl
│       │   │   ├── di/
│       │   │   │   └── AppModule.kt               # Hilt bindings
│       │   │   ├── model/
│       │   │   │   ├── TabataPhase.kt             # PREPARE / WORK / REST / DONE
│       │   │   │   └── TabataSettings.kt          # Data class + validated() factory
│       │   │   ├── service/
│       │   │   │   ├── ServiceNotifier.kt         # Interface
│       │   │   │   ├── IntentServiceNotifier.kt   # Production impl (Intent-based, permission-guarded)
│       │   │   │   ├── NoOpServiceNotifier.kt     # Test/default no-op impl
│       │   │   │   └── TabataForegroundService.kt # mediaPlayback foreground service
│       │   │   ├── ui/
│       │   │   │   ├── navigation/NavGraph.kt
│       │   │   │   ├── screen/
│       │   │   │   │   ├── TimerScreen.kt         # Permission request on Start + snackbar
│       │   │   │   │   └── SettingsScreen.kt      # Sliders + steppers
│       │   │   │   └── theme/Theme.kt             # Dark Material 3 + PhaseColors
│       │   │   ├── viewmodel/
│       │   │   │   └── TabataViewModel.kt         # Core timer logic
│       │   │   ├── MainActivity.kt
│       │   │   └── TabataApp.kt                   # @HiltAndroidApp
│       │   └── res/
│       │       ├── drawable/ic_timer_notification.xml
│       │       ├── raw/                           # WAVs generated at build time
│       │       └── values/
│       │           ├── strings.xml
│       │           └── themes.xml
│       └── test/
│           └── java/com/kostasmavridis/tabataclock/
│               ├── FakeSoundManager.kt            # ISoundManager fake
│               ├── FakeSettingsRepository.kt      # ISettingsRepository fake
│               └── TabataViewModelTest.kt         # 26 tests, 7 suites
├── docs/
│   └── adr/                       # Architecture Decision Records (ADR-001 – ADR-011)
├── scripts/
│   ├── generate_sounds.py         # Generates 4 WAV files (stdlib only)
│   └── README.md
├── gradle/wrapper/
│   └── gradle-wrapper.properties  # Gradle 8.14.1 — JAR bootstrapped by CI
├── build.gradle.kts
├── settings.gradle.kts
├── CONTRIBUTING.md
├── gradlew / gradlew.bat
└── .gitignore
```

---

## Getting Started

### Prerequisites

| Tool | Version |
|---|---|
| Android Studio | Ladybug 2024.2+ |
| JDK | 17 |
| Android SDK | 35 |
| Python | 3.8+ (for sound generation) |
| Gradle | 8.14.1 (wrapper auto-downloads) |

### Clone & Run

```bash
git clone https://github.com/kostasmavridis/tabata-clock.git
cd tabata-clock

# 1. Generate sound files (Python stdlib only — no pip needed)
python scripts/generate_sounds.py

# 2. Bootstrap the Gradle wrapper JAR (first time only)
#    Android Studio does this automatically on project sync
gradle wrapper --gradle-version 8.14.1 --distribution-type bin

# 3. Build & install debug APK
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or simply **open in Android Studio** → sync Gradle, run `python scripts/generate_sounds.py` first, then hit ▶ Run.

### Run Tests

```bash
# All unit tests
./gradlew test

# With HTML coverage report
./gradlew koverHtmlReport
open app/build/reports/kover/html/index.html   # macOS
start app\build\reports\kover\html\index.html  # Windows
```

---

## CI / CD Pipeline

### `build.yml` — runs on every push to `main` and every PR

> **Path-filtered** — only triggers when files under `app/`, `scripts/`, `gradle/`, build config files, or `build.yml` itself change. Commits that touch only `README.md`, `docs/`, or other workflow files do **not** trigger a build.

```
checkout
    │
    ▼
generate_sounds.py             # writes 4 WAV files into res/raw/
    │
    ▼
Bootstrap gradle-wrapper.jar   # downloads Gradle 8.14.1, runs `gradle wrapper`
    │
    ▼
./gradlew test                 # JUnit 5 — 26 tests across 7 suites
    │
    ▼
./gradlew koverXmlReport       # uploads coverage XML as artifact
    │
    ▼
./gradlew assembleDebug        # uploads app-debug.apk as artifact
```

### `release.yml` — runs on `v*` tags (e.g. `v1.0.0`)

```
checkout → generate sounds → bootstrap wrapper
    │
    ▼
./gradlew assembleRelease      # signed with keystore from GitHub Secrets
    │
    ▼
GitHub Release created         # APK attached as release asset
```

### `codeql.yml` — runs on push/PR to `main` and weekly on Saturday

> **Path-filtered** — ignores `*.md`, `*.txt`, `dependabot.yml`, and `scripts/` changes.

### Required GitHub Secrets (for signed builds)

| Secret | Description |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded `.jks` / `.keystore` file |
| `KEYSTORE_PASSWORD` | Store password (`-storepass`) |
| `KEY_ALIAS` | Key alias used when generating the keystore |
| `KEY_PASSWORD` | Key password (`-keypass`) |

> **Why is `gradle-wrapper.jar` not committed?**  
> The GitHub Contents API silently corrupts binary files pushed through it. The bootstrap step downloads the official Gradle distribution and regenerates the JAR fresh on every CI run.

---

## Sound Design

All sounds are synthesised from pure sine waves using only the Python standard library (`math`, `wave`, `struct`). No external dependencies, no binary assets in version control.

| File | Frequency / Notes | Duration | When played |
|---|---|---|---|
| `beep.wav` | 880 Hz (A5) | 120 ms | Last 3 s of any phase |
| `work_start.wav` | C5 → E5 ascending | 360 ms | Start of each Work interval |
| `rest_start.wav` | E5 → C5 descending | 360 ms | Start of each Rest interval |
| `done.wav` | C5 → E5 → G5 + held C major chord | ~1 s | Workout complete |

To customise sounds, edit `scripts/generate_sounds.py` and re-run it, or drop your own WAV/MP3/OGG files into `app/src/main/res/raw/` (they are `.gitignore`d).

---

## Tabata Phase Reference

| Phase | Background | Label | Default Duration |
|---|---|---|---|
| Prepare | 🔵 `#1565C0` Deep Blue | Get Ready | 10 s |
| Work | 🔴 `#B71C1C` Deep Red | WORK | 20 s |
| Rest | 🟢 `#1B5E20` Deep Green | REST | 10 s |
| Done | ⚫ `#212121` Near Black | Done! | — |

All phase transitions are animated with a 400 ms `animateColorAsState` tween. The progress arc uses an 800 ms tween so it glides rather than jumps each second.

---

## Testing Strategy

### Unit Tests (`app/src/test/`)

The test suite has **26 tests across 7 `@Nested` suites** using JUnit 5:

| Suite | What is verified |
|---|---|
| `Initial state` | Phase = PREPARE, not running, secondsLeft = prepareSecs, round = 1 |
| `start()` | Sets `isRunning`, double-start idempotent, countdown ticks, PREPARE → WORK transition |
| `pause() / resume()` | Freezes timer, seconds don't change while paused, resume restores state, countdown continues from paused position, no-op guard |
| `reset()` | Returns to PREPARE, correct `secondsLeft`, clears `isPaused` |
| `Full cycle` | DONE reached, `playWork()` × rounds, `playRest()` × (rounds−1), `playDone()` × 1, `playBeep()` in last 3 s, multi-set cycle |
| `phaseProgress` | 0.0 at start, 1.0 at DONE, 1.0 when `phaseDurationSecs` is 0 (guard branch) |
| `Settings` | `updateSettings()` persists, parametrized across 3 setting combinations |
| `TabataSettings model` | Default values, `totalWorkoutSecs()` with 1 set and 2 sets, parametrized by round count, `validated()` clamps out-of-range values |

### Coverage

Kover generates both **XML** (consumed by CI) and **HTML** (human-readable) reports on every `build.yml` run. The XML artifact is available under **Actions → build → Artifacts**. Excluded from coverage: generated Hilt classes (`*_HiltModules*`, `*_Factory*`), DI modules, and Compose singleton lambdas.

### Test infrastructure

- **`FakeSoundManager`** implements `ISoundManager` — silent, counts calls per method (`beepCount`, `workCount`, `restCount`, `doneCount`)
- **`FakeSettingsRepository`** implements `ISettingsRepository` — `MutableStateFlow`-backed, exposes `.flow` for direct mutation in tests, no DataStore
- **`NoOpServiceNotifier`** is the default in `TabataViewModel` — no `Intent`s fired during tests
- **`StandardTestDispatcher`** + `advanceTimeBy()` — `delay(1_000L)` in the ViewModel becomes instant; a full 13-second cycle completes in < 1 ms
- **Turbine** — `flow.test { awaitItem() }` for asserting on emitted `StateFlow` values

### What is intentionally NOT unit tested

| Class | Reason |
|---|---|
| `MainActivity`, `TabataApp` | Android framework lifecycle — requires instrumentation |
| `SoundManager` | Wraps `SoundPool` and `VibrationEffect` — Android system services |
| `SettingsRepository` | Wraps `DataStore` — requires Android context and filesystem |
| `TabataForegroundService` | Bound to Android `Service` lifecycle |
| `TimerScreen`, `SettingsScreen` | Compose UI — requires instrumentation or Compose test rules |
| Hilt `di/` classes | Generated code — excluded from Kover reports |

---

## Architecture Decision Records

Significant architectural choices are documented as ADRs in [`docs/adr/`](docs/adr/). Key decisions covered:

| ADR | Decision |
|---|---|
| [ADR-001](docs/adr/001-mvvm-architecture.md) | MVVM + StateFlow over MVP / MVI |
| [ADR-002](docs/adr/002-jetpack-compose-ui.md) | Jetpack Compose over XML layouts |
| [ADR-003](docs/adr/003-hilt-dependency-injection.md) | Hilt over Koin / manual DI |
| [ADR-004](docs/adr/004-foreground-service-over-workmanager.md) | Foreground Service over WorkManager |
| [ADR-005](docs/adr/005-datastore-over-sharedpreferences.md) | DataStore over SharedPreferences |
| [ADR-008](docs/adr/008-gradle-wrapper-jar-bootstrap.md) | Gradle wrapper JAR bootstrap strategy + CI pitfalls |

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full guide. Quick summary:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Run `python scripts/generate_sounds.py` and `gradle wrapper --gradle-version 8.14.1` once after cloning
4. Make your changes and add tests
5. Run `./gradlew test` — all 26 tests must pass
6. Open a pull request against `main`

---

## License

```
MIT License

Copyright (c) 2026 Kostas Mavridis

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
provided to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```

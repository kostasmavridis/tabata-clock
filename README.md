<div align="center">

# ⏱ Tabata Clock

**A clean, minimal Tabata interval timer for Android**  
Built with Kotlin · Jetpack Compose · MVVM · Hilt · Coroutines

---

[![Android CI](https://github.com/kostasmavridis/tabata-clock/actions/workflows/build.yml/badge.svg)](https://github.com/kostasmavridis/tabata-clock/actions/workflows/build.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.08-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-brightgreen?logo=android)](https://developer.android.com/about/versions/oreo)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-brightgreen?logo=android)](https://developer.android.com/about/versions/15)
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
- 🎵 **Synthesised sounds** — all four WAV files auto-generated from Python stdlib (no binary assets in git)
- ✅ **Unit tested** — 20 tests with JUnit 5, Turbine, MockK and `kotlinx-coroutines-test`
- 📊 **Code coverage** — Kover XML report generated on every CI run
- 🤖 **GitHub Actions CI** — tests + coverage + debug APK on every push to `main`

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
| Audio | **SoundPool** | Low-latency; loaded once at startup |
| Haptics | **VibrationEffect** | API 26+, one-shot 150ms pulse |
| Background | **Foreground Service** | `foregroundServiceType="health"` |
| Navigation | **Navigation Compose** | `Timer` ↔ `Settings` destinations |
| Testing | **JUnit 5 + Turbine + MockK** | `StandardTestDispatcher` for virtual time |
| Coverage | **Kover 0.8** | XML + HTML reports |
| CI | **GitHub Actions** | Python sounds → tests → coverage → APK |

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
│              TabataForegroundService                      │
│  Persistent notification — phase / seconds / round        │
│  Started by ViewModel on play, stopped on reset/done      │
└───────────────────────────────────────────────────────────┘
```

### Key design decisions

- **Interfaces over concretions** — `ISoundManager` and `ISettingsRepository` are injected into `TabataViewModel`, meaning the ViewModel has zero dependency on Android framework classes. Fakes implement the interfaces directly in tests.
- **`AndroidViewModel` for service control** — `Application` context is needed to start/stop the `ForegroundService`. `AndroidViewModel` provides it safely without leaking an `Activity`.
- **`phaseProgress` as a computed property** — derived from `secondsLeft / phaseDurationSecs` inside `TimerState` data class; no extra state field, no risk of drift.
- **`updateAndGet`** — used in `runPhase()` to atomically update state and capture the new value in one call, avoiding a second `value` read.

---

## Project Structure

```
tabata-clock/
├── .github/
│   └── workflows/
│       └── build.yml              # CI: sounds → tests → coverage → APK
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/kostasmavridis/tabataclock/
│       │   │   ├── audio/
│       │   │   │   ├── ISoundManager.kt       # Interface
│       │   │   │   └── SoundManager.kt        # SoundPool + haptics impl
│       │   │   ├── data/
│       │   │   │   ├── ISettingsRepository.kt # Interface
│       │   │   │   └── SettingsRepository.kt  # DataStore impl
│       │   │   ├── di/
│       │   │   │   └── AppModule.kt           # Hilt bindings
│       │   │   ├── model/
│       │   │   │   ├── TabataPhase.kt         # PREPARE / WORK / REST / DONE
│       │   │   │   └── TabataSettings.kt      # Data class + totalWorkoutSecs()
│       │   │   ├── service/
│       │   │   │   └── TabataForegroundService.kt
│       │   │   ├── ui/
│       │   │   │   ├── navigation/NavGraph.kt
│       │   │   │   ├── screen/
│       │   │   │   │   ├── TimerScreen.kt     # Progress arc + phase colours
│       │   │   │   │   └── SettingsScreen.kt  # Sliders + steppers
│       │   │   │   └── theme/Theme.kt         # Dark Material 3 + PhaseColors
│       │   │   ├── viewmodel/
│       │   │   │   └── TabataViewModel.kt     # Core timer logic
│       │   │   ├── MainActivity.kt
│       │   │   └── TabataApp.kt              # @HiltAndroidApp
│       │   └── res/
│       │       ├── drawable/ic_timer_notification.xml
│       │       ├── raw/                       # WAVs generated at build time
│       │       └── values/
│       │           ├── strings.xml
│       │           └── themes.xml
│       └── test/
│           └── java/com/kostasmavridis/tabataclock/
│               ├── FakeSoundManager.kt        # ISoundManager fake
│               ├── FakeSettingsRepository.kt  # ISettingsRepository fake
│               └── TabataViewModelTest.kt     # 20 tests, 6 suites
├── scripts/
│   ├── generate_sounds.py         # Generates 4 WAV files (stdlib only)
│   └── README.md
├── gradle/wrapper/
│   └── gradle-wrapper.properties  # Gradle 8.7 — JAR bootstrapped by CI
├── build.gradle.kts
├── settings.gradle.kts
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
| Gradle | 8.7 (wrapper auto-downloads) |

### Clone & Run

```bash
git clone https://github.com/kostasmavridis/tabata-clock.git
cd tabata-clock

# 1. Generate sound files (Python stdlib only — no pip needed)
python scripts/generate_sounds.py

# 2. Bootstrap the Gradle wrapper JAR (first time only)
#    Android Studio does this automatically on project sync
gradle wrapper --gradle-version 8.7 --distribution-type bin

# 3. Build & install debug APK
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or simply **open in Android Studio** → it will sync Gradle, generate nothing, so run the sound script first, then hit ▶ Run.

### Run Tests

```bash
# All unit tests
./gradlew test

# With HTML coverage report
./gradlew koverHtmlReport
open app/build/reports/kover/html/index.html
```

---

## CI / CD Pipeline

Every push to `main` and every pull request triggers the GitHub Actions workflow:

```
checkout
    │
    ▼
generate_sounds.py          # writes 4 WAV files into res/raw/
    │
    ▼
Bootstrap gradle-wrapper.jar  # downloads Gradle 8.7, runs `gradle wrapper`
    │                         # (binary JAR cannot be committed via GitHub API)
    ▼
./gradlew test              # JUnit 5 — 20 tests across 6 suites
    │
    ▼
./gradlew koverXmlReport    # uploads coverage XML as artifact
    │
    ▼
./gradlew assembleDebug     # uploads app-debug.apk as artifact
```

> **Why is `gradle-wrapper.jar` not committed?**  
> The GitHub Contents API can only write plain text. Binary `.jar` files pushed through it are silently corrupted. The bootstrap step downloads the official Gradle distribution and runs `gradle wrapper` to regenerate the JAR fresh on every CI run.

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

The test suite has **20 tests across 6 `@Nested` suites** using JUnit 5:

| Suite | What is verified |
|---|---|
| `Initial state` | Phase = PREPARE, not running, secondsLeft = prepareSecs, round = 1 |
| `start()` | Sets `isRunning`, double-start idempotent, countdown ticks, PREPARE → WORK transition |
| `pause() / resume()` | Freezes timer, seconds don't change while paused, resume restores state, no-op guard |
| `reset()` | Returns to PREPARE, correct `secondsLeft`, clears `isPaused` |
| `Full cycle` | DONE reached, `playWork()` × rounds, `playRest()` × (rounds−1), `playDone()` × 1 |
| `phaseProgress` | 0.0 at start, 1.0 at DONE |
| `TabataSettings model` | `totalWorkoutSecs()` correctness, parametrized across 3 round counts |

### Test infrastructure

- **`FakeSoundManager`** implements `ISoundManager` — silent, counts calls per method
- **`FakeSettingsRepository`** implements `ISettingsRepository` — `MutableStateFlow`-backed, no DataStore
- **`StandardTestDispatcher`** + `advanceTimeBy()` — `delay(1_000L)` in the ViewModel becomes instant; a full 13-second cycle completes in < 1 ms
- **Turbine** — `flow.test { awaitItem() }` for asserting on emitted `StateFlow` values

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Run `python scripts/generate_sounds.py` and `gradle wrapper --gradle-version 8.7` once after cloning
4. Make your changes and add tests
5. Run `./gradlew test` — all 20 tests must pass
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

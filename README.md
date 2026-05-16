# ⏱ Tabata Clock

A clean, minimal Tabata interval timer for Android built with **Kotlin + Jetpack Compose**, following MVVM architecture.

---

## Features

- **20s Work / 10s Rest × 8 Rounds** (fully configurable)
- Animated phase background colors (blue → red → green)
- Countdown beep on the last 3 seconds of each phase
- Haptic vibration on every phase transition
- Configurable: prepare time, work time, rest time, rounds, sets
- Settings persisted with **DataStore**
- Foreground service keeps timer running with screen off
- **GitHub Actions CI** — auto-builds debug APK on every push

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + StateFlow |
| Timer Engine | Coroutines (`viewModelScope`) |
| Persistence | DataStore Preferences |
| DI | Hilt |
| Audio | SoundPool |
| Background | Foreground Service |
| CI | GitHub Actions |

## Project Structure

```
app/src/main/java/com/kostasmavridis/tabataclock/
├── audio/          SoundManager.kt
├── data/           SettingsRepository.kt
├── di/             AppModule.kt
├── model/          TabataPhase.kt, TabataSettings.kt
├── service/        TabataForegroundService.kt
├── ui/
│   ├── navigation/ NavGraph.kt
│   ├── screen/     TimerScreen.kt, SettingsScreen.kt
│   └── theme/      Theme.kt
├── viewmodel/      TabataViewModel.kt
├── MainActivity.kt
└── TabataApp.kt
```

## Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2+)
- JDK 17
- Android SDK 35

### Build & Run

```bash
git clone https://github.com/kostasmavridis/tabata-clock.git
cd tabata-clock
./gradlew assembleDebug
```

Or open the project in Android Studio and run on an emulator / device (API 26+).

### Sound Files

Add the following MP3/OGG files to `app/src/main/res/raw/`:

| File | When played |
|---|---|
| `beep.mp3` | Last 3 seconds of any phase |
| `work_start.mp3` | Start of each Work interval |
| `rest_start.mp3` | Start of each Rest interval |
| `done.mp3` | Workout complete |

Free sounds available at [freesound.org](https://freesound.org).

## Phase Color Guide

| Phase | Color |
|---|---|
| Get Ready | 🔵 Deep Blue |
| Work | 🔴 Deep Red |
| Rest | 🟢 Deep Green |
| Done | ⚫ Near Black |

## License

MIT License — see [LICENSE](LICENSE) for details.

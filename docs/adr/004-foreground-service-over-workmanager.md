# ADR-004: Foreground Service for Background Timer (over WorkManager)

**Date:** 2026-05-17  
**Status:** Accepted  
**Deciders:** @kostasmavridis  
**Tags:** architecture, background-processing, service

---

## Context

The Tabata timer must continue running while the screen is off and the app is in the background. Android's process lifecycle aggressively kills background apps to reclaim memory. Two primary mechanisms exist for keeping a process alive:

| Mechanism | Suitable for | Timing precision | User visibility | Process survival |
|---|---|---|---|---|
| **Foreground Service** | Long-running user-initiated tasks | Exact (second-by-second) | Persistent notification (required) | High — OS protects FGS processes |
| WorkManager | Deferrable background work, periodic tasks | Inexact (15-min minimum for periodic) | None (optional notification) | Moderate — subject to Doze, batching |
| `AlarmManager` (exact) | Single future events | Exact | None | N/A — wakes process |
| Kotlin coroutine on `Main` only | Foreground-only | Exact | N/A | None — killed when app backgrounds |

The timer requires:
1. **Second-level precision** — the countdown must tick every 1000ms reliably
2. **Continuous execution** for up to ~60 minutes (typical Tabata session)
3. **A user-visible notification** showing the current phase and time remaining
4. **Immediate stop** when the user presses Reset or the cycle completes

## Decision

Use a **Foreground Service** (`TabataForegroundService`) with `foregroundServiceType="health"`.

The timer coroutine runs inside **`viewModelScope`** (not inside the service). The service's only responsibility is to hold a persistent notification and prevent Android from killing the process. The ViewModel drives all state transitions and calls `serviceNotifier.notify(...)` on every tick to update the notification content.

```
ViewModel coroutine (viewModelScope)
  │  tick every 1s
  │  → serviceNotifier.notify(phase, secondsLeft, round)
  │
  ▼
IntentServiceNotifier
  │  startForegroundService(Intent)
  ▼
TabataForegroundService.onStartCommand()
  │  startForeground(NOTIFICATION_ID, buildNotification(...))
  ▼
Persistent notification updated on every tick
```

This design means:
- **The service contains zero business logic** — it is a pure notification shell
- **`START_NOT_STICKY`** is returned from `onStartCommand` — if Android kills the process, the ViewModel coroutine is also dead; there is nothing to restart
- The `PendingIntent` in the notification uses `FLAG_IMMUTABLE` + `FLAG_UPDATE_CURRENT` (see security fix commit `afbb0f9`)

## Consequences

### Positive
- Second-level timing precision guaranteed — no Doze batching, no 15-minute WorkManager minimums
- User always sees the current timer state in the notification tray
- `android:exported="false"` + explicit Intents from `IntentServiceNotifier` mean no third-party app can interact with the service
- Clean separation: ViewModel owns state, Service owns notification lifecycle

### Negative
- Foreground services require a persistent notification — cannot run silently
- `FOREGROUND_SERVICE` permission is required in the manifest
- `foregroundServiceType="health"` requires Android 14 (API 34) declaration — supported by `targetSdk = 35`
- Google Play reviews foreground service usage; incorrect type (`specialUse`) would trigger a policy rejection (fixed in commit `afbb0f9`)

### Mitigations
- The notification uses `IMPORTANCE_LOW` and `setSilent(true)` — it appears in the tray without making sound or vibrating
- `android:exported="false"` prevents any external IPC surface

## Alternatives Considered

### WorkManager
WorkManager's `PeriodicWorkRequest` has a minimum interval of 15 minutes and is subject to Doze mode batching. It cannot provide second-level tick precision. `OneTimeWorkRequest` chaining could approximate it but with higher latency and no guaranteed ordering. Not suitable.

### `AlarmManager` with exact alarms
Exact alarms require `SCHEDULE_EXACT_ALARM` permission (restricted on Android 12+, user-grantable only). Would need one alarm per second — 3,600 alarms for a 1-hour session. Excessive system overhead and battery drain.

### Background coroutine only (no service)
Android kills processes that have no foreground component within a few seconds of backgrounding. The timer would stop immediately when the user locks the screen. Not viable.

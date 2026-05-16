# ADR-006: SoundPool for Audio Cues (over MediaPlayer)

**Date:** 2026-05-17  
**Status:** Accepted  
**Deciders:** @kostasmavridis  
**Tags:** architecture, audio

---

## Context

Tabata Clock plays four distinct audio cues:
- `beep` — 3-second countdown within a phase
- `work` — phase transition to WORK
- `rest` — phase transition to REST
- `done` — workout complete

These are short (< 1 second), may overlap (beep overlaps with work/rest transition), and must play with minimal latency. Two standard Android audio APIs are candidates:

| API | Latency | Simultaneous sounds | Suitable for | Memory |
|---|---|---|---|---|
| `MediaPlayer` | High (100–500ms) | One per instance | Long-form audio, music | Low — streams |
| **`SoundPool`** | Low (< 30ms) | Yes — configurable | Short UI sounds, game audio | Medium — preloaded |

The WAV files are generated at build time by `scripts/generate_sounds.py` using Python's `wave` stdlib module and stored in `res/raw/`. They are small (< 10 KB each) and fully preloadable.

## Decision

Use **`SoundPool`** with `maxStreams = 4` (one per cue type), preloading all four sounds in `SoundManager.init()`.

```kotlin
class SoundManager @Inject constructor(context: Context) : ISoundManager {
    private val soundPool = SoundPool.Builder().setMaxStreams(4).build()
    // sounds loaded in init { ... }
    override fun playBeep() { soundPool.play(beepId, ...) }
    override fun release()  { soundPool.release() }
}
```

The `ISoundManager` interface allows substitution with a `NoOpSoundManager` in unit tests (no audio hardware required).

## Consequences

### Positive
- Sub-30ms playback latency — the beep fires exactly when the coroutine tick fires
- Sounds can overlap (the 3-second beep continues while the work cue plays)
- All sounds are preloaded at app start — no disk I/O on playback
- `SoundPool.release()` is called in `ViewModel.onCleared()` — no resource leak

### Negative
- `SoundPool` holds all sounds in memory simultaneously — acceptable for < 10 KB files but would be wasteful for longer audio
- Requires explicit `release()` management — handled via `ISoundManager.release()` in `onCleared()`

### Mitigations
- Sound files are generated programmatically (pure sine tones) — they are guaranteed to be small
- The `ISoundManager` interface isolates `SoundPool` from all call sites

## Alternatives Considered

### `MediaPlayer`
High latency makes it unsuitable for countdown beeps that must be time-accurate. Cannot easily play multiple sounds simultaneously without multiple `MediaPlayer` instances.

### `ExoPlayer`
Designed for streaming and long-form media. Significant dependency overhead for four sub-second beep sounds.

### Audio generated at runtime (AudioTrack)
Would eliminate the `res/raw/` WAV files entirely. Higher code complexity; no benefit given the simplicity of the current Python generation script.

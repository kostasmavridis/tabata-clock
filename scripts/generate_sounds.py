#!/usr/bin/env python3
"""
Tabata Clock — Sound Generator
================================
Generates all four WAV sound files required by the app and writes them to
  app/src/main/res/raw/

Usage (from project root):
  python scripts/generate_sounds.py

Requires only the Python standard library (no numpy / scipy needed).
Tested on Python 3.8+.
"""

import math
import os
import struct
import wave

SAMPLE_RATE = 44100
OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")


# ── helpers ───────────────────────────────────────────────────────────────────

def sine_wave(freq: float, duration: float, amplitude: float = 0.7) -> list:
    """Pure sine tone at `freq` Hz for `duration` seconds."""
    n = int(SAMPLE_RATE * duration)
    return [amplitude * math.sin(2 * math.pi * freq * i / SAMPLE_RATE) for i in range(n)]


def apply_envelope(samples: list, attack: float = 0.005, release: float = 0.05) -> list:
    """Linear attack/release fade to avoid clicks."""
    n = len(samples)
    a_len = int(attack * SAMPLE_RATE)
    r_len = int(release * SAMPLE_RATE)
    out = list(samples)
    for i in range(a_len):
        out[i] *= i / a_len
    for i in range(r_len):
        idx = n - r_len + i
        if 0 <= idx < n:
            out[idx] *= (r_len - i) / r_len
    return out


def mix(a: list, b: list) -> list:
    """Mix two equal-length sample lists."""
    return [x + y for x, y in zip(a, b)]


def clamp(samples: list) -> list:
    return [max(-1.0, min(1.0, s)) for s in samples]


def write_wav(filename: str, samples: list) -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    path = os.path.join(OUT_DIR, filename)
    samples = clamp(samples)
    pcm = struct.pack(f"<{len(samples)}h", *[int(s * 32767) for s in samples])
    with wave.open(path, "w") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(SAMPLE_RATE)
        wf.writeframes(pcm)
    print(f"  Written: {path}  ({os.path.getsize(path) / 1024:.1f} KB)")


# ── sound definitions ──────────────────────────────────────────────────────────

def make_beep() -> None:
    """Short 880 Hz pip — plays on the last 3 seconds of each phase."""
    samples = apply_envelope(sine_wave(880, 0.12, amplitude=0.6), attack=0.005, release=0.04)
    write_wav("beep.wav", samples)


def make_work_start() -> None:
    """Ascending C5 → E5 two-tone — energetic start signal."""
    c5 = apply_envelope(sine_wave(523.25, 0.18, amplitude=0.7), release=0.06)
    e5 = apply_envelope(sine_wave(659.25, 0.18, amplitude=0.7), release=0.06)
    write_wav("work_start.wav", c5 + e5)


def make_rest_start() -> None:
    """Descending E5 → C5 two-tone — calming transition to rest."""
    e5 = apply_envelope(sine_wave(659.25, 0.18, amplitude=0.6), release=0.06)
    c5 = apply_envelope(sine_wave(523.25, 0.18, amplitude=0.6), release=0.06)
    write_wav("rest_start.wav", e5 + c5)


def make_done() -> None:
    """C major arpeggio (C5-E5-G5) + held chord — workout complete fanfare."""
    t = 0.18
    c5 = apply_envelope(sine_wave(523.25, t, 0.65), release=0.06)
    e5 = apply_envelope(sine_wave(659.25, t, 0.65), release=0.06)
    g5 = apply_envelope(sine_wave(783.99, t, 0.65), release=0.06)
    # Final held chord: C5 + E5 + G5 together
    chord_dur = 0.45
    chord = clamp(mix(
        mix(sine_wave(523.25, chord_dur, 0.28), sine_wave(659.25, chord_dur, 0.28)),
        sine_wave(783.99, chord_dur, 0.28)
    ))
    chord = apply_envelope(chord, attack=0.01, release=0.15)
    write_wav("done.wav", c5 + e5 + g5 + chord)


# ── main ──────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    print("Generating Tabata Clock sound files...")
    make_beep()
    make_work_start()
    make_rest_start()
    make_done()
    print("Done. All sounds written to app/src/main/res/raw/")

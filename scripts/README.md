# scripts/

## generate_sounds.py

Generates all four WAV audio files needed by the app using only the Python standard library (no pip installs required).

### Usage

From the project root:

```bash
python scripts/generate_sounds.py
```

This will write the following files to `app/src/main/res/raw/`:

| File | Description |
|---|---|
| `beep.wav` | Short 880 Hz pip — plays on the last 3 seconds of any phase |
| `work_start.wav` | Ascending C5 → E5 two-tone — energetic Work interval start |
| `rest_start.wav` | Descending E5 → C5 two-tone — calming Rest interval start |
| `done.wav` | C major arpeggio + held chord — workout complete fanfare |

### Requirements

- Python 3.8+
- No external dependencies (uses `math`, `wave`, `struct` from stdlib only)

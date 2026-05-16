# ADR-002: Use `security-extended` Query Suite for CodeQL Analysis

**Date:** 2026-05-17  
**Status:** Accepted  
**Deciders:** @kostasmavridis

---

## Context

GitHub CodeQL offers three built-in query suites for Kotlin/Java:

| Suite | Rules included | False-positive rate | Typical use case |
|---|---|---|---|
| `security-and-quality` | Security + code quality | Low | Default for most projects |
| `security-extended` | All of the above + higher-precision security rules | Medium | Security-conscious production apps |
| `security-experimental` | All of the above + experimental rules | High | Security research |

Tabata Clock is an Android fitness timer. Its attack surface is narrow (no network, no accounts, no external storage), but it does handle:
- **Android Intents** — `TabataForegroundService` receives explicit Intents from `IntentServiceNotifier`
- **Pending Intents** — used by the foreground service notification
- **DataStore Preferences** — serialised user settings
- **Keystore / signing secrets** — managed in CI

Even a narrow attack surface benefits from higher-precision rules that detect issues like unsafe `PendingIntent` flags, improper `Intent` validation, and insecure IPC patterns — none of which are covered by `security-and-quality` alone.

## Decision

Use `security-extended` as the CodeQL query suite:

```yaml
- name: Initialise CodeQL
  uses: github/codeql-action/init@v3
  with:
    languages: kotlin
    queries: security-extended
```

## Consequences

### Positive
- Detects Android-specific security issues: unsafe `PendingIntent` mutability flags, implicit `Intent` vulnerabilities, insecure IPC
- Higher precision on data-flow queries — fewer false positives than `security-experimental`
- Results appear directly as PR annotations — findings are visible before merge
- SARIF uploaded to Security → Code scanning for persistent tracking

### Negative
- Marginally longer analysis time (~30–60 seconds) compared to `security-and-quality`
- Some rules produce findings that require judgment to triage (not all findings are actionable)

### Mitigations
- Hilt/KSP/Compose generated code is excluded from the database via `paths-ignore` in the CodeQL config block, eliminating the largest source of noise
- The weekly scheduled scan (Saturday 03:00 EEST) catches new CVE-driven rules without requiring a code push

## Alternatives Considered

### `security-and-quality` (default)
Insufficient for an Android app that uses foreground services and Pending Intents. Does not include the Android-specific IPC and Intent security rules.

### `security-experimental`
Too noisy for a solo project. Experimental rules have higher false-positive rates and require significant triage effort that is disproportionate to the project size.

### Custom `.ql` query pack
Would give full control but requires ongoing maintenance as the CodeQL query language evolves. Not justified at this project scale.

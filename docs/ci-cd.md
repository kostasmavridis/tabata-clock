# CI/CD Runbook

This document is the single source of truth for every automated workflow in this repository. It covers what each workflow does, when it runs, how to operate it, and known constraints.

---

## Workflow Overview

| File | Name | Trigger | Purpose |
|---|---|---|---|
| `build.yml` | Android CI | push/PR → `main` | Tests, coverage, debug APK |
| `release.yml` | Release | push `v*` tag | Signed APK + AAB + GitHub Release |
| `dependency-submission.yml` | Dependency Submission | push → `main`, manual | Gradle dependency graph → Dependabot |
| `codeql.yml` | CodeQL Analysis | push/PR → `main`, weekly Sat, manual | Static security analysis |

All workflows share three invariants:
- `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true` — suppresses Node.js deprecation warnings from third-party actions
- Gradle wrapper JAR is **never committed** — regenerated at runtime from the official Gradle distribution (see [ADR-001](adr/001-gradle-wrapper-jar-bootstrap.md))
- No major version bumps via automation — blocked by Dependabot config (see [ADR-003](adr/003-dependabot-version-update-strategy.md))

---

## Concurrency Policy

All workflows declare a `concurrency` block. See [ADR-004](adr/004-workflow-concurrency-cancellation.md) for the rationale.

| Workflow | Group key | `cancel-in-progress` |
|---|---|---|
| `build.yml` | `workflow + ref` | `true` |
| `codeql.yml` | `workflow + ref` (or `scheduled` for cron) | `true` |
| `dependency-submission.yml` | `workflow + ref` | `true` |
| `release.yml` | `workflow + ref` (tag) | **`false`** |

---

## Workflow Details

### `build.yml` — Android CI

**Triggers:** Every push and pull request to `main`.

**Steps:**
1. Generate WAV sound files via `scripts/generate_sounds.py` (Python stdlib, no pip)
2. Set up JDK 17 Temurin
3. Bootstrap `gradle-wrapper.jar` from `gradle-8.9-bin.zip`
4. Set up Gradle with build scan and caching
5. Decode keystore from `KEYSTORE_BASE64` secret
6. Verify keystore with `keytool -list`
7. Run unit tests: `./gradlew test --stacktrace --rerun-tasks`
8. Upload test HTML reports as artifact `test-reports`
9. Generate Kover XML coverage report
10. Upload coverage report as artifact `kover-coverage-report`
11. Build debug APK: `./gradlew assembleDebug`
12. Upload APK as artifact `tabata-clock-debug`

**Required secrets:** `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

**Artifacts produced:**
- `test-reports/` — JUnit HTML reports for debug and release variants
- `kover-coverage-report/report.xml` — Kover XML for badge/tooling integration
- `tabata-clock-debug/app-debug.apk`

---

### `release.yml` — Release

**Triggers:** Push of any tag matching `v[0-9]+.[0-9]+.[0-9]+*`.

**Version code formula:** `MAJOR * 10000 + MINOR * 100 + PATCH`  
Example: `v1.2.3` → `versionCode = 10203`

**Steps:**
1. Generate sound files
2. Set up JDK 17
3. Bootstrap Gradle wrapper JAR
4. Set up Gradle with build scan and caching
5. Decode keystore from `KEYSTORE_BASE64`
6. Build: `./gradlew assembleRelease bundleRelease`
7. Rename outputs: `tabata-clock-{version}.apk` / `.aab`
8. Generate changelog from `git log` (conventional commit prefixes: `feat`, `fix`)
9. Create GitHub Release with APK + AAB attached

**Pre-release detection:** Any tag containing `-` (e.g. `v1.0.0-beta.1`) is marked as pre-release.

**Required secrets:** `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

**Concurrency:** `cancel-in-progress: false` — a release in progress is never aborted.

#### How to cut a release

```bash
git tag v1.2.3
git push origin v1.2.3
```

The workflow triggers automatically. The GitHub Release is created with the APK and AAB attached.

---

### `dependency-submission.yml` — Dependency Submission

**Triggers:** Every push to `main`; also `workflow_dispatch` for manual re-submission.

**Purpose:** Submits the resolved Gradle dependency graph to GitHub so that:
- Dependabot can detect vulnerable transitive dependencies
- The **Insights → Dependency graph** tab stays up to date

**Why sound generation is needed here:** The Gradle configuration phase scans `res/raw/` for WAV files. Without them, the configuration phase fails before dependency resolution can start.

**Steps:**
1. Generate sound files
2. Set up JDK 17
3. Bootstrap Gradle wrapper JAR
4. Decode keystore
5. Run `gradle/actions/dependency-submission@v4`

**Required secrets:** `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

---

### `codeql.yml` — CodeQL Analysis

**Triggers:**
- Push to `main` (skips `.md`, `.txt`, `scripts/**`, `dependabot.yml`)
- Pull requests to `main` (same path filters)
- Weekly cron: Saturday 00:00 UTC (03:00 EEST)
- `workflow_dispatch`

**Language:** `kotlin` (compiled — CodeQL hooks into the Kotlin compiler during `assembleDebug`)

**Query suite:** `security-extended` — includes all `security-and-quality` rules plus higher-precision security rules. See [ADR-002](adr/002-codeql-security-extended-queries.md).

**Excluded from analysis:**
- `app/build/` — build outputs
- `**/*_HiltModules*`, `**/*_Factory*`, `**/*_MembersInjector*`, `**/*Hilt_*` — Hilt/KSP generated code
- `**/ComposableSingletons*` — Compose compiler generated code

**Results:** Uploaded as SARIF to **Security → Code scanning**. Findings appear as annotations on PRs.

**Steps:**
1. Generate sound files
2. Set up JDK 17
3. Bootstrap Gradle wrapper JAR
4. Decode keystore
5. `github/codeql-action/init@v3` ← must precede build
6. `./gradlew assembleDebug` ← CodeQL traces the compiler
7. `github/codeql-action/analyze@v3` ← uploads SARIF

---

## Secrets Reference

| Secret | Used by | Description |
|---|---|---|
| `KEYSTORE_BASE64` | all four workflows | Base64-encoded PKCS12 keystore file |
| `KEYSTORE_PASSWORD` | all four workflows | Keystore store password |
| `KEY_ALIAS` | all four workflows | Signing key alias inside the keystore |
| `KEY_PASSWORD` | all four workflows | Signing key password |

All secrets are stored in **Settings → Secrets and variables → Actions** and are masked in all log output.

#### Rotating the keystore

1. Generate a new keystore: `keytool -genkey -v -keystore new.keystore -storetype PKCS12 -alias tabata -keyalg RSA -keysize 2048 -validity 10000`
2. Base64-encode it: `base64 -w 0 new.keystore`
3. Update `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` in GitHub Secrets
4. Trigger a manual `build.yml` run to verify the new keystore works before cutting a release

---

## Dependabot

Configuration: `.github/dependabot.yml`. See [ADR-003](adr/003-dependabot-version-update-strategy.md) for the full grouping rationale.

**Ecosystems monitored:**
- `gradle` (Maven) — weekly Monday 06:00 EEST, 7 dependency groups
- `github-actions` — weekly Monday 06:00 EEST, all actions in one group

**Rule:** Major version bumps are blocked for both ecosystems. Minor and patch updates are automated.

**Labels applied:** `dependencies` + ecosystem label (`gradle` or `github-actions`)

---

## Troubleshooting

### `Could not find or load main class org.gradle.wrapper.GradleWrapperMain`

The `gradle-wrapper.jar` is missing or corrupt. This happens when:
- The bootstrap step was skipped
- The JAR was committed via the GitHub Contents API (which corrupts binary files)

**Fix:** Ensure the bootstrap step runs before any `./gradlew` invocation. See [ADR-001](adr/001-gradle-wrapper-jar-bootstrap.md).

### CodeQL scan finds no files / empty database

The build step failed before CodeQL could extract any code. Check that:
1. Sound files were generated successfully
2. The keystore decoded without error
3. `assembleDebug` completed — CodeQL needs the compiler to run

### Dependabot PR fails CI

Dependabot PRs run the full `build.yml` + `codeql.yml` suite. If they fail:
1. Check if it's a major version bump that slipped through (shouldn't happen with the ignore rule)
2. Check if `kotlin` and `ksp` were bumped in separate PRs — they must be bumped together (see `kotlin-ksp` group in `dependabot.yml`)
3. Merge the relevant group PR and close the individual ones

### Release workflow produces unsigned APK

Verify that all four signing secrets are set and non-empty. Run `build.yml` manually first — the "Debug signing config" step prints the key alias and verifies the keystore with `keytool`.

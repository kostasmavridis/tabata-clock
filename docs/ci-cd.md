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
- Gradle wrapper JAR is **never committed** — regenerated at runtime from the official Gradle distribution (see [ADR-001](adr/001-gradle-wrapper-jar-bootstrap.md))
- The downloaded Gradle distribution is **SHA-256 verified** before execution in all three build workflows — see [Gradle SHA-256 maintenance](#gradle-sha-256-maintenance) below
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

## Security Hardening

### Gradle binary verification
All three build workflows verify the SHA-256 of the downloaded Gradle distribution before
unzipping or executing it:

```bash
EXPECTED_SHA256="bafc141b619ad6350fd975fc903156dd5c151998cc8b058e8c1044ab5f7b031f"
echo "${EXPECTED_SHA256}  /tmp/gradle.zip" | sha256sum --check --strict
```

If the check fails, the workflow aborts immediately with:
```
sha256sum: WARNING: 1 computed checksum did NOT match
/tmp/gradle.zip: FAILED
```

### Signing credentials passed via `env:` only
Signing credentials (`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) are passed to
Gradle exclusively as environment variables, never as `-P` project properties. Process
arguments are visible to any process that can read `/proc/*/cmdline` on the same runner
node and appear in Gradle `--info` logs.

### Script injection prevention
All `${{ github.* }}` and `${{ steps.*.outputs.* }}` expressions in `run:` blocks are
routed through `env:` variables so that shell-metacharacter injection via a crafted tag
name or branch name is not possible. `GITHUB_OUTPUT` redirects are always quoted
(`>> "$GITHUB_OUTPUT"`).

### Keystore file lifecycle
The keystore file is decoded immediately before the build step and removed immediately
after via an `if: always()` cleanup step. This limits the window during which a
third-party action or build scan upload could access it.

### `KEYSTORE_BASE64` routing
The keystore secret is always passed through `env:` before being piped to `base64
--decode`, rather than being interpolated directly into the shell string.

---

## Gradle SHA-256 Maintenance

> ⚠️ **Action required when bumping `GRADLE_VERSION`:** update `EXPECTED_SHA256` in
> the bootstrap step of **all three** workflows at the same time:
> - `.github/workflows/build.yml`
> - `.github/workflows/release.yml`
> - `.github/workflows/codeql.yml`
>
> The correct hash for each release is listed under **"checksums"** at
> <https://gradle.org/releases/>. Use the **binary-only (`-bin`) ZIP** checksum row,
> not the complete (`-all`) ZIP. Updating `GRADLE_VERSION` without updating
> `EXPECTED_SHA256` in all three files will cause every CI run to fail immediately with
> `sha256sum: WARNING: 1 computed checksum did NOT match`.

If a CI run fails with that error:
1. Visit <https://gradle.org/releases/> and locate the release matching `GRADLE_VERSION`
2. Copy the SHA-256 from the **binary-only (`-bin`) ZIP** row
3. Update `EXPECTED_SHA256` in `build.yml`, `release.yml`, and `codeql.yml`
4. Commit all three changes in a single `ci:` commit

---

## Workflow Details

### `build.yml` — Android CI

**Triggers:** Every push and pull request to `main` that touches `app/`, `scripts/`,
`gradle/`, `build.gradle.kts`, `settings.gradle.kts`, `gradlew`, `gradlew.bat`, or
`build.yml` itself. Documentation-only commits do not trigger a build.

**Steps:**
1. Generate WAV sound files via `scripts/generate_sounds.py` (Python stdlib, no pip)
2. Set up JDK 17 Temurin
3. Bootstrap `gradle-wrapper.jar`: download `gradle-9.5.1-bin.zip`, verify SHA-256, extract, run `gradle wrapper`
4. Set up Gradle with build scan and caching
5. Decode keystore from `KEYSTORE_BASE64` secret (via `env:`, not inline interpolation)
6. Verify keystore with `keytool -list` (no secret values printed to log)
7. Run unit tests: `./gradlew test --stacktrace --rerun-tasks`
8. Upload test HTML reports as artifact `test-reports`
9. Generate Kover XML coverage report
10. Upload coverage report as artifact `kover-coverage-report`
11. Build debug APK: `./gradlew assembleDebug`
12. Remove keystore (`if: always()`)
13. Upload APK as artifact `tabata-clock-debug`

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
3. Parse version from tag (via `env:` — not inline `${{ github.ref }}` — to prevent script injection)
4. Bootstrap Gradle wrapper JAR: download, verify SHA-256, extract, run `gradle wrapper`
5. Set up Gradle with build scan and caching
6. Decode keystore from `KEYSTORE_BASE64` (via `env:`)
7. Build: `./gradlew assembleRelease bundleRelease` — signing credentials passed **only** as `env:` vars, never as `-P` flags
8. Remove keystore (`if: always()`)
9. Rename outputs: `tabata-clock-{version}.apk` / `.aab`
10. Generate changelog from `git log` (conventional commit prefixes: `feat`, `fix`)
11. Create GitHub Release with APK + AAB attached

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
3. Bootstrap Gradle wrapper JAR: download, verify SHA-256, extract, run `gradle wrapper`
4. Decode keystore (via `env:`)
5. `github/codeql-action/init@v4` ← must precede build
6. `./gradlew assembleDebug` ← CodeQL traces the compiler
7. Remove keystore (`if: always()`)
8. `github/codeql-action/analyze@v4` ← uploads SARIF

---

## Secrets Reference

| Secret | Used by | Description |
|---|---|---|
| `KEYSTORE_BASE64` | all four workflows | Base64-encoded PKCS12 keystore file |
| `KEYSTORE_PASSWORD` | all four workflows | Keystore store password |
| `KEY_ALIAS` | all four workflows | Signing key alias inside the keystore |
| `KEY_PASSWORD` | all four workflows | Signing key password |

All secrets are stored in **Settings → Secrets and variables → Actions** and are masked in all log output. Secrets are always passed through `env:` blocks — never interpolated inline into `run:` shell strings.

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

### `sha256sum: WARNING: 1 computed checksum did NOT match`

The `EXPECTED_SHA256` in the bootstrap step does not match the downloaded Gradle distribution. This almost always means `GRADLE_VERSION` was bumped without updating the hash.

**Fix:**
1. Visit <https://gradle.org/releases/> and find the entry for the `GRADLE_VERSION` value set in the failing workflow
2. Copy the SHA-256 from the **binary-only (`-bin`) ZIP** row
3. Update `EXPECTED_SHA256` in `build.yml`, `release.yml`, and `codeql.yml`
4. Commit all three in a single `ci:` commit

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

Verify that all four signing secrets are set and non-empty. Run `build.yml` manually first — the "Verify keystore" step validates the keystore with `keytool` without printing any secret values to the log.

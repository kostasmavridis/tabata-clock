---
name: documentation-project-hygiene
description: "Conventional commits, co-commit rule, CHANGELOG maintenance, README badge and prerequisites hygiene, and ADR authorship for Tabata Clock — including ADR topics already decided and future candidates."
---

## Documentation & Project Hygiene

Documentation is a **first-class deliverable** in this project. Version numbers,
badges, ADRs, and CHANGELOG entries must never lag behind the code that changes them.
A "fix docs" follow-up commit is a smell — it means the original commit was incomplete.

---

### Conventional Commit Prefixes

Every commit message starts with one of:

| Prefix | Use for |
|---|---|
| `feat:` | New user-facing feature |
| `fix:` | Bug fix |
| `ci:` | CI workflow changes |
| `docs:` | Documentation only — no code changes |
| `test:` | Test additions or fixes |
| `refactor:` | Code restructuring with no behaviour change |
| `chore:` | Dependency bumps, version catalog, build config, toolchain |

Rules:
- Imperative mood, present tense: `feat: add rest-phase haptic feedback` ✓
- One prefix per commit. If a commit genuinely needs two prefixes, split it.
- The body (optional, after a blank line) explains *why*, not *what*.
  The diff shows what; the body adds context the diff cannot.

---

### The Co-Commit Rule — Non-Negotiable

> When a toolchain file changes, the corresponding docs change in the **same commit**.

Specific applications:

| Change | Must accompany in the same commit |
|---|---|
| Version bump in `libs.versions.toml` | `README.md` badge(s) + prerequisites table |
| New CI workflow step | `docs/ci-cd.md` update |
| New architecture decision | New `docs/adr/NNN-*.md` file |
| Raised `minSdk` / `targetSdk` | README prerequisites + CHANGELOG entry |
| New contributor setup command | `CONTRIBUTING.md` update |

This rule exists because documentation drift is discovered at the worst possible
time — when a new contributor follows stale instructions and gets a broken build.

---

### README Badge & Prerequisites Hygiene

The README contains:
- **Version badges** — Kotlin, AGP, Compose BOM, min SDK, target SDK
- **Prerequisites table** — Android Studio version, JDK version, Gradle version

When any versioned component changes:
1. Update `gradle/libs.versions.toml` (source of truth).
2. Update the README badge URL(s) to reflect the new version.
3. Update the prerequisites table row.
4. Update `CONTRIBUTING.md` if any contributor setup command changes
   (e.g. `./gradlew wrapper --gradle-version X.Y.Z`).

All four changes go in one commit with prefix `chore:`.

---

### CHANGELOG Maintenance

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
versioned with [Semantic Versioning](https://semver.org/).

```markdown
## [Unreleased]

### Added
- Rest-phase haptic feedback with configurable pattern

### Changed
- Minimum SDK raised from 26 to 28

### Fixed
- Timer continues correctly across screen rotation

---

## [1.2.0] - 2026-03-14

### Added
- Foreground service keeps timer running when app is backgrounded
...
```

Rules:
- Every user-visible change is added to `## [Unreleased]` as it merges to `main`.
- On release, `[Unreleased]` is renamed to `[version] - YYYY-MM-DD` and a new
  empty `[Unreleased]` section is created above it.
- **Do not include** in the CHANGELOG: CI node bumps, patch dependency updates,
  internal refactors, or test-only changes. These clutter the user-facing history.
- **Do include:** any change that affects the installed app's behaviour,
  performance, or minimum device requirements.

---

### ADR Authorship

File: `docs/adr/NNN-short-kebab-title.md`

```markdown
# NNN. Short Title in Title Case

**Date:** YYYY-MM-DD  
**Status:** Accepted

## Context

What situation or constraint requires a decision?
What alternatives exist?

## Decision

What was decided, and why this option over the alternatives?

## Consequences

### Positive
- ...

### Negative / Trade-offs
- ...
```

**Superseding an ADR:**
- Set the old ADR status to: `Superseded by [ADR-NNN](NNN-new-title.md)`
- Do not modify the old ADR's body — it is an immutable historical record.
- The new ADR's Context section must reference the superseded ADR number.

**Decisions already documented or due:**

| Topic | Status |
|---|---|
| Why MVVM over MVI | Document if not yet in `docs/adr/` |
| Why Hilt over manual DI | Document if not yet in `docs/adr/` |
| Why KSP over KAPT | Document if not yet in `docs/adr/` |
| Multi-module extraction | Create ADR when the decision is made |
| Kotlin Multiplatform | Create ADR when evaluated |
| Dark-mode theming strategy | Create ADR when the approach is finalised |
| Play Store release pipeline | Create ADR when CI deploy is added |

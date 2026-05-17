---
name: documentation-project-hygiene
description: "Conventional commits, CHANGELOG maintenance, README badge hygiene, co-commit rules, and ADR authorship for Tabata Clock."
---

## Documentation & Project Hygiene

Documentation is a first-class deliverable in this project. Version numbers,
badges, and ADRs must never lag behind the code that changes them.

---

### Conventional Commit Prefixes

Every commit message starts with one of:

| Prefix | Use for |
|---|---|
| `feat:` | New user-facing feature |
| `fix:` | Bug fix |
| `ci:` | CI workflow changes |
| `docs:` | Documentation only (no code) |
| `test:` | Test additions or fixes |
| `refactor:` | Code change with no behaviour change |
| `chore:` | Dependency bumps, version catalog, build config |

Commit messages are imperative mood, present tense:
`feat: add rest-phase haptic feedback` ✓
`feat: added rest-phase haptic feedback` ✗

---

### Co-Commit Rule — Non-Negotiable

> When a toolchain file changes, the corresponding docs change in the **same commit**.

This means:
- A version bump in `libs.versions.toml` → update `README.md` badges and
  prerequisites table in the **same commit**.
- A new CI workflow step → update `docs/ci-cd.md` in the **same commit**.
- A new architecture decision → create the ADR file in the **same commit**
  as the code implementing it.

Follow-up "fix docs" commits are a code smell here. They mean the original
commit was incomplete.

---

### README Badge & Prerequisites Hygiene

The README contains version badges (Kotlin, AGP, Compose BOM, min SDK, target SDK)
and a prerequisites table (Android Studio version, JDK version, Gradle version).

When any of these change:
1. Update `gradle/libs.versions.toml` (the source of truth).
2. Update the README badge URL(s).
3. Update the prerequisites table.
4. Update `CONTRIBUTING.md` if the contributor setup commands change.

All four changes go in one commit with prefix `chore:`.

---

### CHANGELOG Maintenance

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

```markdown
## [Unreleased]

### Added
- Rest-phase haptic feedback pattern

### Changed
- Minimum SDK raised from 26 to 28

### Fixed
- Timer continues across screen rotation

## [1.2.0] - 2026-03-14
...
```

Rules:
- Every user-visible change goes in `## [Unreleased]` as it merges.
- On release, `[Unreleased]` is renamed to `[version] - YYYY-MM-DD`.
- Internal refactors, CI changes, and dependency bumps go under `### Changed`
  only if they affect the build environment (e.g. raised minimum SDK).
- Pure maintenance (CI node bumps, patch dependency updates) is omitted
  from the CHANGELOG — it clutters the user-facing history.

---

### ADR Authorship Quick Reference

File: `docs/adr/NNN-short-kebab-title.md`

```markdown
# NNN. Short Title

**Date:** YYYY-MM-DD
**Status:** Accepted

## Context

What is the situation that requires a decision?

## Decision

What was decided and why?

## Consequences

### Positive
- ...

### Negative / Trade-offs
- ...
```

When superseding an ADR:
- Set the old ADR status to: `Superseded by [ADR-NNN](NNN-new-title.md)`
- Do not modify the old ADR's body.
- The new ADR's context section must reference the old ADR number.

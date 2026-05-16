# Architecture Decision Records

This directory contains Architecture Decision Records (ADRs) for the Tabata Clock project.

ADRs document significant decisions — *what* was decided, *why*, what alternatives were considered, and what the consequences are. They are **append-only**: past decisions are never edited. If a decision is reversed or superseded, a new ADR is created and the old one is marked `Status: Superseded by ADR-NNN`.

## Index

### Application Architecture

| # | Title | Status |
|---|---|---|
| [ADR-001](001-mvvm-architecture.md) | MVVM as the Application Architecture Pattern | Accepted |
| [ADR-002](002-jetpack-compose-ui.md) | Jetpack Compose as the UI Toolkit | Accepted |
| [ADR-003](003-hilt-dependency-injection.md) | Hilt for Dependency Injection | Accepted |
| [ADR-004](004-foreground-service-over-workmanager.md) | Foreground Service for Background Timer (over WorkManager) | Accepted |
| [ADR-005](005-datastore-over-sharedpreferences.md) | DataStore Preferences over SharedPreferences | Accepted |
| [ADR-006](006-soundpool-for-audio-cues.md) | SoundPool for Audio Cues (over MediaPlayer) | Accepted |
| [ADR-007](007-gradle-version-catalog.md) | Gradle Version Catalog (`libs.versions.toml`) | Accepted |

### CI/CD & Build Infrastructure

| # | Title | Status |
|---|---|---|
| [ADR-008](008-gradle-wrapper-jar-bootstrap.md) | Bootstrap `gradle-wrapper.jar` at Runtime | Accepted |
| [ADR-009](009-codeql-security-extended-queries.md) | `security-extended` Query Suite for CodeQL | Accepted |
| [ADR-010](010-dependabot-version-update-strategy.md) | Dependabot Version Update Grouping Strategy | Accepted |
| [ADR-011](011-workflow-concurrency-cancellation.md) | Workflow Concurrency and Cancellation Strategy | Accepted |

## How to Add a New ADR

1. Copy the template below into a new file: `NNN-short-title.md` (next sequential number)
2. Fill in Context, Decision, Consequences, Alternatives Considered
3. Add a row to the index above
4. If the new ADR supersedes an existing one, update the old ADR's `Status` field

```markdown
# ADR-NNN: Title

**Date:** YYYY-MM-DD
**Status:** Accepted | Superseded by ADR-NNN | Deprecated
**Deciders:** @username
**Tags:** architecture | ci-cd | security | build

## Context
## Decision
## Consequences
### Positive
### Negative
### Mitigations
## Alternatives Considered
```

# ADR-003: Dependabot Version Update Grouping and Major-Version Blocking Strategy

**Date:** 2026-05-17  
**Status:** Accepted  
**Deciders:** @kostasmavridis

---

## Context

This project uses a Gradle version catalog (`gradle/libs.versions.toml`) where several dependencies share version references or have hard coupling constraints:

- **Kotlin + KSP**: KSP versions are formatted as `<kotlin-version>-<ksp-patch>`. They cannot be bumped independently — a Kotlin bump without a matching KSP bump causes `Plugin ... was not found` errors.
- **AGP + Gradle wrapper**: AGP release notes always specify a minimum supported Gradle version. Bumping one without the other produces incompatible API warnings or build failures.
- **Compose BOM**: The `androidx.compose:compose-bom` artifact governs all transitive `compose.*` versions. Navigation and Activity Compose APIs are tightly integrated with Compose BOM compatibility windows.
- **Lifecycle**: All three lifecycle artifacts (`lifecycle-runtime-ktx`, `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`) share the `lifecycle` version ref. Partial upgrades cause `AbstractMethodError` at runtime.
- **Hilt**: `hilt-android` and `hilt-navigation-compose` must be version-compatible. `hilt-navigation-compose` has its own version but its Hilt integration APIs must match the Dagger version.

Without grouping, Dependabot would raise one PR per dependency, potentially creating broken intermediate states (e.g. Kotlin bumped, KSP not yet bumped).

## Decision

Configure Dependabot with **seven named groups** for Gradle and **one group** for GitHub Actions:

| Group | Dependencies | Coupling reason |
|---|---|---|
| `kotlin-ksp` | `org.jetbrains.kotlin*`, `com.google.devtools.ksp*` | KSP version ties to Kotlin |
| `agp-gradle` | `com.android.tools.build*`, `com.android.application*` | AGP requires minimum Gradle version |
| `compose-ui` | `androidx.compose*`, `androidx.activity*`, `androidx.navigation*` | BOM governs compose.* transitives |
| `lifecycle` | `androidx.lifecycle*` | Shared version ref — partial upgrade fails |
| `hilt` | `com.google.dagger*`, `androidx.hilt*` | DI wiring must stay consistent |
| `androidx-core` | `androidx.core*`, `androidx.datastore*` | Low-churn utilities, batched for less noise |
| `test-dependencies` | JUnit 5, Turbine, MockK, coroutines-test, Kover, android-junit5 | Isolated from production deps |
| `github-actions-all` | `*` (actions ecosystem) | All action bumps are low-risk; one PR per week |

Additionally, a universal `ignore` rule blocks all `semver-major` updates for both ecosystems:

```yaml
ignore:
  - dependency-name: "*"
    update-types: ["version-update:semver-major"]
```

## Consequences

### Positive
- Maximum 8 open Dependabot PRs per week instead of one per dependency (potentially 20+)
- Coupled dependencies always land in the same PR — no broken intermediate states
- Major version migrations are always deliberate human decisions with a proper migration guide review
- Test dependency PRs never block production security fixes (separate group)

### Negative
- A security patch in a major version (e.g. `hilt` 3.x patch) will not be auto-raised — requires manual intervention
- If a group PR fails CI, the entire group is blocked until the issue is resolved

### Mitigations
- Security vulnerability alerts (from `dependency-submission.yml` + Dependabot alerts) are separate from version update PRs and are not blocked by the major-version ignore rule
- Groups can be temporarily dissolved by editing `dependabot.yml` if a split is needed

## Alternatives Considered

### No grouping (one PR per dependency)
Creates broken intermediate states for coupled dependencies (Kotlin/KSP, Lifecycle). Produces excessive PR noise.

### Single `all-dependencies` mega-group
Simplest possible config, but a failure in any one dependency blocks all updates. Also obscures what changed.

### Allow major version bumps
Major versions of AGP, Kotlin, and Compose BOM consistently require migration work (source-incompatible APIs, new DSL, deprecated APIs removed). Automating them risks silent build failures on `main`.

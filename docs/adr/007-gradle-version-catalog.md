# ADR-007: Gradle Version Catalog (`libs.versions.toml`)

**Date:** 2026-05-17  
**Status:** Accepted  
**Deciders:** @kostasmavridis  
**Tags:** build, dependency-management

---

## Context

Android projects typically declare dependency versions inline in `build.gradle.kts` files using string literals:

```kotlin
implementation("androidx.compose.ui:compose-ui:1.7.0")
```

As the dependency count grows, this leads to:
- Version duplication across modules
- No single place to see all versions at once
- Dependabot raising one PR per hardcoded version string (no grouping)
- Accidental partial upgrades (e.g. `compose-ui` updated but `compose-material3` not)

Gradle 7.4+ introduced **Version Catalogs** (`gradle/libs.versions.toml`) as the standard solution.

## Decision

Declare all dependency versions in **`gradle/libs.versions.toml`** using:
- `[versions]` — named version strings referenced by multiple dependencies
- `[libraries]` — type-safe dependency accessors (`libs.androidx.core.ktx`)
- `[plugins]` — plugin declarations with version refs

Example:
```toml
[versions]
kotlin = "2.0.21"
ksp   = "2.0.21-1.0.28"

[libraries]
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
```

This feeds directly into the Dependabot grouping strategy (ADR-008): groups are defined by `dependency-name` patterns that match Maven group IDs, which correspond to version refs in the catalog.

## Consequences

### Positive
- Single file to review when auditing all dependency versions
- Type-safe accessors in `build.gradle.kts` — IDE auto-complete, no string typos
- Shared version refs make coupled upgrades (Kotlin + KSP) visible in one place
- Dependabot reads the catalog and generates accurate update PRs

### Negative
- Requires Gradle 7.4+ — already satisfied by this project's Gradle 8.9
- TOML syntax is unfamiliar to developers who only know Groovy/Kotlin DSL
- Version refs must be kept consistent with the catalog — a mismatch is a build error (caught at sync time)

## Alternatives Considered

### Inline version strings in `build.gradle.kts`
Simple but does not scale. No de-duplication, no type safety, harder to audit.

### `buildSrc` with Kotlin version constants
Programmatic approach using a Kotlin object. Works, but adds a `buildSrc` module that must be compiled before the main project, slowing initial build times. Also not natively understood by Dependabot.

### Convention plugins with version BOMs
Useful for multi-module projects. Disproportionate complexity for a single-module app.

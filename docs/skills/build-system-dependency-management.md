---
name: build-system-dependency-management
description: "Gradle Version Catalog, AGP lifecycle, KSP2, R8/ProGuard, source-set isolation, and Maven artifact verification for Tabata Clock."
---

## Build System & Dependency Management

This is the highest-risk domain in the project. Every major incident in the
project's history has originated here. Read this skill carefully before touching
any version, plugin, or build configuration.

---

### The One Rule: Version Catalog Is the Single Source of Truth

- **All** dependency and plugin versions live exclusively in `gradle/libs.versions.toml`.
- Build files reference versions only via `libs.versions.*`, `libs.plugins.*`,
  and `libs.bundles.*` accessors.
- **Never** write a version string directly in a `build.gradle.kts` file.
- Plugin applications always use `alias(libs.plugins.*)` — never an inline id + version.

```toml
# gradle/libs.versions.toml — correct
[versions]
agp = "8.10.1"
kotlin = "2.1.21"

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

```kotlin
// app/build.gradle.kts — correct
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}
```

---

### AGP Lifecycle & Plugin Ownership

> **Lesson that must not be relearned:** When AGP takes ownership of a compilation
> step, the old plugin providing that step becomes **actively forbidden**, not
> silently redundant. It will hard-fail.

- Before any major AGP bump, read the migration guide section titled
  *"What this version now owns"* and identify which plugins must be removed.
- AGP 9+ owns the `kotlin.android` compilation step.
  The `kotlin.android` plugin is **removed**, but `kotlin.compose` remains required.
- Verify each concern independently after a major AGP upgrade:
  compilation ✓, annotation processing ✓, each compiler plugin ✓.

---

### KSP2 — Annotation Processing

- The project is fully on KSP2. KAPT is not used and must not be introduced.
- KSP version coupling: before KSP 2.3.0, the KSP version had to match the
  Kotlin version (e.g. `2.1.21-2.0.1`). From KSP 2.3.0+ the versions are
  standalone. Check the current scheme before upgrading.
- Hilt runs through KSP2. The `ksp` configuration is used, not `kapt`.

```toml
[versions]
ksp = "2.1.21-2.0.1"   # format: kotlinVersion-kspSuffix (pre-2.3.0)

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

---

### R8 / ProGuard & BuildConfig

- `buildConfig` generation is **opt-in** since AGP 8.x. Enable it explicitly:

```kotlin
// app/build.gradle.kts
android {
    buildFeatures {
        buildConfig = true
        compose = true
    }
}
```

- `BuildConfig` requires an explicit import in Kotlin:
  `import com.kostasmavridis.tabataclock.BuildConfig`
- Debug-only classes (e.g. `LogExporter`) must never be called from the `main`
  source set via FQN. Use the dual source-set pattern:
  - `main`: interface only (e.g. `DebugActions`)
  - `debug`: `RealDebugActions` implementing the interface
  - `release`: `NoOpDebugActions` with empty stubs

---

### BOM Scope Gotcha

A BOM in `implementation` constrains `implementation` dependencies **only**.
It does **not** constrain `debugImplementation`, `testImplementation`, or
`androidTestImplementation`. Pin those explicitly.

```kotlin
// Wrong: assumes BOM constrains debugImplementation
dependencies {
    implementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.tooling) // version NOT constrained by BOM
}

// Correct
dependencies {
    implementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.tooling) // explicit version in toml
}
```

---

### Maven Artifact Verification

> **Rule:** Verify the artifact exists on Maven Central or Google Maven
> **before** committing a version bump. A non-existent version wastes a full CI cycle.

- Check URLs:
  - Google Maven: `https://maven.google.com/web/index.html`
  - Maven Central: `https://central.sonatype.com/artifact/<group>/<artifact>`
- Some libraries change their versioning scheme between major releases
  (e.g. KSP moved from `kotlinVersion-suffix` to standalone). When a bump
  fails with "artifact not found", check the scheme change before assuming
  the version number is wrong.
- Check interdependent groups together: annotation processor ↔ compiler ↔ runtime.
  Upgrading one without the others can break the build in non-obvious ways.

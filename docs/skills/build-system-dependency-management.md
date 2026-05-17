---
name: build-system-dependency-management
description: "Gradle Version Catalog, AGP lifecycle and plugin matrix, KSP vs KAPT, R8/ProGuard, source-set isolation, BOM scope, and Maven artifact verification for Tabata Clock."
---

## Build System & Dependency Management

This is the highest-risk domain in the project. Every major incident in the
project's history originated here. Read every section before touching any version,
plugin, or build configuration.

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
agp     = "8.10.1"
kotlin  = "2.1.21"

[plugins]
android-application = { id = "com.android.application",      version.ref = "agp"    }
kotlin-android      = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

```kotlin
// app/build.gradle.kts — correct
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}
```

---

### AGP Lifecycle & Plugin Matrix

> **Lesson that must not be relearned:** When AGP takes ownership of a compilation
> step, the old plugin that provided that step becomes **actively forbidden**,
> not silently redundant. It will hard-fail.

- Before any major AGP bump, read the migration guide section titled
  *"What this version now owns"* and identify which plugins must be removed.
- **AGP 9+:** owns the `kotlin.android` compilation step.
  The `kotlin.android` plugin is **removed**; `kotlin.compose` remains required.
- Verify each concern independently after a major AGP upgrade:
  compilation ✓, annotation processing ✓, each compiler plugin ✓.
- Understand the full plugin matrix for this project:

| Plugin | AGP < 9 | AGP 9+ |
|---|---|---|
| `kotlin.android` | Required | **Forbidden** |
| `kotlin.compose` | Required | Required |
| `com.google.devtools.ksp` | Required | Required |
| `dagger.hilt.android.plugin` | Required | Required |
| `org.jetbrains.kotlinx.kover` | Required | Verify compatibility |

---

### KSP vs KAPT

- The project is **fully on KSP2**. KAPT is not used and must not be introduced.
- **KGP–KSP version coupling (pre-2.3.0):** before KSP 2.3.0, the KSP version had
  to embed the Kotlin version (e.g. `2.1.21-2.0.1`). The format is
  `{kotlinVersion}-{kspSuffix}`.
- **KSP 2.3.0+:** versions are standalone and no longer coupled to Kotlin's version.
  When a bump fails with "artifact not found", check whether the versioning scheme
  changed before assuming the version number is simply wrong.
- Hilt uses the `ksp` configuration, not `kapt`:

```kotlin
// app/build.gradle.kts — correct
dependencies {
    ksp(libs.hilt.compiler)          // ✓ KSP
    // kapt(libs.hilt.compiler)      // ✗ KAPT — do not use
}
```

---

### R8 / ProGuard & BuildConfig

- `buildConfig` generation is **opt-in** since AGP 8.x. Enable it explicitly:

```kotlin
// app/build.gradle.kts
android {
    buildFeatures {
        buildConfig = true
        compose     = true
    }
}
```

- `BuildConfig` requires an explicit import in Kotlin:
  `import com.kostasmavridis.tabataclock.BuildConfig`
- **Source-set isolation rule:** debug-only classes (e.g. `LogExporter`) must never
  be called from the `main` source set via FQN. The release compiler correctly
  rejects this because debug classes do not exist in the release classpath.
  Use the dual source-set pattern:
  - `main` → interface only (e.g. `DebugActions`)
  - `debug` → `RealDebugActions` implementing the interface
  - `release` → `NoOpDebugActions` with empty stubs

```kotlin
// main/DebugActions.kt
interface DebugActions {
    fun exportLog()
}

// debug/RealDebugActions.kt
class RealDebugActions : DebugActions {
    override fun exportLog() = LogExporter.export()  // debug-only class: safe here
}

// release/NoOpDebugActions.kt
class NoOpDebugActions : DebugActions {
    override fun exportLog() = Unit  // release compiler never sees LogExporter
}
```

---

### BOM Scope Gotcha

A BOM placed in `implementation` constrains **only** `implementation` dependencies.
It does **not** constrain `debugImplementation`, `testImplementation`, or
`androidTestImplementation`. Pin those configurations explicitly in the catalog.

```kotlin
// Wrong assumption: BOM constrains all configurations
dependencies {
    implementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.tooling)  // version NOT constrained by BOM
}

// Correct: explicit version via the catalog
// In libs.versions.toml:
// compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling", version.ref = "compose-ui-tooling" }
```

---

### Maven Artifact Verification

> **Rule:** Verify the artifact exists on Maven Central or Google Maven
> **before** committing a version bump. A non-existent version wastes a full CI cycle.

- Check URLs:
  - Google Maven: `https://maven.google.com/web/index.html`
  - Maven Central / Sonatype: `https://central.sonatype.com/artifact/<group>/<artifact>`
- When a bump fails with "artifact not found", check whether the **versioning scheme
  changed** before assuming the version number is wrong. This has bitten this project
  with KSP (scheme changed at 2.3.0) and `hilt-navigation-compose` (pre-release
  beta had a different coordinate).
- Always check the **full toolchain compatibility matrix** before upgrading any
  component that participates in code generation:
  annotation processor ↔ compiler ↔ runtime ↔ generated code.
  Upgrading one without checking the others can break the build in ways the
  error message does not make obvious.
- Interdependent groups to always upgrade together:
  - `kotlin` + `ksp` (pre-2.3.0)
  - `hilt` + `hilt-compiler` (same version ref in toml)
  - `kotlin` + `kotlin-compose-compiler-plugin`

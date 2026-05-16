# ADR-008: Bootstrap `gradle-wrapper.jar` at Runtime Instead of Committing It

**Date:** 2026-05-17  
**Status:** Accepted  
**Deciders:** @kostasmavridis  
**Tags:** ci-cd, build

> ℹ️ This ADR was previously numbered ADR-001. Renumbered to ADR-008 when application architecture ADRs (001–007) were introduced.

---

## Context

The standard Gradle setup commits `gradle/wrapper/gradle-wrapper.jar` to the repository. This binary file allows any developer or CI runner to invoke `./gradlew` without having Gradle pre-installed.

In this project, the JAR cannot be committed because **the GitHub Contents API silently corrupts binary files** when they are created or updated via API calls (e.g. from automation tools, MCP servers, or the GitHub web UI). The corruption manifests as:

```
Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain
Caused by: java.lang.ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain
```

This error occurs because the Contents API encodes the file as UTF-8 text, stripping or mangling bytes that are valid in a ZIP/JAR but not in UTF-8.

## Decision

The `gradle-wrapper.jar` is **not committed** to the repository. Instead, every CI workflow that needs `./gradlew` runs a bootstrap step that:

1. Downloads the official `gradle-8.9-bin.zip` from `https://services.gradle.org/distributions/`
2. Unzips it to a temp directory
3. Runs `gradle wrapper --gradle-version 8.9 --distribution-type bin` to regenerate the JAR in-place
4. Sets executable permission on `gradlew`

```yaml
- name: Bootstrap Gradle wrapper JAR
  run: |
    GRADLE_VERSION=8.9
    curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
         -o /tmp/gradle.zip
    unzip -q /tmp/gradle.zip -d /tmp/gradle-dist
    /tmp/gradle-dist/gradle-${GRADLE_VERSION}/bin/gradle wrapper \
      --gradle-version ${GRADLE_VERSION} \
      --distribution-type bin
    chmod +x gradlew
```

**Critical constraint:** The bootstrap step must always appear **before** any action or step that invokes `./gradlew` — including actions that invoke it internally. See the [Operational Discovery](#operational-discovery-dependency-submission-action-timing-failure) section below.

This step appears in all four workflows: `build.yml`, `release.yml`, `dependency-submission.yml`, and `codeql.yml`.

## Consequences

### Positive
- No binary blobs in the repository — cleaner git history, no risk of API-induced corruption
- The JAR is always freshly generated from the official, verified Gradle distribution
- Supply-chain trust: the download URL is the canonical Gradle services endpoint

### Negative
- Every CI run adds ~10–15 seconds to download and unzip `gradle-8.9-bin.zip`
- Requires an outbound HTTPS connection to `services.gradle.org` during the bootstrap step
- The Gradle version is hardcoded in four workflow files — bumping it requires updating all four
- Some third-party Gradle actions invoke `./gradlew` **internally via their own JVM process**, bypassing the shell entirely. These actions must be used in a mode where they do NOT invoke `gradlew` themselves — see operational discovery below.

### Mitigations
- The Gradle distribution download is fast (~50 MB) and cached implicitly by the runner's HTTP layer
- The hardcoded version will be caught by the `agp-gradle` Dependabot group (see ADR-010)

---

## Operational Discovery: `dependency-submission` Action Timing Failure

**Discovered:** 2026-05-17  
**Symptom:** `dependency-submission.yml` consistently failed with the same `ClassNotFoundException` even though the bootstrap step had already run successfully:

```
Warning: Gradle wrapper script '.../gradlew' is not executable. Action will set executable permission and continue.
Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain
Caused by: java.lang.ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain
Error: Gradle build failed: see console output for details
No dependency graph files found to submit.
```

**Root cause:** `gradle/actions/dependency-submission@v4` uses an **internal JVM launcher** — it does not invoke `gradlew` through the shell. Instead it:
1. Detects `gradlew` in the working directory
2. Reinitialises the Gradle User Home (`~/.gradle`) as part of its own setup
3. Spawns a fresh JVM process pointing at `gradle/wrapper/gradle-wrapper.jar`

Step 2 is the problem: the action's internal Gradle User Home reinitialisation invalidates the JAR lookup that our bootstrap step set up, even though the JAR file itself is present on disk at the correct path. The action effectively runs in a different Gradle environment than the one our bootstrap created.

**Fix applied (commit `c8720d45`):**

Replaced `gradle/actions/dependency-submission@v4` with `gradle/actions/setup-gradle@v4` using `dependency-graph: generate-and-submit` mode. In this mode `setup-gradle` registers the `ForceDependencyResolutionPlugin` internally but does **not** invoke `gradlew` itself. The `./gradlew` call is then made explicitly by us in the next step, using our bootstrapped JAR:

```yaml
- name: Setup Gradle
  uses: gradle/actions/setup-gradle@v4
  with:
    dependency-graph: generate-and-submit   # registers plugin; does NOT invoke gradlew

- name: Resolve dependencies and submit graph
  run: |
    ./gradlew \
      -Dorg.gradle.configureondemand=false \
      -Dorg.gradle.dependency.verification=off \
      :ForceDependencyResolutionPlugin_resolveAllDependencies \
      --stacktrace
```

`setup-gradle`'s **post-step** (which runs after the job completes) then reads the generated graph JSON and submits it to GitHub's dependency submission API.

**General rule derived from this discovery:**

> Any GitHub Action that invokes `./gradlew` internally must be used in a mode where it does NOT make that internal invocation. Always control the `./gradlew` call yourself so the bootstrapped JAR is guaranteed to be in place.

---

## Alternatives Considered

### Commit the JAR using `git` directly (not the Contents API)
Would work, but requires local clone operations and cannot be done via API-based tooling. Also introduces a binary blob that is hard to audit for supply-chain integrity.

### Use `gradle/actions/setup-gradle` autobuild without bootstrap
The `setup-gradle` action assumes `./gradlew` already works. Without the bootstrap step it fails with the same `ClassNotFoundException`.

### Pin `gradle/actions/dependency-submission` to a version that uses shell invocation
No such version exists — the action has always used an internal JVM launcher for `gradlew` invocation. The `generate-and-submit` mode of `setup-gradle` is the documented solution for this class of problem.

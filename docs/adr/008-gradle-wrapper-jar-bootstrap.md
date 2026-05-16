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

### Mitigations
- The Gradle distribution download is fast (~50 MB) and cached implicitly by the runner's HTTP layer
- The hardcoded version will be caught by the `agp-gradle` Dependabot group (see ADR-009)

## Alternatives Considered

### Commit the JAR using `git` directly (not the Contents API)
Would work, but requires local clone operations and cannot be done via API-based tooling. Also introduces a binary blob that is hard to audit for supply-chain integrity.

### Use `gradle/actions/setup-gradle` autobuild
The `setup-gradle` action does not bootstrap the JAR — it assumes `./gradlew` already works. It would fail with the same `ClassNotFoundException`.

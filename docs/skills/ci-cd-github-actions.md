---
name: ci-cd-github-actions
description: "GitHub Actions workflow authoring, Gradle wrapper bootstrap, path filters, Node 24 runtime, --no-configuration-cache, Kover coverage, and CodeQL for the Tabata Clock CI/CD pipeline."
---

## CI/CD & GitHub Actions

The project runs a production-grade pipeline. Every hard invariant below has been
earned through a real failure. Mismanaging CI costs full build cycles. Do not
relax any of these constraints.

---

### Invariants — Read Before Touching Any Workflow

| Invariant | Rule |
|---|---|
| Gradle wrapper JAR | **Never committed.** Always bootstrapped at CI runtime. |
| `setup-gradle` version | Must ship Node 24 natively. Never add `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24`. |
| Configuration cache | `--no-configuration-cache` on **every** `./gradlew` invocation. |
| `--warning-mode all` | On every `./gradlew` invocation so deprecation warnings surface inline. |
| Path filters | Doc-only paths excluded from `build.yml` and `codeql.yml`. |
| Artifact upload | Build outputs (APKs, reports) uploaded with `actions/upload-artifact`. |

---

### GitHub Actions Workflow Authoring

- Workflow YAML lives in `.github/workflows/`. The two primary workflows are
  `build.yml` (compile + test + coverage) and `codeql.yml` (security scanning).
- Use `on: push` + `on: pull_request` triggers with `paths-ignore` to skip
  doc-only commits.
- Matrix strategies are available for running the same job across multiple
  API levels or configurations, but are not currently used — add only when
  there is a concrete multi-API regression risk.
- Artifacts (APKs, Kover XML, test results) are uploaded using
  `actions/upload-artifact@v4` — always the `v4` major to stay current.

---

### Gradle Wrapper Bootstrap Step

The wrapper JAR is a binary. The GitHub Contents API silently corrupts binary
files. Therefore the JAR is **never committed** and must be regenerated in CI.

Every job that calls `./gradlew` must include this bootstrap:

```yaml
- name: Bootstrap Gradle wrapper
  run: |
    GRADLE_VERSION=$(grep distributionUrl gradle/wrapper/gradle-wrapper.properties \
      | grep -oP '\d+\.\d+(\.\d+)?')
    wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
    unzip -q "gradle-${GRADLE_VERSION}-bin.zip"
    "gradle-${GRADLE_VERSION}/bin/gradle" wrapper --gradle-version "${GRADLE_VERSION}"
```

This downloads the exact version pinned in `gradle-wrapper.properties`,
runs `gradle wrapper` to regenerate the JAR, and then all subsequent
`./gradlew` calls work normally.

---

### `gradle/actions/setup-gradle` Versioning

- Always use the major version that ships **Node 24 natively**.
- Deprecation warnings about runner Node.js versions are **scheduled failures**,
  not cosmetic noise. The expiry date is real.
- The fix is always to bump the action version — not to add an env-var workaround.
- `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24` is **explicitly forbidden** in this project.
  Its presence in a workflow signals that the action version needs a bump.

```yaml
# Correct
- uses: gradle/actions/setup-gradle@v4  # ships Node 24 natively

# Wrong — workaround with an expiry date
- uses: gradle/actions/setup-gradle@v3
  env:
    FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true
```

---

### `--no-configuration-cache`

Configuration cache serialisation failures are a real failure mode this project
has experienced (`field librarySourceSets ... error writing value of type
DefaultConfigurableFileCollection`). Until AGP and all plugins fully support
configuration cache, every `./gradlew` invocation in CI uses
`--no-configuration-cache`.

```yaml
# Standard pattern for all Gradle invocations in CI
- name: Run unit tests
  run: ./gradlew test --no-configuration-cache --warning-mode all

- name: Assemble debug
  run: ./gradlew assembleDebug --no-configuration-cache --warning-mode all --stacktrace

- name: Assemble release
  run: ./gradlew assembleRelease --no-configuration-cache --warning-mode all

- name: Kover XML report
  run: ./gradlew koverXmlReport --no-configuration-cache --warning-mode all
```

---

### Path Filters — Build & CodeQL

Documentation commits must not trigger build or security jobs.

```yaml
# .github/workflows/build.yml  (and codeql.yml — same list)
on:
  push:
    paths-ignore:
      - '**.md'
      - 'docs/**'
      - '.github/ISSUE_TEMPLATE/**'
      - 'CODE_OF_CONDUCT.md'
      - 'CONTRIBUTING.md'
      - 'CHANGELOG.md'
  pull_request:
    paths-ignore:
      - '**.md'
      - 'docs/**'
```

If a PR touches both code and docs, the build **will** trigger — path filters
work on the union, not intersection.

---

### Kover Coverage Reports

- Kover has had task-registration compatibility issues with AGP 9. After any
  AGP major bump, verify `koverXmlReport` still resolves correctly before merging.
- Coverage thresholds are enforced in `app/build.gradle.kts` via the
  `koverReport { verify { rule { ... } } }` block.
- The **XML report** is the source of truth for CI coverage gates.
  HTML reports are for local developer inspection only and must not be committed.
- Kover plugin version must be compatible with both the Kotlin and AGP versions
  in use — check the Kover compatibility matrix before upgrading any of the three.

---

### CodeQL Security Scanning

- Language: `kotlin`.
- Triggers: `push` to `main`, `pull_request` targeting `main`, weekly schedule.
- Path filters identical to `build.yml` — doc-only commits must not trigger a scan.
  An unnecessary CodeQL run consumes Actions minutes with zero security value.
- Do not override the default CodeQL query suite unless a specific, documented
  security requirement demands it. The defaults cover the relevant Kotlin/Android
  vulnerability classes.

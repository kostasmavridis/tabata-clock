---
name: ci-cd-github-actions
description: "GitHub Actions workflows, Gradle wrapper bootstrap, path filters, Node runtime versioning, Kover, and CodeQL for the Tabata Clock CI/CD pipeline."
---

## CI/CD & GitHub Actions

The project runs a production-grade pipeline. Every hard invariant below has
been earned through a real failure. Do not relax them.

---

### Invariants — Read Before Touching Any Workflow

| Invariant | Rule |
|---|---|
| Gradle wrapper JAR | **Never committed.** Always bootstrapped at CI runtime. |
| `setup-gradle` version | Must ship Node 24 natively. Never use `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24`. |
| Configuration cache | `--no-configuration-cache` on every `./gradlew` invocation. |
| `--warning-mode all` | On every `./gradlew` invocation so deprecation warnings are visible. |
| Path filters | Doc-only paths excluded from `build.yml` and `codeql.yml`. |

---

### Gradle Wrapper Bootstrap Step

Every job that calls `./gradlew` must include this bootstrap before use:

```yaml
- name: Bootstrap Gradle wrapper
  run: |
    GRADLE_VERSION=$(grep -oP '(?<=gradle-).*(?=-)' gradle/wrapper/gradle-wrapper.properties || \
      grep distributionUrl gradle/wrapper/gradle-wrapper.properties | \
      grep -oP '\d+\.\d+(\.\d+)?')
    wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
    unzip -q "gradle-${GRADLE_VERSION}-bin.zip"
    "gradle-${GRADLE_VERSION}/bin/gradle" wrapper --gradle-version "${GRADLE_VERSION}"
```

Reason: the GitHub Contents API silently corrupts binary files. The wrapper JAR
is binary — it must never be pushed via the API and must always be regenerated.

---

### `gradle/actions/setup-gradle` Versioning

- Always use the major version that ships Node 24 natively.
- Deprecation warnings about runner Node.js versions are **scheduled failures**,
  not noise. The fix is a version bump of the action, not an env-var workaround.
- `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24` has an expiry date and is explicitly
  forbidden in this project.

```yaml
# Correct
- uses: gradle/actions/setup-gradle@v4

# Wrong — forces a workaround instead of fixing the root cause
- uses: gradle/actions/setup-gradle@v3
  env:
    FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true
```

---

### Path Filters

Documentation commits must not trigger build or security jobs.

```yaml
# .github/workflows/build.yml
on:
  push:
    paths-ignore:
      - '**.md'
      - 'docs/**'
      - '.github/ISSUE_TEMPLATE/**'
      - 'CODE_OF_CONDUCT.md'
      - 'CONTRIBUTING.md'
      - 'CHANGELOG.md'
```

Apply the same `paths-ignore` to `codeql.yml`.

---

### Standard Gradle Invocation Pattern

All `./gradlew` calls in CI follow this pattern:

```yaml
- name: Run unit tests
  run: ./gradlew test --no-configuration-cache --warning-mode all

- name: Assemble debug
  run: ./gradlew assembleDebug --no-configuration-cache --warning-mode all --stacktrace

- name: Kover XML report
  run: ./gradlew koverXmlReport --no-configuration-cache --warning-mode all
```

---

### Kover Coverage

- Kover has had task-registration compatibility issues with AGP 9. After any
  AGP major bump, verify `koverXmlReport` still resolves correctly.
- Coverage thresholds are enforced in `app/build.gradle.kts` via
  `koverReport { verify { rule { ... } } }`.
- The XML report is the source of truth for CI coverage gates; HTML reports
  are for local developer use only.

---

### CodeQL

- Language: `kotlin`
- Trigger: `push` to `main`, `pull_request` to `main`, weekly schedule.
- Path filters identical to `build.yml` — doc commits must not trigger a scan.
- Do not override the default CodeQL query suite unless a specific security
  requirement demands it.

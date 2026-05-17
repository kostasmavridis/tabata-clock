# Tabata Clock — Development Principles & Lessons Learned

> Living document. Update after every significant migration, incident, or architectural decision.
> Hard version numbers and tech requirements live in `gradle/libs.versions.toml`, `README.md`, and `docs/adr/`.
> This document captures **why** decisions were made and **what patterns to reapply** — not what the current versions are.

---

## 1. Toolchain Migrations

### Verify artifacts exist before committing
When upgrading to a newly released tool or library version, confirm the
artifact is actually published on Maven Central / Google Maven before
pushing the change. Build failures caused by non-existent artifacts waste
a full CI cycle. Check the release page or Maven search first, then commit.

### Versioning schemes change — read the changelog
Major libraries occasionally change their versioning format between major
versions (e.g. a plugin that previously coupled its version to a compiler
version may switch to standalone versioning). When a dependency upgrade
fails with "artifact not found", the first thing to check is whether the
versioning scheme itself changed, not just the version number.

### Understand what a framework upgrade owns
When a framework takes ownership of something it previously delegated
(e.g. a build tool absorbing a compilation step), the old plugin that
provided that step becomes not just redundant but actively forbidden —
it will hard-fail, not silently no-op. Before any major framework upgrade,
read the migration guide to identify what the new version now owns and
what must be explicitly removed from the build configuration.

### Ownership ≠ auto-wiring
A framework taking over one responsibility does not mean it takes over
all related responsibilities. Verify each concern individually:
compilation, annotation processing, compiler plugins, code generation.
Do not assume that removing one plugin makes everything automatic.

### Coordinate toolchain upgrades as a bundle
Some dependencies have a hard compatibility relationship with each other.
Upgrading one without the other breaks the build in ways that are not
obvious from the error message alone. Always check the compatibility
matrix for the full toolchain group (build tool + compiler + annotation
processor + DI framework) before upgrading any individual component.

---

## 2. CI & GitHub Actions

### Never commit binary files via the GitHub API
The GitHub Contents API silently corrupts binary files. Any binary that
must be present at build time should be bootstrapped by CI at runtime
(download → extract → generate), not committed to the repository.
This applies to wrapper JARs, keystore files, and generated assets.

### Keep the bootstrap step in every workflow that needs it
If CI generates a file at runtime, every workflow that depends on that
file must include the generation step. A workflow that worked when copied
from another will silently break if it runs in a different order or skips
a step. Treat bootstrap steps as mandatory preamble, not as optimisation
candidates.

### Verify every downloaded binary before executing it
Any file fetched from an external URL (CDN, Maven, GitHub Releases) should
have its SHA-256 verified against a known-good hash before it is
extracted or executed. This prevents MITM attacks and CDN compromises
from silently running malicious build tooling.

For the Gradle bootstrap step, the authoritative hash for each release
is published at <https://gradle.org/releases/> under "checksums" — use
the **binary-only (`-bin`) ZIP** row. When `GRADLE_VERSION` is bumped,
`EXPECTED_SHA256` must be updated in **all three** workflows
(`build.yml`, `release.yml`, `codeql.yml`) in the same commit. A version
bump without a hash update will cause every CI run to abort with
`sha256sum: WARNING: 1 computed checksum did NOT match`.

### Never pass credentials as process arguments
Process arguments (`-P`, `--property`, positional args) are visible to
any co-tenant process that can read `/proc/*/cmdline` on the runner node
and appear in verbose build logs. Pass all secrets exclusively through
`env:` blocks and read them via `System.getenv()` in build scripts.

### Route all `github.*` expressions through `env:` in `run:` blocks
GitHub Actions interpolates `${{ github.* }}` expressions directly into
shell scripts before the shell sees them. A tag name or branch name
containing shell metacharacters (backticks, `$()`, semicolons) would
execute arbitrary code. Assigning the expression to an `env:` variable
first makes it a safe shell string regardless of its content.

### Upgrade GitHub Actions versions proactively
GitHub Actions runners deprecate Node.js runtimes on a rolling basis.
Actions that target a deprecated runtime emit warnings before they become
errors. Treat deprecation warnings as scheduled work — bump the action
version to one that ships the current runtime natively before the
deprecation deadline. Never rely on force-override environment variables
as a permanent fix; they are workarounds, not solutions.

### Path-filter workflows to skip documentation-only commits
A CI pipeline that runs on every commit regardless of what changed wastes
runner minutes and creates noise in the commit status. Filter workflows
to only trigger when source files, build configuration, or the workflow
itself changes. Documentation, ADR, and changelog commits should not
trigger a build.

### `--no-configuration-cache` on AGP tasks
AGP's configuration cache support lags behind Gradle's. When in doubt,
disable it on Gradle tasks invoked from CI to avoid non-deterministic
cache poisoning failures.

---

## 3. Architecture

### Interface every Android-coupled class
Any class that wraps an Android system service (audio, vibration, storage,
notifications, services) should be accessed exclusively through an
interface. The ViewModel must have zero Android framework imports except
`Application` (via `AndroidViewModel`). This is what makes the ViewModel
fully unit-testable without instrumentation.

### Write Fakes, not Mocks, for simple interfaces
For interfaces with simple observable behaviour (call counts, state
changes, flow emissions), a hand-written `Fake` class in `src/test/` is
more readable, faster, and easier to assert on than a mock. Use a mocking
framework only for complex or third-party interfaces where writing a Fake
is impractical.

### No hardcoded magic values in tests
Test durations, counts, and thresholds should be derived from test
configuration objects, not hardcoded. When the configuration changes,
all assertions stay correct automatically.

### Virtual time for timer logic
Any ViewModel that uses `delay()` must be tested with a
`TestCoroutineDispatcher` and `advanceTimeBy()`. Real time in unit tests
is always wrong: it makes the test suite slow and timing-dependent.
A full multi-minute timer cycle should complete in milliseconds in tests.

### Computed state over duplicated state
Any value that can be derived deterministically from other state should
be a computed property on the state data class, not a separate state
field. Separate fields drift; computed properties cannot.

### Foreground Service type must match actual behaviour
Android enforces that the declared `foregroundServiceType` matches what
the service actually does at the permission level. Declaring the wrong
type (e.g. `health` for an app that plays audio) causes runtime failures
on newer Android versions. Choose the type that matches the dominant
user-visible action, not the one that sounds most general.

---

## 4. Code & Build Organisation

### Single source of truth for versions
All dependency and plugin versions live in one place (the version
catalog). No version strings anywhere else in the build — not in
`build.gradle.kts` files, not in workflow YAML, not in comments that
could drift. When a version changes, one file changes.

**Exception — Gradle bootstrap SHA-256:** The `EXPECTED_SHA256` constant
in the three CI bootstrap steps is inherently tied to a specific Gradle
version and cannot live in the version catalog. It must be treated as
part of the same changeset as `GRADLE_VERSION` in those steps: one change,
one commit, all three files.

### Compose BOM in every configuration that uses Compose
Declare the Compose BOM as a `platform()` dependency in every Gradle
configuration that pulls in Compose libraries (including debug-only
configurations). A BOM declared in `implementation` does not
automatically constrain `debugImplementation` versions.

### Sound / asset generation at build time
Generated binary assets (sounds, icons, test fixtures) should be
produced by a script at build time from source parameters, not committed
as binaries. This keeps the repository clean, makes customisation trivial
(edit the script), and avoids binary diff noise in PRs.

---

## 5. Documentation Hygiene

### Version numbers in docs decay immediately
Every version number written in a README, CONTRIBUTING guide, or
Architecture Decision Record is wrong the moment the next upgrade lands.
Keep documentation version-agnostic wherever possible. Where versions
must appear (badges, prerequisites tables), treat them as part of the
same changeset as `libs.versions.toml` — they are updated together in
the same commit, never separately.

### ADRs capture the why, not the what
An Architecture Decision Record is only valuable if it documents the
alternatives that were considered and rejected, and the specific
constraints that made the chosen option correct at the time. A record
that only states what was decided (without why) is redundant with the
code itself.

### The CONTRIBUTING guide is a contract with future contributors
Every command in `CONTRIBUTING.md` must work exactly as written.
Stale version numbers, outdated commands, or missing steps erode trust
in the entire guide. When a toolchain change is made, the CONTRIBUTING
guide is updated in the same commit.

---

## 6. Dependency Management

### Stable releases only — no exceptions
This project upgrades to **stable releases only**. Alpha, beta, RC,
preview, dev, and any other pre-release versions are never used on `main`,
never evaluated as candidates, and never mentioned in upgrade discussions.
If the latest stable version does not yet provide a needed feature,
the upgrade waits until a stable release does. Pre-release versions on
a dedicated experiment branch are also not supported — the added
complexity and instability is not worth it for a single-developer project.

### Check the full compatibility matrix, not just the direct dependency
Many Android ecosystem libraries have implicit compatibility constraints
with each other (annotation processor ↔ compiler ↔ runtime ↔ generated
code). Before upgrading any library that participates in a compilation
pipeline, check whether its transitive dependencies impose version
constraints on other parts of the stack.

### Security and deprecation warnings are scheduled work
Dependabot alerts, deprecated action warnings, and CodeQL findings are
not noise — they are a queue. Triage them on a regular cadence.
A security alert left open for weeks is a risk; a deprecation warning
left open becomes a broken build.

---

## 7. Incident Patterns (What Has Gone Wrong and Why)

| Pattern | Root cause | Prevention |
|---|---|---|
| "Plugin not found" on a new dependency version | Versioning scheme changed in a major release; old-format artifacts don't exist | Check release notes and Maven before committing any version bump |
| Forbidden plugin hard-fail after a build tool upgrade | Build tool absorbed ownership of a compiler step; the old plugin now conflicts | Read the migration guide before any major version bump |
| Missing compiler plugin after removing an old one | Build tool owns compilation but does NOT auto-wire all related compiler plugins | Verify each concern individually; ownership ≠ auto-wiring |
| Binary file corrupted in repo | GitHub Contents API silently corrupts binary files | Never commit binary files via the API; bootstrap at CI runtime |
| Workflow deprecation warning on every run | Action pinned to a version targeting a deprecated runtime | Bump action versions proactively when deprecation warnings appear |
| Stale version in README / CONTRIBUTING | Docs updated separately from the version catalog | Always update docs in the same commit as the version bump |
| `sha256sum: 1 computed checksum did NOT match` | `GRADLE_VERSION` bumped without updating `EXPECTED_SHA256` in all three workflows | Update hash in `build.yml`, `release.yml`, and `codeql.yml` together; source from https://gradle.org/releases/ |
| Signing credentials visible in build logs or process list | Credentials passed as `-P` Gradle project properties | Pass all secrets exclusively via `env:` blocks; read with `System.getenv()` |
| Shell injection via crafted tag/branch name | `${{ github.* }}` interpolated directly into `run:` script body | Route all GitHub context expressions through `env:` before using in shell |

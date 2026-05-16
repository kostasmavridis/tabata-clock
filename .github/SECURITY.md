# Security Policy

## Overview

The security of **Tabata Clock** and its users is taken seriously. This document describes the supported versions, how to responsibly disclose a vulnerability, what to expect after you report one, and the security posture of the project.

---

## Supported Versions

Only the latest release on the `main` branch receives security fixes. Older tags are unsupported.

| Version | Supported |
|---|---|
| Latest (`main`) | ✅ Active |
| Previous tags | ❌ No patches |

---

## Reporting a Vulnerability

**Please do NOT open a public GitHub Issue for security vulnerabilities.**  
Public disclosure before a fix is available puts all users at risk.

### Preferred channel — GitHub Private Security Advisory

1. Go to the [Security Advisories](https://github.com/kostasmavridis/tabata-clock/security/advisories/new) page
2. Click **"Report a vulnerability"**
3. Fill in the details described in the [Report Contents](#report-contents) section below
4. Submit — only maintainers can see this report

### Alternative channel — email

If you are unable to use GitHub's private advisory system, send a PGP-encrypted or plain-text email to:

```
kostas.mavridis@live.com
```

Subject line: `[SECURITY] Tabata Clock — <short description>`

---

## Report Contents

A good report helps us triage and fix the issue faster. Please include as many of the following as possible:

- **Description** — what the vulnerability is and where in the code it exists
- **Impact** — what an attacker could achieve (e.g. data exfiltration, privilege escalation, DoS)
- **Affected version(s)** — commit SHA or release tag
- **Affected component** — e.g. `TabataForegroundService`, `SettingsRepository`, CI pipeline
- **Steps to reproduce** — minimal reproduction with commands, inputs, or a PoC script
- **Suggested fix** — optional, but appreciated
- **Your contact details** — so we can credit you if desired

---

## Response Timeline

| Milestone | Target |
|---|---|
| Acknowledgement of your report | Within **48 hours** |
| Initial triage & severity assessment | Within **5 business days** |
| Fix development & testing | Within **14 days** for Critical/High; **30 days** for Medium/Low |
| Coordinated public disclosure | After fix is released, or 90 days — whichever comes first |

If a deadline cannot be met, we will communicate proactively.

---

## Severity Classification

We use the [CVSS v3.1](https://www.first.org/cvss/calculator/3-1) scoring system as a guide:

| Severity | CVSS Score | Examples |
|---|---|---|
| **Critical** | 9.0 – 10.0 | RCE via malicious input, complete credential theft |
| **High** | 7.0 – 8.9 | Privilege escalation, sensitive data leakage |
| **Medium** | 4.0 – 6.9 | Partial data exposure, foreground service abuse |
| **Low** | 0.1 – 3.9 | Minor info leak, low-impact denial of service |
| **Informational** | N/A | Best-practice deviation with no direct exploit path |

---

## Scope

### In scope

- `app/src/main/` — all Kotlin application source code
- `.github/workflows/` — CI/CD pipeline scripts (supply-chain attacks, secret exposure)
- `scripts/generate_sounds.py` — Python build script executed in CI
- `app/build.gradle.kts` / `settings.gradle.kts` — dependency supply chain
- Gradle wrapper integrity (`gradle-wrapper.properties`)
- Android `AndroidManifest.xml` — permission declarations and exported components

### Out of scope

- Third-party libraries (report to their maintainers directly)
- Vulnerabilities in Android OS itself
- Issues requiring physical access to a rooted device
- UI cosmetic bugs with no security impact
- Theoretical vulnerabilities with no realistic exploit path
- Previously reported and acknowledged issues

---

## Security Design Notes

This section documents the intentional security decisions in the codebase so researchers understand the attack surface.

### Permissions

| Permission | Reason |
|---|---|
| `FOREGROUND_SERVICE` | Required to keep the timer running with the screen off |
| `FOREGROUND_SERVICE_HEALTH` | Android 14+ categorisation for fitness timer services |
| `VIBRATE` | Haptic feedback on phase transitions |
| `POST_NOTIFICATIONS` | Android 13+ permission for the foreground service notification |

No `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `INTERNET`, `CAMERA`, `LOCATION`, or `CONTACTS` permissions are requested or used.

### Data storage

- Settings are stored in **DataStore Preferences** (`app_settings.preferences_pb`) in the app's private data directory. No data is written to external storage or transmitted over the network.
- No analytics SDKs, crash reporters, or third-party telemetry libraries are included.
- No user accounts or authentication of any kind.

### Network

The app has **no `INTERNET` permission** and makes **no network requests** at runtime. The only network activity occurs at build time (Gradle dependency resolution and CI).

### Foreground Service

`TabataForegroundService` is started and stopped exclusively by `IntentServiceNotifier` via explicit Intents. It is **not exported** (`android:exported="false"`) in the manifest, preventing third-party apps from binding or sending Intents to it.

### CI / Supply-chain

- GitHub Actions workflows pin third-party actions to full commit SHAs where possible.
- The `gradle-wrapper.jar` is regenerated on every CI run from the official Gradle distribution URL; it is never committed to the repository.
- The Python sound-generation script (`scripts/generate_sounds.py`) uses only the Python standard library — no `pip` installs, no external network calls.
- Release APKs are signed with a keystore stored exclusively in GitHub Secrets; the keystore is never written to the repository or exposed in logs.

### Secrets

| Secret name | Usage | Exposure risk |
|---|---|---|
| `KEYSTORE_BASE64` | APK signing keystore | GitHub Secrets only — masked in logs |
| `KEYSTORE_PASSWORD` | Keystore store password | GitHub Secrets only — masked in logs |
| `KEY_ALIAS` | Signing key alias | GitHub Secrets only — masked in logs |
| `KEY_PASSWORD` | Signing key password | GitHub Secrets only — masked in logs |

None of these values appear in committed files, build outputs, or CI log output.

---

## Dependency Management

Dependencies are declared in `app/build.gradle.kts` and resolved via Maven Central / Google Maven. We recommend:

- Enabling **Dependabot** version alerts (can be done in the repository's Security tab)
- Reviewing the dependency tree periodically with `./gradlew app:dependencies`
- Not adding new dependencies with `INTERNET`-facing runtime behaviour without explicit justification

---

## Disclosure Policy

We follow a **coordinated responsible disclosure** model:

1. Reporter submits vulnerability privately
2. Maintainer acknowledges and begins triage
3. Fix is developed in a private branch or fork
4. Fix is released with a patch version tag
5. A [GitHub Security Advisory](https://github.com/kostasmavridis/tabata-clock/security/advisories) is published with full details and credit to the reporter
6. If the maintainer cannot produce a fix within 90 days, the reporter is free to disclose publicly

---

## Credit

Reporters who responsibly disclose valid vulnerabilities will be credited in the associated GitHub Security Advisory and in the release notes, unless they prefer to remain anonymous.

---

## Contact

| Channel | Address |
|---|---|
| GitHub Private Advisory | [Report here](https://github.com/kostasmavridis/tabata-clock/security/advisories/new) |
| Email | kostas.mavridis@live.com |

---

*This policy is reviewed whenever a new major version is released or at least once per year.*

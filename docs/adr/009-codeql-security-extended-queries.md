# ADR-009: Use `security-extended` Query Suite for CodeQL Analysis

**Date:** 2026-05-17  
**Status:** Accepted  
**Deciders:** @kostasmavridis  
**Tags:** ci-cd, security

> ℹ️ This ADR was previously numbered ADR-002. Renumbered to ADR-009 when application architecture ADRs (001–007) were introduced.

---

## Context

GitHub CodeQL offers three built-in query suites for Kotlin/Java:

| Suite | Rules included | False-positive rate | Typical use case |
|---|---|---|---|
| `security-and-quality` | Security + code quality | Low | Default for most projects |
| **`security-extended`** ✅ | All of the above + higher-precision security rules | Medium | Security-conscious production apps |
| `security-experimental` | All of the above + experimental rules | High | Security research |

Tabata Clock handles Android Intents, PendingIntents, and a foreground service with an IPC surface — all areas where `security-extended` adds meaningful rules over the default suite.

## Decision

Use `security-extended` as the CodeQL query suite. See the original ADR-002 (now ADR-009) for full rationale.

## Consequences

See original ADR-002 content — unchanged. Marginally longer scan time; meaningful additional coverage for PendingIntent flags, Intent validation, and insecure IPC patterns.

## Alternatives Considered

See original ADR-002. `security-and-quality` is insufficient; `security-experimental` is too noisy.

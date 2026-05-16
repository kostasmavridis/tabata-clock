# ADR-004: Workflow Concurrency and In-Progress Cancellation Strategy

**Date:** 2026-05-17  
**Status:** Accepted  
**Deciders:** @kostasmavridis

---

## Context

Without a `concurrency` configuration, GitHub Actions queues all triggered workflow runs independently. For a project where commits can be pushed in quick succession (e.g. several doc fixes, then a code change), this results in:

- Multiple parallel `build.yml` runs for the same branch, all consuming runner minutes
- Multiple parallel `codeql.yml` runs, each taking ~4–5 minutes
- Multiple `dependency-submission.yml` runs submitting increasingly stale graphs
- (For releases) a partially complete release run potentially being killed mid-signing

GitHub's `concurrency` key allows workflows to declare a group and an in-progress cancellation policy.

## Decision

Apply a `concurrency` block to all four workflows with policies differentiated by workflow type:

### Build, CodeQL, Dependency Submission — cancel in progress

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

For these workflows, only the **latest** run for a given branch/PR is meaningful. An in-progress run based on an older commit is stale and wastes runner minutes. Cancelling it immediately frees the runner for the newer, relevant run.

### CodeQL scheduled runs — fixed group key

The cron trigger always runs on `refs/heads/main`, so all scheduled runs would share the same group key as push runs and cancel each other unexpectedly. The CodeQL workflow uses a conditional group key:

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.event_name == 'schedule' && 'scheduled' || github.ref }}
  cancel-in-progress: true
```

This gives scheduled runs their own isolated group (`CodeQL Analysis-scheduled`) while still allowing push/PR runs to cancel each other per-branch.

### Release — queue, never cancel

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: false
```

A release workflow that is killed mid-run may have already:
- Decoded and written the keystore to disk
- Built and signed the APK/AAB
- Partially uploaded artifacts to a GitHub Release draft

Killing it mid-flight leaves the release in an indeterminate state. With `cancel-in-progress: false`, a second run for the same tag is queued and waits for the first to complete (or fail cleanly). Two tags being pushed simultaneously is an extremely unlikely edge case that does not justify the risk of mid-release cancellation.

## Consequences

### Positive
- Eliminates redundant runner usage on fast-push workflows
- PRs always show the result of the latest commit, not a stale one
- Release builds are never interrupted mid-signing
- Scheduled CodeQL scans do not interfere with push-triggered scans

### Negative
- A cancelled `build.yml` run will show as "Cancelled" in the PR check list until the replacement run completes — this can briefly make a PR appear to have no passing checks
- If two release tags are pushed in rapid succession, the second release is delayed until the first completes

### Mitigations
- GitHub updates PR check status as soon as the replacement run completes — the cancelled status is transient
- Releasing two tags simultaneously is an anti-pattern that should be avoided regardless of this ADR

## Alternatives Considered

### No concurrency configuration (default)
Leads to multiple parallel runs consuming runner minutes and producing confusing check results on PRs.

### `cancel-in-progress: true` for all workflows including release
Unacceptable for release — risks partial releases with orphaned signed artifacts or incomplete GitHub Releases.

### Per-job concurrency instead of per-workflow
More granular but adds complexity without benefit for single-job workflows. All current workflows have one job.

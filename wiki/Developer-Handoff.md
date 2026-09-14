# Developer Handoff

Use this page as the reader-facing Wiki pointer for resuming development. Repository-internal current status lives in `CHECKPOINT.md`; resume instructions live in `HANDOFF.md`.

## Current phase

**Product Experience & Work Management Enrichment**

Search, Command Palette, Favorites/Recently Viewed, My Work, Saved Views and the capability-aware Dashboard are complete. The next implementation slice is the **Calendar / Deadline View**.

## Read first

Inside the Wiki:

1. [[Architecture]]
2. [[Authorization]]
3. [[Security-and-Authentication]]
4. [[Roadmap]]
5. [[Testing-and-CI]]

Inside the repository:

1. `AGENTS.md`
2. `CHECKPOINT.md`
3. `HANDOFF.md`
4. `guides/current_architecture.md`
5. `guides/ENGINEERING_STANDARDS.md`
6. the focused guide for the domain being changed

## Engineering rule from this point forward

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Current reference implementations include Search contributor contracts, Personal Workspace resolver adapters, `MyWorkTaskSource`, the Saved Views context-validator SPI, and Dashboard frontend composition over existing authorized contracts.

For the Calendar / Deadline View, project task dates into a time-oriented surface without rebuilding task authorization or task lifecycle rules inside a new calendar god-service.

## Calendar resume guidance

The first calendar slice should emphasize deadlines rather than general scheduling:

- authorized task due dates in a bounded date range
- existing project deadlines where an owning domain can expose them safely
- month/list-style views with deep links back to owning work
- explicit timezone interpretation/rendering
- useful empty states when a date range has no deadlines
- bounded date-range queries rather than tenant-wide browser filtering

If an aggregate backend endpoint is justified, prefer a narrow `CalendarDeadlineSource` implemented by the project/task domain. Defer meetings, resource booking, leave management and external calendar synchronization.

## Dashboard checkpoint to preserve

The Dashboard Refresh provides:

- My Work summary + bounded attention preview
- Favorites + Recently Viewed
- capability-aware quick actions using the shared workspace-navigation contract
- existing tenant-wide health metrics
- local failure isolation for personal widgets
- no new dashboard backend service or migration

Dashboard widgets remain UX composition. Backend authorization remains authoritative and existing domains continue to own their classification, validation and resource-resolution rules.

## Preserve these system invariants

- tenant isolation precedes resource access
- backend authorization is authoritative
- stored favorites/recents/saved-view definitions never grant resource access
- calendar/deadline results are authorized before exposure
- Explain Access and enforcement use the same evaluator
- delegated authority never exceeds its current direct parent authority
- protected authorization permissions remain non-delegable
- public APIs expose DTOs rather than persistence entities
- applied Flyway migrations remain append-only
- provider secrets remain server-side
- retryable/concurrent flows consider idempotency and locking
- new search/calendar/analytics/automation/AI paths must filter through tenant and authorization boundaries before exposing results

## Immediate product sequence

1. calendar/deadline view
2. subtasks, dependencies and labels
3. recurring work + templates
4. bulk actions + import/export
5. custom fields/forms + workflows/approvals + knowledge/documents
6. user-facing analytics and selected differentiated experiments

## Deferred work

Production Operations & Disaster Recovery remains deliberately deferred behind the current product-enrichment phase. It still includes restore drills, alert/runbook work, broader load/failure-recovery validation and production R2 verification.

See [[Roadmap]] for product direction and the repository `guides/DEFERRED_PLATFORM_WORK.md` for deferred platform work.

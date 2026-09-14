# Developer Handoff

Use this page as the reader-facing Wiki pointer for resuming development. Repository-internal current status lives in `CHECKPOINT.md`; resume instructions live in `HANDOFF.md`.

## Current phase

**Product Experience & Work Management Enrichment**

Search, Command Palette, Favorites/Recently Viewed, My Work and Saved Views are complete. The next implementation slice is a **capability-aware Dashboard Refresh**.

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

Current reference implementations include Search contributor contracts, Personal Workspace resolver adapters, `MyWorkTaskSource`, and the Saved Views context-validator SPI.

For the Dashboard Refresh, compose those existing contracts. Do not build a new dashboard service by injecting project, task, authorization, billing, user and notification services into one orchestrator.

## Dashboard resume guidance

The first dashboard slice should emphasize daily operational value:

- My Work attention summary and a bounded high-priority preview
- Favorites
- Recently Viewed
- capability-aware quick actions that invoke owning feature flows
- bounded deadline/activity context where an owning domain can supply it cleanly
- useful empty states/onboarding when data is sparse

Dashboard widgets remain UX composition. Backend authorization remains authoritative and existing domains continue to own their classification, validation and resource-resolution rules.

## Preserve these system invariants

- tenant isolation precedes resource access
- backend authorization is authoritative
- stored favorites/recents/saved-view definitions never grant resource access
- Explain Access and enforcement use the same evaluator
- delegated authority never exceeds its current direct parent authority
- protected authorization permissions remain non-delegable
- public APIs expose DTOs rather than persistence entities
- applied Flyway migrations remain append-only
- provider secrets remain server-side
- retryable/concurrent flows consider idempotency and locking
- new search/dashboard/analytics/automation/AI paths must filter through tenant and authorization boundaries before exposing results

## Immediate product sequence

1. capability-aware dashboard refresh + onboarding/empty-state polish
2. calendar/deadline view
3. subtasks, dependencies and labels
4. recurring work + templates
5. bulk actions + import/export
6. custom fields/forms + workflows/approvals + knowledge/documents
7. user-facing analytics and selected differentiated experiments

## Deferred work

Production Operations & Disaster Recovery remains deliberately deferred behind the current product-enrichment phase. It still includes restore drills, alert/runbook work, broader load/failure-recovery validation and production R2 verification.

See [[Roadmap]] for product direction and the repository `guides/DEFERRED_PLATFORM_WORK.md` for deferred platform work.

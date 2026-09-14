# Developer Handoff

Use this page as the reader-facing Wiki pointer for resuming development. Repository-internal current status lives in `CHECKPOINT.md`; resume instructions live in `HANDOFF.md`.

## Current phase

**Product Experience & Work Management Enrichment**

Search, Command Palette, Favorites/Recently Viewed, My Work, Saved Views, Dashboard and Calendar/Deadline View are complete. The next implementation slice is **Subtasks + Task Dependencies + Labels/Tags**.

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

Current reference implementations include Search contributor contracts, Personal Workspace resolver adapters, `MyWorkTaskSource`, the Saved Views context-validator SPI, Dashboard frontend composition and Calendar's narrow deadline-source contract.

## Calendar checkpoint

Calendar #137 is a bounded projection over authorized task deadlines:

- `/calendar` month grid and selected-day agenda
- local-time rendering with previous/next/Today navigation
- existing task deep links
- `[from,to)` backend query semantics
- maximum 93-day request range and 500 returned deadlines with explicit truncation signaling
- task-owned candidate retrieval and authoritative task-read revalidation
- no synthetic project deadlines and no schema migration

Do not convert Calendar into a second task system or put unrelated scheduling behavior into its service.

## Next feature guidance

For subtasks, dependencies and labels, establish product invariants before schema/API/UI work.

Subtasks should use a bounded parent/child model, preferably same-project in v1, with self-parenting and ancestry cycles rejected.

Dependencies should be directed edges with no self-edge, no duplicate pair and no directed cycles. Prefer same-project dependencies in v1 unless a concrete cross-project requirement exists.

Labels should be project-scoped in v1 unless tenant-global labels are explicitly required. Normalize names, prevent duplicate names per project, and prevent cross-project task-label assignments.

All three capabilities must preserve tenant isolation and existing task/project authorization. Any persistence changes must use new append-only Flyway migration(s) after V46.

## Preserve these system invariants

- tenant isolation precedes resource access
- backend authorization is authoritative
- stored favorites/recents/saved-view definitions never grant resource access
- calendar/deadline results are authorized before exposure
- task relationship edges never bypass tenant/project/task authorization
- graph traversal and collections remain bounded
- Explain Access and enforcement use the same evaluator
- delegated authority never exceeds its current direct parent authority
- protected authorization permissions remain non-delegable
- public APIs expose DTOs rather than persistence entities
- applied Flyway migrations remain append-only
- provider secrets remain server-side
- retryable/concurrent flows consider idempotency and locking

## Immediate product sequence

1. subtasks + task dependencies + labels/tags
2. recurring work + templates
3. bulk actions + import/export
4. custom fields/forms + workflows/approvals + knowledge/documents
5. user-facing analytics and selected differentiated experiments

## Deferred work

Production Operations & Disaster Recovery remains deliberately deferred behind the current product-enrichment phase. It still includes restore drills, alert/runbook work, broader load/failure-recovery validation and production R2 verification.

See [[Roadmap]] for product direction and the repository `guides/DEFERRED_PLATFORM_WORK.md` for deferred platform work.

# Developer Handoff

Use this page as the reader-facing Wiki pointer for resuming development. Repository-internal current status lives in `CHECKPOINT.md`; resume instructions live in `HANDOFF.md`.

## Current phase

**Product Experience & Work Management Enrichment**

Search, Command Palette, Favorites/Recently Viewed, My Work, Saved Views, Dashboard, Calendar/Deadline View and Task Relationships/Task Planning are established. The next implementation slice is **Recurring Work + Project/Task Templates**.

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
6. `guides/task_relationships.md`
7. the focused guide for the domain being changed

## Engineering rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Current reference implementations include Search contributor contracts, Personal Workspace resolver adapters, `MyWorkTaskSource`, the Saved Views context-validator SPI, Dashboard frontend composition, Calendar's narrow deadline-source contract, and Task Relationships' task gateway/change sink.

## Task Relationships checkpoint

Backend #139 + frontend #140 establish:

- V47 task parent/dependency/project-label persistence
- same-project parent hierarchy with self/cycle rejection
- directed `blocking -> dependent` edges with duplicate/self/cycle prevention
- bounded hierarchy/dependency traversal and reads
- project-scoped normalized labels
- explicit `taskrelationships` backend domain split into query/graph/label responsibilities
- `/task-planning` frontend feature domain using authorization-safe Global Search selection
- shared-navigation integration so Command Palette and Dashboard quick actions inherit Task Planning

Relationship metadata does not automatically change task status. Do not turn this domain into generic graph infrastructure.

Detailed rules live in repository guide `guides/task_relationships.md`.

## Calendar checkpoint

Calendar remains a bounded projection over authorized task deadlines:

- `/calendar` month grid and selected-day agenda
- local-time rendering
- `[from,to)` backend semantics
- maximum 93-day range and 500 returned deadlines with truncation signaling
- task-owned candidate retrieval and authoritative task-read revalidation
- no synthetic project deadlines

Do not convert Calendar into a scheduler or recurrence engine.

## Next feature guidance — Recurring Work + Templates

Before schema/API/UI work, define product semantics explicitly.

Recurring work needs decisions on:

- supported cadence/schedule representation
- timezone and DST ownership
- occurrence materialization horizon
- idempotency/concurrency for occurrence creation
- pause/resume/edit/end behavior
- behavior when previous occurrences are incomplete
- source linkage/auditability for generated tasks

Templates need decisions on:

- tenant versus project scope
- task versus project templates
- copy/snapshot semantics and optional versioning
- which fields, labels, subtasks and dependencies are copied
- authorization for catalog management versus use
- bounded template size and instantiation

Recurrence/templates should use an explicit owning domain and narrow task/project creation contracts. Do not add them to `ProjectTaskService`, `TaskGraphService`, Calendar or shell components merely because they touch tasks.

Any persistence change must use new append-only Flyway migration(s) after **V47**.

## Preserve these system invariants

- tenant isolation precedes resource access
- backend authorization is authoritative
- stored favorites/recents/saved-view definitions never grant resource access
- calendar/deadline and Task Planning data are authorized before exposure
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

1. recurring work + project/task templates
2. bulk actions + CSV import/export
3. custom fields/forms + workflows/approvals + knowledge/documents
4. user-facing analytics and selected differentiated experiments
5. ongoing onboarding/workspace-switching/personalization polish

## Deferred work

Production Operations & Disaster Recovery remains deliberately deferred behind the current product-enrichment phase. It still includes restore drills, alert/runbook work, broader load/failure-recovery validation and production R2 verification.

See [[Roadmap]] for product direction and repository guide `guides/DEFERRED_PLATFORM_WORK.md` for deferred platform work.

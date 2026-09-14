# Developer Handoff

Use this page as the reader-facing Wiki pointer for resuming development. Repository-internal current status lives in `CHECKPOINT.md`; resume instructions live in `HANDOFF.md`.

## Current phase

**Product Experience & Work Management Enrichment**

Search, Command Palette, Favorites/Recently Viewed, My Work, Saved Views, Dashboard, Calendar/Deadline View, Task Relationships/Task Planning, recurring work, project-scoped task templates, tenant-scoped project templates, and the Work Automation & Templates workspace are established through PR #143.

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
7. `guides/recurring_work_and_templates.md`
8. the focused guide for the domain being changed

## Engineering rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Current reference implementations include Search contributor contracts, Personal Workspace resolver adapters, `MyWorkTaskSource`, the Saved Views context-validator SPI, Dashboard frontend composition, Calendar's narrow deadline-source contract, Task Relationships' task gateway/change sink, task-owned `TaskCreationPort`, and project-owned `ProjectCreationPort`.

## Work-generation checkpoint

V48/V49 establish:

- `recurringwork` owning recurring definitions/materialization/history
- `tasktemplates` owning project-scoped task templates
- `projecttemplates` owning tenant-scoped project templates
- task-owned `tasks/creation/TaskCreationPort`
- project-owned `projects/creation/ProjectCreationPort`
- explicit IANA timezone recurrence with `DAILY`, `WEEKLY`, `MONTHLY`
- bounded due discovery/catch-up generation with locking and occurrence idempotency
- pause/resume/edit/end/count semantics
- project-scoped task-template snapshot/copy behavior
- tenant project-template snapshot/copy behavior with max 50 ordered starter tasks
- ordinary project quota/actor/initial-lead/audit/lifecycle behavior preserved by the project-owned adapter
- ordinary task lifecycle behavior preserved by the task-owned adapter
- standalone `/work-automation` frontend workspace

Calendar remains a deadline projection and Task Relationships remains hierarchy/dependency/label ownership.

Detailed rules live in `guides/recurring_work_and_templates.md`.

## Resume here

Start from current `main`. Portable PostgreSQL migrations extend through **V49**; new persistence begins at **V50+**.

The next product slice is **bulk actions + CSV import/export**. Before implementation, choose an explicit owning domain and narrow cross-domain contracts. Do not add import/bulk orchestration by expanding legacy god-services.

Then continue with:

1. custom fields/forms
2. workflows/approvals + knowledge/documents
3. user-facing analytics/reporting and selected differentiated experiments
4. onboarding/workspace-switching/personalization polish

## Preserve these system invariants

- tenant isolation precedes resource access
- backend authorization is authoritative
- stored favorites/recents/saved-view definitions never grant resource access
- Calendar, Task Planning and Work Automation data are authorized before exposure
- task relationship edges never bypass tenant/project/task authorization
- recurring/template generation reuses authoritative project/task access rules
- graph traversal, reads and generation batches remain bounded
- project-template creation enforces ordinary project quota and actor/lead/lifecycle rules
- Explain Access and enforcement use the same evaluator
- delegated authority never exceeds its current direct parent authority
- public APIs expose DTOs rather than persistence entities
- retryable/concurrent flows use idempotency and locking where needed
- provider secrets remain server-side

## Deferred work

Production Operations & Disaster Recovery remains deliberately deferred behind the current product-enrichment phase. It still includes restore drills, alert/runbook work, broader load/failure-recovery validation and production R2 verification.

See [[Roadmap]] for product direction and repository guide `guides/DEFERRED_PLATFORM_WORK.md` for deferred platform work.

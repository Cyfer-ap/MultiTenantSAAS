# Developer Handoff

Use this page as the reader-facing Wiki pointer for resuming development. Repository-internal current status lives in `CHECKPOINT.md`; resume instructions live in `HANDOFF.md`.

## Current phase

**Product Experience & Work Management Enrichment**

Search, Command Palette, Favorites/Recently Viewed, My Work, Saved Views, Dashboard, Calendar/Deadline View and Task Relationships/Task Planning are established. PR #141 is merged and adds the V48 recurring-task/project-task-template backend foundation. The broader recurring/templates milestone remains open.

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

Current reference implementations include Search contributor contracts, Personal Workspace resolver adapters, `MyWorkTaskSource`, the Saved Views context-validator SPI, Dashboard frontend composition, Calendar's narrow deadline-source contract, Task Relationships' task gateway/change sink, and the V48 `TaskCreationPort` work-generation boundary.

## V48 work-generation checkpoint

Merged PR #141 (`3460785aa9a1644768f10c696ccaef27422535f8`) establishes:

- `recurringwork` owning recurring definitions/materialization/history
- `tasktemplates` owning project-scoped task templates
- task-owned `tasks/creation/TaskCreationPort`
- V48 recurring definitions, occurrence linkage and project task templates
- explicit IANA timezone recurrence with `DAILY`, `WEEKLY`, `MONTHLY`
- bounded due discovery and catch-up generation
- pessimistic materialization locking + database occurrence uniqueness
- pause/resume/edit/end/count semantics
- project-scoped task-template snapshot/copy behavior
- ordinary activity/audit/assignment-notification/webhook lifecycle for generated tasks

Calendar remains a deadline projection and Task Relationships remains hierarchy/dependency/label ownership.

Detailed rules live in `guides/recurring_work_and_templates.md`.

## Immediate next slice

Start from current `main`. New persistence begins at **V49+**. Recommended feature branch: `feat/project-templates-workspace`.

Finish the same milestone before starting bulk productivity:

1. tenant-scoped project-template backend
2. project-owned narrow `ProjectCreationPort`
3. bounded project-template task snapshots
4. feature-local recurring-work UI
5. feature-local task-template UI
6. project-template catalog/instantiate UI
7. final recurring/templates milestone documentation and full validation

Project-template instantiation must preserve project quota, actor validation, initial owner/lead membership, audit and project lifecycle behavior without injecting the full legacy `ProjectService` into the template domain.

Applied V48 and earlier migrations remain immutable.

## Preserve these system invariants

- tenant isolation precedes resource access
- backend authorization is authoritative
- stored favorites/recents/saved-view definitions never grant resource access
- Calendar and Task Planning data are authorized before exposure
- task relationship edges never bypass tenant/project/task authorization
- recurring/template generation reuses authoritative project/task access rules
- graph traversal, reads and generation batches remain bounded
- Explain Access and enforcement use the same evaluator
- delegated authority never exceeds its current direct parent authority
- public APIs expose DTOs rather than persistence entities
- retryable/concurrent flows use idempotency and locking where needed
- provider secrets remain server-side

## Immediate product sequence

1. finish recurring work + project/task templates
2. bulk actions + CSV import/export
3. custom fields/forms + workflows/approvals + knowledge/documents
4. user-facing analytics and selected differentiated experiments
5. onboarding/workspace-switching/personalization polish

## Deferred work

Production Operations & Disaster Recovery remains deliberately deferred behind the current product-enrichment phase. It still includes restore drills, alert/runbook work, broader load/failure-recovery validation and production R2 verification.

See [[Roadmap]] for product direction and repository guide `guides/DEFERRED_PLATFORM_WORK.md` for deferred platform work.
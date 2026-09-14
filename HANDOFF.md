# MultiTenantSAAS — Development Handoff

Updated: 2026-09-14

This is the **single repository-side resume document**. Current status lives in `CHECKPOINT.md`; architecture/quality rules live in `AGENTS.md` and `guides/ENGINEERING_STANDARDS.md`.

## Read first

1. `AGENTS.md`
2. `CHECKPOINT.md`
3. `guides/current_architecture.md`
4. `guides/ENGINEERING_STANDARDS.md`
5. `guides/task_relationships.md`
6. `guides/recurring_work_and_templates.md`
7. `guides/Wild_Thoughts.md`
8. the focused guide for the domain being changed
9. `wiki/Roadmap.md` when planning product direction

## Current state

Major application/product milestones are complete through:

- billing/catalog — #106
- tenant outbound webhooks — #112
- enterprise OIDC SSO — #119
- authorization delegation + Explain Access — #125/#126
- product vision/documentation governance — #127/#128
- Global Search — #129
- Command Palette — #130
- Favorites + Recently Viewed — #131/#132
- My Work — #133
- Saved Views — #134
- Dashboard Refresh — #136
- Calendar / Deadline View — #137/#138
- task relationships backend — #139
- Task Planning UI — #140
- recurring-task + project-scoped task-template backend foundation — #141

PR #141 is **merged into `main`** at merge commit `3460785aa9a1644768f10c696ccaef27422535f8`.

Portable common Flyway migrations extend through **V48**.

Stripe is the validated deployed Test Mode billing path. Razorpay integration/catalog provisioning remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

## Current direction

Continue **Product Experience & Work Management Enrichment**.

The current milestone is **Recurring Work + Project/Task Templates**. Its backend task-generation half is established in merged PR #141, but the milestone is **not closed**.

### Resume here

Start from current `main` and create the next feature branch for the completion slice (recommended: `feat/project-templates-workspace`). New persistence must be **V49+**.

Immediate next work:

1. tenant-scoped project-template backend
2. project-owned narrow `ProjectCreationPort` preserving quota/owner-membership/audit/lifecycle behavior
3. bounded project-template task snapshots and deterministic instantiation semantics
4. feature-local recurring-work and task-template frontend UX
5. project-template frontend UX
6. milestone docs/CI closure

Then continue with bulk actions/CSV import-export, tenant adaptability, analytics, and ongoing UX polish.

## Architecture rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Backend features should use explicit domain packages. Frontend features should preserve locality under `features/<domain>/...`.

Do not expand `ProjectTaskService` or `ProjectService` merely because a new feature eventually creates tasks/projects.

## Work-generation architecture established by #141

```text
recurringwork ───────────┐
                         ├─> TaskCreationPort
project task templates ──┘          ↓
                            task-owned adapter
                                   ↓
                            normal task lifecycle
                            ├─ persistence
                            ├─ activity
                            ├─ audit
                            ├─ assignment notification
                            └─ TASK_CREATED webhook
```

The owning backend modules are:

- `recurringwork`
- `tasktemplates`
- `tasks/creation` for the narrow task creation boundary

Neither recurring work nor templates depends on the full `ProjectTaskService`.

## Recurring-task checkpoint to preserve

V48 tables:

- `recurring_task_definitions`
- `recurring_task_occurrences`
- `project_task_templates`

Recurring task rules:

- task recurrence only in v1
- `DAILY`, `WEEKLY`, `MONTHLY`
- explicit IANA timezone
- interval 1–52
- optional due offset/end/max occurrences
- generated tasks are snapshots; editing a definition never rewrites prior tasks
- previous occurrence completion does not gate later generation
- pause stops generation
- resume skips paused-period occurrences and starts at the first future schedule
- due discovery capped at 50 definitions/pass
- catch-up capped at 5 occurrences/definition/pass
- per-definition pessimistic materialization lock
- occurrence uniqueness `(definition_id, scheduled_for)` provides database idempotency
- generation failure pauses the definition with a bounded diagnostic
- schedule arithmetic preserves local wall time across DST and uses calendar-month semantics

Calendar remains a deadline projection and must not become the recurrence scheduler.

## Project-scoped task-template checkpoint to preserve

- project-scoped catalog
- maximum 100 templates/project
- normalized unique name/project
- template snapshot: title, description, priority, optional assignee, optional due offset
- instantiate through `TaskCreationPort`
- current actor becomes creator
- template edit/delete never mutates existing tasks
- optional assignee is revalidated as active/project-member at task creation
- no child/dependency/label/custom-field/workflow snapshot in v1

Detailed contract: `guides/recurring_work_and_templates.md`.

## Next backend slice — tenant-scoped project templates

Use a dedicated owning domain; do not turn `tasktemplates` into a generic task/project template god-service if separate ownership is clearer.

Recommended v1 semantics:

- tenant-scoped reusable project template catalog
- normalized unique template name per tenant
- project metadata snapshot: name seed/title, optional description, initial status/visibility only where existing project model supports them
- bounded embedded task snapshots, preferably max 50/template
- task snapshot fields should initially match project task templates: title, description, priority, optional due offset
- do not include dependency/subtask graph in first version
- instantiation is snapshot/copy semantics; later template edits never mutate created projects/tasks
- actor invoking instantiation becomes project owner/lead through existing ownership rules
- project quota must be enforced exactly as ordinary project creation does
- project creation + initial owner membership + task creation need deterministic transaction/failure semantics

Required boundary:

```text
projecttemplates
      ↓
ProjectCreationPort       TaskCreationPort
      ↓                         ↓
project-owned adapter      task-owned adapter
```

The project-owned adapter should preserve the ordinary project lifecycle invariants currently embedded in `ProjectService`: tenant validation, quota enforcement, actor validation, owner membership, audit and relevant lifecycle events. Do not simply inject `ProjectService` into the template domain.

## Next frontend slice

Inspect the current project details/navigation surfaces before editing. Prefer explicit feature domains such as:

```text
features/recurring-work/
features/task-templates/
features/project-templates/
```

or another equally explicit ownership split based on the final UX.

Requirements:

- permission-aware reads/mutations using existing project-task/project authority
- recurring rule create/edit/pause/resume with timezone visible to the user
- clear next-occurrence, status and generation-history presentation
- task-template catalog + create/edit/delete/instantiate
- project-template catalog + instantiate flow
- no recurrence business logic inside Calendar
- no template business logic inside `ProjectTasksSection` or `AppShell`
- shared navigation metadata only if a standalone workspace is justified

## Existing boundaries to preserve

### Task Relationships

V47 owns parent hierarchy, directed dependencies and project-scoped labels. Recurrence/templates are not graph metadata and must stay outside `taskrelationships`.

### Calendar

Calendar owns authorization-safe projection of actual task due dates. It does not pre-generate recurrence instances and does not own schedule rules.

### Dashboard / Command Palette

These are composition/discovery surfaces. They may link into recurring/templates later but do not own their lifecycle.

## Database rule

V48 is the current portable common baseline. New project-template persistence must be **V49+**, append-only. Never rewrite V48 or earlier migrations.

## Validation before merge

For every slice, require applicable green gates:

- Repository Hygiene
- Backend build/test/verify
- PostgreSQL/Flyway
- Frontend format/tests/coverage/lint/build when frontend changes
- Security
- Container CI
- Qodana
- Wiki Sync when Wiki source changes

Do not merge around failed gates.
# MultiTenantSAAS — Development Handoff

Updated: 2026-09-15

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

Major milestones are complete through the Recurring Work + Project/Task Templates program:

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
- task relationships backend + Task Planning — #139/#140
- recurring-task + project-scoped task-template backend foundation — #141
- documentation checkpoint — #142
- tenant-scoped project templates + Work Automation & Templates workspace — #143

Portable PostgreSQL Flyway migrations now extend through **V49**. New persistence must be **V50+**; never modify V49 or earlier applied migrations.

Stripe remains the validated deployed Test Mode billing path. Razorpay integration/catalog provisioning remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

## Architecture rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Backend features should use explicit domain packages. Frontend features should preserve locality under `features/<domain>/...`.

Do not expand `ProjectTaskService` or `ProjectService` merely because a new feature eventually creates tasks/projects.

## Work automation architecture to preserve

```text
recurringwork ───────────┐
                         ├─> TaskCreationPort
project task templates ──┘          ↓
                            task-owned adapter
                                   ↓
                            normal task lifecycle

projecttemplates ──> ProjectCreationPort ──> project-owned adapter ──> normal project lifecycle
       │
       └────────────> TaskCreationPort ─────> task-owned adapter ─────> starter tasks
```

Owning domains/contracts:

- `recurringwork`
- `tasktemplates`
- `projecttemplates`
- `tasks/creation/TaskCreationPort`
- `projects/creation/ProjectCreationPort`

Ordinary project creation and project-template creation converge on the same project-owned adapter. That adapter preserves tenant/actor validation, subscription project quota, initial `PROJECT_LEAD` membership, persistence, audit, and project lifecycle/webhook behavior.

Project-template orchestration must not inject the full `ProjectService`. Recurring work and templates must not inject the full `ProjectTaskService`.

## Recurring-work checkpoint

V48 tables:

- `recurring_task_definitions`
- `recurring_task_occurrences`
- `project_task_templates`

Rules to preserve:

- task recurrence only in v1
- `DAILY`, `WEEKLY`, `MONTHLY`
- explicit IANA timezone
- interval 1–52
- optional due offset/end/max occurrences
- generated tasks are snapshots; editing a rule never rewrites prior tasks
- pause stops generation; resume skips paused-period occurrences
- due discovery capped at 50 definitions/pass
- catch-up capped at 5 occurrences/definition/pass
- pessimistic per-definition materialization lock
- unique `(definition_id, scheduled_for)` database idempotency
- generation failure pauses the definition with bounded diagnostics
- schedule arithmetic preserves local wall time across DST and uses calendar-month semantics

Calendar remains a deadline projection and must not become the recurrence scheduler.

## Task-template checkpoint

- project-scoped catalog
- maximum 100 templates/project
- normalized unique name/project
- snapshot: title, description, priority, optional assignee, optional due offset
- instantiate through `TaskCreationPort`
- current actor is task creator
- optional assignee is revalidated through ordinary task creation
- template edit/delete never mutates existing tasks
- no child/dependency/label/custom-field/workflow snapshot in v1

## Project-template checkpoint

V49 tables:

- `project_templates`
- `project_template_tasks`

Rules to preserve:

- tenant-scoped catalog
- normalized unique template name/tenant
- project name seed, optional description, non-archived initial status
- maximum 50 ordered starter-task snapshots/template
- starter snapshot: title, optional description, priority, optional due offset
- no dependency/subtask/label/custom-field/workflow graph in v1
- snapshot/copy semantics; later template edits do not mutate instantiated work
- project created through `ProjectCreationPort`
- starter tasks created through `TaskCreationPort`
- actor becomes initial project lead through ordinary lifecycle behavior
- project quota enforced exactly as ordinary project creation
- project + starter-task instantiation is transactional
- task due offsets are relative to the created project's creation instant

Detailed contract: `guides/recurring_work_and_templates.md`.

## Frontend checkpoint

The standalone `/work-automation` workspace owns the UX and is split across:

```text
features/recurring-work/
features/task-templates/
features/project-templates/
features/work-automation/
```

It supports:

- authorization-safe discovery of tenant-wide and project-scoped project access
- recurring rule create/edit/pause/resume/history with timezone visible
- task-template create/edit/delete/instantiate
- project-template create/edit/delete/instantiate
- optional project-name override
- bounded 50-row starter-task editor

Do not move recurrence/template business logic into Calendar, `ProjectTasksSection`, `AppShell`, or task-relationship graph code.

## Existing boundaries to preserve

### Task Relationships

V47 owns parent hierarchy, directed dependencies and project-scoped labels. Recurrence/templates are not graph metadata.

### Calendar

Calendar owns authorization-safe projection of actual task due dates. It does not pre-generate recurrence instances and does not own schedule rules.

### Dashboard / Command Palette

These are composition/discovery surfaces. They may link into automation but do not own its lifecycle.

## Resume here

After #143 is merged green, continue **Product Experience & Work Management Enrichment** in this order:

1. bulk actions + CSV import/export
2. custom fields/forms
3. workflows/approvals + knowledge/documents
4. user-facing analytics/reporting and selected differentiated experiments
5. onboarding/workspace-switching/personalization polish

For the next slice, first identify the explicit owning domain and its narrow contracts before implementation. Do not bolt bulk/import behavior into existing god-services.

## Validation before merge

For every slice require applicable green gates:

- Repository Hygiene
- Backend build/test/verify
- PostgreSQL/Flyway
- Frontend format/tests/coverage/lint/build when frontend changes
- Security
- Container CI
- Qodana
- Wiki Sync when Wiki source changes

Do not merge around failed gates.

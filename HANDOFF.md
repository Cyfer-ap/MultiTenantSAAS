# MultiTenantSAAS — Development Handoff

Updated: 2026-09-14

This is the **single repository-side resume document**. Current status lives in `CHECKPOINT.md`; architecture/quality rules live in `AGENTS.md` and `guides/ENGINEERING_STANDARDS.md`.

## Read first

1. `AGENTS.md`
2. `CHECKPOINT.md`
3. `guides/current_architecture.md`
4. `guides/ENGINEERING_STANDARDS.md`
5. `guides/task_relationships.md`
6. `guides/Wild_Thoughts.md`
7. the focused guide for the domain being changed
8. `wiki/Roadmap.md` when planning product direction

## Current state

Major application foundations are complete through:

- billing/catalog: #106
- tenant outbound webhooks: #112
- enterprise OIDC SSO: #119
- authorization delegation + Explain Access: #125
- authorization milestone closure: #126
- product vision/Wild Thoughts refresh: #127
- documentation/engineering-governance consolidation: #128
- permission-aware Global Search: #129
- capability-aware Command Palette: #130
- Favorites + Recently Viewed: #131
- contextual favorite controls: #132
- My Work attention queue: #133
- server-backed Saved Views: #134
- capability-aware Dashboard Refresh: #136
- authorization-safe Calendar / Deadline View: #137
- Calendar UI refresh: #138
- task-relationship backend foundation: #139
- Task Planning UI: #140

Portable common Flyway migrations extend through **V47**.

Stripe is the validated deployed Test Mode billing path. Razorpay integration/catalog provisioning remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

## Current direction

Continue **Product Experience & Work Management Enrichment** before returning to the deferred operations/DR milestone.

Search, Command Palette, Favorites/Recent, My Work, Saved Views, Dashboard, Calendar and task relationships/Task Planning are established. The next implementation slice is **Recurring Work + Project/Task Templates**.

Recommended sequence:

1. recurring work + project/task templates
2. bulk actions + CSV import/export
3. custom fields/forms + workflows/approvals + knowledge/documents
4. user-facing analytics + selected differentiated experiments
5. onboarding/workspace-switching/personalization polish as product flows deepen

## Architecture rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

For backend features, prefer explicit domain packages rather than expanding the global `service/controller/entity/repository/dto` buckets.

For frontend features, preserve feature locality under `features/<domain>/...`.

Existing large services should not receive more dependencies casually. If new work would do that, extract an orchestrator, narrow query/service contract, adapter, or application/domain event as appropriate.

Important boundary rules should gain architecture/static regression tests when practical.

## Product-enrichment foundations to reuse

```text
Global Search
    ↓
search coordinator → contributor contracts → project/task/user adapters

Personal Workspace
    ↓
personal state coordinator → project/task resolver adapters

My Work
    ↓
attention coordinator → MyWorkTaskSource

Saved Views
    ↓
view persistence/validation → contextual validator SPI

Dashboard
    ↓
frontend composition → existing authorized feature queries + workspace navigation contract

Calendar
    ↓
calendar coordinator → CalendarDeadlineSource → task-owned authorized deadline adapter

Task Relationships
    ↓
query / graph / label services
    ↓
TaskRelationshipTaskGateway + TaskRelationshipChangeSink
    ↓
existing task persistence + audit/activity
```

The frontend keeps these concerns in separate feature domains. Preserve that ownership model.

## Task-relationship checkpoint to preserve

Backend PR #139 and frontend PR #140 implement the first complete task-relationship slice.

### Persistence and invariants

V47 adds:

- optional `project_tasks.parent_task_id`
- directed `task_dependencies`
- project-scoped `project_task_labels`
- task-label assignment table
- composite tenant/project constraints for relationships

Subtasks:

- one optional parent
- same tenant/project
- no self-parent
- no ancestry cycle
- ancestry traversal bounded at 64
- direct-child reads capped at 200 with explicit truncation
- no automatic child status propagation

Dependencies:

- `blocking task -> dependent task`
- same tenant/project
- no self-dependency
- duplicate edges are idempotent + DB-unique
- no directed cycles
- project validation capped at 1,000 edges
- blocker/dependent reads capped at 200 with explicit truncation
- no automatic task-status changes

Labels:

- project-scoped
- normalized name unique per project
- bounded catalog: 200/project
- bounded assignment: 20/task
- optional six-digit hex display color
- labels are metadata, not custom fields

### Backend ownership

```text
TaskRelationshipController
        ├── TaskRelationshipQueryService
        ├── TaskGraphService
        └── TaskLabelService
                  ↓
       TaskRelationshipTaskGateway
                  ↓
       existing project/task persistence
```

Audit/activity fan-out is isolated behind `TaskRelationshipChangeSink`.

Do not move recurrence, templates, workflows, automation, scheduling, or custom fields into these relationship services merely because they operate on tasks.

### Frontend ownership

`/task-planning` is implemented under `features/task-relationships`.

It:

- uses Global Search for authorization-safe task discovery
- shows parent/subtasks, blockers/dependents and labels
- supports same-project parent/blocker selection
- provides project label lifecycle + task assignment
- derives management capability from the established task-management permission/project-lead rules
- is registered through the shared workspace navigation contract, so Command Palette and Dashboard quick actions inherit it

The feature deliberately does **not** expand the already-large `ProjectTasksSection` or turn Dashboard/Calendar into relationship owners.

See `guides/task_relationships.md`.

## Next feature guidance — Recurring Work + Templates

Treat recurrence and templates as a new work-generation/configuration capability, not as another field bolted onto task relationships.

### Recurring work

Decide semantics before schema/API work:

- what can recur: task only in v1, or project/template instances too?
- schedule representation: explicit cadence/RRULE-like rule versus a deliberately smaller supported schedule model
- timezone ownership: tenant, user, project, or schedule-specific; do not silently use server timezone
- materialization horizon: generate on demand/worker versus pre-generate bounded future instances
- idempotency key for each recurrence occurrence so retries cannot create duplicate tasks
- behavior when a previous occurrence is incomplete
- start/end/until/count semantics
- pause/resume/edit behavior and what happens to already-materialized tasks
- assignee/labels/priority/due offset copying
- authorization for create/edit/pause recurrence rules
- auditable source linkage from generated task back to its recurrence definition

Do not put recurrence generation in Calendar. Calendar remains a projection of deadlines, not a scheduler.

A good backend shape is likely:

```text
recurringwork domain
    ↓
recurrence definition + occurrence materializer
    ↓
narrow task-creation contract
    ↓
existing task domain
```

The recurrence domain should not inject the entire `ProjectTaskService` if a narrower task-creation command/port can express the required operation.

### Project/task templates

Decide template ownership explicitly:

- project-scoped versus tenant-scoped template catalogs
- task templates versus project templates
- template copy/snapshot semantics: existing created work must not mutate when a template changes
- optional versioning if users need repeatable historical definitions
- which fields are copied: title, description, priority, due offset, labels, assignee strategy, child task structure, dependency structure
- whether project templates may instantiate multiple task relationships in one transaction
- authorization for template catalog management versus template use
- limits on template size/nested task count

Templates are not Saved Views and not custom fields. Keep their lifecycle in an explicit owning module.

### Recommended delivery sequence

1. write the recurrence/template product semantics and invariants
2. define narrow task/project creation contracts needed by the new domain
3. append new Flyway migration(s); never rewrite V47 or earlier
4. implement recurrence/template persistence + services with idempotency/concurrency tests
5. expose DTO/API contracts without persistence entities
6. build feature-local frontend flows
7. add audit/activity where meaningful through narrow sinks/events
8. update docs and run all CI/security/static gates

## Calendar checkpoint to preserve

Calendar remains an **authorization-safe time projection**, not a scheduling domain:

- `/calendar` workspace
- local-time Monday-start six-week month grid
- selected-day agenda
- task due dates only
- `[from,to)` API ranges capped at 93 days
- results capped at 500 with `truncated=true`
- no Calendar-specific task mutation lifecycle

Do not put recurrence engines, reminders, meeting scheduling, room booking or external calendar sync into `CalendarDeadlineService`.

## Dashboard checkpoint to preserve

Dashboard remains frontend composition:

- My Work summary + bounded attention preview
- Favorites and Recently Viewed
- quick actions from shared capability-aware navigation
- existing tenant-wide health metrics
- local degradation of personal widgets
- no broad `DashboardService`

## Invariants to preserve

- backend authorization remains authoritative
- tenant isolation precedes resource access
- discovery constrains access before returning entity data
- stored favorites/recents/saved-view definitions never grant authorization
- task hierarchy/dependency edges never bypass task authorization
- applied Flyway migrations are append-only
- graph traversals and collections remain bounded
- retryable/competing mutations consider idempotency and concurrency
- provider/webhook lifecycle remains verified and auditable
- provider secrets/sensitive identifiers remain server-side
- Explain Access and enforcement share the evaluator
- delegated authority remains a current permission/scope/validity subset of its direct source

## Validation workflow

Use branch-first PR development. GitHub Actions is authoritative where local environments cannot cover the full stack.

Before merge, applicable gates should be green:

- Repository Hygiene
- Backend
- PostgreSQL/Flyway
- Frontend format/tests/lint/build
- Security
- Container CI
- Qodana
- Wiki validation when Wiki source changes

Do not bypass failing checks to finish quickly; inspect and repair the root cause.

## Deferred work

Deferred but still important:

- PostgreSQL backup/restore drills
- monitoring/alerting/runbooks
- broader failure-recovery/load validation
- production R2 verification
- optional SAML/SCIM
- optional MFA/passkeys/device-management expansion

See `guides/DEFERRED_PLATFORM_WORK.md`.

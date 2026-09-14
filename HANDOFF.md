# MultiTenantSAAS — Development Handoff

Updated: 2026-09-14

This is the **single repository-side resume document**. Current status lives in `CHECKPOINT.md`; architecture/quality rules live in `AGENTS.md` and `guides/ENGINEERING_STANDARDS.md`.

## Read first

1. `AGENTS.md`
2. `CHECKPOINT.md`
3. `guides/current_architecture.md`
4. `guides/ENGINEERING_STANDARDS.md`
5. `guides/Wild_Thoughts.md`
6. the focused guide for the domain being changed
7. `wiki/Roadmap.md` when planning product direction

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

Portable common Flyway migrations extend through **V46**. Calendar #137 introduces no schema migration.

Stripe is the validated deployed Test Mode billing path. Razorpay integration/catalog provisioning remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

## Current direction

Continue **Product Experience & Work Management Enrichment** before returning to the deferred operations/DR milestone.

Search, Command Palette, Favorites/Recent, My Work, Saved Views, Dashboard and Calendar/Deadline View are complete. The next implementation slice is **Subtasks + Task Dependencies + Labels/Tags**.

Recommended sequence:

1. subtasks + task dependencies + labels/tags
2. recurring work + project/task templates
3. bulk actions + import/export
4. custom fields/forms + workflows/approvals + knowledge/documents
5. user-facing analytics + selected differentiated experiments
6. onboarding/workspace-switching/personalization polish as product flows deepen

## Architecture rule from this point forward

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

For backend features, prefer explicit domain packages rather than expanding the global `service/controller/entity/repository/dto` buckets.

For frontend features, preserve feature locality under `features/<domain>/...`.

Existing large services should not receive more dependencies casually. If new work would do that, extract an orchestrator, narrow query/service contract, or application/domain event as appropriate.

Important boundary rules should gain architecture/static regression tests when practical.

## Product-enrichment foundations to reuse

The current product layer exposes reusable bounded contracts:

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
```

The frontend keeps Search, Command Palette, Personal Workspace, My Work, Saved Views, Dashboard and Calendar in separate feature domains. Preserve that ownership model.

Create Task remains deliberately absent from the global palette. Effective task-management authority can arise from project-lead membership in addition to scoped authorization, so a future global Create Task action needs a project-aware capability/picker contract rather than shell-side permission guessing.

## Calendar checkpoint to preserve

PR #137 implements Calendar as a **time-oriented projection of authorized tasks**, not a second task system.

Current behavior:

- `/calendar` workspace route and shared navigation/command exposure
- Monday-start six-week month grid rendered in the browser's local timezone
- previous/next month and Today navigation
- selected-day agenda with status, priority, project context and task deep links
- task due dates only; projects currently have no deadline field and no synthetic project dates were introduced
- API: `GET /api/tenants/{tenantId}/calendar/deadlines?from=<instant>&to=<instant>&limit=<n>`
- half-open `[from,to)` range semantics
- maximum range of 93 days
- maximum returned items of 500, with explicit `truncated=true` when the bound is exceeded
- no new table or migration; existing indexed `project_tasks.due_at` is reused

Backend shape:

```text
CalendarDeadlineController
        ↓
CalendarDeadlineService
        ↓
CalendarDeadlineSource
        ↓
TaskCalendarDeadlineSource
        ↓
tenant/date-bounded task query
        ↓
project-scope / membership narrowing
        ↓
authoritative task-read revalidation
```

The generic project-membership read helper now lives under `projects.query.ProjectMembershipQueryService` and is shared by Search and Calendar.

Do not later move task mutations, recurrence, reminders, meeting scheduling, room booking or external calendar sync into `CalendarDeadlineService`. Calendar remains a projection/composition domain unless a future capability has a genuinely separate lifecycle.

## Next feature guidance — Subtasks + Dependencies + Labels

Treat these as **task relationships and task metadata**, owned by the project/task domain. Do not build a generic graph platform first.

### Subtasks

A sensible v1 is one optional parent task per task.

Required invariants:

- parent and child belong to the same tenant
- v1 should keep parent and child in the same project unless a strong product requirement justifies cross-project hierarchy
- a task cannot parent itself
- assigning a parent must not create an ancestry cycle
- traversal must be bounded; do not recursively materialize an unbounded tree
- deleting/archiving/completing a parent must have an explicit documented effect on children
- reads/mutations must reuse the same project/task authorization boundary as ordinary tasks

Prefer an explicit task-relationship service/contract rather than adding hierarchy traversal and dependency orchestration directly into the existing large `ProjectTaskService`.

### Task dependencies

Model dependencies as directed edges with clear semantics, for example:

```text
blocking task  ──blocks──>  dependent task
```

Required invariants:

- no self-dependency
- duplicate edges are rejected/idempotently prevented by a database uniqueness constraint
- dependency creation must reject directed cycles
- v1 should strongly prefer same-project dependencies to keep authorization and UX coherent; broaden later only if there is a concrete cross-project need
- both endpoints of an edge must be tenant-safe and readable/manageable under the owning task rules
- graph traversal/cycle checks are bounded and tested
- deleting or archiving either task has deterministic dependency cleanup/history behavior

Do not derive task status automatically from dependencies in the first migration unless the product rule is explicitly designed. A dependency can initially be metadata/visibility used by UX and later automation.

### Labels / tags

Prefer project-scoped labels for v1 unless there is a clear tenant-global label requirement.

A clean model is:

```text
project label
    ↓ many-to-many
project task
```

Important rules:

- tenant + project ownership on every label
- normalized label names unique per project
- task-label assignments cannot cross tenant/project boundaries
- reading task labels follows task-read access
- creating/editing labels and assigning them should follow existing project/task management authority rather than inventing shell-side role checks
- keep color/display metadata optional and bounded; labels are not custom fields

### Schema and delivery strategy

V46 is the current portable migration baseline. Any new task-relationship schema must be added through **new append-only migration(s)**; never rewrite V46 or earlier migrations.

Before writing the migration, inspect the current `ProjectTask`, task repository/service/controller, authorization helpers, task board UI and project details flow. Decide and document the relationship semantics first, then design constraints/indexes around those semantics.

A practical delivery sequence is:

1. model + migration + repository constraints for parent/dependency/label relationships
2. narrow backend services/contracts with cycle/tenant/project validation
3. DTO/API integration without returning persistence entities
4. focused integration tests for cross-tenant/cross-project/cycle/duplicate cases
5. project task UI for subtasks/dependencies/labels
6. board/list/filter integration where useful
7. docs/checkpoint and full CI/security/static validation

## Dashboard checkpoint to preserve

PR #136 turns the tenant dashboard into a composition surface rather than a new backend domain:

- My Work counts + bounded attention preview
- Favorites and Recently Viewed
- quick actions derived from the shared capability-aware navigation contract
- existing tenant-wide health metrics retained
- personal widget failures degrade locally
- no new backend endpoint or migration

Do not later move these concerns into one broad `DashboardService` merely for convenience.

## Invariants to preserve

- backend authorization remains authoritative
- tenant isolation precedes resource access
- search/discovery and saved/recent resolution constrain access before returning entity data
- stored favorites, recents or saved-view definitions never grant authorization
- calendar/deadline results are authorized before exposure
- task hierarchy/dependency edges never bypass tenant/project/task authorization
- Explain Access and enforcement share the evaluator
- delegated authority remains a current permission/scope/validity subset of its direct source
- `authorization.manage` and `authorization.delegate` remain non-delegable
- provider/webhook lifecycle remains verified and auditable
- applied Flyway migrations are append-only
- provider secrets and sensitive identifiers remain server-side
- unbounded collections and graph traversals are paginated/bounded
- concurrency/idempotency is considered for retryable or competing mutations

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

Do not accidentally pull the project back into operations work before the current product-enrichment phase is developed.

Deferred but still important:

- PostgreSQL backup/restore drills
- monitoring/alerting/runbooks
- broader failure-recovery/load validation
- production R2 verification
- optional SAML/SCIM
- optional MFA/passkeys/device-management expansion

See `guides/DEFERRED_PLATFORM_WORK.md`.

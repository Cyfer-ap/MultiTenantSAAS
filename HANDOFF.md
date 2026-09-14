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

Portable common Flyway migrations extend through **V46**.

Stripe is the validated deployed Test Mode billing path. Razorpay integration/catalog provisioning remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

## Current direction

Continue **Product Experience & Work Management Enrichment** before returning to the deferred operations/DR milestone.

Search, Command Palette, Favorites/Recent, My Work, Saved Views and the capability-aware Dashboard are complete. The next implementation slice is the **Calendar / Deadline View**.

Recommended sequence:

1. calendar/deadline view
2. subtasks, dependencies and labels
3. recurring work + project/task templates
4. bulk actions + import/export
5. custom fields/forms + workflows/approvals + knowledge/documents
6. user-facing analytics + selected differentiated experiments
7. onboarding/workspace-switching/personalization polish as product flows deepen

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
```

The frontend keeps Search, Command Palette, Personal Workspace, My Work, Saved Views and Dashboard composition separate. Preserve that ownership model.

Create Task remains deliberately absent from the global palette. Effective task-management authority can arise from project-lead membership in addition to scoped authorization, so a future global Create Task action needs a project-aware capability/picker contract rather than shell-side permission guessing.

## Next feature guidance — Calendar / Deadline View

Treat the calendar as a **time-oriented projection of authorized work**, not a second task-management system.

The first slice should be deliberately bounded:

- show authorized task due dates in month/list-style time views
- include existing project deadlines only where an owning domain can expose them safely
- support navigation back to the owning project/task surface
- preserve task/project authorization before events are returned
- render timestamps consistently and design timezone handling explicitly
- provide useful empty states for periods with no deadlines
- keep queries bounded by date range rather than loading all tenant work

Architecture constraints:

- do not duplicate task-read authorization in a calendar controller/service
- do not create a calendar service that directly imports many unrelated repositories
- prefer a narrow calendar/deadline source contract implemented by the project/task domain if a backend aggregate endpoint is justified
- reuse current task status/due-date semantics rather than inventing calendar-specific copies
- calendar reads must be tenant-bound and date-bounded
- no meeting scheduling, room booking, leave management or external calendar sync in the first slice
- timezone behavior should be explicit from the start so later events/reminders do not inherit ambiguous date handling

A sensible first backend shape, if the existing task APIs cannot support an efficient bounded projection, is:

```text
CalendarQueryService
        ↓
CalendarDeadlineSource
        ↓
TaskCalendarDeadlineSource
        ↓
tenant/date-bounded authorized task query
```

Keep the first implementation focused on deadlines. Subtasks/dependencies and richer scheduling semantics come later.

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
- calendar/deadline results must be authorized before exposure
- Explain Access and enforcement share the evaluator
- delegated authority remains a current permission/scope/validity subset of its direct source
- `authorization.manage` and `authorization.delegate` remain non-delegable
- provider/webhook lifecycle remains verified and auditable
- applied Flyway migrations are append-only
- provider secrets and sensitive identifiers remain server-side
- unbounded collections are paginated/bounded
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

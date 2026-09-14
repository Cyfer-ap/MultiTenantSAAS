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

Portable common Flyway migrations extend through **V46**.

Stripe is the validated deployed Test Mode billing path. Razorpay integration/catalog provisioning remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

## Current direction

Continue **Product Experience & Work Management Enrichment** before returning to the deferred operations/DR milestone.

The first personal-productivity sequence is complete: Search, Command Palette, Favorites/Recent, My Work and Saved Views are all delivered. The next implementation slice is a **capability-aware Dashboard Refresh**.

Recommended sequence:

1. capability-aware dashboard refresh + onboarding/empty-state polish
2. calendar/deadline view
3. subtasks, dependencies and labels
4. recurring work + project/task templates
5. bulk actions + import/export
6. custom fields/forms + workflows/approvals + knowledge/documents
7. user-facing analytics + selected differentiated experiments

## Architecture rule from this point forward

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

For backend features, prefer explicit domain packages rather than expanding the global `service/controller/entity/repository/dto` buckets.

For frontend features, preserve feature locality under `features/<domain>/...`.

Existing large services should not receive more dependencies casually. If new work would do that, extract an orchestrator, narrow query/service contract, or application/domain event as appropriate.

Important boundary rules should gain architecture/static regression tests when practical.

## Product-enrichment foundations to reuse

The current product layer already exposes reusable bounded contracts:

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
```

The frontend keeps Search, Command Palette, Personal Workspace, My Work and Saved Views in separate feature domains. Preserve that ownership model.

Create Task remains deliberately absent from the global palette. Effective task-management authority can arise from project-lead membership in addition to scoped authorization, so a future global Create Task action needs a project-aware capability/picker contract rather than shell-side permission guessing.

## Next feature guidance — Capability-aware Dashboard Refresh

Treat the dashboard as a **composition surface**, not a new source of truth.

The first dashboard slice should provide a useful operational home using existing authorized data:

- My Work attention summary: open, overdue, due soon, blocked and in progress
- a bounded preview of the highest-priority attention items
- Favorites
- Recently Viewed
- capability-aware quick actions that invoke owning feature flows
- a bounded deadline/activity snapshot where an existing domain can supply it safely
- useful empty states and onboarding hints when the workspace has little data

Architecture constraints:

- do not create a dashboard service that injects project, task, authorization, billing, user and notification repositories/services directly
- reuse existing frontend query contracts where the required information already exists
- where a combined backend read is justified, introduce narrow summary-provider contracts owned by the relevant domains
- dashboard visibility must never become an alternative authorization system
- do not duplicate My Work attention classification, Saved Views validation, or Personal Workspace resolution
- keep cards/widgets capability-aware rather than hard-coded only to role names
- keep every collection bounded

The first slice should favor a coherent personal dashboard over manager/admin analytics. Rich workload analytics belong to a later reporting phase.

## Invariants to preserve

- backend authorization remains authoritative
- tenant isolation precedes resource access
- search/discovery and saved/recent resolution constrain access before returning entity data
- stored favorites, recents or saved-view definitions never grant authorization
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

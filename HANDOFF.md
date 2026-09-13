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

Portable common Flyway migrations extend through V44.

Stripe is the validated deployed Test Mode billing path. Razorpay integration/catalog provisioning remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

## Current direction

Build **Product Experience & Work Management Enrichment** before returning to the deferred operations/DR milestone.

Global Search and Command Palette are complete. The next implementation slice is **Favorites + Recently Viewed**.

Recommended sequence:

1. favorites + recently viewed
2. My Work
3. saved views + dashboard refresh
4. calendar/deadline view + richer task relationships
5. templates/recurring work/bulk/import/export
6. custom fields/forms/workflows/knowledge
7. analytics + selected differentiated experiments

## Architecture rule from this point forward

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

For backend features, prefer explicit domain packages rather than expanding the global `service/controller/entity/repository/dto` buckets.

For frontend features, preserve feature locality under `features/<domain>/...`.

Existing large services should not receive more dependencies casually. If new work would do that, extract an orchestrator, narrow query/service contract, or application/domain event as appropriate.

Important boundary rules should gain architecture/static regression tests when practical.

## Discovery + command foundation to reuse

PR #129 establishes the backend discovery layer:

```text
backend search coordinator
        ↓
GlobalSearchContributor contracts
        ↓
project / task / user search adapters
        ↓
bounded permission-aware candidate queries
        ↓
authoritative scoped revalidation where required
```

PR #130 adds a separate frontend orchestration domain:

```text
features/command-palette
        ↓
features/search query contract
        ↓
permission-filtered workspace navigation
        ↓
existing project/invitation domain dialogs for mutations
```

Current search API:

```text
GET /api/tenants/{tenantId}/search?q=<query>&limit=<bounded-limit>
```

The palette must not become a second authorization/search/business-logic layer. New commands should call owning feature contracts/components or narrow command adapters.

Create Task is deliberately absent from the global palette for now. Task-management authority can arise from project-lead membership in addition to scoped authorization, so a future global Create Task action needs a project-aware capability/picker contract rather than shell-side permission guessing.

## Next feature guidance — Favorites + Recently Viewed

Treat personal productivity state as an explicit domain, not component-local storage scattered across pages.

Design goals:

- tenant-bound favorites and recent entities
- permission-aware resolution: saving an identifier never grants continued access
- bounded/reasonable recency history
- stable entity references for initially supported project/task destinations
- reusable UI/query contract for later My Work, command palette, mobile and dashboard consumers
- graceful handling when an entity is deleted, archived, moved out of scope or access is revoked

Do not mix this with semantic search, recommendations or analytics yet.

## Invariants to preserve

- backend authorization remains authoritative
- tenant isolation precedes resource access
- search/discovery and saved/recent resolution must constrain access before returning entity data
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

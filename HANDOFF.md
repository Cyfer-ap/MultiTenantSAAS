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

Portable common Flyway migrations extend through V44.

Stripe is the validated deployed Test Mode billing path. Razorpay integration/catalog provisioning remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

## Current direction

Build **Product Experience & Work Management Enrichment** before returning to the deferred operations/DR milestone.

Global Search is the first completed enrichment slice. The next implementation slice is the **Command Palette**.

Recommended sequence:

1. Command Palette on the Global Search/discovery foundation
2. favorites + recently viewed
3. My Work
4. saved views + dashboard refresh
5. Kanban/calendar + richer task relationships
6. templates/recurring work/bulk/import/export
7. custom fields/forms/workflows/knowledge
8. analytics + selected differentiated experiments

## Architecture rule from this point forward

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

For backend features, prefer explicit domain packages rather than expanding the global `service/controller/entity/repository/dto` buckets.

For frontend features, preserve feature locality under `features/<domain>/...`.

Existing large services should not receive more dependencies casually. If new work would do that, extract an orchestrator, narrow query/service contract, or application/domain event as appropriate.

Important boundary rules should gain architecture/static regression tests when practical.

## Global Search foundation to reuse

PR #129 establishes:

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

Frontend structure:

```text
features/search/
    api/
    components/
    hooks/
    types/
```

Current API:

```text
GET /api/tenants/{tenantId}/search?q=<query>&limit=<bounded-limit>
```

The Command Palette should reuse this foundation for discovery and add a separate narrow action registry for commands such as create/open/navigation actions. Do not duplicate search ranking or authorization logic in the palette.

## Invariants to preserve

- backend authorization remains authoritative
- tenant isolation precedes resource access
- search/discovery must constrain access before returning results
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

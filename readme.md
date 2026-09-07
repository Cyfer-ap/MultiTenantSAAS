# Multi-Tenant SaaS Platform

A full-stack multi-tenant SaaS platform focused on tenant isolation, permission-oriented authorization, project collaboration, subscription enforcement, external billing, PostgreSQL correctness and production-oriented engineering.

> **Current documentation snapshot**
>
> Repository: `Cyfer-ap/MultiTenantSAAS`
> Branch: `main`
> Application state reviewed through: PR #106 (`486f592`)
> Snapshot date: 2026-09-07
> Current phase: **Billing/catalog lifecycle complete at application level; outbound webhooks next**

## Platform capabilities

### Tenant plane

- secure tenant onboarding, JWT/browser sessions, invitations, users and organization hierarchy
- scoped permission-oriented authorization
- projects, tasks, comments, mentions, replies, pins and activity history
- S3/R2-compatible attachments
- durable in-app/email notifications and recipient preferences
- subscription visibility, lifecycle restrictions, recovery actions and quotas
- professional paid-plan selection and hosted provider checkout
- provider-backed cancellation, safe period-end retirement and subscription reconciliation
- immutable subscription purchase/history snapshots
- tenant API keys restricted to `/api/external/**`
- metered and plan-limited external API requests

### System plane

- separate system-admin identity/control plane
- tenant, user, subscription-plan and subscription administration
- managed Stripe/Razorpay provider catalog provisioning
- billing subscription/event/history visibility
- read-only provider reconciliation
- durable usage summaries and plan-level API limits
- tenant/platform audit logs

System administrators are not tenant users with an elevated tenant role.

## Technology stack

**Backend:** Java 21, Spring Boot 4.0.8, Spring Security/JWT, Spring Data JPA/Hibernate, Flyway, PostgreSQL 17, Testcontainers, AWS SDK v2 and Actuator/Micrometer.

**Frontend:** React 19.2, TypeScript 6, Vite 8, Material UI 9, React Router 7, TanStack React Query, Axios, React Hook Form/Zod, Vitest and Testing Library.

## Enforcement pipeline

```text
authentication
    ↓
tenant isolation
    ↓
authorization / scoped permission evaluation
    ↓
subscription lifecycle access
    ↓
resource and API-usage quotas
    ↓
domain invariants
```

Billing checkout is explicitly permitted as a subscription-recovery action for a read-only workspace, while normal tenant authorization still applies.

Tenant API keys are stored as hashes, revealed only at creation and accepted only under `/api/external/**`. They cannot impersonate browser users.

# Billing & Payments

## Milestone status

**Billing, cancellation hardening and managed provider catalogs are complete at application level through PR #106.**

Implemented:

- provider-neutral Stripe/Razorpay boundary
- professional plan → provider → hosted-checkout UX
- signed provider webhook verification
- durable/idempotent provider-event persistence
- webhook-driven subscription lifecycle synchronization
- verified provider-aware cancellation and provider-history recovery
- cross-provider overwrite protection
- period-end cancellation semantics
- idempotent repair when provider state is terminal but local state is stale
- `ACTIVE`, `INACTIVE` and terminal `RETIRED` catalog lifecycle
- safe plan retirement that blocks new sales immediately while preserving existing entitlement through the paid period
- durable TEST/LIVE provider catalog mappings
- automatic Stripe Product + recurring Price provisioning
- immutable Stripe Price replacement for price/currency/interval edits
- Stripe Product/Price archival for new sales during retirement
- automatic Razorpay Plan provisioning and replacement on provider-visible edits
- retry-safe Razorpay Plan adoption through deterministic provider metadata
- immutable purchased-plan snapshots and durable tenant subscription history
- tenant/system-admin billing-history UX
- billing operations/admin visibility and read-only reconciliation
- usage metering, tenant API keys and API request quotas

Live-provider production readiness is intentionally separate from the completed application milestone.

## Provider catalog lifecycle

Application plans live in the platform database; provider billing objects remain separate and are linked through durable provider mappings.

For enabled managed providers:

```text
System-admin plan create/update
        ↓
provider-neutral catalog coordinator
        ↓
Stripe Product/Price and/or Razorpay Plan
        ↓
subscription_plan_provider_mappings
        ↓
new hosted checkout
```

Provider catalog environment is separated as TEST/LIVE. Existing legacy environment mappings remain supported as compatibility fallback/import paths.

### Stripe

New paid application plans can provision a Stripe Product and recurring Price automatically. Provider-visible price/currency/billing-interval changes create a replacement Price; the previous Price remains retained for historical/existing subscription references and is inactive for new purchase. Retiring a plan removes the active Product/Price from new sale while preserving historical provider references.

### Razorpay

New paid application plans can provision Razorpay Plans automatically. Razorpay does not expose the same mutable/deactivation lifecycle as Stripe, so provider-visible edits create a replacement Plan and archive the old **local mapping**. Existing subscriptions can continue resolving the historical Plan ID while new checkout uses the active replacement mapping.

## Subscription history and retirement

Purchased terms are snapshotted independently of the mutable catalog plan. Historical plan name, description, interval, price/currency and resource limits therefore remain accurate after later plan edits or retirement.

`RETIRED` means:

- unavailable for new checkout immediately
- not editable/reactivatable through normal catalog lifecycle
- valid existing subscriptions remain entitled through their current paid period
- provider subscriptions are scheduled to end at period/cycle boundary
- local subscription state remains webhook-authoritative
- durable retirement operations can retry incomplete provider cleanup

`INACTIVE` remains the administrative hard-disable state.

## Stripe Test Mode

**Stripe is the validated deployed Test Mode provider.**

Confirmed:

- hosted subscription Checkout completes with Stripe Test Mode cards
- signed subscription lifecycle webhooks update local state
- cancellation reaches Stripe
- provider/local reconciliation works
- webhook endpoint listens for `customer.subscription.created`, `customer.subscription.updated` and `customer.subscription.deleted`

The prior stale cancellation incident was caused by `customer.subscription.deleted` being absent from the configured webhook endpoint. That configuration was corrected and the application now includes defensive terminal-state reconciliation.

This is Test Mode validation, not a live-production claim.

## Razorpay Test Mode

**Razorpay application integration and managed catalog provisioning are implemented, but recurring Test Mode authorization remains provider-sandbox blocked.**

The application can create/manage Razorpay Plans, create test subscriptions and reach hosted checkout, but attempted sandbox cards fail inside Razorpay before recurring authorization completes. This external provider behavior does not keep the application billing milestone open.

Current decision:

- Stripe remains the validated Test Mode payment path
- Razorpay remains implemented but provider-sandbox blocked for recurring-card E2E validation
- live-provider credentials/catalog readiness remain deferred until a separate provider-specific review
- never commit keys, API secrets, webhook secrets or provider plan/price IDs

Webhook targets:

```text
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/stripe
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/razorpay
```

Opening these URLs in a browser sends GET and is not a webhook test.

## Database checkpoint

```text
multitenant-saas/src/main/resources/db/migration    historical H2 V1-V17
multitenant-saas/src/main/resources/db/postgresql  PostgreSQL V17 baseline
multitenant-saas/src/main/resources/db/common      portable V18+
```

Shared migrations currently extend through **V36**:

- V28 billing foundation
- V29 provider subscription linkage
- V30 billing usage events
- V31 tenant API keys
- V32 API-key last-used metadata
- V33 subscription-plan usage limits
- V34 provider catalog mappings and immutable purchased-plan snapshots
- V35 durable plan-retirement operations
- V36 immutable tenant subscription history

Never rewrite an applied Flyway migration.

## Verification

Required GitHub Actions gates include Backend, PostgreSQL/Flyway, Frontend, Repository Hygiene, Security/Trivy, Container CI and Qodana.

The #100–#106 catalog/history sequence was developed under those repository gates. Mock/provider-contract coverage validates application behavior; it does not prove Razorpay sandbox availability or live-provider readiness.

## Deployment

- Frontend: `https://multitenantsaas-frontend.onrender.com`
- Backend: `https://multitenantsaas-akxn.onrender.com`
- Production profile: `SPRING_PROFILES_ACTIVE=postgres,production`

All secrets remain environment configured.

## Documentation and Wiki

Source-of-truth order:

1. current code and tests
2. current Flyway migrations
3. focused current guides
4. historical planning/progress notes

Start with:

- `CHECKPOINT.md`
- `HANDOFF.md`
- `guides/subscription_billing.md`
- `guides/DEFERRED_PLATFORM_WORK.md`
- `wiki/Home.md`
- `wiki/Roadmap.md`

The repository `wiki/` directory is canonical Wiki source. `.github/workflows/wiki-sync.yml` validates Wiki source on relevant pull requests and automatically publishes merged `main` changes to the live GitHub Wiki using `scripts/publish-wiki.ps1`.

## Next platform milestone

Billing/catalog lifecycle is closed at the application level. The recommended next major product feature is:

1. **tenant-configurable outbound webhooks**
2. enterprise SSO
3. authorization delegation and explain-access
4. backup/restore drills, monitoring, alerts and operational runbooks
5. broader load/failure-recovery and production R2 validation

Provider-specific live billing readiness can proceed independently when required.

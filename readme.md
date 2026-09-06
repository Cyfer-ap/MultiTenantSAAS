# Multi-Tenant SaaS Platform

A full-stack multi-tenant SaaS platform focused on tenant isolation, permission-oriented authorization, project collaboration, subscription enforcement, external billing, PostgreSQL correctness and production-oriented engineering.

> **Current documentation snapshot**
>
> Repository: `Cyfer-ap/MultiTenantSAAS`
> Branch: `main`
> Application state reviewed through: PR #98 (`87319f8`)
> Snapshot date: 2026-09-06
> Current phase: **Billing & Payments complete at application level; next product milestone selection**

## Platform capabilities

### Tenant plane

- secure tenant onboarding, JWT/browser sessions, invitations, users and organization hierarchy
- scoped permission-oriented authorization
- projects, tasks, comments, mentions, replies, pins and activity history
- S3/R2-compatible attachments
- durable in-app/email notifications and recipient preferences
- subscription visibility, lifecycle restrictions, recovery actions and quotas
- professional paid-plan selection and hosted provider checkout
- provider-backed cancellation and subscription reconciliation
- tenant API keys restricted to `/api/external/**`
- metered and plan-limited external API requests

### System plane

- separate system-admin identity/control plane
- tenant, user, subscription-plan and subscription administration
- billing subscription/event visibility
- read-only provider reconciliation
- durable usage summaries and plan-level API limits
- tenant/platform audit logs

System administrators are not tenant users with an elevated tenant role.

## Technology stack

**Backend:** Java 21, Spring Boot 4.0.7, Spring Security/JWT, Spring Data JPA/Hibernate, Flyway, PostgreSQL 17, Testcontainers, AWS SDK v2 and Actuator/Micrometer.

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

**Billing & Payments is complete at application level.**

Implemented through PR #98:

- provider-neutral Stripe/Razorpay boundary
- professional plan → provider → hosted-checkout UX
- signed provider webhook verification
- durable/idempotent provider-event persistence
- webhook-driven subscription lifecycle synchronization
- provider-backed cancellation
- provider ownership and durable-history linkage recovery
- cross-provider overwrite protection
- idempotent repair when provider state is already terminal but local state is stale
- billing operations/admin visibility
- read-only provider reconciliation
- usage metering
- tenant API keys and API request quotas
- duplicate active-subscription protection
- read-only workspace checkout recovery
- HTTP/security regression coverage for webhook boundaries

Live-provider production readiness is intentionally separate from the completed application milestone.

## Provider-plan boundary

Application plans live in the platform database. Stripe Products/Prices and Razorpay Plans are separate provider objects.

Creating a new plan through system administration does **not** currently create a Stripe Product/Price or Razorpay Plan automatically. Provider checkout requires explicit server-side plan-code → provider-ID mapping.

Automatic provider catalog provisioning can be added later as a separate feature with idempotency, partial-failure recovery, provider-specific lifecycle handling and Test/Live separation.

## Stripe Test Mode

**Stripe is the validated deployed Test Mode provider.**

Confirmed:

- hosted subscription Checkout completes with Stripe Test Mode cards
- signed subscription lifecycle webhooks update local state
- cancellation reaches Stripe
- provider/local reconciliation works
- the Stripe webhook endpoint listens for:
  - `customer.subscription.created`
  - `customer.subscription.updated`
  - `customer.subscription.deleted`

During final cancellation validation, Stripe was cancelling subscriptions successfully while local application state remained `ACTIVE`. The root cause was deployment configuration: `customer.subscription.deleted` had not been enabled on the Stripe webhook endpoint. The endpoint was corrected, and PR #98 added defensive idempotent repair for subscriptions already terminal at Stripe.

This is Test Mode validation, not a live-production claim.

## Razorpay Test Mode

**Razorpay integration is implemented, but recurring Test Mode authorization remains provider-sandbox blocked.**

The application can create Razorpay test subscriptions and reach hosted checkout, but attempted sandbox cards fail inside Razorpay before recurring authorization completes. Razorpay remains available in code; this external provider behavior does not keep the application billing milestone open.

Current decision:

- Stripe remains the validated Test Mode payment path
- Razorpay remains implemented but provider-sandbox blocked
- live provider credentials/plans remain deferred until a separate provider-specific readiness review
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

Shared migrations currently extend through **V33**:

- V28 billing foundation
- V29 provider subscription linkage
- V30 billing usage events
- V31 tenant API keys
- V32 API-key last-used metadata
- V33 subscription-plan usage limits

Never rewrite an applied Flyway migration.

## Verification

Required GitHub Actions gates include:

- Backend
- PostgreSQL/Flyway
- Frontend
- Repository Hygiene
- Security/Trivy
- Container CI
- Qodana

The billing hardening sequence through PR #98 was protected by these gates and supplemented with deployed Stripe Test Mode checkout/webhook/cancellation validation. Razorpay sandbox availability is outside what CI can prove.

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
- `guides/README.md`
- `guides/subscription_billing.md`
- `guides/DEFERRED_PLATFORM_WORK.md`
- `wiki/Home.md`
- `wiki/Roadmap.md`

The repository `wiki/` directory is canonical Wiki source. `.github/workflows/wiki-sync.yml` validates Wiki source on relevant pull requests and automatically publishes merged `main` changes to the live GitHub Wiki using `scripts/publish-wiki.ps1`. Manual publishing is only a fallback; see `wiki/Wiki-Maintenance.md`.

## Next platform milestone

Billing is closed at the application level. The recommended next major product feature is:

1. **tenant-configurable outbound webhooks**
2. enterprise SSO
3. authorization delegation and explain-access
4. backup/restore drills, monitoring, alerts and operational runbooks
5. broader load/failure-recovery and production R2 validation

Provider-specific live billing readiness can proceed independently when required.

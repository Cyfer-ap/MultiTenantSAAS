# Multi-Tenant SaaS Platform

A full-stack multi-tenant SaaS platform focused on tenant isolation, permission-oriented authorization, project collaboration, subscription enforcement, external billing, PostgreSQL correctness, and production-oriented engineering.

> **Current documentation snapshot**
>
> Repository: `Cyfer-ap/MultiTenantSAAS`
> Branch: `main`
> Base reviewed state: post-PR #98 (`87319f8`)
> Snapshot date: 2026-09-06
> Current phase: **billing complete at application level; next product milestone selection**

## Platform capabilities

### Tenant plane

- secure tenant onboarding, JWT/browser sessions, invitations, users, organization hierarchy, projects and tasks
- permission-oriented scoped authorization
- task comments, mentions, replies, pins, activity, R2/S3-compatible attachments and notifications
- subscription visibility, lifecycle restrictions, recovery actions and resource quotas
- tenant API-key lifecycle plus API-key authentication in the isolated `/api/external/**` namespace
- metered and plan-limited external API requests
- paid-plan discovery, professional plan selection, hosted provider checkout and provider-backed cancellation

### System plane

- separate system-admin identity and control plane
- tenant, user, subscription-plan and subscription administration
- billing subscription/event visibility
- read-only provider reconciliation
- durable usage ingestion and summaries
- plan-level API request limits
- tenant and platform audit logs

System administrators are not tenant users with an elevated tenant role.

## Technology stack

**Backend:** Java 21, Spring Boot 4.0.7, Spring Security/JWT, Spring Data JPA/Hibernate, Flyway, PostgreSQL 17, Testcontainers, AWS SDK v2, Actuator/Micrometer.

**Frontend:** React 19.2, TypeScript 6, Vite 8, Material UI 9, React Router 7, TanStack React Query, Axios, React Hook Form/Zod, Vitest and Testing Library.

## Security and enforcement

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

Billing checkout is an explicitly permitted subscription-recovery action for a read-only workspace, while the existing `tenant.update` authorization remains required.

API keys are tenant-bound, stored only as hashes, revealed once at creation and accepted only on `/api/external/**`. They cannot impersonate browser users.

## Subscription, billing and metering

### Application-level milestone status

**Billing & Payments is complete at application level.**

Implemented through PR #98:

- provider-neutral billing boundary with configuration-gated Stripe and Razorpay adapters
- professional tenant subscription UX: plan selection → provider selection → hosted checkout
- signed Stripe and Razorpay webhook ingestion
- durable provider-event persistence and replay/duplicate protection
- webhook-driven subscription lifecycle synchronization
- provider-backed cancellation with provider ownership recovery
- cross-provider linkage protection and durable-history recovery
- idempotent repair when the provider is already terminal but local state is stale
- system-admin billing operations views and read-only reconciliation
- append-only usage metering
- tenant API keys and external API authentication
- per-plan `API_REQUESTS` quotas with atomic consumption and `429 Retry-After`
- duplicate active-subscription prevention
- read-only workspace recovery through checkout
- HTTP/security regression coverage for webhook boundaries

Live-provider/production readiness is intentionally separate from the application milestone.

### Provider-plan boundary

Application plans live in the platform database; Stripe Products/Prices and Razorpay Plans are separate provider objects.

Current checkout uses server-side mappings from application plan code to provider billing IDs. Creating a new plan through the system-admin API/UI does **not** automatically create a Stripe Product/Price or Razorpay Plan. A new paid plan must be mapped to each provider before that provider can offer checkout for it.

Automatic provider provisioning is a possible future enhancement, not part of the closed billing milestone.

### Stripe Test Mode status

**Stripe is working in the deployed Test Mode environment.**

Confirmed:

- hosted subscription Checkout completes with Stripe Test Mode cards
- signed subscription webhooks synchronize local state
- cancellation reaches Stripe
- the Test Mode webhook endpoint listens for `customer.subscription.created`, `customer.subscription.updated` and `customer.subscription.deleted`
- provider/local reconciliation works

During final cancellation validation, Stripe was cancelling subscriptions successfully while the application remained `ACTIVE`. The root cause was configuration: `customer.subscription.deleted` had not been enabled on the Stripe webhook endpoint. The endpoint was corrected and PR #98 added idempotent local-state repair for already-cancelled provider subscriptions.

This validates the current Test Mode application path; it is not a live-mode or production-readiness claim.

### Razorpay Test Mode status

**Razorpay integration is implemented, but real recurring authorization remains provider-sandbox blocked.**

The deployed application can create Razorpay test subscriptions and open hosted checkout, but attempted Test Mode cards fail inside Razorpay before recurring authorization completes. International acceptance is unavailable in the current path, and domestic recurring-compatible test attempts have also failed.

Treat this as an external provider sandbox limitation rather than unfinished application architecture. Razorpay remains available in code and Test Mode; live readiness is deferred.

Current deployment decision:

- Stripe remains the validated Test Mode payment path
- Razorpay remains available but provider-sandbox blocked
- live keys/plans remain deferred for both providers until a separate readiness review
- never commit keys, secrets, webhook secrets or provider plan/price IDs

Webhook targets:

```text
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/stripe
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/razorpay
```

Opening either URL in a browser sends GET and is not a webhook test.

## Database checkpoint

Migration layout:

```text
multitenant-saas/src/main/resources/db/migration    historical H2 V1-V17
multitenant-saas/src/main/resources/db/postgresql  PostgreSQL V17 baseline
multitenant-saas/src/main/resources/db/common      portable V18+
```

Current shared migrations extend through **V33**:

- V28 billing foundation
- V29 provider subscription linkage
- V30 billing usage events
- V31 tenant API keys
- V32 API-key last-used metadata
- V33 subscription-plan usage limits

Never rewrite an applied migration.

## Verification

GitHub Actions is authoritative for this environment because local Docker is unavailable. Required gates include backend tests/verification, PostgreSQL and Flyway, frontend lint/tests/build, repository hygiene, security scanning, container validation and Qodana.

The billing hardening sequence through PR #98 was validated through these gates. Provider contract tests are supplemented by deployed Stripe Test Mode checkout, webhook and cancellation validation. Razorpay provider availability remains outside what CI can prove.

## Deployment

- Frontend: `https://multitenantsaas-frontend.onrender.com`
- Backend: `https://multitenantsaas-akxn.onrender.com`
- Production profile: `SPRING_PROFILES_ACTIVE=postgres,production`

Use environment configuration for all secrets. See `wiki/Production-Deployment.md`.

## Documentation

Source-of-truth order:

1. current code and tests
2. current Flyway migrations
3. focused guides under `guides/`
4. historical planning/progress notes

Start with:

- `CHECKPOINT.md`
- `HANDOFF.md`
- `guides/README.md`
- `guides/subscription_billing.md`
- `guides/DEFERRED_PLATFORM_WORK.md`
- `wiki/Home.md`
- `wiki/Roadmap.md`

Version-controlled Wiki source is under `wiki/`. Publish it with `scripts/publish-wiki.ps1`.

## Next steps

Billing is closed as an application milestone. The next major product work should move away from payment-provider debugging.

Recommended sequence:

1. tenant-configurable outbound webhooks
2. enterprise SSO
3. authorization delegation and explain-access
4. backup/restore drills, monitoring, alerts and operational runbooks
5. broader load/failure-recovery and production R2 verification

Provider-specific live-mode readiness can proceed independently when required.

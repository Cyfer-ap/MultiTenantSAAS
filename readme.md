# Multi-Tenant SaaS Platform

A full-stack multi-tenant SaaS platform focused on tenant isolation, permission-oriented authorization, project collaboration, subscription enforcement, external billing foundations, PostgreSQL correctness, and production-oriented engineering.

> **Current documentation snapshot**
>
> Repository: `Cyfer-ap/MultiTenantSAAS`
> Branch: `main`
> Base reviewed state: post-PR #84 (`b97a3ef`)
> Snapshot date: 2026-08-26
> Current phase: **billing test-mode validation and operational hardening**

## Platform capabilities

### Tenant plane

- secure tenant onboarding, JWT/browser sessions, invitations, users, organization hierarchy, projects and tasks
- permission-oriented scoped authorization
- task comments, mentions, replies, pins, activity, R2/S3-compatible attachments and notifications
- subscription visibility, lifecycle restrictions, recovery actions and resource quotas
- tenant API-key lifecycle plus API-key authentication in the isolated `/api/external/**` namespace
- metered and plan-limited external API requests
- paid-plan discovery, hosted provider checkout and provider-backed cancellation

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

The billing checkout POST is an explicitly permitted subscription-recovery action for a read-only workspace, while the existing `tenant.update` authorization remains required.

API keys are tenant-bound, stored only as hashes, revealed once at creation and accepted only on `/api/external/**`. They cannot impersonate browser users.

## Subscription, billing and metering

Implemented through PR #84:

- provider-neutral billing boundary with configuration-gated Stripe and Razorpay adapters
- hosted subscription checkout API
- signed Stripe and Razorpay webhook ingestion
- durable provider-event persistence and replay/duplicate protection
- webhook-driven subscription lifecycle synchronization
- provider-backed cancellation
- system-admin billing operations views and read-only reconciliation
- append-only usage metering
- tenant API keys and external API authentication
- per-plan `API_REQUESTS` quotas with atomic consumption and `429 Retry-After`
- safe checkout configuration discovery and tenant subscription-page checkout
- duplicate active-subscription prevention
- read-only workspace recovery through checkout

Internal application plans and Razorpay plans are separate. App plan codes such as `PRO` and `ENTERPRISE` map server-side to `RAZORPAY_PLAN_PRO` and `RAZORPAY_PLAN_ENTERPRISE`. Provider plan IDs and secrets are never returned to the browser.

### Stripe parallel option

Stripe is supported as an additional hosted subscription provider and does not replace Razorpay. If both are enabled, the tenant sees separate Stripe and Razorpay actions.

Stripe deployment requires a server-side Test Mode secret key, recurring Price IDs for `PRO` and `ENTERPRISE`, success/cancel URLs pointing to `/subscription`, and a separate webhook signing secret. The deployed webhook target is:

```text
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/stripe
```

Razorpay remains configured and visible independently. A successful Stripe E2E test must still be completed after the Test Mode values are added to Render.

### Razorpay test-mode status

**Razorpay is not working end to end yet.** The deployed application can create a test subscription and open Razorpay's hosted checkout, but every attempted test card currently fails inside Razorpay before an authorization can complete. International test cards are rejected because international acceptance is unavailable, and recurring-compatible domestic test cards have also returned provider errors.

This means the application-side integration is implemented and CI-tested with mock provider contracts, but real Razorpay sandbox activation, webhook delivery and resulting local subscription activation have not been successfully validated. Treat this as a provider-level end-to-end blocker under investigation, not as production-ready billing.

Current deployment decision:

- Razorpay stays in **Test Mode**
- live keys and live plans are deferred
- do not use real cards in Test Mode
- never commit keys, key secrets, webhook secrets or plan IDs
- rotate any credential that has been exposed in a screenshot or log

Correct deployed webhook target:

```text
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/razorpay
```

It belongs to the backend. Opening that URL in a browser sends a GET request and is not a webhook test.

Relevant variables:

```dotenv
RAZORPAY_BILLING_ENABLED=true
RAZORPAY_KEY_ID=...
RAZORPAY_KEY_SECRET=...
RAZORPAY_PLAN_PRO=plan_...
RAZORPAY_PLAN_ENTERPRISE=plan_...
RAZORPAY_SUBSCRIPTION_TOTAL_COUNT=120
RAZORPAY_WEBHOOK_ENABLED=true
RAZORPAY_WEBHOOK_SECRET=...
```

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

Provider contract tests do not replace a real Razorpay Test Mode checkout and webhook smoke test.

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

1. isolate the Razorpay sandbox failure by creating/testing a subscription directly in Razorpay and recording the failed payment fields: `code`, `description`, `source`, `step` and `reason`
2. raise the result with Razorpay Support if the dashboard-created subscription also fails
3. optionally add a feature-flagged, system-admin-only billing lifecycle simulator for application testing
4. repeat the deployed Razorpay activation/webhook/cancellation flow
5. move to KYC/live keys/live plans only after Test Mode succeeds
6. continue operational recovery, tenant outbound webhooks, broader failure testing and enterprise SSO

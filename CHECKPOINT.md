# MultiTenantSAAS — Checkpoint

Repository: `Cyfer-ap/MultiTenantSAAS`  
Branch: `main`  
Date: 2026-08-26  
Base reviewed state: post-PR #84 (`b97a3ef`)

## Current phase

**Billing test-mode validation and operational hardening**

The provider-neutral billing, Razorpay adapter, verified webhooks, lifecycle synchronization, cancellation, reconciliation, usage metering, API keys, API request quotas and tenant checkout UI are implemented. The active blocker is real Razorpay Test Mode authorization: hosted checkout opens, but no tested payment method has completed successfully.

## Delivered since the previous checkpoint

- PRs #67-#73: billing foundation, Stripe/Razorpay adapters, checkout API, signed webhooks and lifecycle synchronization
- PR #74: provider-backed tenant cancellation
- PRs #75-#76: billing operations visibility and read-only reconciliation
- PR #77: durable usage metering
- PRs #78-#79: tenant API-key lifecycle, authentication and metering
- PR #80: plan-level external API quotas
- PRs #81-#82: safe checkout discovery and tenant hosted-checkout UI
- PR #83: Razorpay provider startup injection fix for Render
- PR #84: allow checkout as a recovery action in read-only workspaces

## Razorpay status

**Not working end to end yet.**

Confirmed:

- server configuration loads and the deployed backend starts
- Pro and Enterprise app plans appear in the tenant UI
- checkout creation returns a Razorpay hosted subscription URL
- the browser reaches Razorpay Test Mode checkout

Blocked:

- all tested cards fail inside Razorpay before recurring authorization completes
- international cards are rejected because international acceptance is unavailable
- domestic recurring-compatible test cards have also failed
- therefore activation webhooks and application-side subscription activation have not been validated against the real sandbox

Do not describe Razorpay as production-ready. Keep it in Test Mode and defer live plans/keys.

## Configuration boundary

Application plans are stored in the platform database. Razorpay plans are separately created provider objects and are mapped server-side:

```text
PRO        -> RAZORPAY_PLAN_PRO
ENTERPRISE -> RAZORPAY_PLAN_ENTERPRISE
```

Required deployed variables are `RAZORPAY_BILLING_ENABLED`, key ID/secret, one or more mapped plan IDs, `RAZORPAY_WEBHOOK_ENABLED` and `RAZORPAY_WEBHOOK_SECRET`.

Never document actual values. Rotate exposed credentials.

Webhook endpoint:

```text
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/razorpay
```

## Database checkpoint

Portable migrations extend through **V33**:

```text
V28 billing foundation
V29 provider subscription linkage
V30 durable billing usage events
V31 tenant API keys
V32 API-key last-used metadata
V33 subscription-plan usage limits
```

Never rewrite an applied migration.

## Verification checkpoint

PR #84 passed the repository's GitHub Actions gates, including backend, PostgreSQL/Flyway, frontend, security, containers and Qodana. CI remains authoritative while local Docker is unavailable.

Mocked provider-contract tests prove application behavior, not Razorpay sandbox availability.

## Immediate next steps

1. reproduce through a subscription created directly in Razorpay Dashboard
2. capture failed-payment diagnostics (`code`, `description`, `source`, `step`, `reason`)
3. contact Razorpay Support if the direct Dashboard flow also fails
4. optionally implement a feature-flagged system-admin billing simulator
5. rerun deployed activation, webhook, reconciliation and cancellation
6. only then prepare live keys and live plans

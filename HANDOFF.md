# MultiTenantSAAS — Development Handoff

Use this document to resume without relying on chat history.

## Repository checkpoint

```text
Repository: Cyfer-ap/MultiTenantSAAS
Branch: main
Reviewed state: post-PR #90 (e9257b3)
Date: 2026-08-27
Current phase: billing provider validation and operational hardening
```

## Read first

1. `readme.md`
2. `CHECKPOINT.md`
3. `guides/HANDOFF.md`
4. `guides/subscription_billing.md`
5. `wiki/Subscriptions-and-Quotas.md`
6. `wiki/Production-Deployment.md`
7. `wiki/Testing-and-CI.md`

## Current result

Billing is substantially implemented through PR #90: provider-neutral checkout, Razorpay/Stripe adapters, signed durable webhooks, lifecycle synchronization, cancellation, operations views, reconciliation, metering, API keys, API-request quotas and tenant checkout UI.

Stripe works end to end in the deployed Test Mode environment, including hosted checkout and signed webhook-driven local subscription synchronization. PR #90 fixed the Stripe-enabled Render startup regression.

Razorpay remains externally blocked. Hosted Test Mode checkout opens, but all attempted cards fail within Razorpay before authorization. No real sandbox activation/webhook cycle has succeeded, so live mode is deferred.

## Boundaries to preserve

- authentication, tenant isolation, authorization, subscription lifecycle and quotas remain separate
- checkout may bypass lifecycle read-only enforcement only as an explicit recovery action; `tenant.update` is still required
- local subscription state remains webhook-authoritative
- provider and plan mapping comes only from server-side configuration/verified metadata
- API keys authenticate only `/api/external/**`, are tenant-bound and cannot impersonate users
- never expose or commit provider keys, secrets, webhook secrets or plan IDs

## Next action

Preserve the working Stripe Test Mode path. Test a Dashboard-created Razorpay subscription to distinguish account/sandbox capability from application integration. Capture the provider's structured failure fields and escalate to Razorpay Support if that direct flow also fails.

A feature-flagged, system-admin-only lifecycle simulator is a valid parallel follow-up for testing application transitions without claiming provider validation.

## Verification

GitHub Actions is authoritative while local Docker is unavailable. Before merge require backend, PostgreSQL/Flyway, frontend, security, container and Qodana checks to pass.

# MultiTenantSAAS Wiki

MultiTenantSAAS is a full-stack multi-tenant SaaS platform with tenant isolation, scoped authorization, project collaboration, subscription enforcement, external billing foundations, usage metering, API keys and PostgreSQL-oriented production engineering.

Version-controlled Wiki source lives under `wiki/`. See [[Wiki-Maintenance]].

Current snapshot: **post-PR #84 (`b97a3ef`), 2026-08-26**.

## Current platform state

Implemented capabilities include:

- separate tenant and system-admin control planes
- secure JWT/browser sessions, invitations and account recovery
- shared-schema tenant isolation and scoped permission authorization
- organization hierarchy, projects, tasks and collaboration
- R2/S3-compatible attachments
- durable notifications, email delivery and preferences
- internal subscription lifecycle, read-only enforcement and quotas
- provider-neutral billing with Stripe and Razorpay adapters
- hosted checkout, signed durable webhooks and webhook-driven lifecycle synchronization
- provider cancellation, billing operations visibility and read-only reconciliation
- durable billing usage events
- tenant API-key lifecycle and authentication under `/api/external/**`
- plan-level API request quotas
- tenant paid-plan discovery and hosted-checkout UI
- PostgreSQL 17, Flyway, Testcontainers, CI, security and container checks

## Razorpay status

**Razorpay Test Mode is not working end to end yet.**

The deployed application starts, lists Pro and Enterprise, creates a Razorpay subscription and opens hosted checkout. All attempted test cards then fail inside Razorpay before recurring authorization. International cards are rejected because international acceptance is unavailable, while domestic recurring-compatible test cards have also failed.

A real activation webhook and local subscription activation have therefore not been validated. Keep live keys and live plans deferred and do not call billing production-ready.

## Architecture boundary

```text
authentication
  -> tenant isolation
  -> authorization
  -> subscription access
  -> resource/API quotas
  -> domain invariants
```

Provider plan IDs and secrets stay server-side. Application plans are mapped to Razorpay plans by environment configuration.

## Database checkpoint

Portable common migrations extend through **V33**. V28-V33 add billing persistence, provider linkage, usage metering, tenant API keys and plan usage limits.

## Start here

- [[Architecture]]
- [[Security-and-Authentication]]
- [[Authorization]]
- [[Subscriptions-and-Quotas]]
- [[Production-Deployment]]
- [[Testing-and-CI]]
- [[Roadmap]]
- [[Developer-Handoff]]

## Current next step

Isolate the Razorpay sandbox failure through a Dashboard-created subscription, capture the structured payment failure fields and contact Razorpay Support if the direct provider flow also fails. An internal, feature-flagged billing simulator may be added for application lifecycle tests, but it does not replace real provider validation.

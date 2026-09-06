# MultiTenantSAAS Wiki

MultiTenantSAAS is a full-stack multi-tenant SaaS platform with tenant isolation, scoped authorization, project collaboration, subscription enforcement, external billing, usage metering, API keys and PostgreSQL-oriented production engineering.

Version-controlled Wiki source lives under `wiki/`. See [[Wiki-Maintenance]].

Current snapshot: **post-PR #98 (`87319f8`), 2026-09-06**.

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
- professional plan/provider checkout UX
- signed durable webhooks and webhook-driven lifecycle synchronization
- provider-aware cancellation, linkage recovery and stale-terminal-state repair
- billing operations visibility and read-only reconciliation
- durable billing usage events
- tenant API-key lifecycle and authentication under `/api/external/**`
- plan-level API request quotas
- PostgreSQL 17, Flyway, Testcontainers, CI, security and container checks

## Billing milestone

**Billing & Payments is complete at application level.**

Stripe works in deployed Test Mode for hosted subscription Checkout, signed lifecycle webhooks and provider-side cancellation. During final validation, Stripe cancellations succeeded but the application remained locally `ACTIVE` because `customer.subscription.deleted` was missing from the webhook endpoint's enabled events. The endpoint now includes created/updated/deleted subscription events, and PR #98 adds idempotent state repair for already-terminal provider subscriptions.

Razorpay remains available but recurring Test Mode authorization is provider-sandbox blocked: hosted checkout opens while attempted test cards fail before recurring authorization. This external limitation does not keep the application billing milestone open.

## Provider plan boundary

Application plans and provider billing objects are separate. Creating a system-admin application plan does not automatically provision a Stripe Product/Price or Razorpay Plan. Provider checkout currently requires explicit server-side plan-code mappings.

## Architecture boundary

```text
authentication
  -> tenant isolation
  -> authorization
  -> subscription access
  -> resource/API quotas
  -> domain invariants
```

Provider plan/price IDs and secrets stay server-side.

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

Begin the next major product milestone rather than extending billing for provider-sandbox behavior. Recommended: **tenant-configurable outbound webhooks**. Enterprise SSO, authorization delegation/explain-access and deeper operational recovery/load testing follow on the roadmap.

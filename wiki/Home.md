# MultiTenantSAAS Wiki

MultiTenantSAAS is a full-stack multi-tenant SaaS platform with tenant isolation, scoped authorization, project collaboration, subscription enforcement, external billing, usage metering, API keys and PostgreSQL-oriented production engineering.

Version-controlled Wiki source lives under `wiki/`. See [[Wiki-Maintenance]].

Current snapshot: **post-PR #106 (`486f592`), 2026-09-07**.

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
- verified provider-aware cancellation, linkage recovery and stale-terminal-state repair
- durable TEST/LIVE provider catalog mappings
- automatic Stripe Product/Price provisioning and immutable Price replacement
- automatic Razorpay Plan provisioning and replacement mappings
- terminal safe plan retirement with paid-period continuation
- immutable purchased-plan snapshots and subscription history
- tenant/system-admin billing-history UX
- billing operations visibility and read-only reconciliation
- durable billing usage events
- tenant API-key lifecycle and plan-level API request quotas
- PostgreSQL 17, Flyway, Testcontainers, CI, security and container checks

## Billing/catalog milestone

**Billing, cancellation hardening and managed provider catalogs are complete at application level through PR #106.**

Stripe is the validated deployed Test Mode payment path. Razorpay application integration and managed catalog provisioning are implemented, while recurring Test Mode authorization remains provider-sandbox blocked.

## Provider catalog lifecycle

Application plans and provider objects remain separate but are linked through durable environment-specific mappings.

- Stripe: managed Product/Price creation, immutable replacement pricing and retirement from new sales.
- Razorpay: managed Plan creation and replacement mappings; historical provider Plan references remain preserved.
- `RETIRED`: no new checkout; existing valid subscriptions retain entitlement through the current paid period and are scheduled not to renew.
- `INACTIVE`: administrative hard-disable.

Provider identifiers and secrets stay server-side.

## Database checkpoint

Portable common migrations extend through **V36**. V34 adds provider catalog mappings and purchased-plan snapshots, V35 durable retirement operations, and V36 immutable tenant subscription history.

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

Begin **tenant-configurable outbound webhooks**. Recommended order: endpoint/signing foundation → durable delivery engine → product event integration/replay → tenant-admin delivery UX. Enterprise SSO follows afterward.

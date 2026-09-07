# MultiTenantSAAS Wiki

MultiTenantSAAS is a full-stack multi-tenant SaaS platform with tenant isolation, scoped authorization, project collaboration, subscription enforcement, external billing, durable outbound integrations, usage metering, API keys and PostgreSQL-oriented production engineering.

Version-controlled Wiki source lives under `wiki/`. See [[Wiki-Maintenance]].

Current snapshot: **post-PR #112 (`8324ae9`), 2026-09-07**.

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
- signed durable provider webhooks and webhook-driven lifecycle synchronization
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
- tenant-configurable outbound webhook endpoints/event subscriptions
- generated/rotatable encrypted signing secrets
- durable HMAC-signed webhook delivery with retry/backoff/leases/timeouts
- transactional project/task/comment/member/subscription event publication
- immutable delivery-attempt history and manual replay
- tenant Integrations UX for endpoint administration and delivery observability
- PostgreSQL 17, Flyway, Testcontainers, CI, security and container checks

## Completed milestones

### Billing/catalog

**Billing, cancellation hardening and managed provider catalogs are complete at application level through PR #106.**

Stripe is the validated deployed Test Mode payment path. Razorpay application integration and managed catalog provisioning are implemented, while recurring Test Mode authorization remains provider-sandbox blocked.

### Tenant outbound webhooks

**Tenant-configurable outbound webhooks are complete at application level through PR #112.**

The platform now supports tenant endpoint lifecycle, event subscriptions, secure signing-secret rotation/storage, SSRF-safe HTTPS validation, durable signed delivery, retries/backoff/leasing, delivery history/attempts, guarded replay and a permission-gated Integrations workspace.

Initial events cover project, task, comment/reply, membership and selected subscription lifecycle mutations.

## Database checkpoint

Portable common migrations extend through **V39**. V37 adds outbound webhook endpoints/event subscriptions, V38 durable events/deliveries and V39 immutable delivery attempts.

## Start here

- [[Architecture]]
- [[Security-and-Authentication]]
- [[Authorization]]
- [[Subscriptions-and-Quotas]]
- [[Notifications]]
- [[Production-Deployment]]
- [[Testing-and-CI]]
- [[Roadmap]]
- [[Developer-Handoff]]

## Current next step

Begin **enterprise SSO / identity federation**. Prefer a provider-neutral federation boundary with OIDC first, safe account linking/domain discovery, optional/enforced tenant policy and recovery controls. Add SAML through the same boundary where enterprise requirements justify it.

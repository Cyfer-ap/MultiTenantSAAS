# MultiTenantSAAS Wiki

MultiTenantSAAS is a full-stack multi-tenant SaaS platform with tenant isolation, scoped authorization, collaboration, subscription enforcement, external billing, durable outbound integrations, enterprise OIDC SSO, bounded authorization delegation, Explain Access, usage metering, API keys and PostgreSQL-oriented production engineering.

Version-controlled Wiki source lives under `wiki/`. See [[Wiki-Maintenance]].

Current snapshot: **post-PR #125 (`0694403`), 2026-09-13**.

## Current platform state

Implemented capabilities include:

- separate tenant and system-admin control planes
- JWT/browser sessions, invitations, password recovery and workspace discovery
- shared-schema tenant isolation and scoped permission authorization
- bounded one-level authorization delegation with runtime source revalidation
- Explain Access with direct/delegated matched-grant provenance
- organization hierarchy, projects/tasks/collaboration and R2 attachments
- durable notifications/email delivery/preferences
- subscription lifecycle, quotas, API keys and usage metering
- provider-neutral billing with Stripe and Razorpay
- tenant-configurable HMAC-signed outbound webhooks with durable delivery/history/replay
- enterprise OIDC SSO with tenant IdP configuration, secure runtime, optional/required policy, break-glass recovery, browser completion, admin UX and audit events
- PostgreSQL 17, Flyway, Testcontainers, CI, security and container checks

## Completed milestones

### Billing/catalog

Complete through PR #106. Stripe is the validated deployed Test Mode payment path. Razorpay application/catalog integration is implemented while recurring Test Mode authorization remains provider-sandbox blocked.

### Tenant outbound webhooks

Complete through PR #112.

### Enterprise OIDC SSO

Complete through PR #119. See [[Enterprise-SSO]].

### Authorization delegation and Explain Access

Complete through PR #125. See [[Authorization]].

## Database checkpoint

Portable common migrations extend through **V44**. V44 adds authorization delegation provenance and `authorization.delegate`.

## Start here

- [[Architecture]]
- [[Security-and-Authentication]]
- [[Enterprise-SSO]]
- [[Authorization]]
- [[Subscriptions-and-Quotas]]
- [[Notifications]]
- [[Production-Deployment]]
- [[Testing-and-CI]]
- [[Roadmap]]
- [[Developer-Handoff]]

## Current next step

Start **Production Operations & Disaster Recovery**: PostgreSQL backup/restore drills, monitoring, alerts and operational runbooks, followed by broader failure-recovery/load and production R2 verification.

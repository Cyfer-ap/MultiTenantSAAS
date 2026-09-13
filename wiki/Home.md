# MultiTenantSAAS Wiki

MultiTenantSAAS is a full-stack multi-tenant SaaS platform with tenant isolation, scoped authorization, collaboration, subscription enforcement, external billing, durable outbound integrations, enterprise OIDC SSO, bounded authorization delegation, Explain Access, usage metering, API keys and PostgreSQL-oriented production engineering.

Version-controlled Wiki source lives under `wiki/`. See [[Wiki-Maintenance]].

Current snapshot: **post-PR #126 (`5013260`), 2026-09-13**.

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

Complete through PR #125, with documentation closure in PR #126. See [[Authorization]].

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

Repository-side `guides/Wild_Thoughts.md` is the living product-idea vault and now includes a current implementation audit, core product gaps and differentiated experiments.

## Current next step

Start **Product Experience & Work Management Enrichment**: discoverability/search/commands, My Work and saved views, dashboard improvements, deeper task/project views and relationships, then templates/custom fields/workflows/knowledge/analytics.

Production Operations & Disaster Recovery remains important but is deliberately deferred until after the current user-facing enrichment phase.

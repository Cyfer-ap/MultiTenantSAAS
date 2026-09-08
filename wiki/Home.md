# MultiTenantSAAS Wiki

MultiTenantSAAS is a full-stack multi-tenant SaaS platform with tenant isolation, scoped authorization, collaboration, subscription enforcement, external billing, durable outbound integrations, enterprise OIDC SSO, usage metering, API keys and PostgreSQL-oriented production engineering.

Version-controlled Wiki source lives under `wiki/`. See [[Wiki-Maintenance]].

Current snapshot: **post-PR #119 (`c36de3f`), 2026-09-08**.

## Current platform state

Implemented capabilities include:

- separate tenant and system-admin control planes
- JWT/browser sessions, invitations, password recovery and workspace discovery
- shared-schema tenant isolation and scoped permission authorization
- organization hierarchy, projects/tasks/collaboration and R2 attachments
- durable notifications/email delivery/preferences
- subscription lifecycle, quotas, API keys and usage metering
- provider-neutral billing with Stripe and Razorpay
- durable provider catalog mappings, signed webhooks, cancellation/reconciliation and subscription history
- tenant-configurable HMAC-signed outbound webhooks with durable delivery/history/replay
- enterprise OIDC SSO with tenant IdP configuration, provider verification, state/nonce/PKCE, safe existing-user linking, optional/required policy, break-glass recovery, browser completion, Authentication admin UX and federation audit events
- PostgreSQL 17, Flyway, Testcontainers, CI, security and container checks

## Completed milestones

### Billing/catalog

Complete at application level through PR #106. Stripe is the validated deployed Test Mode payment path. Razorpay application/catalog integration is implemented while recurring Test Mode authorization remains provider-sandbox blocked.

### Tenant outbound webhooks

Complete at application level through PR #112.

### Enterprise OIDC SSO

Complete at application level through PR #119. See [[Enterprise-SSO]]. SAML is intentionally optional/demand-driven.

## Database checkpoint

Portable common migrations extend through **V43**. V40–V43 implement the enterprise SSO persistence layers.

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

Start **authorization delegation and explain-access**. Preserve backend-authoritative authorization and tenant isolation while adding controlled delegation and auditable access-decision explanations.

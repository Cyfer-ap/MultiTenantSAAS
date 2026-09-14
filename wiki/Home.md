# MultiTenantSAAS Wiki

MultiTenantSAAS is a full-stack multi-tenant SaaS platform with tenant isolation, scoped authorization, collaboration, subscription enforcement, external billing, durable outbound integrations, enterprise OIDC SSO, bounded authorization delegation, Explain Access, usage metering, API keys and PostgreSQL-oriented production engineering.

Version-controlled Wiki source lives under `wiki/`. See [[Wiki-Maintenance]].

The Wiki intentionally avoids duplicating the repository's volatile current checkpoint. Repository-side `CHECKPOINT.md` owns current status and `HANDOFF.md` owns the current resume point.

## Platform state

Implemented foundations include:

- separate tenant and system-admin control planes
- JWT/browser sessions, invitations, password recovery and workspace discovery
- shared-schema tenant isolation and scoped permission authorization
- bounded one-level authorization delegation with runtime source revalidation
- Explain Access with direct/delegated matched-grant provenance
- organization hierarchy, projects/tasks/collaboration and R2/S3-compatible attachments
- permission-aware Global Search and capability-aware Command Palette
- server-backed Favorites + Recently Viewed with contextual favorite controls
- My Work personal attention queue and server-backed Saved Views
- durable notifications/email delivery/preferences
- subscription lifecycle, quotas, API keys and usage metering
- provider-neutral billing with Stripe and Razorpay
- tenant-configurable HMAC-signed outbound webhooks with durable delivery/history/replay
- enterprise OIDC SSO with tenant IdP configuration, secure runtime, optional/required policy, break-glass recovery, browser completion, admin UX and audit events
- PostgreSQL 17, Flyway, Testcontainers, CI, security and container checks

## Architecture direction

MultiTenantSAAS remains an intentional modular monolith. Future features must follow a stronger domain-boundary rule:

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

See [[Architecture]] for the current architecture and known debt.

## Database checkpoint

Portable common migrations currently extend through **V46**. Applied Flyway migrations remain append-only.

Recent product migrations are V45 for personal-workspace favorites/recent items and V46 for saved views.

## Start here

- [[Architecture]]
- [[Security-and-Authentication]]
- [[Enterprise-SSO]]
- [[Authorization]]
- [[Tenancy-and-Data-Model]]
- [[Subscriptions-and-Quotas]]
- [[Projects-and-Tasks]]
- [[Notifications]]
- [[Testing-and-CI]]
- [[Roadmap]]
- [[Developer-Handoff]]

Repository-side `guides/Wild_Thoughts.md` is the living idea vault; `guides/ENGINEERING_STANDARDS.md` is the technical-health and engineering-rules guide.

## Current product direction

The active product phase is **Product Experience & Work Management Enrichment**. Global Search, Command Palette, Favorites/Recently Viewed, My Work and Saved Views are established foundations. The next slice is a **capability-aware dashboard refresh**, followed by calendar/deadline views, richer work-management relationships, recurring work/templates, bulk productivity and tenant adaptability.

Production Operations & Disaster Recovery remains important but deliberately deferred behind the current user-facing enrichment phase. See [[Roadmap]].

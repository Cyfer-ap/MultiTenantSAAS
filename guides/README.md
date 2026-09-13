# MultiTenantSAAS Guides

Current snapshot: post-PR #126 (`5013260`), 2026-09-13.

These guides supplement code, tests and Flyway migrations. When historical notes conflict with current implementation, prefer current code/tests, migrations and focused guides.

## Start here

- `../CHECKPOINT.md` — repository checkpoint
- `../HANDOFF.md` — resume instructions
- `Wild_Thoughts.md` — audited product-idea vault, core gaps and differentiated experiments
- `authorization_model.md` — scoped authorization, delegation and Explain Access
- `enterprise-sso-foundation.md` — complete OIDC SSO architecture, deployment and test procedure
- `current_architecture.md` — platform architecture
- `subscription_billing.md` — billing lifecycle/provider model
- `outbound-webhook-events.md` — outbound integration event contract
- `outbound-webhook-delivery-history.md` — delivery/attempt/replay behavior
- `outbound-webhook-admin-ux.md` — tenant Integrations UX

## Current milestone status

- Billing/catalog: complete through PR #106
- Tenant outbound webhooks: complete through PR #112
- Enterprise OIDC SSO: complete through PR #119
- Authorization delegation and Explain Access: complete through PR #125
- Authorization documentation closure: PR #126

Portable common Flyway migrations extend through **V44**.

## Provider status

Stripe is working and validated in deployed Test Mode. Razorpay application/catalog integration remains implemented, while recurring Test Mode authorization is provider-sandbox blocked. Live-provider readiness is separate.

## Next product milestone

Recommended next major milestone: **Product Experience & Work Management Enrichment**.

Start with discoverability and daily-work UX (search, command palette, recents/favorites, My Work, saved views/dashboard), then deepen work management (Kanban/calendar, task relationships, recurring work/templates) before moving into custom fields/forms, workflows/approvals, knowledge/documents and analytics.

Production Operations & Disaster Recovery remains an important deferred milestone, followed later by load/failure-recovery and production R2 verification. SAML/SCIM and richer notifications remain optional/demand-driven.

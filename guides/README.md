# MultiTenantSAAS Guides

Current snapshot: post-PR #125 (`0694403`), 2026-09-13.

These guides supplement code, tests and Flyway migrations. When historical notes conflict with current implementation, prefer current code/tests, migrations and focused guides.

## Start here

- `../CHECKPOINT.md` — repository checkpoint
- `../HANDOFF.md` — resume instructions
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

Portable common Flyway migrations extend through **V44**.

## Provider status

Stripe is working and validated in deployed Test Mode. Razorpay application/catalog integration remains implemented, while recurring Test Mode authorization is provider-sandbox blocked. Live-provider readiness is separate.

## Next product milestone

Recommended next major milestone: **Production Operations & Disaster Recovery** — backup/restore drills, monitoring, alerts and operational runbooks.

Follow-up work includes broader load/failure-recovery testing and production R2 verification. SAML/SCIM and richer notifications remain optional/demand-driven.

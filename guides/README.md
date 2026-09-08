# MultiTenantSAAS Guides

Current snapshot: post-PR #119 (`c36de3f`), 2026-09-08.

These guides supplement code, tests and Flyway migrations. When historical notes conflict with current implementation, prefer current code/tests, migrations and focused guides.

## Start here

- `../CHECKPOINT.md` — repository checkpoint
- `../HANDOFF.md` — resume instructions
- `enterprise-sso-foundation.md` — complete OIDC SSO architecture, deployment and test procedure
- `current_architecture.md` — platform architecture
- `authorization_model.md` — current authorization model
- `subscription_billing.md` — billing lifecycle/provider model
- `outbound-webhook-events.md` — outbound integration event contract
- `outbound-webhook-delivery-history.md` — delivery/attempt/replay behavior
- `outbound-webhook-admin-ux.md` — tenant Integrations UX

## Current milestone status

- Billing/catalog: complete at application level through PR #106
- Tenant outbound webhooks: complete at application level through PR #112
- Enterprise OIDC SSO: complete at application level through PR #119

Portable common Flyway migrations extend through **V43**.

## Provider status

Stripe is working and validated in deployed Test Mode. Razorpay application/catalog integration remains implemented, while recurring Test Mode authorization is provider-sandbox blocked. Live-provider readiness is separate.

## Next product milestone

Recommended next major feature: **authorization delegation and explain-access**.

SAML remains optional through the provider-neutral federation boundary and should be added only when a real enterprise requirement justifies it. Follow-up roadmap work includes backup/restore drills, monitoring/alerts/runbooks, broader load/failure-recovery testing and production R2 verification.

# Documentation Package Index

Current snapshot: post-PR #125 (`0694403`), 2026-09-13.

## Primary status

- `readme.md` — platform overview and completed milestones
- `CHECKPOINT.md` — authoritative concise checkpoint
- `HANDOFF.md` — resume instructions
- `MANIFEST.json` — machine-readable status
- `GUIDE_UPDATE_MANIFEST.md` — documentation refresh inventory

## Focused implementation guides

- `guides/authorization_model.md` — scoped authorization, Explain Access and bounded delegation
- `guides/enterprise-sso-foundation.md` — OIDC SSO architecture, administration, deployment and test procedure
- `guides/subscription_billing.md` — billing/provider lifecycle
- `guides/outbound-webhook-events.md` — outbound event contract
- `guides/outbound-webhook-delivery-history.md` — durable deliveries/attempts/replay
- `guides/outbound-webhook-admin-ux.md` — tenant Integrations UX
- `guides/DEFERRED_PLATFORM_WORK.md` — remaining/demand-driven work

## Wiki source

Canonical Wiki source is under `wiki/` and is published from merged `main`.

Key pages include `Home`, `Authorization`, `Enterprise-SSO`, `Security-and-Authentication`, `Production-Deployment`, `Roadmap` and `Developer-Handoff`.

## Current milestone status

- billing/catalog complete through PR #106
- tenant outbound webhooks complete through PR #112
- enterprise OIDC SSO complete through PR #119
- authorization delegation and Explain Access complete through PR #125
- portable Flyway migrations through V44

## Provider truth

Stripe is working/validated in deployed Test Mode. Razorpay application/catalog integration remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

## Next product milestone

**Production Operations & Disaster Recovery** — PostgreSQL backup/restore drills, monitoring, alerts and operational runbooks.

Historical planning/recovery files remain implementation history and are not current specifications where they conflict with code/tests, migrations, checkpoint documents or focused guides.

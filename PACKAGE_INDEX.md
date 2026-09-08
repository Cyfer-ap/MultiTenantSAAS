# Documentation Package Index

Current snapshot: post-PR #119 (`c36de3f`), 2026-09-08.

## Primary status

- `readme.md` — platform overview and current completed milestones
- `CHECKPOINT.md` — authoritative concise checkpoint
- `HANDOFF.md` — resume instructions
- `MANIFEST.json` — machine-readable status
- `GUIDE_UPDATE_MANIFEST.md` — documentation refresh inventory

## Focused implementation guides

- `guides/enterprise-sso-foundation.md` — OIDC SSO architecture, administration, deployment and test procedure
- `guides/authorization_model.md` — current permission/scoped authorization model
- `guides/subscription_billing.md` — billing/provider lifecycle
- `guides/outbound-webhook-events.md` — outbound event contract
- `guides/outbound-webhook-delivery-history.md` — durable deliveries/attempts/replay
- `guides/outbound-webhook-admin-ux.md` — tenant Integrations UX
- `guides/DEFERRED_PLATFORM_WORK.md` — remaining/demand-driven work

## Wiki source

Canonical Wiki source is under `wiki/` and is published from merged `main`.

Key pages:

- `wiki/Home.md`
- `wiki/Enterprise-SSO.md`
- `wiki/Security-and-Authentication.md`
- `wiki/Production-Deployment.md`
- `wiki/Roadmap.md`
- `wiki/Developer-Handoff.md`

## Current milestone status

- billing/catalog complete through PR #106
- tenant outbound webhooks complete through PR #112
- enterprise OIDC SSO complete through PR #119
- portable Flyway migrations through V43

## Provider truth

Stripe is working/validated in deployed Test Mode. Razorpay application/catalog integration remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

## Next product milestone

**Authorization delegation and explain-access.**

Historical planning/recovery files remain implementation history and are not current specifications where they conflict with code/tests, migrations, checkpoint documents or focused guides.

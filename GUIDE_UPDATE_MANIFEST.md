# Guide Update Manifest

```text
Repository: Cyfer-ap/MultiTenantSAAS
Reviewed application state: post-PR #119 (c36de3f)
Snapshot date: 2026-09-08
Phase: enterprise OIDC SSO complete at application level
Next recommended product milestone: authorization delegation and explain-access
```

## Updated status documents

- `readme.md`
- `CHECKPOINT.md`
- `HANDOFF.md`
- `guides/README.md`
- `guides/CHECKPOINT.md`
- `guides/HANDOFF.md`
- `guides/progress.md`
- `guides/DEFERRED_PLATFORM_WORK.md`
- `PACKAGE_INDEX.md`
- `MANIFEST.json`

## SSO guide/deployment updates

- `guides/enterprise-sso-foundation.md` upgraded from the #116 foundation snapshot to full #119 milestone documentation
- `.env.example` now includes backend callback, frontend completion and OIDC transaction/handoff settings
- `.env.production.example` now includes explicit hosted SSO callback/completion variables
- `wiki/Enterprise-SSO.md` added as a focused live-Wiki source page
- `wiki/Security-and-Authentication.md` expanded with SSO policy/runtime security
- `wiki/Production-Deployment.md` expanded with IdP registration and environment setup

## Wiki status updates

- `wiki/Home.md`
- `wiki/Roadmap.md`
- `wiki/Developer-Handoff.md`
- `wiki/Security-and-Authentication.md`
- `wiki/Production-Deployment.md`
- `wiki/Enterprise-SSO.md`

## Milestone truth captured

- OIDC SSO is complete through PRs #114–#119
- portable Flyway migrations extend through V43
- Stripe is working/validated in deployed Test Mode
- Razorpay recurring Test Mode authorization remains provider-sandbox blocked while application/catalog integration remains implemented
- SAML remains optional/demand-driven
- next core milestone is authorization delegation/explain-access

Wiki source under `wiki/` is published from merged `main` by `.github/workflows/wiki-sync.yml` via `scripts/publish-wiki.ps1`.

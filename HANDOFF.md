# MultiTenantSAAS — Development Handoff

Use this document to resume without relying on chat history.

## Repository checkpoint

```text
Repository: Cyfer-ap/MultiTenantSAAS
Branch: main
Application state reviewed through: PR #125 (0694403)
Date: 2026-09-13
Current phase: authorization delegation and Explain Access complete at application level
Recommended next product milestone: Production Operations & Disaster Recovery
```

## Read first

1. `readme.md`
2. `CHECKPOINT.md`
3. `guides/authorization_model.md`
4. `guides/enterprise-sso-foundation.md`
5. `wiki/Authorization.md`
6. `wiki/Security-and-Authentication.md`
7. `wiki/Production-Deployment.md`
8. `wiki/Testing-and-CI.md`
9. `wiki/Roadmap.md`

## Current result

Completed application-level milestones now include billing/catalog lifecycle, tenant-configurable outbound webhooks, enterprise OIDC SSO, and authorization delegation/Explain Access.

Authorization completion through PR #125 provides:

- structured access decisions from the same evaluator used for enforcement
- tenant-admin Explain Access with stable grant/denial reasoning
- V44 durable delegation provenance and `authorization.delegate`
- bounded create/list/revoke delegation lifecycle
- one direct parent assignment for each delegated grant
- permission/scope/validity subset enforcement
- no re-delegation and no delegation of protected authorization permissions
- runtime parent-source revalidation so later source revocation/expiry/narrowing invalidates delegated access
- delegation-safe reference data for non-admin delegators
- direct-vs-delegated Explain Access provenance
- manager Authorization workspace plus delegate-only Delegations access

## Authorization boundaries to preserve

- backend evaluator remains authoritative
- delegation must never create authority the delegator does not currently hold directly
- do not trust a delegated role assignment without validating its delegation source
- `authorization.manage` and `authorization.delegate` remain non-delegable
- cross-tenant subjects/resources remain invalid
- Explain Access must not expose unrelated grants or tenant data
- frontend filtering is convenience only; API validation remains final

## Database checkpoint

Common portable migrations extend through **V44**. Never rewrite an applied Flyway migration.

- V40 identity-provider configuration
- V41 OIDC authorization transactions and tenant federated identities
- V42 tenant SSO policy
- V43 one-time OIDC browser session handoffs
- V44 authorization delegation provenance and delegation permission

## Provider boundary

- Stripe is working and validated in deployed Test Mode
- Razorpay integration and managed Plan provisioning remain implemented, but recurring Test Mode authorization is provider-sandbox blocked
- keep both providers; live readiness remains an independent operational review

## Next action

Start **Production Operations & Disaster Recovery**.

Recommended first slice:

1. define PostgreSQL backup retention/export and a safe isolated restore-drill procedure
2. add/verify health, readiness, metrics and alertable failure signals
3. document runbooks for deployment failure, DB recovery, SSO/provider outage, billing/webhook incidents and secret rotation
4. follow with load/failure-recovery testing and production R2 verification

SAML/SCIM and richer notification channels remain optional until requirements justify them.

## Verification

GitHub Actions remains authoritative where local Docker is unavailable. Before merge require Backend, PostgreSQL/Flyway, Frontend, Repository Hygiene, Security, Container CI and Qodana to pass. Wiki source changes should also satisfy Wiki Sync validation.

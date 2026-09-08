# Guides Checkpoint

Snapshot date: 2026-09-08
Reviewed state: post-PR #119 (`c36de3f`)

## Completed application milestones

- billing/catalog lifecycle through PR #106
- tenant-configurable outbound webhooks through PR #112
- enterprise OIDC SSO / identity federation through PR #119

## Enterprise SSO checkpoint

PRs #114–#119 deliver tenant-scoped OIDC configuration, encrypted client secrets, controlled provider verification, state/nonce/PKCE callback runtime, safe existing-user linking, verified workspace auth-mode discovery, `OPTIONAL`/`REQUIRED` policy, password-capable tenant-admin break-glass, browser SSO completion, tenant-admin Authentication UX and tenant audit visibility.

Portable migrations extend through **V43**:

- V40 identity-provider configuration
- V41 OIDC authorization transactions + federated identities
- V42 tenant SSO policy
- V43 one-time browser session handoffs

Never rewrite an applied migration.

## Provider status

- Stripe: working and validated in deployed Test Mode
- Razorpay: application/catalog integration implemented; recurring Test Mode authorization remains provider-sandbox blocked

## Next checkpoint

The next product checkpoint belongs to **authorization delegation and explain-access**. SAML is optional/demand-driven and does not keep the current OIDC milestone open.

Code/tests and current migrations remain authoritative over historical planning documents.

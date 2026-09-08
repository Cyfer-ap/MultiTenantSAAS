# Security and Authentication

## Tenant authentication

Tenant authentication supports password login plus tenant-scoped OIDC federation. Successful password and SSO flows converge on the existing platform browser/JWT session architecture.

Core controls include access-token validation, refresh-token rotation, hashed refresh-token persistence, logout/logout-all, password change/reset, lockout, session-version invalidation and live database-backed account/tenant validation.

## Workspace discovery and authentication modes

Verified email-first workspace discovery can expose:

```text
PASSWORD_ONLY
PASSWORD_OR_SSO
SSO_ONLY
SSO_REQUIRED
```

The backend decides the mode. Frontend rendering never substitutes for server enforcement.

## Enterprise OIDC SSO

See [[Enterprise-SSO]] for the full deployment/admin procedure.

Security controls include:

- tenant-scoped provider and identity bindings
- encrypted write-only provider client secret
- HTTPS/public-routable discovery/JWKS/token endpoints with DNS/SSRF revalidation
- disabled provider HTTP redirects
- high-entropy state/nonce with hashes at rest
- PKCE S256 with encrypted verifier
- single-use authorization transaction consumed before token exchange
- ID-token signature/algorithm, issuer, audience/azp, time-claim and nonce validation
- no federation auto-provisioning
- first link requires provider-verified email matching an existing active user in the same tenant
- short-lived opaque single-use browser session handoff after callback

## SSO policy and recovery

Tenant SSO policy is `OPTIONAL` or `REQUIRED`.

`REQUIRED` is allowed only while the provider is verified and at least one active tenant administrator retains a usable password-based break-glass path. Provider edits, secret rotation or disable invalidate verification and force safe fallback to `OPTIONAL` where necessary.

The tenant-admin break-glass path is explicit and audited.

## Provider lifecycle

Provider states are `DRAFT`, `VERIFIED` and `DISABLED`. Re-enabling a disabled provider returns it only to `DRAFT`; it must be verified again.

## System administrators

System administrators use their own authentication/control plane and are not tenant members with an elevated tenant role.

## Public endpoint hardening

Public authentication/recovery/onboarding paths use bounded process-local rate limiting. Horizontal production scale would require shared/distributed rate-limit state.

Password-reset responses avoid account enumeration and raw reset-token exposure is disabled in production.

## Audit and sensitive-data rules

Federation configuration/policy/lifecycle and trusted tenant-attributed OIDC success/failure events reuse the existing tenant audit ledger.

Do not store/log raw client secrets, authorization codes, state, nonce, PKCE verifiers, provider access/ID tokens or sensitive provider response payloads. Invalid/untrusted callback state is not attributed to a tenant audit record.

## Security principle

The backend is authoritative for authentication, tenant isolation, SSO policy and authorization. Frontend guards exist for usability only.

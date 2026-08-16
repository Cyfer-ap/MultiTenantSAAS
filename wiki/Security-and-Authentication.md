# Security and Authentication

## Tenant authentication

Tenant authentication uses JWT access tokens and refresh-token lifecycle management.

Important behaviors include:

- access-token validation
- refresh-token rotation
- hashed refresh-token persistence
- single-session logout
- logout-all
- password change
- password reset
- account lockout
- session-version invalidation
- live database-backed account/tenant validation

## Session version

Security-sensitive events can invalidate already-issued tenant access tokens by incrementing a persisted session version checked during JWT validation.

Typical invalidating events include:

- logout all devices
- password change
- password reset

## System administrators

System administrators use their own authentication/control plane.

System-admin identity must not be merged into tenant membership.

## Public endpoint hardening

Public authentication/recovery/onboarding paths use bounded process-local rate limiting. This is appropriate for the current single-instance staging model; horizontal production scale would require shared/distributed state.

Password-reset responses are designed to avoid account enumeration, and raw reset-token exposure is disabled in production.

## Observability

Security-relevant counters include:

```text
saas.security.login.attempts
saas.security.account.locks
saas.security.rate_limit.rejections
```

See [[Operations-and-Observability]].

## Security principle

The backend is authoritative. Frontend guards improve usability but never replace server-side authentication, tenant isolation or authorization.

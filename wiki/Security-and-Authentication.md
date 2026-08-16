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

## Session version

Security-sensitive events can invalidate already-issued tenant access tokens by incrementing a persisted session version checked during JWT validation.

Typical invalidating events include:

- logout all devices
- password change
- password reset

## System administrators

System administrators use their own authentication/control plane.

System-admin identity must not be merged into tenant membership.

## Production hardening

Production configuration hides internal exception details, suppresses sensitive health details, disables Flyway clean, and limits operational endpoint exposure.

## Security principle

Frontend guards improve UX, but the backend remains authoritative.

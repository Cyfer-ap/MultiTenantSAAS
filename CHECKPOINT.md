# MultiTenantSAAS — Checkpoint

Repository: `Cyfer-ap/MultiTenantSAAS`  
Branch: `main`  
Reviewed commit: `3808c0ddf95d075aed7114bf060518640c19d6c2`  
Date: 2026-08-16

## Current phase

**Step 40 — Transaction & Concurrency Hardening**

The production-profile foundation is also present on `main`.

## Confirmed current architecture

- Spring Boot backend
- React/Vite frontend
- tenant/system administration separation
- tenant-scoped authorization
- subscription plans/lifecycle/quotas
- central workspace read-only enforcement
- PostgreSQL 17 execution path
- H2 historical migration path
- Flyway PostgreSQL V17 baseline
- future common migrations from V18
- Testcontainers PostgreSQL verification
- Docker Compose support
- production profile and `.env.production.example`

## Step 40 completed slice

Slice 40.1 serializes tenant subscription state transitions:

- quota-sensitive writes use subscription-row pessimistic locking
- plan/lifecycle changes load subscription state under write lock
- subscription creation locks the tenant row before duplicate check/insert

## Remaining Step 40 work

1. invitation single-use acceptance/replacement races
2. failed-login counter races
3. session-version/password/logout-all lost updates
4. duplicate/integrity race normalization
5. PostgreSQL concurrency integration tests
6. lock-order/deadlock review

## Migration invariant

```text
db/migration   -> historical H2 V1-V17
db/postgresql  -> PostgreSQL V17 baseline
db/common      -> all future portable V18+
```

Never rewrite an applied migration.

## Production configuration checkpoint

`main` includes:

```text
.env.production.example
application-production.properties
```

Production should use:

```text
SPRING_PROFILES_ACTIVE=postgres,production
```

and preserve the hardening rules documented in the root README.

# MultiTenantSAAS — Development Handoff

Use this document to resume development in a new session without relying on chat history.

## Repository checkpoint

```text
Repository: Cyfer-ap/MultiTenantSAAS
Branch: main
Commit reviewed: 3808c0ddf95d075aed7114bf060518640c19d6c2
Current engineering phase: Step 40
```

## Read first

1. `readme.md`
2. `guides/README.md`
3. `guides/current_architecture.md`
4. `guides/authorization_model.md`
5. `guides/subscription_billing.md`
6. `guides/postgresql_and_migrations.md`
7. `guides/step40_transaction_concurrency.md`

## Architecture boundaries to preserve

Do not collapse:

```text
authentication
tenant isolation
authorization
subscription lifecycle
quota enforcement
```

System-admin identity must remain separate from tenant-user identity.

## Database/migration rule

H2 and PostgreSQL have different starting migration histories, but all new shared schema work starts at V18 in `db/common`.

Never edit V1-V17 merely to make a new implementation easier.

## Current concurrency work

Slice 40.1 established database locking for subscription invariants.

Continue with invitation races, failed-login counters, session-version/password/logout-all lost updates, duplicate/integrity normalization, and PostgreSQL concurrency tests.

Prefer database constraints/locks over JVM-local mutexes.

## Production-profile state

Latest main includes a `production` Spring profile and `.env.production.example`.

Do not remove:

- hidden error details
- `open-in-view=false`
- `ddl-auto=validate`
- Flyway clean disabled
- restricted Actuator exposure
- health-detail suppression
- production rate-limit configuration

## Definition of a safe next change

A change is not complete until:

```text
backend tests pass
frontend lint/tests/build pass when affected
PostgreSQL schema/integration path remains valid
git diff --check passes
tenant isolation is preserved
authorization/subscription contracts are not conflated
documentation is updated
```

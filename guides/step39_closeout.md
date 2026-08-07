# Step 39 closeout - PostgreSQL production readiness

## Goal

Establish a real PostgreSQL execution path without destroying the existing H2/Flyway development history, and verify schema parity with an integration test.

## Implemented foundation

- PostgreSQL 17 Docker Compose service
- PostgreSQL JDBC runtime dependency
- PostgreSQL Flyway database support
- Testcontainers JUnit/PostgreSQL dependencies
- PostgreSQL application profile
- PostgreSQL V17 current-schema baseline
- shared future migration directory `db/common`
- Hibernate schema validation on the PostgreSQL profile
- PostgreSQL schema integration test guarded by Docker availability

## Migration contract

Historical H2 migrations remain in:

```text
db/migration     V1-V17 history
```

PostgreSQL starts from:

```text
db/postgresql    V17 current-schema baseline
```

Future portable migrations start from:

```text
db/common        V18+
```

Both ordinary H2 tests and PostgreSQL execution must load `db/common`.

## Closeout defect 1 - stale subscription test

`SubscriptionQuotaEnforcementIntegrationTest` still expected `SERVICE_UNAVAILABLE` after the central subscription mutation interceptor had become authoritative for ordinary business writes.

Current behavior is intentionally:

```text
HTTP 409
restriction = WORKSPACE_READ_ONLY
resource = workspace
accessReason = CANCELLED   (for the cancelled scenario)
```

The test is updated to preserve resource-specific expectations for normal quota failures while asserting `workspace` for `WORKSPACE_READ_ONLY`.

## Closeout defect 2 - H2 test migration parity

`application-test.properties` loaded only `classpath:db/migration`.

That would become a hidden defect as soon as V18 is added to `db/common`, because PostgreSQL would see the migration while the ordinary H2 test profile would not.

The test profile now loads:

```properties
spring.flyway.locations=classpath:db/migration,classpath:db/common
```

## Verification

Backend:

```powershell
cd D:\Projects\multitenant-saas\multitenant-saas
.\mvnw.cmd test
```

Repository checks:

```powershell
cd D:\Projects\multitenant-saas
git diff --check
git status --short
```

Frontend, when verifying the complete checkpoint:

```powershell
cd D:\Projects\multitenant-saas\multitenant-saas-frontend
npm test
npm run lint
npm run build
```

## Next step

**Step 40 - Transaction & Concurrency Hardening**

Do not start payment-provider work until the core write paths have clear transactional boundaries and concurrency behavior.

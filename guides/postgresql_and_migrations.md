# PostgreSQL and migration strategy

Step 39 adds a PostgreSQL production-readiness foundation while preserving the existing H2 migration history used during development and testing.

## Local PostgreSQL

The repository contains:

`docker-compose.postgres.yml`

From the repository root:

```powershell
docker compose -f .\docker-compose.postgres.yml up -d
```

This starts the PostgreSQL service defined by the project Compose configuration.

## Runtime profiles

### Default/H2 path

The existing H2 development/test workflow continues to use the historical migration chain.

### PostgreSQL path

The PostgreSQL profile uses the PostgreSQL JDBC driver and the PostgreSQL Flyway database module. Hibernate validates the resulting schema rather than creating it.

The PostgreSQL baseline is:

`multitenant-saas/src/main/resources/db/postgresql/V17__current_schema_baseline.sql`

It represents the current schema at the end of the historical V1-V17 development chain.

## Migration directories

### Historical H2 chain

```text
multitenant-saas/src/main/resources/db/migration
```

Contains the already-established V1-V17 history.

### PostgreSQL baseline

```text
multitenant-saas/src/main/resources/db/postgresql
```

Starts PostgreSQL from the current V17 baseline.

### Future shared migrations

```text
multitenant-saas/src/main/resources/db/common
```

All new portable migrations begin with **V18** here.

## Required Flyway locations

Normal H2 runtime/tests must include:

```properties
classpath:db/migration,classpath:db/common
```

PostgreSQL must include:

```properties
classpath:db/postgresql,classpath:db/common
```

This is why `application-test.properties` must also include `db/common`: otherwise a future V18 migration could pass PostgreSQL startup while being absent from the standard H2-backed test suite.

## Migration rules

1. Never edit an already-applied V1-V17 migration.
2. Never copy V1-V17 into `db/common`.
3. New shared schema changes start at V18 in `db/common`.
4. Keep migrations portable between H2 and PostgreSQL where practical.
5. When a database-specific feature is unavoidable, document and test the divergence explicitly.

## PostgreSQL integration verification

The backend contains a PostgreSQL/Testcontainers integration test that starts PostgreSQL 17 when Docker is available and verifies that Spring/Flyway/Hibernate can build and validate the expected schema.

With Docker installed and running, this test should execute rather than being skipped by the Docker-availability guard.

## Verification commands

From `multitenant-saas`:

```powershell
.\mvnw.cmd test
```

From the repository root after changes:

```powershell
git diff --check
git status --short
```

## Step 40 hand-off

PostgreSQL support changes the concurrency and transactional assumptions compared with a simple in-memory development database. Step 40 should therefore review transactions, locking/races, uniqueness enforcement, retry/idempotency behavior, and quota-sensitive writes under concurrent requests.

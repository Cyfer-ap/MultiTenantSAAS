# PostgreSQL and migration strategy

## Historical H2 migration chain

```text
multitenant-saas/src/main/resources/db/migration
```

Contains historical V1-V17 migrations.

## PostgreSQL baseline

```text
multitenant-saas/src/main/resources/db/postgresql/V17__current_schema_baseline.sql
```

## Future portable migrations

```text
multitenant-saas/src/main/resources/db/common
```

All new shared migrations begin at V18.

## Required locations

```text
H2:
classpath:db/migration,classpath:db/common

PostgreSQL:
classpath:db/postgresql,classpath:db/common
```

## Rules

1. Never edit applied V1-V17 migrations.
2. Do not copy historical migrations to `db/common`.
3. Put future portable migrations in `db/common`.
4. Keep PostgreSQL/Testcontainers verification green.
5. Document and test unavoidable database-specific behavior.

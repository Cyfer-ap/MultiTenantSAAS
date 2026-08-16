# PostgreSQL and Flyway

## Production database path

PostgreSQL 17 is the production-readiness database target.

H2 remains part of the historical/local-test migration path, so the repository intentionally preserves two starting histories.

## Migration locations

Historical H2 chain:

```text
multitenant-saas/src/main/resources/db/migration
```

PostgreSQL current-schema baseline:

```text
multitenant-saas/src/main/resources/db/postgresql
V17__current_schema_baseline.sql
```

Future portable migrations:

```text
multitenant-saas/src/main/resources/db/common
```

## Runtime mapping

```text
H2
  -> classpath:db/migration
  -> classpath:db/common

PostgreSQL
  -> classpath:db/postgresql
  -> classpath:db/common
```

## Migration invariant

All new portable schema changes begin at **V18+** under `db/common`.

Never rewrite an already-applied migration.

## Production schema ownership

Production uses:

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.clean-disabled=true
```

Flyway owns schema evolution; Hibernate verifies the result.

## PostgreSQL verification

The Testcontainers path should verify:

- PostgreSQL starts successfully
- Flyway builds the expected schema
- Hibernate validation succeeds
- PostgreSQL-specific query semantics behave correctly
- lock/concurrency behavior is exercised where relevant

## Query portability note

Avoid nullable static-JPQL guards whose parameter typing depends on H2 behavior, such as broad `:param IS NULL OR ...` patterns. PostgreSQL-sensitive optional filters should use query construction that produces correctly typed predicates.

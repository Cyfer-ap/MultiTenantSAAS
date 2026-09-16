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

Portable shared migrations:

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

The shared portable chain begins at V18 and currently extends through **V52**.

Recent work-management/product migrations:

- V45 personal workspace favorites/recent items
- V46 saved views
- V47 task parent/dependency/project-label relationships
- V48 recurring task definitions/occurrences + project task templates
- V49 tenant project templates + bounded starter-task snapshots
- V50 workflow definitions/nodes/edges/executions
- V51 project whiteboards/nodes/connectors
- V52 project form definitions/fields/submissions

Never rewrite an already-applied migration. **V52 is immutable; new persistence starts at V53+.**

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
- Flyway builds the expected schema/version
- Hibernate validation succeeds
- PostgreSQL-specific query semantics behave correctly
- lock/concurrency behavior is exercised where relevant

## Query portability note

Avoid nullable static-JPQL guards whose parameter typing depends on H2 behavior, such as broad `:param IS NULL OR ...` patterns. PostgreSQL-sensitive optional filters should use query construction that produces correctly typed predicates.

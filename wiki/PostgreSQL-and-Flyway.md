# PostgreSQL and Flyway

## Database paths

Historical H2:

```text
db/migration
```

PostgreSQL baseline:

```text
db/postgresql/V17__current_schema_baseline.sql
```

Future shared migrations:

```text
db/common
```

## Rule

All new portable schema work starts at V18 in `db/common`.

## Runtime locations

H2:

```text
classpath:db/migration,classpath:db/common
```

PostgreSQL:

```text
classpath:db/postgresql,classpath:db/common
```

## Why this matters

The project must not silently let PostgreSQL and H2 test schemas diverge.

## Testcontainers

The PostgreSQL integration path should verify that Flyway can build the expected schema and Hibernate can validate it.

## Non-negotiable migration rule

Never rewrite an already-applied migration.

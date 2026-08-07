# Shared Flyway migrations

This directory is the shared migration stream for H2 and PostgreSQL.

Rules:

- Existing H2 history remains in `db/migration` and must not be rewritten.
- The PostgreSQL current-schema baseline is `db/postgresql/V17__current_schema_baseline.sql`.
- New schema migrations must start at **V18** and live in this directory.
- Shared migrations must use SQL supported by both H2 and PostgreSQL, or be split deliberately before merge.
- Never copy V1-V17 migrations into this directory; doing so would create duplicate Flyway versions.

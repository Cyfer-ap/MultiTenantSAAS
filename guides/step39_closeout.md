# Step 39 closeout — PostgreSQL production readiness

Step 39 established:

- PostgreSQL 17 Docker path
- PostgreSQL JDBC support
- PostgreSQL Flyway module
- Testcontainers PostgreSQL dependencies
- PostgreSQL application profile
- V17 current-schema baseline
- shared `db/common` future migration path
- Hibernate schema validation
- PostgreSQL integration verification

It also aligned the subscription read-only test contract and ensured the H2 test profile loads `db/common`.

The handoff from Step 39 is **Step 40 — Transaction & Concurrency Hardening**.

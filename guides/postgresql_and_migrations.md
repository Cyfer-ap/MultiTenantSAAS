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

## Portable migration chain

```text
multitenant-saas/src/main/resources/db/common
```

Shared portable migrations begin at V18. The merged application currently extends through **V55**.

Recent work-management/product migrations include:

- V45 — personal workspace favorites/recent items
- V46 — saved views
- V47 — task parent/dependency/project-label relationships
- V48 — recurring task definitions/occurrences + project task templates
- V49 — tenant project templates + bounded starter-task snapshots
- V50 — workflow definitions/nodes/edges/executions
- V51 — project whiteboards/nodes/connectors
- V52 — project form definitions/fields/submissions
- V53 — approval definitions/stages/reviewers, durable request snapshots and workflow approval state
- V54 — external-access grants/capabilities and hashed guest sessions
- V55 — explicit external-guest task-comment provenance and scoped constraints

**V55 and every earlier applied migration are immutable. New merged persistence starts at V56+.** Draft PR #160 may contain V56 work, but it is not part of the merged schema until that PR (or a successor) lands.

## Required locations

```text
H2:
classpath:db/migration,classpath:db/common

PostgreSQL:
classpath:db/postgresql,classpath:db/common
```

## Rules

1. Never edit an applied migration, including the historical V1-V17 chain and merged `db/common` migrations.
2. Do not copy historical migrations to `db/common`.
3. Put future portable migrations in `db/common`; after guest comments #159, use V56 or later. Never renumber or rewrite draft/merged migration history casually; reconcile with the current `main` ceiling before merge.
4. Keep PostgreSQL/Testcontainers/Flyway/Hibernate-validation verification green.
5. Document and test unavoidable database-specific behavior.
6. When a product checkpoint advances the migration ceiling, update this guide and the canonical checkpoint rather than creating another migration-status document.

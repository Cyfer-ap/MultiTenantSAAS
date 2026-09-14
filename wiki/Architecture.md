# Architecture

Updated: 2026-09-14

MultiTenantSAAS is an intentional **modular monolith** with a separate React/TypeScript client and PostgreSQL/Flyway persistence.

## Stack

- Java 21 / Spring Boot 4.x
- Spring Security with JWT/OIDC
- Spring Data JPA/Hibernate
- PostgreSQL 17 + Flyway + Testcontainers
- React 19 / TypeScript 6 / Vite 8
- React Query, Material UI, React Router
- S3-compatible object storage for attachments

Tenant users and system administrators remain separate identity/control planes.

## System shape

```text
React / TypeScript web client
            ↓ HTTPS/JSON
Spring Boot modular monolith
            ↓
PostgreSQL + Flyway
            ↓
external providers / object storage / tenant webhooks
```

Microservices, Kafka, Redis, Kubernetes and sharding are not current requirements. Introduce them only when measured scale/reliability requirements justify the added complexity.

## Enforcement boundary

```text
authentication / federation
        ↓
tenant identity + isolation
        ↓
scoped authorization
        ↓
subscription / entitlement access
        ↓
resource and API quotas
        ↓
domain invariants
        ↓
transaction + database constraints
```

The backend is authoritative. Frontend guards exist for UX only.

## Major domains

Established capabilities include:

- authentication/workspace discovery/password recovery
- users, invitations and organization hierarchy
- scoped authorization, delegation and Explain Access
- projects, tasks and collaboration
- subtasks, directed task dependencies and project-scoped labels
- Global Search and capability-aware Command Palette
- Favorites/Recently Viewed, My Work and Saved Views
- capability-aware Dashboard composition
- authorization-safe Calendar / Deadline View
- Task Planning workspace
- attachments and notifications
- subscriptions, plans, quotas and usage metering
- Stripe/Razorpay provider integration
- billing webhooks/reconciliation/history
- tenant-configurable outbound webhooks
- enterprise OIDC SSO
- tenant API keys/external APIs
- auditability and observability

The next product slice is **recurring work + project/task templates**, followed by bulk productivity, tenant adaptability, workflows/knowledge and analytics.

## Cross-domain composition patterns

Recent product work intentionally avoids central god-services:

```text
Global Search
    coordinator → contributor contracts → owning-domain adapters

Personal Workspace
    coordinator → resource resolver contracts → project/task adapters

My Work
    attention coordinator → MyWorkTaskSource → task adapter

Saved Views
    definition persistence → context-validator SPI

Dashboard
    frontend composition → existing authorized feature contracts

Calendar
    CalendarDeadlineService → CalendarDeadlineSource → task-owned deadline adapter

Task Relationships
    query / graph / label services
        ↓
    TaskRelationshipTaskGateway + TaskRelationshipChangeSink
        ↓
    existing task persistence + audit/activity
```

Search and Calendar share the neutral `projects.query.ProjectMembershipQueryService` instead of depending on one another's product package.

## Task Relationships and Task Planning

Backend PR #139 and V47 establish:

- one optional same-project parent per task
- self-parent and ancestry-cycle prevention
- bounded parent traversal
- directed `blocking -> dependent` dependency edges
- self/duplicate/directed-cycle prevention for dependencies
- bounded dependency validation/reads
- project-scoped normalized labels
- explicit task/label assignment limits
- existing task read/manage authorization as the authoritative access model

Frontend PR #140 owns `/task-planning` under `features/task-relationships`.

Task selection uses Global Search so the shell does not guess project/task access. Hierarchy, dependencies and labels remain owned by the task-relationships feature instead of being pushed into the already-large project task section, Dashboard or Calendar.

See repository guide `guides/task_relationships.md` for detailed limits and API contracts.

## Calendar boundary

Calendar reads are tenant/date bounded, use existing task due dates, and revalidate project/task readability before exposure. The API caps ranges at 93 days and results at 500 with explicit truncation signaling.

Calendar is a projection, not a scheduling engine. Recurrence/reminders/meetings/resource booking do not belong in `CalendarDeadlineService`.

## Authorization

Authorization is permission- and scope-oriented rather than role-name-only. Explain Access and enforcement use the same evaluator. Delegated authority is bounded by a direct parent assignment and revalidated at access time.

Cross-cutting views such as Search, My Work, Personal Workspace, Calendar and Task Planning do not become alternative permission models; they filter or resolve through authoritative resource-access rules.

See [[Authorization]].

## Tenancy and persistence

Production uses shared-schema tenancy. Tenant-owned data is accessed through tenant-scoped repository/query behavior and cross-tenant IDs must never be trusted without ownership validation.

Flyway owns schema evolution. Applied migrations are append-only. Portable common migrations currently extend through **V47**.

Recent work-management migrations:

- V45 personal workspace favorites/recent items
- V46 saved views
- V47 task parent/dependency/project-label relationships

See [[Tenancy-and-Data-Model]] and [[PostgreSQL-and-Flyway]].

## Billing and integrations

Billing is provider-neutral at the application boundary. Stripe and Razorpay are provider implementations rather than architectural centers of the system. Normal lifecycle synchronization is webhook-authoritative and durable provider/application history supports reconciliation.

Tenant-configurable outbound webhooks use durable delivery/history/retry semantics and HMAC signing.

See [[Subscriptions-and-Quotas]].

## Architecture direction

The most important rule for future development is:

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

New backend features should prefer domain-oriented packages instead of expanding broad global `service`, `controller`, `entity`, `repository` and `dto` buckets.

New frontend features should remain local under `features/<domain>/...` and should not push business logic into central routing/navigation files.

For recurring work/templates, define cadence/timezone/idempotency and template scope/copy/version semantics before schema work. Build an explicit owning domain that consumes narrow task/project creation contracts. Do not add recurrence/template orchestration to `TaskGraphService`, `TaskLabelService`, `ProjectTaskService`, Calendar or shell components.

When a boundary is important enough to regress silently, add architecture/static tests so the rule is machine-enforced rather than relying only on review discipline.

## Known architecture debt

The current codebase does **not** require a rewrite, but the following need active control as the product grows:

- inconsistent backend package/domain boundaries in older code
- existing legacy services with growing orchestration dependency counts
- tenant isolation relying partly on tenant-scoped query conventions in older areas
- manually duplicated backend/frontend API contracts
- increasingly central frontend route/navigation aggregation
- unmeasured large-tenant/load characteristics
- deferred operational exercises such as restore drills and broader failure/load validation

Improve these incrementally during feature work rather than through a speculative repository-wide refactor.

## Scalability

The application tier can remain horizontally replicated and stateless while PostgreSQL acts as the primary durable coordination layer.

The current strategy is PostgreSQL-first and evidence-driven:

- paginate or otherwise bound unbounded reads
- bound graph traversal and date-window projections
- use database locking/constraints for correctness
- add indexes for real query patterns
- measure search/calendar/relationship/load behavior before adding distributed infrastructure

Search, My Work, Personal Workspace, Saved Views, Calendar and Task Relationships are designed around bounded server-side reads rather than tenant-wide browser materialization.

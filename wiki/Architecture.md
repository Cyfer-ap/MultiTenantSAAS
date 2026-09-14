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
- recurring-task definitions/materialization
- project-scoped task templates
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

The current product slice is finishing **Recurring Work + Project/Task Templates** with tenant-scoped project templates and frontend management.

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

Recurring Work / Task Templates
    recurringwork + tasktemplates
        ↓
    TaskCreationPort
        ↓
    task-owned creation adapter
```

## Task Relationships and Task Planning

Backend PR #139 and V47 establish bounded same-project parent hierarchy, cycle-safe directed dependencies and project-scoped normalized labels. Frontend PR #140 owns `/task-planning` under `features/task-relationships` and uses authorization-safe Global Search task selection.

Relationship metadata does not automate task status and does not become generic graph infrastructure.

## Recurring Work and task templates

PR #141 and V48 establish the backend generation boundary.

Recurring work owns schedule definition, timezone/cadence semantics, materialization state and occurrence history. V1 supports `DAILY`, `WEEKLY` and `MONTHLY` rules with explicit IANA timezone, bounded scheduler batches/catch-up, pause/resume and optional end/count/due-offset semantics.

Correctness uses PostgreSQL coordination:

- pessimistic write lock per definition during materialization
- optimistic entity versioning for ordinary definition updates
- unique `(definition_id, scheduled_for)` occurrence key
- transactional task + occurrence + cursor update
- failed materialization pauses the definition rather than retrying indefinitely

Project-scoped task templates own reusable task snapshots. Instantiation crosses the task boundary through the narrow `TaskCreationPort`, preserving normal task activity, audit, assignment notification and `TASK_CREATED` webhook behavior without injecting the full legacy `ProjectTaskService`.

Calendar remains a projection of materialized task deadlines; it is not the recurrence scheduler.

Detailed semantics live in repository guide `guides/recurring_work_and_templates.md`.

## Authorization

Authorization is permission- and scope-oriented rather than role-name-only. Explain Access and enforcement use the same evaluator. Delegated authority is bounded by a direct parent assignment and revalidated at access time.

Cross-cutting views and generation/configuration APIs reuse authoritative project/task access rules instead of creating alternative permission models.

See [[Authorization]].

## Tenancy and persistence

Production uses shared-schema tenancy. Tenant-owned data is accessed through tenant-scoped repository/query behavior and cross-tenant IDs must never be trusted without ownership validation.

Flyway owns schema evolution. Applied migrations are append-only. Portable common migrations currently extend through **V48**.

Recent work-management migrations:

- V45 personal workspace favorites/recent items
- V46 saved views
- V47 task parent/dependency/project-label relationships
- V48 recurring task definitions/occurrences + project task templates

See [[Tenancy-and-Data-Model]] and [[PostgreSQL-and-Flyway]].

## Billing and integrations

Billing is provider-neutral at the application boundary. Stripe and Razorpay are provider implementations rather than architectural centers of the system. Normal lifecycle synchronization is webhook-authoritative and durable provider/application history supports reconciliation.

Tenant-configurable outbound webhooks use durable delivery/history/retry semantics and HMAC signing.

See [[Subscriptions-and-Quotas]].

## Architecture direction

The most important rule for future development is:

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

The next project-template backend must cross into project creation through a **project-owned narrow creation contract** that preserves quota, actor validation, owner membership, audit and lifecycle behavior. Do not inject the full `ProjectService` into a generic template god-service.

Frontend recurring/template work should remain feature-local and must not push lifecycle behavior into Calendar, `ProjectTasksSection` or `AppShell`.

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

The current strategy is PostgreSQL-first and evidence-driven: bound reads/generation, use database locking/constraints for correctness, add indexes for real query patterns, and measure before introducing distributed infrastructure.

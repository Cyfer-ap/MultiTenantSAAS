# Current Architecture

Reviewed: 2026-09-14

This is the canonical technical architecture overview for MultiTenantSAAS. Detailed domain behavior belongs in focused guides such as `authorization_model.md`, `subscription_billing.md`, `enterprise-sso-foundation.md`, `task_relationships.md`, `recurring_work_and_templates.md`, and the outbound-webhook guides. Engineering rules and the technical-debt register live in `ENGINEERING_STANDARDS.md`.

## Architectural shape

MultiTenantSAAS is an intentional **modular monolith** with a separate React web client.

```text
React / TypeScript web client
            ↓ HTTPS/JSON
Spring Boot application
            ↓
PostgreSQL + Flyway
            ↓
external providers / S3-compatible storage
```

The current scale and feature set do not justify microservices, Kafka, Redis, Kubernetes or database sharding. PostgreSQL is intentionally the primary persistence/coordination layer.

## Core stack

Backend: Java 21, Spring Boot 4.x, Spring Security/JWT/OIDC, Spring Data JPA/Hibernate, PostgreSQL 17, Flyway, Testcontainers, Actuator/Micrometer and AWS SDK v2.

Frontend: React 19, TypeScript 6, Vite 8, Material UI, React Router, TanStack React Query, Axios, React Hook Form/Zod, Vitest and Testing Library.

## Non-negotiable domain rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

The preferred pattern is composition at the boundary and ownership inside the domain. Cross-cutting read surfaces may coordinate several narrow adapters; mutation behavior remains with the owning domain.

## Control planes and enforcement

Tenant users and system administrators are separate identity/control planes. A system administrator is not a tenant user with a stronger tenant role.

The request boundary is:

```text
authentication / federation
        ↓
tenant identity + tenant isolation
        ↓
scoped authorization
        ↓
subscription lifecycle / entitlement access
        ↓
resource and API quotas
        ↓
domain invariants
        ↓
transaction + database constraints
```

The backend is authoritative at every security and entitlement boundary. Frontend guards exist for UX only.

## Major application domains

Established domains include:

- authentication, workspace discovery, password recovery and browser sessions
- users, invitations and organization hierarchy
- scoped authorization, bounded delegation and Explain Access
- projects, members, tasks and task collaboration
- task relationships: subtasks, directed dependencies and project-scoped labels
- recurring task definitions/materialization
- project-scoped task templates
- permission-aware Global Search
- capability-aware Command Palette
- Personal Workspace: Favorites + Recently Viewed
- My Work personal attention queue
- Saved Views
- tenant Dashboard composition
- Calendar / Deadline View
- attachments through S3/R2-compatible storage
- in-app/email notifications and preferences
- subscription plans, subscriptions, quotas and usage metering
- Stripe/Razorpay billing adapters and verified provider synchronization
- tenant-configurable outbound webhooks
- enterprise OIDC SSO and tenant IdP configuration
- tenant API keys/external APIs
- tenant/platform audit trails

The active product slice is completing **recurring work + project/task templates** with tenant-scoped project templates and frontend management.

## Reference composition patterns

### Global Search

```text
GlobalSearchService
        ↓
GlobalSearchContributor
        ↓
project / task / user adapters
        ↓
domain-owned bounded queries + authorization
```

Search performs bounded candidate selection before materialization. Project/task hits are revalidated through the same authorization boundary used by their owning APIs.

### Personal Workspace

```text
personal-workspace coordinator
        ↓
resource resolver contract
        ↓
project / task adapters
```

Stored favorites/recent references never grant access. Reads re-resolve resources, so revoked/deleted/inaccessible resources disappear safely.

### My Work

```text
MyWorkService
      ↓
MyWorkTaskSource
      ↓
TaskMyWorkSource
```

My Work owns attention classification, not task persistence. Candidate reads are bounded and project readability is revalidated.

### Saved Views

```text
SavedViewService
      ↓
normalized definitions
      ↓
SavedViewContextValidator SPI
```

Saved definitions do not store authorization results and never grant access.

### Dashboard

Dashboard is frontend composition over existing authorized contracts. It owns presentation/intent only; My Work, Personal Workspace and navigation capability rules remain with their domains. There is no broad `DashboardService`.

### Calendar / Deadline View

```text
CalendarDeadlineController
        ↓
CalendarDeadlineService
        ↓
CalendarDeadlineSource
        ↓
TaskCalendarDeadlineSource
        ↓
tenant/date/project-bounded query
        ↓
authoritative task-read revalidation
```

Calendar is a time projection, not a scheduler. V1 uses task `dueAt` only, `[from,to)` ranges are capped at 93 days, results at 500 with truncation signaling, and no synthetic project deadline exists.

Do not move recurrence, reminders, meetings, resource booking or external calendar sync into `CalendarDeadlineService`.

## Task Relationships architecture

Backend PR #139 introduces V47 and the explicit `taskrelationships` domain.

```text
TaskRelationshipController
        ├── TaskRelationshipQueryService
        ├── TaskGraphService
        └── TaskLabelService
                 ↓
      TaskRelationshipTaskGateway
                 ↓
      existing project/task persistence

relationship changes
        ↓
TaskRelationshipChangeSink
        ↓
audit + task activity
```

Important invariants:

- hierarchy is same-tenant/same-project, one optional parent, no self-parent, no ancestry cycle
- parent traversal is bounded to 64 levels
- dependencies are directed `blocking -> dependent`, same-project, DB-unique and cycle-safe
- dependency validation is bounded to 1,000 edges
- relationship lists are capped at 200 per direction with explicit truncation
- labels are project-scoped, normalized unique, bounded at 200/project and 20/task
- relationship metadata does not automatically change task status
- ordinary task read/manage authorization remains authoritative

Detailed semantics: `task_relationships.md`.

## Task Planning frontend

Frontend PR #140 owns the user-facing surface under `features/task-relationships`.

```text
/task-planning
    ↓
TaskPlanningPage
    ↓
authorization-safe Global Search task selection
    ↓
TaskRelationshipsPanel
    ├── hierarchy
    ├── blockers/dependents
    └── labels → TaskLabelManagerDialog
```

This surface is intentionally separate from the already-large `ProjectTasksSection` and collaboration drawer. Task Planning is registered through shared workspace navigation, so Command Palette and Dashboard quick actions inherit it without duplicating shell logic.

## Recurring Work and task-template architecture

PR #141 introduces V48 and three explicit boundaries:

```text
recurringwork ───────────┐
                         ├─> TaskCreationPort
project task templates ──┘          ↓
                            task-owned adapter
                                   ↓
                            normal task lifecycle
                            ├─ task persistence
                            ├─ task activity
                            ├─ audit
                            ├─ assignment notification
                            └─ TASK_CREATED webhook
```

`recurringwork` owns schedule definition, timezone/cadence semantics, occurrence cursor, materialization and occurrence history. `tasktemplates` owns the project-scoped task-template catalog. `tasks/creation` is a narrow task-owned mutation boundary and deliberately does not expose the large `ProjectTaskService`.

Recurring task concurrency is database-backed:

- due definitions are discovered in bounded batches
- materialization obtains a pessimistic write lock on one definition
- `@Version` protects ordinary definition updates
- `(definition_id, scheduled_for)` is unique in PostgreSQL
- a materialization transaction includes generated task, occurrence linkage and cursor advance
- a failed generation pauses the definition instead of retrying forever

Schedule arithmetic uses the definition's IANA timezone and calendar increments, preserving local wall time through DST and month-length changes.

Task-template instantiation uses snapshot semantics; existing tasks are not coupled to subsequent template edits.

Detailed semantics: `recurring_work_and_templates.md`.

## Persistence and tenancy

Production uses shared-schema multi-tenancy with explicit tenant ownership. Repository/query methods for tenant-owned resources should include tenant scope; cross-tenant resource IDs are never trusted without ownership validation.

Flyway exclusively owns production schema evolution. Portable common migrations extend through **V48**.

Recent product migrations:

- V45 — personal workspace favorites/recent items
- V46 — saved views
- V47 — task parent/dependency/project-label relationships
- V48 — recurring task definitions/occurrences + project task templates

Dashboard #136 and Calendar #137 required no schema migration. Applied migrations are append-only.

## Billing and integrations

The subscription model is provider-neutral. Stripe and Razorpay are adapters around application billing state rather than architecture sources of truth. Normal provider lifecycle synchronization is webhook-authoritative and auditable.

Stripe is the validated deployed Test Mode path. Razorpay application/catalog integration remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

Tenant outbound webhooks use durable events/deliveries/attempts. Secondary delivery concerns should stay behind narrow application contracts or durable event mechanisms rather than hidden repository writes inside unrelated services.

## Frontend architecture

Healthy feature modules commonly contain:

```text
features/<domain>/
    api/
    components/
    hooks/
    pages/
    types/
```

Current product-enrichment ownership includes:

- `features/search`
- `features/command-palette`
- `features/personal-workspace`
- `features/my-work`
- `features/saved-views`
- `features/dashboard`
- `features/calendar`
- `features/task-relationships`

The next frontend slice should preserve explicit ownership for recurring work, task templates and project templates instead of expanding `ProjectTasksSection` or `AppShell` with lifecycle logic.

Server state is managed with React Query. `AppShell` and central routes/navigation are integration infrastructure and must not absorb domain business rules.

Central route/navigation aggregation remains a known growth point. Prefer shared integration metadata/contracts where useful, but do not introduce abstraction merely to hide a small explicit route list.

## API boundary

Public APIs use explicit request/response DTOs and validation rather than JPA entities.

The web client currently maintains TypeScript API contracts manually. This is acceptable for one primary client, but generated/shared contracts should be considered before a substantial mobile client is introduced.

Task-relationship APIs are documented in `task_relationships.md`. Recurring/task-template APIs are documented in `recurring_work_and_templates.md`.

## Scalability model

The Spring application is stateless enough for horizontal application replication; PostgreSQL remains the coordination layer.

Current bounded patterns include:

- bounded Global Search candidates/results
- bounded Personal Workspace/Recent state
- bounded My Work source reads
- bounded dashboard previews
- Calendar range/result caps
- bounded task hierarchy/dependency traversal and relationship lists
- bounded project label/task assignment counts
- recurring-work due batches and per-definition catch-up limits
- bounded task-template catalog sizes

Large-tenant latency, sustained mutation throughput, database contention, scheduler contention, delivery throughput and heavy integration workloads remain unproven until measured. Optimize from evidence rather than pre-emptively introducing distributed infrastructure.

## Next architecture direction — project templates + frontend

The V48 task-generation boundary should be reused, not bypassed.

Tenant-scoped project templates need their own project creation boundary:

```text
projecttemplates
      ↓
ProjectCreationPort       TaskCreationPort
      ↓                         ↓
project-owned adapter      task-owned adapter
```

The project-owned adapter must preserve ordinary quota, actor, owner-membership, audit and project lifecycle behavior without injecting the full `ProjectService` into a template domain.

Project-template v1 should use bounded snapshot/copy semantics and avoid importing task-relationship graphs until there is a concrete product need.

Frontend recurring/template lifecycle should remain feature-local. Calendar may display deadlines of materialized recurring tasks, but it does not own or pre-generate recurrence rules.

## Production boundary

The codebase has strong production-oriented security, database, provider and CI practices, but full enterprise operational maturity is not yet claimed.

Deferred work still includes backup/restore drills, broader failure-recovery exercises, load/concurrency characterization, production R2 verification and deeper alert/runbook coverage. These are deliberately deferred behind the current user-facing product-enrichment phase, not forgotten.

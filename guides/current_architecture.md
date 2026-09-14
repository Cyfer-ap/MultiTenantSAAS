# Current Architecture

Reviewed: 2026-09-14

This is the canonical technical architecture overview for MultiTenantSAAS. Detailed domain behavior belongs in focused guides such as `authorization_model.md`, `subscription_billing.md`, `enterprise-sso-foundation.md`, `task_relationships.md`, and the outbound-webhook guides. Engineering rules and the technical-debt register live in `ENGINEERING_STANDARDS.md`.

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

The next product-enrichment slice is **recurring work + project/task templates**.

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
        │       └── bounded relationship projection
        ├── TaskGraphService
        │       └── parent/dependency mutations + cycle checks
        └── TaskLabelService
                └── project-label lifecycle + assignment

relationship services
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

This surface is intentionally separate from the already-large `ProjectTasksSection` and collaboration drawer. It uses existing task-management permission plus the established project-lead membership fallback for mutation UX; backend authorization remains authoritative.

Task Planning is registered through shared workspace navigation, so Command Palette and Dashboard quick actions inherit it without duplicating shell logic.

## Persistence and tenancy

Production uses shared-schema multi-tenancy with explicit tenant ownership. Repository/query methods for tenant-owned resources should include tenant scope; cross-tenant resource IDs are never trusted without ownership validation.

Flyway exclusively owns production schema evolution. Portable common migrations extend through **V47**.

Recent product migrations:

- V45 — personal workspace favorites/recent items
- V46 — saved views
- V47 — task parent/dependency/project-label relationships

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

Server state is managed with React Query. `AppShell` and central routes/navigation are integration infrastructure and must not absorb domain business rules.

Central route/navigation aggregation remains a known growth point. Prefer shared integration metadata/contracts where useful, but do not introduce abstraction merely to hide a small explicit route list.

## API boundary

Public APIs use explicit request/response DTOs and validation rather than JPA entities.

The web client currently maintains TypeScript API contracts manually. This is acceptable for one primary client, but generated/shared contracts should be considered before a substantial mobile client is introduced.

Task-relationship APIs are documented in `task_relationships.md`.

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

Large-tenant latency, sustained mutation throughput, database contention, delivery throughput and heavy integration workloads remain unproven until measured. Optimize from evidence rather than pre-emptively introducing distributed infrastructure.

## Next architecture direction — recurring work + templates

Recurring work and templates require their own owning domain. They must not be implemented by expanding `TaskGraphService`, `TaskLabelService`, `ProjectTaskService`, Calendar or application-shell code.

Before schema work, define:

- supported recurrence schedule/cadence model
- timezone ownership and DST behavior
- occurrence materialization horizon
- occurrence idempotency/concurrency key
- pause/resume/edit/end semantics
- behavior when previous occurrences remain incomplete
- task/project template scope
- copy/snapshot semantics and optional versioning
- fields/relationships copied during instantiation
- authorization for template management/use and recurrence management
- bounded template size/generated work

A likely shape is:

```text
recurringwork / templates domain
        ↓
recurrence definitions + template catalog
        ↓
materializer / instantiator
        ↓
narrow task/project creation contracts
        ↓
existing owning domains
```

The exact split between recurrence and template modules should follow product semantics, but neither should become another god-service.

## Production boundary

The codebase has strong production-oriented security, database, provider and CI practices, but full enterprise operational maturity is not yet claimed.

Deferred work still includes backup/restore drills, broader failure-recovery exercises, load/concurrency characterization, production R2 verification and deeper alert/runbook coverage. These are deliberately deferred behind the current user-facing product-enrichment phase, not forgotten.

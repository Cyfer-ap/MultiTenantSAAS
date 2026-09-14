# Current Architecture

Reviewed: 2026-09-14

This is the canonical technical architecture overview for MultiTenantSAAS. Detailed domain behavior belongs in focused guides such as `authorization_model.md`, `subscription_billing.md`, `enterprise-sso-foundation.md`, and the outbound-webhook guides. Engineering rules and the technical-debt register live in `ENGINEERING_STANDARDS.md`.

## Architectural shape

MultiTenantSAAS is a **modular monolith** with a separate React web client.

```text
React / TypeScript web client
            ↓ HTTPS/JSON
Spring Boot application
            ↓
PostgreSQL + Flyway
            ↓
external providers / S3-compatible storage
```

The monolith is intentional. The current scale and feature set do not justify microservices, Kafka, Redis, Kubernetes, or database sharding.

## Core stack

### Backend

- Java 21
- Spring Boot 4.x
- Spring Security + JWT/OIDC
- Spring Data JPA/Hibernate
- PostgreSQL 17
- Flyway
- Testcontainers
- Actuator/Micrometer
- AWS SDK v2 for S3-compatible object storage

### Frontend

- React 19
- TypeScript 6
- Vite 8
- Material UI
- React Router
- TanStack React Query
- Axios
- React Hook Form + Zod
- Vitest + Testing Library

## Control planes

Tenant users and system administrators are separate identity/control planes. A system administrator is not implemented as a tenant user with a stronger tenant role.

The tenant plane owns normal workspace activity. The system plane owns platform administration such as tenants, plans, provider catalogs, platform audit, and subscription operations.

## Enforcement pipeline

The intended request boundary is:

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

Current mature domains include:

- authentication, workspace discovery, password recovery and browser sessions
- users, invitations and organization hierarchy
- scoped authorization, delegation and Explain Access
- projects, members, tasks and task collaboration
- permission-aware Global Search
- capability-aware Command Palette
- Personal Workspace: Favorites + Recently Viewed
- My Work personal attention queue
- Saved Views
- capability-aware tenant Dashboard composition
- authorization-safe Calendar / Deadline View
- attachments through S3/R2-compatible storage
- in-app/email notifications and preferences
- subscription plans, tenant subscriptions, quotas and usage metering
- provider-neutral billing with Stripe and Razorpay implementations
- durable signed provider webhook processing and reconciliation
- tenant-configurable outbound webhooks with retries/history/replay
- enterprise OIDC SSO and tenant IdP configuration
- tenant API keys and isolated external API authentication
- tenant/platform audit trails
- production hardening and observability

The next product-enrichment stage is richer task relationships—subtasks, dependencies and labels—followed by recurring work/templates, bulk productivity, tenant adaptability, workflows/knowledge and user-facing analytics.

## Authorization architecture

Authorization is scope-aware rather than role-name-only.

Important scopes include tenant, project, organizational unit, organizational subtree, direct reports and self. Delegated authority is bounded by a current direct parent assignment and is revalidated at access time.

Explain Access uses the same evaluator as enforcement so diagnostic output cannot drift into a second permission model.

See `authorization_model.md` for the complete model.

## Search/discovery architecture

Global Search established the reference backend pattern for cross-domain read composition.

The coordinator does not import project/task/user repositories. Instead:

```text
GlobalSearchService
        ↓
GlobalSearchContributor contract
        ↓
+----------------+----------------+----------------+
| project adapter| task adapter   | user adapter   |
+----------------+----------------+----------------+
        ↓                 ↓                ↓
domain-owned bounded queries and authorization rules
```

Important properties:

- tenant identity is validated at the API boundary
- candidate queries are bounded before data is materialized
- user search requires tenant-level `user.read`
- project search uses tenant-wide or explicit project-grant scope and revalidates scoped hits through the authorization evaluator
- task search uses tenant-wide permission, explicit project scope or current project membership and revalidates through the same task-read rule used by task APIs
- generic project membership lookup is owned by `projects.query.ProjectMembershipQueryService`, not by Search, so other authorized projections can reuse the same narrow contract
- ranking is a search-domain concern; access policy remains a domain/authorization concern
- the frontend `features/search` module owns the reusable API client, query hook and types rather than a product-specific shell UI

This contributor pattern is preferred when a cross-cutting read feature needs data from several domains: central orchestration owns composition, while each domain retains its query/access knowledge behind a narrow contract.

## Command-palette architecture

The Command Palette is a separate frontend orchestration domain under `features/command-palette`. It consumes discovery rather than owning discovery.

```text
features/command-palette
        ├── capability-filtered navigation commands
        ├── direct quick actions
        └── accessible search results
                 ↓
       features/search hook/API
```

Mutation commands do not duplicate forms, API calls, validation or cache logic. Create Project and Invite User invoke the existing project/invitation domain dialogs. Visibility requires the relevant authorization capability and current subscription entitlement; the backend remains authoritative when the mutation executes.

This is the preferred frontend boundary pattern for cross-cutting action surfaces: the shell/palette composes **intent**, while the owning feature retains business behavior.

Create Task is intentionally not exposed globally yet. Effective task-management authority can derive from project-lead membership as well as scoped authorization. A future global Create Task action should therefore use a project-aware capability/picker contract supplied by the project/task domain instead of rebuilding task access logic in `AppShell` or the palette.

## Personal Workspace architecture

Favorites and Recently Viewed are server-backed personal-productivity state rather than browser-local page state.

```text
personal-workspace coordinator
        ↓
resource resolver contract
        ↓
+----------------+----------------+
| project adapter| task adapter   |
+----------------+----------------+
        ↓                 ↓
domain-owned tenant/access checks
```

Important properties:

- persistence is scoped by tenant, user, resource type and resource ID
- storing a resource identifier never grants continued access
- writes resolve the resource through an owning-domain authorization adapter
- reads re-resolve stored references, so deleted, revoked or otherwise inaccessible resources disappear safely
- unfavorite is idempotent and remains possible after access is lost
- recent history is bounded
- project/task repositories are not imported into the personal-workspace coordinator

The frontend `features/personal-workspace` domain owns the consolidated Favorites/Recent surface, contextual favorite integration and the reusable project/task target-navigation contract.

## My Work architecture

My Work is an attention composition domain, not another task repository/service layer.

```text
MyWorkService
      ↓
MyWorkTaskSource
      ↓
TaskMyWorkSource adapter
      ↓
tenant/user-scoped candidate query + project/task readability checks
```

The current source returns assigned open tasks only. Attention classification belongs to My Work and currently distinguishes overdue, blocked, due-soon, in-progress and remaining assigned work.

The source read is bounded and authorization checks are memoized per project within the request. This leaves room for future sources such as mentions, approvals or other actionable work without injecting unrelated repositories into `MyWorkService`.

## Saved Views architecture

Saved Views persist normalized user-owned view definitions independently of the surfaces that consume them.

```text
SavedViewService
      ↓
normalized/allow-listed definitions
      ↓
SavedViewContextValidator SPI
      ↓
optional owning-domain context adapter
```

Important properties:

- views are tenant/user scoped
- definitions are allow-listed and normalized by target before persistence
- My Work does not require external context validation
- the contract is prepared for `PROJECT_TASKS` with project/task context validation behind a narrow adapter
- saved definitions never store authorization results or grant access
- applying a view filters an already-authorized surface

The frontend `features/saved-views` domain owns API/query/mutation/UI behavior and is currently integrated with My Work.

## Dashboard architecture

PR #136 implements the tenant dashboard as a **frontend composition surface**, not a new source of truth.

```text
DashboardPage
    ↓
features/dashboard composition
    ├── DashboardMyWorkCard → features/my-work query
    ├── DashboardPersonalContextCard → features/personal-workspace query
    ├── DashboardQuickActionsCard → shared workspace-navigation capability contract
    └── existing tenant dashboard summary → tenant-wide health metrics
```

Important properties:

- no new dashboard backend service or schema migration
- My Work classification remains owned by `mywork`
- Favorites/Recent authorization resolution remains owned by `personal-workspace`
- quick actions use the same authorization/navigation contract as the workspace shell rather than a second permission model
- personal widget failures degrade locally instead of blanking the whole dashboard
- collection previews are bounded
- focused dashboard components keep the composition surface from becoming another large frontend god-component

The dashboard is the reference pattern for frontend composition over several authorized feature domains: compose presentation and intent, but leave business rules and access policy with the owning domains.

## Calendar / Deadline architecture

PR #137 implements Calendar as a **time-oriented projection of authorized task work**, not a second task-management system.

```text
CalendarDeadlineController
        ↓
CalendarDeadlineService
        ↓
CalendarDeadlineSource SPI
        ↓
TaskCalendarDeadlineSource
        ↓
tenant/date-bounded task query
        ↓
project grant / membership narrowing
        ↓
authoritative task-read revalidation
```

Important properties:

- the calendar domain owns range validation, response composition and projection contracts; task data/access logic remains task-owned
- Calendar v1 uses existing task `dueAt` values only; `Project` has no deadline field and no synthetic project dates are introduced
- reads use half-open `[from,to)` instant ranges
- a request cannot exceed 93 days
- returned results are capped at 500; the API exposes `truncated=true` when the authorized set exceeds the requested bound
- tenant-wide task-read permission, explicit project-scoped task-read grants and current project membership can contribute candidate projects
- candidate data is narrowed by tenant/date/project before materialization and revalidated through `AuthorizationSecurityService.canReadProjectTasks` per project
- project access decisions are memoized within the adapter request
- the existing `project_tasks.due_at` index is reused; #137 requires no Flyway migration
- the generic `ProjectMembershipQueryService` is shared by Search and Calendar instead of making Calendar depend on a Search-specific service

Frontend ownership is under `features/calendar`:

```text
CalendarPage
    ├── useCalendarDeadlines
    ├── CalendarMonthGrid
    ├── CalendarDeadlineAgenda
    └── local calendar-date utilities
```

The browser computes month/grid boundaries in local calendar time and sends the resulting instants to the backend. The API remains timezone-neutral because it accepts/returns instants; presentation groups deadlines by the browser's local date.

Calendar is exposed as a general tenant workspace destination without inventing a frontend calendar permission. The backend result itself remains authoritative and contains only tasks currently readable by the actor.

Meetings, room/resource booking, leave management, recurrence/reminders and external calendar synchronization are not part of this domain today.

## Persistence and tenancy

The production database uses shared-schema multi-tenancy with tenant ownership represented explicitly in tenant-owned data.

Repository/query methods for tenant-owned resources should include tenant scope. Cross-tenant resource IDs must never be trusted without ownership validation.

Flyway exclusively owns production schema evolution. Portable common migrations currently extend through **V46**.

Recent product migrations:

- V45 — personal workspace favorites/recent items
- V46 — saved views

Dashboard #136 and Calendar #137 require no schema migration.

The architecture intentionally uses PostgreSQL locking/constraints where correctness depends on concurrent mutations, including sensitive subscription/delivery flows.

## Billing and provider integration

The internal subscription model is provider-neutral. Stripe and Razorpay are adapters around the application billing model rather than the source of application architecture.

Normal billing lifecycle synchronization is webhook-authoritative. Durable provider/customer/subscription/catalog mappings and immutable subscription history allow reconciliation and recovery without exposing provider internals to tenant clients.

Stripe is the validated deployed Test Mode path. Razorpay application/catalog integration remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

See `subscription_billing.md` for details.

## Integration/event architecture

The application currently uses a mixture of direct synchronous application-service calls and durable event/delivery subsystems.

Synchronous calls are appropriate when an invariant must succeed inside the same transaction. Secondary reactions such as outbound delivery should be isolated through narrow application contracts or durable event mechanisms rather than becoming hidden repository writes inside unrelated domains.

Future domains should follow the modularity rules in `ENGINEERING_STANDARDS.md`.

## Frontend architecture

The frontend is increasingly feature-oriented. Healthy feature modules commonly contain:

```text
features/<domain>/
    api/
    components/
    hooks/
    pages/
    types/
```

Server state is managed with React Query. Route/permission guards improve navigation and UX but do not replace backend enforcement.

Current product-enrichment ownership is intentionally split:

- `features/search` — reusable discovery contracts
- `features/command-palette` — command/navigation composition
- `features/personal-workspace` — Favorites/Recent state and views
- `features/my-work` — personal attention queue
- `features/saved-views` — reusable persisted view definitions
- `features/dashboard` — dashboard-only composition components consuming owning domains
- `features/calendar` — date-range query/view composition over authorized task deadline contracts

`AppShell` should remain integration/navigation infrastructure and must not absorb domain business rules as new product surfaces are added.

The main future risk is growth of central routing/navigation aggregation. New modules should expose narrow integration metadata where appropriate rather than moving domain logic into central application files.

## API boundary

Public APIs use explicit request/response DTOs and validation rather than returning JPA entities.

The current web client maintains TypeScript API contracts manually. That is acceptable for the current single-client web application, but a generated/shared contract strategy should be adopted before a substantial mobile client is added so backend/web/mobile models do not drift independently.

Current product-enrichment APIs include:

```text
GET    /api/tenants/{tenantId}/search
GET    /api/tenants/{tenantId}/personal-workspace
PUT    /api/tenants/{tenantId}/personal-workspace/favorites/{type}/{resourceId}
DELETE /api/tenants/{tenantId}/personal-workspace/favorites/{type}/{resourceId}
POST   /api/tenants/{tenantId}/personal-workspace/recent/{type}/{resourceId}
GET    /api/tenants/{tenantId}/my-work
GET    /api/tenants/{tenantId}/saved-views
POST   /api/tenants/{tenantId}/saved-views
PUT    /api/tenants/{tenantId}/saved-views/{viewId}
DELETE /api/tenants/{tenantId}/saved-views/{viewId}
GET    /api/tenants/{tenantId}/calendar/deadlines?from=<instant>&to=<instant>&limit=<n>
```

The Dashboard Refresh reuses existing contracts and the dashboard summary; it does not add an API. Calendar adds one bounded read projection and no mutation API.

## Scalability model

The Spring application is designed to remain stateless enough for horizontal application replication. PostgreSQL is intentionally the primary coordination/persistence layer.

This architecture should comfortably support significant product growth before distributed infrastructure becomes justified.

Global Search v1 uses bounded PostgreSQL substring queries and deliberately avoids adding a separate search engine. If measured large-tenant latency later requires it, PostgreSQL trigram/full-text indexing should be considered before external search infrastructure.

Personal Workspace, My Work and Saved Views use bounded reads/writes rather than unbounded tenant-wide materialization. Dashboard previews remain bounded and reuse those existing queries. Calendar reads are restricted by tenant, project-access candidates, a maximum 93-day range and a 500-item result cap while reusing the existing due-date index.

The following remain unproven until measured:

- large-tenant search/query latency
- dense-calendar query behavior near the 500-item bound
- sustained concurrent mutation throughput
- database contention envelopes
- background delivery throughput at scale
- large attachment/integration workloads

The policy is to optimize from evidence. Do not introduce distributed infrastructure simply because a future scale problem is imaginable.

## Production boundary

The application code has strong production-oriented security, database, provider and CI practices, but full enterprise operational maturity is not yet claimed.

Deferred work still includes:

- backup/restore drills
- broader failure-recovery exercises
- load/concurrency characterization
- production R2 verification
- deeper operational alert/runbook coverage

These are deliberately deferred behind the current user-facing product-enrichment phase, not forgotten.

## Architecture direction from this point

All new functionality must follow this rule:

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

The product-enrichment sequence now provides concrete reference implementations: Search uses contributor contracts; Command Palette composes owning frontend features; Personal Workspace uses resolver adapters; My Work uses a narrow source contract; Saved Views uses a context-validator SPI; Dashboard composes authorized frontend contracts; Calendar uses a projection coordinator plus a task-owned deadline source.

The next task-relationship slice should preserve the same discipline. Subtask hierarchy, dependency edges and labels belong to task/project-owned domain contracts with explicit tenant/project invariants, bounded graph traversal and database constraints. Do not implement them by expanding Dashboard, Calendar, `AppShell`, or a generic graph service into new sources of truth.

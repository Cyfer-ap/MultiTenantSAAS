# Current Architecture

Reviewed: 2026-09-16

This is the canonical technical architecture overview for MultiTenantSAAS. Detailed domain behavior belongs in focused guides such as `authorization_model.md`, `subscription_billing.md`, `enterprise-sso-foundation.md`, `task_relationships.md`, `recurring_work_and_templates.md`, `visual_workflow_builder.md`, `forms_workflow_engine.md`, `project_simulation.md`, `collaborative_whiteboard.md`, `project_risk_radar.md` and the outbound-webhook guides. Engineering rules and the technical-debt register live in `ENGINEERING_STANDARDS.md`.

## Architectural shape

MultiTenantSAAS is an intentional **modular monolith** with a separate React web client.

```text
React / TypeScript web client
            ↓ HTTPS/JSON
Spring Boot modular monolith
            ↓
PostgreSQL + Flyway
            ↓
external providers / S3-compatible storage
```

The current scale and feature set do not justify microservices, Kafka, Redis, Kubernetes or database sharding. PostgreSQL is intentionally the primary persistence and coordination layer.

## Core stack

Backend: Java 21, Spring Boot 4.x, Spring Security/JWT/OIDC, Spring Data JPA/Hibernate, PostgreSQL 17, Flyway, Testcontainers, Actuator/Micrometer and AWS SDK v2.

Frontend: React 19, TypeScript 6, Vite 8, Material UI, React Router, TanStack React Query, Axios, React Hook Form/Zod, Vitest and Testing Library.

## Non-negotiable domain rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

The preferred pattern is composition at the boundary and ownership inside the domain. Cross-cutting read surfaces may coordinate several narrow adapters; mutation behavior remains with the owning domain.

Consequences:

- do not add unrelated orchestration to `ProjectService`, `ProjectTaskService` or `WorkflowService`
- do not write another domain's repositories directly merely because the tables are in the same process/database
- stored configuration never grants authorization to the resource it references
- generated/mutated work re-enters the owning domain's authorization, quota, audit and lifecycle rules
- graph/document/form/batch inputs remain explicitly bounded

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

Established domains/surfaces include:

- authentication, workspace discovery, password recovery and browser sessions
- users, invitations and organization hierarchy
- scoped authorization, bounded delegation and Explain Access
- projects, members, tasks and task collaboration
- task relationships: subtasks, directed dependencies and project-scoped labels
- recurring work and task/project templates
- Visual Workflow Builder and execution history
- bounded internal Forms -> Workflow intake
- Project Simulation / What-If Engine
- Collaborative Whiteboard
- Project Health / Risk Radar
- permission-aware Global Search and Command Palette
- Personal Workspace: Favorites + Recently Viewed
- My Work personal attention queue
- Saved Views
- capability-aware Dashboard
- Calendar / Deadline View
- attachments through S3/R2-compatible storage
- durable in-app/email notifications and preferences
- subscription plans, subscriptions, quotas and usage metering
- Stripe/Razorpay billing adapters and verified provider synchronization
- tenant-configurable outbound webhooks
- enterprise OIDC SSO and tenant IdP configuration
- tenant API keys/external APIs
- tenant/platform audit trails

The active next product slice is **Approval Workflows**. It must extend the existing workflow architecture through an explicit human-decision domain rather than turning workflow/task services into approval state owners.

## Reference composition patterns

### Read projections and personal surfaces

Global Search, Personal Workspace, My Work, Saved Views, Dashboard and Calendar use narrow source/resolver/validator contracts rather than taking ownership of project/task persistence.

Representative patterns:

```text
GlobalSearchService
        ↓
GlobalSearchContributor
        ↓
domain-owned bounded queries + authorization
```

```text
MyWorkService
      ↓
MyWorkTaskSource
      ↓
task-owned source adapter
```

```text
CalendarDeadlineService
        ↓
CalendarDeadlineSource
        ↓
TaskCalendarDeadlineSource
```

Stored favorites, saved views and projection state never grant access. Authoritative resources are re-resolved/re-authorized when exposed.

### Task Relationships

The explicit `taskrelationships` domain owns hierarchy, dependency and project-label orchestration without absorbing task persistence.

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

Hierarchy/dependency traversal and relationship lists are bounded; ordinary task read/manage authorization remains authoritative.

Detailed semantics: `task_relationships.md`.

### Work generation: recurring work and templates

Recurring work and templates reuse domain-owned creation boundaries:

```text
recurringwork ───────────┐
                         ├─> TaskCreationPort
project task templates ──┘          ↓
                            task-owned adapter

projecttemplates ───────────> ProjectCreationPort
        │                           ↓
        └───────────────────> TaskCreationPort
```

The ports preserve ordinary actor/project validation, quotas, ownership/membership, audit, activity, notifications/webhooks and lifecycle behavior without injecting large legacy services into generator domains.

Recurring materialization uses bounded due batches, per-definition locking/versioning and database uniqueness. Templates use bounded snapshot/copy semantics.

Detailed semantics: `recurring_work_and_templates.md`.

### Visual Workflow Builder

V50 establishes the typed workflow graph/runtime:

```text
task transaction
    -> task-domain event
    -> after-commit workflow listener
    -> active workflow graph
    -> conditions/actions
    -> TaskAutomationMutationPort
    -> task-owned authorization + mutation
```

Workflow administration permission does not grant authority over target tasks. Runtime actions re-check the originating actor's current authority through the task-owned mutation contract.

Graphs are bounded, acyclic and typed. Arbitrary scripts/expressions are not accepted. Executions are idempotently recorded with trigger/source/version/outcome provenance.

Detailed semantics: `visual_workflow_builder.md`.

### Forms -> Workflow Engine

V52 introduces the explicit `forms` domain:

```text
forms
    -> ProjectAccessPort
    -> TaskCreationPort
    -> WorkflowFormSubmissionPort
```

The form domain owns definitions, fields and submissions. It does not own task/project/workflow repositories.

Accepted submissions create normal tasks through `TaskCreationPort`. An optional selected workflow uses the typed `TRIGGER_FORM_SUBMITTED` entry through `WorkflowFormSubmissionPort`. Form-specific workflow dispatch occurs after the form/task transaction commits; the ordinary task-created event remains intact.

Definitions/submissions are bounded and non-executable. Supported v1 field types are `TEXT`, `TEXTAREA`, `NUMBER`, `DATE`, `BOOLEAN` and `SELECT`. Public/anonymous intake is intentionally not part of v1.

Detailed semantics: `forms_workflow_engine.md`.

### Project Simulation

```text
projectsimulation
    -> ProjectSimulationTaskSource
    -> ProjectSimulationDependencySource
```

Simulation owns hypothetical scenario computation, not task/dependency persistence. It remains advisory and has no hidden apply path.

Detailed semantics: `project_simulation.md`.

### Collaborative Whiteboard

```text
whiteboards
    -> ProjectAccessPort
    -> TaskCreationPort
```

Whiteboards own persisted visual documents and optimistic versioning. Node -> task conversion re-enters normal task creation. The persisted document model is transport-independent; live presence/cursors remain optional later work.

Detailed semantics: `collaborative_whiteboard.md`.

### Project Health / Risk Radar

```text
projectrisk
    -> ProjectRiskTaskSource
    -> ProjectRiskDependencySource
```

Risk Radar is an explainable read model over authorized task/dependency state. It is bounded, advisory and does not score employee productivity or mutate project/task state.

Detailed semantics: `project_risk_radar.md`.

## Persistence and tenancy

Production uses shared-schema multi-tenancy with explicit tenant ownership. Repository/query methods for tenant-owned resources should include tenant scope; cross-tenant resource IDs are never trusted without ownership validation.

Flyway exclusively owns production schema evolution. Portable common migrations extend through **V52**:

- V45 — personal workspace favorites/recent items
- V46 — saved views
- V47 — task parent/dependency/project-label relationships
- V48 — recurring task definitions/occurrences + project task templates
- V49 — tenant project templates + bounded starter-task snapshots
- V50 — workflow definitions/nodes/edges/executions
- V51 — project whiteboards/nodes/connectors
- V52 — project form definitions/fields/submissions

Project Simulation and Risk Radar require no migration. Applied migrations are append-only. **V52 is immutable; future persistence starts at V53+.**

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

Current differentiated/product-enrichment ownership includes:

- `features/search`
- `features/command-palette`
- `features/personal-workspace`
- `features/my-work`
- `features/saved-views`
- `features/dashboard`
- `features/calendar`
- `features/task-relationships`
- recurring/template feature modules under Work Automation
- `features/workflow-builder`
- `features/forms`
- `features/project-simulation`
- `features/whiteboards`
- `features/project-risk`
- `features/project-tools` for project-level tool navigation integration

Server state is managed with React Query. `AppShell` and central routes/navigation are integration infrastructure and must not absorb domain business rules.

Central route/navigation aggregation remains a known growth point. Prefer shared integration metadata/contracts when it genuinely reduces coupling, but do not introduce abstraction merely to hide a small explicit route list.

## API boundary

Public APIs use explicit request/response DTOs and validation rather than JPA entities.

The web client currently maintains TypeScript API contracts manually. This remains acceptable for one primary client, but generated/shared contracts should be considered before a substantial mobile or external SDK surface is introduced.

Focused guides document domain API semantics and invariants; those contracts take precedence over stale historical planning notes.

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
- bounded task/project template catalog/snapshot sizes
- bounded workflow graph nodes/edges and execution history
- bounded form fields/options/payloads
- bounded simulation tasks/dependencies/overrides
- bounded whiteboard nodes/connectors
- bounded Risk Radar tasks/dependencies/signals

Large-tenant latency, sustained mutation throughput, database contention, scheduler contention, delivery throughput and heavy integration workloads remain unproven until measured. Optimize from evidence rather than pre-emptively introducing distributed infrastructure.

## Next architecture direction — Approval Workflows

Approval Workflows should add an explicit human-decision boundary rather than embed approval state in task or workflow services.

Target composition:

```text
workflow runtime
      ↓ narrow approval entry/wait contract
approval domain
      ↓ reviewer eligibility/authorization contract
human decision
      ↓ narrow workflow resume/outcome event
workflow runtime
      ↓ existing domain-owned mutation ports
```

The approval domain should own bounded definition/stage configuration plus durable request/decision provenance. It must not grant reviewer authority merely because a user appears in configuration; reviewer eligibility must be revalidated at decision time.

Concurrency/idempotency must make one logical stage decision deterministic under racing/replayed requests. Self-approval/separation-of-duties, expiry, cancellation, reassignment and escalation need explicit product semantics rather than accidental defaults.

If the slice needs persistence, start at **V53+**. External/client approvals belong behind the later Client / Guest Portal security boundary.

## Production boundary

The codebase has strong production-oriented security, database, provider and CI practices, but full enterprise operational maturity is not yet claimed.

Deferred work still includes backup/restore drills, broader failure-recovery exercises, load/concurrency characterization, production R2 verification and deeper alert/runbook coverage. These are deliberately deferred behind the current user-facing product-enrichment phase, not forgotten.

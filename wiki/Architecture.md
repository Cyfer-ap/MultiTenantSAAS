# Architecture

Updated: 2026-09-16

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

## Core architecture rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Stored configuration never grants access to referenced resources. Generated or automated work re-enters the owning domain's authorization, quota, audit and lifecycle behavior.

## Major domains

Established capabilities include:

- authentication/workspace discovery/password recovery
- users, invitations and organization hierarchy
- scoped authorization, delegation and Explain Access
- projects, tasks and collaboration
- subtasks, directed task dependencies and project-scoped labels
- recurring work and task/project templates
- Visual Workflow Builder and execution history
- Forms -> Workflow Engine internal intake
- Project Simulation / What-If Engine
- Collaborative Whiteboard
- Project Health / Risk Radar
- Global Search and capability-aware Command Palette
- Favorites/Recently Viewed, My Work and Saved Views
- capability-aware Dashboard and Calendar / Deadline View
- attachments and notifications
- subscriptions, plans, quotas and usage metering
- Stripe/Razorpay provider integration
- tenant-configurable outbound webhooks
- enterprise OIDC SSO
- tenant API keys/external APIs
- auditability and observability

**Approval Workflows is the active next product slice.**

## Cross-domain composition patterns

Recent product work intentionally avoids central god-services:

```text
Global Search
    coordinator -> contributor contracts -> owning-domain adapters

Personal Workspace / My Work / Calendar
    feature coordinator -> narrow source/resolver contracts -> owning-domain adapters

Task Relationships
    relationship services -> task gateway/change sink -> task persistence + audit/activity

Recurring Work / Task Templates
    recurringwork + tasktemplates -> TaskCreationPort -> task-owned creation

Project Templates
    projecttemplates -> ProjectCreationPort + TaskCreationPort -> owning-domain creation

Visual Workflows
    domain event / typed entry -> workflow runtime -> TaskAutomationMutationPort

Forms
    forms -> ProjectAccessPort + TaskCreationPort + WorkflowFormSubmissionPort

Project Simulation
    projectsimulation -> task/dependency source ports

Whiteboards
    whiteboards -> ProjectAccessPort + TaskCreationPort

Risk Radar
    projectrisk -> task/dependency source ports
```

The common principle is that a feature owns its orchestration/model while resource mutation and authorization remain with the resource-owning domain.

## Work generation and workflow boundaries

Recurring work and templates create ordinary work through task/project-owned ports, preserving normal actor validation, quotas, membership, audit and lifecycle behavior.

V50 workflow definitions are bounded typed acyclic graphs. Task lifecycle enters workflow execution through task-domain events; form submissions may enter through the typed `TRIGGER_FORM_SUBMITTED` contract. Workflow task actions use task-owned `TaskAutomationMutationPort` and re-check target authority.

V52 Forms owns project-scoped form definitions, fields and submissions. Forms never writes task/project/workflow persistence directly. Accepted submissions create tasks through `TaskCreationPort`; optional form-specific workflow dispatch occurs after commit through `WorkflowFormSubmissionPort`.

Detailed repository contracts: `guides/recurring_work_and_templates.md`, `guides/visual_workflow_builder.md` and `guides/forms_workflow_engine.md`.

## Advisory/project-tool domains

Project Simulation and Risk Radar consume authorized task/dependency state through narrow source contracts. Simulation has no hidden apply path; Risk Radar is read-only/explainable and does not perform employee productivity scoring.

Collaborative Whiteboard owns bounded persisted visual documents. Sticky/text node conversion to a real task crosses through task-owned creation behavior. The stored V51 document model is independent of optional future presence/cursor transport.

## Authorization

Authorization is permission- and scope-oriented rather than role-name-only. Explain Access and enforcement use the same evaluator. Delegated authority is bounded by a direct parent assignment and revalidated at access time.

Workflow, Forms and future Approval configuration never grants authority to target resources. Authorization is rechecked where exposure, human decision or mutation occurs.

See [[Authorization]].

## Tenancy and persistence

Production uses shared-schema tenancy. Tenant-owned data is accessed through tenant-scoped repository/query behavior and cross-tenant IDs must never be trusted without ownership validation.

Flyway owns schema evolution. Applied migrations are append-only. Portable common migrations currently extend through **V52**:

- V45 personal workspace favorites/recent items
- V46 saved views
- V47 task parent/dependency/project-label relationships
- V48 recurring task definitions/occurrences + project task templates
- V49 tenant project templates + bounded starter-task snapshots
- V50 workflow definitions/nodes/edges/executions
- V51 project whiteboards/nodes/connectors
- V52 project form definitions/fields/submissions

**V52 is immutable. New persistence starts at V53+.**

See [[Tenancy-and-Data-Model]] and [[PostgreSQL-and-Flyway]].

## Billing and integrations

Billing is provider-neutral at the application boundary. Stripe and Razorpay are provider implementations rather than architectural centers of the system. Normal lifecycle synchronization is webhook-authoritative and durable provider/application history supports reconciliation.

Tenant-configurable outbound webhooks use durable delivery/history/retry semantics and HMAC signing.

See [[Subscriptions-and-Quotas]].

## Next architecture direction — Approval Workflows

Approval Workflows should introduce an explicit human-decision domain rather than storing approval state inside task or workflow services.

Target composition:

```text
workflow runtime
      ↓ narrow approval entry/wait contract
approval domain
      ↓ reviewer eligibility/authorization contract
human decision
      ↓ outcome/resume contract or event
workflow runtime
      ↓ existing domain-owned mutation ports
```

The approval domain should own bounded definitions/stages, durable requests and immutable decision provenance. Reviewer eligibility must be revalidated at decision time. Configuration cannot grant authority. Racing/replayed decisions must be concurrency-safe/idempotent, and external/client approvals belong behind the later Client / Guest Portal boundary.

If persistence is needed, it begins at V53+.

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

The current strategy is PostgreSQL-first and evidence-driven: bound reads/generation/graphs/documents/forms, use database locking/constraints for correctness, add indexes for real query patterns, and measure before introducing distributed infrastructure.

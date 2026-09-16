# Multi-Tenant SaaS Platform

A production-oriented full-stack multi-tenant SaaS platform focused on tenant isolation, permission-oriented authorization, collaboration, subscription enforcement, external billing, durable integrations, enterprise OIDC SSO, PostgreSQL correctness, and an expanding work-management/product layer.

For current project status and next work, use **`CHECKPOINT.md`** and **`HANDOFF.md`** rather than this README. This file intentionally avoids volatile milestone detail.

## Platform capabilities

### Tenant plane

- secure tenant onboarding, workspace discovery, JWT/browser sessions, password recovery and invitations
- users and organization hierarchy
- scoped permission authorization with bounded delegation and Explain Access
- projects, tasks, priorities, due dates, assignees, comments, replies, mentions, pins and activity
- bounded subtasks, directed task dependencies and project-scoped task labels
- dedicated Task Planning workspace over authorization-safe task discovery
- recurring-task definitions/materialization with explicit timezone and idempotent occurrence tracking
- project-scoped reusable task templates
- tenant-scoped reusable project templates with bounded starter-task snapshots
- dedicated Work Automation workspace for recurring rules, task/project templates, visual workflows and internal Forms
- tenant-scoped Visual Workflow Builder with validated trigger/condition/action graphs, permission-aware task actions and execution history
- project-scoped internal Forms with bounded typed schemas, authenticated submissions, task creation through task-owned contracts and optional form-submitted workflow entry
- project-scoped Project Simulation / What-If workspace for advisory due-date, assignee and dependency scenarios with downstream conflict/workload impact
- project-scoped Collaborative Whiteboard with versioned persisted boards, draggable/resizable visual nodes/connectors, optimistic autosave/recovery and sticky/text-to-task conversion through task-owned creation
- project-scoped **Project Health / Risk Radar** with explainable overdue, blocked, stale, unassigned-critical and dependency-bottleneck signals, explicit bounds and no employee scoring
- permission-aware Global Search and capability-aware Command Palette
- server-backed Favorites + Recently Viewed with contextual favorite controls
- My Work personal attention queue and server-backed Saved Views
- capability-aware dashboard combining personal attention/context with tenant-wide health metrics
- authorization-safe Calendar / Deadline View over accessible task due dates
- S3/R2-compatible attachments
- durable in-app/email notifications and preferences
- subscription lifecycle enforcement, quotas and usage metering
- provider-backed checkout, cancellation, reconciliation and immutable subscription history
- tenant API keys restricted to external APIs with metering/plan limits
- tenant-configurable HMAC-signed outbound webhooks with retries/history/replay
- enterprise OIDC SSO with safe identity linking, optional/required policy, break-glass recovery and tenant-admin configuration

### System plane

- separate system-admin identity/control plane
- tenant, subscription-plan and subscription administration
- managed Stripe/Razorpay provider catalog provisioning
- billing event/history visibility and read-only provider reconciliation
- usage summaries and plan-level limits
- tenant/platform audit logs

System administrators are not tenant users with an elevated tenant role.

## Technology stack

**Backend:** Java 21, Spring Boot 4.x, Spring Security/JWT/OIDC, Spring Data JPA/Hibernate, Flyway, PostgreSQL 17, Testcontainers, AWS SDK v2 and Actuator/Micrometer.

**Frontend:** React 19, TypeScript 6, Vite 8, Material UI, React Router, TanStack React Query, Axios, React Hook Form/Zod, Vitest and Testing Library.

## Architecture

MultiTenantSAAS is intentionally a **modular monolith** with a separate web client.

```text
React / TypeScript
        ↓ HTTPS/JSON
Spring Boot modular monolith
        ↓
PostgreSQL + Flyway
        ↓
Stripe / Razorpay / Brevo / S3-compatible storage / tenant webhooks
```

The backend remains authoritative at every security, entitlement and domain-invariant boundary.

Full architecture: `guides/current_architecture.md`.

Engineering rules and the technical-debt register: `guides/ENGINEERING_STANDARDS.md`.

## Architecture rule for future development

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

This rule is part of the persistent repository contract in `AGENTS.md`.

## Major completed product foundations

- billing/catalog lifecycle with Stripe and Razorpay provider abstractions
- tenant-configurable outbound webhooks
- enterprise OIDC SSO / identity federation
- scoped authorization, bounded delegation and Explain Access
- collaboration, notifications, attachments, API keys, usage limits and auditability
- Global Search, Command Palette, Favorites/Recently Viewed, My Work and Saved Views
- capability-aware dashboard and Calendar / Deadline View
- Task Relationships + Task Planning
- recurring work and project/task templates through narrow creation ports
- Visual Workflow Builder through explicit workflow ownership, task-domain events and task-owned mutation contract
- Forms -> Workflow Engine through explicit `forms` ownership, task-owned creation and workflow-owned form-submission entry
- Project Simulation / What-If Engine through explicit `projectsimulation` ownership and narrow task/dependency sources
- Collaborative Whiteboard through explicit `whiteboards` ownership, optimistic concurrency and task-owned conversion
- Project Health / Risk Radar through explicit `projectrisk` ownership and narrow task/dependency sources

Stripe is the validated deployed Test Mode payment path. Razorpay application/catalog integration remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

## Database

Production schema evolution is owned by Flyway. Shared PostgreSQL migrations extend through **V52**. Never rewrite an applied migration; later persistence begins at **V53+**.

Recent product migrations include V45 personal-workspace favorites/recent items, V46 saved views, V47 task parent/dependency/label relationships, V48 recurring task definitions/occurrences plus project task templates, V49 tenant project templates with bounded starter-task snapshots, V50 visual workflow definitions/nodes/edges/executions, V51 project whiteboards/nodes/connectors, and V52 project form definitions/fields/submissions.

Project Simulation and Risk Radar are read-model/orchestration features and add no migration.

## Verification

GitHub Actions is the authoritative automated validation path. Applicable gates cover:

- repository hygiene
- backend Maven verification
- PostgreSQL/Flyway integration validation
- frontend formatting/tests/coverage/lint/build
- security scanning
- container validation
- Qodana/static analysis
- Wiki source validation when Wiki files change

## Deployment

- Frontend: `https://multitenantsaas-frontend.onrender.com`
- Backend: `https://multitenantsaas-akxn.onrender.com`
- Production profile: `SPRING_PROFILES_ACTIVE=postgres,production`

Use `.env.production.example` as the deployment-variable inventory. Never commit real keys or secrets.

## Documentation

- `CHECKPOINT.md` — current repository/application status
- `HANDOFF.md` — current resume instructions and next action
- `AGENTS.md` — persistent development/quality contract
- `guides/README.md` — documentation index and ownership policy
- `guides/current_architecture.md` — canonical technical architecture
- `guides/ENGINEERING_STANDARDS.md` — technical health, debt and engineering rules
- `guides/recurring_work_and_templates.md` — work-generation contracts
- `guides/visual_workflow_builder.md` — workflow graph/runtime/canvas contract
- `guides/forms_workflow_engine.md` — Forms intake/task/workflow contract
- `guides/project_simulation.md` — advisory What-If simulation contract
- `guides/collaborative_whiteboard.md` — whiteboard persistence/workspace contract
- `guides/project_risk_radar.md` — Risk Radar signal/bounds/UI contract
- focused guides — domain-specific behavior
- `guides/Wild_Thoughts.md` — product idea vault and committed differentiated sequence
- `wiki/*.md` — canonical source for the published reader-facing Wiki
- `wiki/Roadmap.md` — product direction and deferred milestones

The Wiki is automatically validated and published from merged `main` by `.github/workflows/wiki-sync.yml` using `scripts/publish-wiki.ps1`.

## Current product direction

Visual Workflow Builder, Project Simulation / What-If Engine, Collaborative Whiteboard, Project Health / Risk Radar and **Forms -> Workflow Engine are complete**. **Approval Workflows is active next**, followed by Client / Guest Portal, Team Workload Engine, Workspace Knowledge Graph and AI / Agent Teammates.

Live whiteboard presence/cursors remain a later optional collaboration enhancement. Bulk actions/CSV, broader custom fields, knowledge/documents and broader analytics remain parked backlog unless priorities are explicitly changed.

Production Operations & Disaster Recovery remains an important deferred milestone rather than the immediate development focus.

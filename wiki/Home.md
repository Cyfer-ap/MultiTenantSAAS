# MultiTenantSAAS Wiki

MultiTenantSAAS is a full-stack multi-tenant SaaS platform with tenant isolation, scoped authorization, collaboration, subscription enforcement, external billing, durable outbound integrations, enterprise OIDC SSO, bounded authorization delegation, Explain Access, usage metering, API keys and PostgreSQL-oriented production engineering.

Version-controlled Wiki source lives under `wiki/`. See [[Wiki-Maintenance]].

The Wiki intentionally avoids duplicating the repository's volatile current checkpoint. Repository-side `CHECKPOINT.md` owns current status and `HANDOFF.md` owns the current resume point.

## Platform state

Implemented foundations include:

- separate tenant and system-admin control planes
- JWT/browser sessions, invitations, password recovery and workspace discovery
- shared-schema tenant isolation and scoped permission authorization
- bounded one-level authorization delegation with runtime source revalidation
- Explain Access with direct/delegated matched-grant provenance
- organization hierarchy, projects/tasks/collaboration and R2/S3-compatible attachments
- bounded subtasks, directed dependencies and project-scoped task labels
- dedicated authorization-safe Task Planning workspace
- recurring-task definitions/materialization with explicit timezone and idempotent occurrence tracking
- project-scoped reusable task templates
- tenant-scoped reusable project templates with bounded starter-task snapshots
- Work Automation workspace with recurring work, task/project templates and visual workflows
- Visual Workflow Builder with bounded validated graphs, task-domain event runtime, permission-aware task actions and execution history
- Project Simulation / What-If Engine with advisory due-date/assignee/dependency scenarios, downstream conflict analysis and workload deltas
- permission-aware Global Search and capability-aware Command Palette
- server-backed Favorites + Recently Viewed with contextual favorite controls
- My Work personal attention queue and server-backed Saved Views
- capability-aware Dashboard combining personal attention/context with tenant-wide health metrics
- authorization-safe Calendar / Deadline View over accessible task due dates
- durable notifications/email delivery/preferences
- subscription lifecycle, quotas, API keys and usage metering
- provider-neutral billing with Stripe and Razorpay
- tenant-configurable HMAC-signed outbound webhooks with durable delivery/history/replay
- enterprise OIDC SSO with tenant IdP configuration, secure runtime, optional/required policy, break-glass recovery, browser completion, admin UX and audit events
- PostgreSQL 17, Flyway, Testcontainers, CI, security and container checks

## Architecture direction

MultiTenantSAAS remains an intentional modular monolith. Future features must follow the domain-boundary rule:

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Recurring work and task templates cross into task creation through the task-owned `TaskCreationPort`. Tenant project templates cross into project creation through `ProjectCreationPort` and create starter tasks through `TaskCreationPort`.

Visual workflows consume task-domain events and cross back into task mutation only through `TaskAutomationMutationPort`, which re-checks current authority. The workflow domain does not own task repositories or the full task service.

Project Simulation reads task and dependency snapshots through narrow domain-owned simulation source ports. It has no hidden mutation/apply path and does not inject the full task or graph service.

See [[Architecture]] for the current architecture and known debt.

## Database checkpoint

PostgreSQL Flyway migrations remain through **V50**. Project Simulation adds no persistence migration. Applied migrations remain append-only; new persistence begins at **V51+**.

Recent product migrations are V45 for personal-workspace favorites/recent items, V46 for saved views, V47 for task parent/dependency/label relationships, V48 for recurring task definitions/occurrences plus project task templates, V49 for tenant project templates plus bounded starter-task snapshots, and V50 for workflow definitions/nodes/edges/executions.

## Start here

- [[Architecture]]
- [[Security-and-Authentication]]
- [[Enterprise-SSO]]
- [[Authorization]]
- [[Tenancy-and-Data-Model]]
- [[Subscriptions-and-Quotas]]
- [[Projects-and-Tasks]]
- [[Notifications]]
- [[Testing-and-CI]]
- [[Roadmap]]
- [[Developer-Handoff]]

Repository-side `guides/Wild_Thoughts.md` is the living idea vault; `guides/ENGINEERING_STANDARDS.md` owns technical-health rules; `guides/task_relationships.md` owns task hierarchy/dependency/label semantics; `guides/recurring_work_and_templates.md` owns work-generation semantics; `guides/visual_workflow_builder.md` owns workflow graph/runtime/canvas semantics; and `guides/project_simulation.md` owns advisory What-If simulation semantics.

## Current product direction

Visual Workflow Builder and Project Simulation / What-If Engine are the first two completed items in the committed differentiated sequence. The next product feature is **Collaborative Whiteboard**, followed by Project Health / Risk Radar, Forms -> Workflow Engine, Approval Workflows, Client / Guest Portal, Team Workload Engine, Workspace Knowledge Graph and AI / Agent Teammates.

Bulk/CSV, custom fields, knowledge/documents and broader analytics remain parked backlog unless explicitly reprioritized.

Production Operations & Disaster Recovery remains important but deliberately deferred behind the current user-facing product sequence. See [[Roadmap]].

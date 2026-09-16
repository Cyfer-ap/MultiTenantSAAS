# MultiTenantSAAS Wiki

MultiTenantSAAS is a full-stack multi-tenant SaaS platform with tenant isolation, scoped authorization, collaboration, subscription enforcement, external billing, durable outbound integrations, enterprise OIDC SSO, bounded authorization delegation, Explain Access, usage metering, API keys and PostgreSQL-oriented production engineering.

Version-controlled Wiki source lives under `wiki/`. See [[Wiki-Maintenance]]. Repository-side `CHECKPOINT.md` owns current status and `HANDOFF.md` owns the current resume point.

## Platform state

Implemented foundations include:

- separate tenant and system-admin control planes
- JWT/browser sessions, invitations, password recovery and workspace discovery
- shared-schema tenant isolation and scoped permission authorization
- bounded one-level authorization delegation with runtime source revalidation
- Explain Access with direct/delegated matched-grant provenance
- organization hierarchy, projects/tasks/collaboration and R2/S3-compatible attachments
- Task Relationships + Task Planning
- recurring work and project/task templates
- Work Automation + Visual Workflow Builder
- **Forms -> Workflow Engine** with bounded authenticated internal intake, task creation and optional workflow entry
- Project Simulation / What-If Engine
- Collaborative Whiteboard with persisted versioned boards, optimistic recovery and node -> task conversion
- Project Health / Risk Radar with explainable bounded project-health signals and no employee scoring
- Global Search, Command Palette, Favorites/Recently Viewed, My Work and Saved Views
- capability-aware Dashboard and Calendar / Deadline View
- durable notifications/email delivery/preferences
- subscription lifecycle, quotas, API keys and usage metering
- Stripe/Razorpay provider-neutral billing
- tenant-configurable signed outbound webhooks with durable history/replay
- enterprise OIDC SSO with tenant IdP configuration and break-glass recovery
- PostgreSQL 17, Flyway, Testcontainers, CI, security and container checks

## Architecture direction

MultiTenantSAAS remains an intentional modular monolith.

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Recurring work/templates cross through task-owned `TaskCreationPort` and project-owned `ProjectCreationPort`. Visual workflows consume typed domain entries/events and mutate through `TaskAutomationMutationPort`. Forms create work through `TaskCreationPort` and enter workflows through `WorkflowFormSubmissionPort`. Project Simulation and Risk Radar read task/dependency state through narrow source contracts. Collaborative Whiteboard crosses project/task boundaries through project-owned access and task-owned creation ports.

See [[Architecture]] for the current architecture and known debt.

## Database checkpoint

PostgreSQL Flyway migrations extend through **V52**. Applied migrations remain append-only; later persistence begins at **V53+**.

V50 owns workflow persistence, V51 owns project whiteboards/nodes/connectors and V52 owns project form definitions/fields/submissions. Project Simulation and Risk Radar add no migration.

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

Repository-side focused guides own detailed contracts, including `guides/visual_workflow_builder.md`, `guides/forms_workflow_engine.md`, `guides/project_simulation.md`, `guides/collaborative_whiteboard.md` and `guides/project_risk_radar.md`.

## Current product direction

Visual Workflow Builder, Project Simulation / What-If Engine, Collaborative Whiteboard, Project Health / Risk Radar and **Forms -> Workflow Engine are complete**. **Approval Workflows is active next**, followed by Client / Guest Portal, Team Workload Engine, Workspace Knowledge Graph and AI / Agent Teammates.

Live whiteboard presence/cursors remain a later optional enhancement. Bulk/CSV, broader custom fields, knowledge/documents and broader analytics remain parked backlog unless explicitly reprioritized.

Production Operations & Disaster Recovery remains important but deliberately deferred behind the current user-facing product sequence. See [[Roadmap]].

# MultiTenantSAAS Guides

This directory contains focused technical guides. It is **not** a second checkpoint/handoff system.

## Documentation ownership

Use one source of truth per kind of information:

- `../readme.md` — stable public platform overview
- `../CHECKPOINT.md` — current repository/application status
- `../HANDOFF.md` — current resume instructions and next action
- `../AGENTS.md` — persistent development/quality contract
- `current_architecture.md` — canonical technical architecture
- `ENGINEERING_STANDARDS.md` — technical-health assessment, debt register and engineering rules
- `Wild_Thoughts.md` — audited idea vault; Section 1.3 records the committed differentiated sequence
- `DEFERRED_PLATFORM_WORK.md` — deliberately deferred operational/platform work
- focused guides below — detailed domain contracts and operational notes
- `../wiki/*.md` — canonical source for the published reader-facing Wiki
- `../wiki/Roadmap.md` — product direction and deferred milestones

Do **not** create another checkpoint, handoff, progress mirror, package-status manifest or milestone-summary document unless a genuinely different consumer requires it.

When a fact changes, update the document that owns that fact rather than copying the update into every guide.

## Read first for development

1. `../AGENTS.md`
2. `../CHECKPOINT.md`
3. `../HANDOFF.md`
4. `current_architecture.md`
5. `ENGINEERING_STANDARDS.md`
6. the focused guide for the domain being changed

## Core architecture and platform guides

- `current_architecture.md` — current modular-monolith architecture and system boundaries
- `ENGINEERING_STANDARDS.md` — mandatory modularity rules, technical debt and quality gates
- `authorization_model.md` — scoped authorization, delegation and Explain Access
- `enterprise-sso-foundation.md` — OIDC SSO architecture, administration, deployment and testing
- `subscription_billing.md` — subscription and provider lifecycle
- `collaboration_and_notifications.md` — collaboration/notification behavior
- `task_relationships.md` — subtasks, directed dependencies, project-scoped labels and Task Planning ownership
- `recurring_work_and_templates.md` — V48 recurring work/task-template contracts, V49 tenant project templates, narrow creation ports and the Work Automation & Templates workspace
- `visual_workflow_builder.md` — V50 workflow graph, runtime/event boundaries, task mutation port, execution history and visual canvas contract
- `project_simulation.md` — advisory What-If baseline/simulation APIs, narrow source ports, scenario validation and private frontend workspace
- `collaborative_whiteboard.md` — V51 project-scoped board/document persistence, optimistic concurrency, project/task ports, persisted visual workspace and node-to-task conversion contract
- `project_risk_radar.md` — explainable Risk Radar signal model, bounds, narrow task/dependency sources, guardrails and project-facing read-only surface
- `data_model.md` — data-model notes
- `postgresql_and_migrations.md` — PostgreSQL/Flyway behavior
- `outbound-webhook-events.md` — outbound event contract
- `outbound-webhook-delivery-history.md` — durable deliveries/attempts/replay
- `outbound-webhook-admin-ux.md` — tenant Integrations UX

## Planning material

- `Wild_Thoughts.md` — broad product/experiment vault plus committed differentiated sequence in Section 1.3
- `DEFERRED_PLATFORM_WORK.md` — important work intentionally postponed

Historical planning/recovery files may remain for provenance, but they are not current specifications. Code, tests, migrations and the canonical documents above take precedence.

## Current direction

The committed differentiated sequence is complete through **Project Health / Risk Radar (#148)**. Risk Radar is an explainable, advisory project-health read model over authorized task/dependency state and adds no persistence migration.

Portable migrations extend through **V51**. V51 is immutable; later persistence starts at **V52+**.

**Forms -> Workflow Engine is the active next feature.** After that continue with Approval Workflows, Client / Guest Portal, Team Workload Engine, Workspace Knowledge Graph and AI / Agent Teammates.

Live whiteboard presence/cursors remain a later optional collaboration enhancement. Bulk actions/CSV, custom fields, broader knowledge/documents and analytics remain valuable but parked behind the committed differentiated sequence unless explicitly reprioritized.

Production Operations & Disaster Recovery remains important but intentionally deferred until after the current user-facing product sequence.

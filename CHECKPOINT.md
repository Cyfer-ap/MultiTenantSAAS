# MultiTenantSAAS — Current Checkpoint

Updated: 2026-09-15
Repository: `Cyfer-ap/MultiTenantSAAS`
Branch target: `main` after PR #145 merge

This file is the **single repository-side source of truth for current project status**. Do not create additional progress/checkpoint mirrors.

## Current phase

**Differentiated Work Platform Sequence**

Delivered product milestones include:

- Global Search — #129
- Command Palette — #130
- Favorites + Recently Viewed — #131/#132
- My Work — #133
- Saved Views — #134
- capability-aware Dashboard Refresh — #136
- Calendar / Deadline View — #137/#138
- Task Relationships + Task Planning — #139/#140
- recurring-task + project-scoped task-template foundation — #141
- tenant-scoped project templates + Work Automation & Templates workspace — #143
- Visual Workflow Builder — #144
- Project Simulation / What-If Engine — #145

The **Project Simulation / What-If Engine milestone is implementation-complete in #145** and ready for merge once the final PR head remains green.

## Project simulation checkpoint

### Backend ownership and contract

The explicit `projectsimulation` domain owns advisory scenario orchestration. It does not mutate live project/task/dependency state.

API surface:

```text
GET  /api/tenants/{tenantId}/projects/{projectId}/simulation/baseline
POST /api/tenants/{tenantId}/projects/{projectId}/simulation
```

Both endpoints require project-level `project.task.manage` authority.

The simulation coordinator reads authoritative state only through narrow domain-owned ports:

```text
projectsimulation
    -> ProjectSimulationTaskSource -> task-owned adapter
    -> ProjectSimulationDependencySource -> Task Relationships-owned adapter
```

It deliberately does not inject `ProjectTaskService` or `TaskGraphService`.

### Current simulation model

A scenario can evaluate hypothetical:

- task due-date changes
- task assignee changes
- dependency additions
- dependency removals

The engine reports:

- direct task changes
- downstream tasks exposed through dependencies
- baseline vs simulated dependency deadline conflicts
- new/resolved conflicts
- assignee open-task workload deltas
- reassignment counts

Guardrails:

- maximum 500 project tasks
- maximum 1,000 dependency edges
- maximum 100 task overrides and 100 dependency changes per request
- unknown tasks and invalid project assignees are rejected
- self-dependencies, duplicate changes and cyclic simulated graphs are rejected
- no persistence/apply path exists
- no project completion date is fabricated because the current task model has no duration/effort field

### Frontend

The private project route is:

```text
/projects/:projectId/simulation
```

The What-If Simulator loads the project baseline, allows one task override plus one dependency change in the first UI slice, and displays direct/downstream impact, dependency conflicts and workload deltas. The UI is explicitly advisory and exposes no apply action.

Detailed contract: `guides/project_simulation.md`.

## Visual workflow checkpoint

### Definition model

- explicit `workflows` backend domain
- tenant-scoped workflow catalog with normalized unique names
- `DRAFT`, `ACTIVE`, `PAUSED` lifecycle
- stable node keys and persisted canvas coordinates
- graph bounded to 2–50 nodes and 1–100 edges
- exactly one trigger
- DAG and reachability validation
- trigger/action `DEFAULT` branches; condition `TRUE`/`FALSE` branches
- typed configuration only; no arbitrary executable code
- active definitions must be paused before editing
- definition version increments when edited

Initial operations:

- task-created trigger
- task-status-changed trigger
- task-priority-equals condition
- task-status-equals condition
- set-task-priority action
- set-task-status action

### Runtime and cross-domain boundaries

Task lifecycle changes reach workflows through `tasks/events` domain events. Workflow execution does not inject the full task service.

Automated mutations cross into the task domain only through `tasks/automation/TaskAutomationMutationPort`, where the originating actor's current project/task authority is re-checked before mutation.

Runtime behavior:

- task events are consumed after the originating transaction commits
- only ACTIVE workflows are considered
- matching trigger -> deterministic condition/action traversal
- one execution row per `(tenant, workflow, event)` for idempotency
- outcomes: `RUNNING`, `SUCCEEDED`, `FAILED`, `SKIPPED`
- execution history stores workflow version, trigger, source entity, explanation/error and timestamps
- workflow-driven task mutations do not recursively publish new workflow events in v1, preventing accidental feedback loops

### Frontend

`/work-automation` owns four surfaces:

- recurring work
- task templates
- project templates
- Workflow builder

The workflow tab provides a dependency-free draggable node canvas, connection rendering, node inspector, branch-target editing, save/activate/pause lifecycle and tenant-wide recent execution history. Active workflows are read-only until paused.

Detailed workflow contract: `guides/visual_workflow_builder.md`.

## Existing work-generation checkpoint

Recurring work and templates remain unchanged from #143:

- V48 recurring definitions/occurrences + project task templates
- V49 tenant project templates + bounded starter-task snapshots
- recurring/task-template creation goes through task-owned `TaskCreationPort`
- project-template creation goes through project-owned `ProjectCreationPort`
- project-template starter tasks go through `TaskCreationPort`
- ordinary project/task quota, actor, authorization, audit and lifecycle rules remain authoritative
- snapshot/copy semantics; later template edits never rewrite instantiated work

Detailed contract: `guides/recurring_work_and_templates.md`.

## Database checkpoint

Portable PostgreSQL Flyway migrations extend through **V50**.

```text
V45 personal workspace favorites/recent items
V46 saved views
V47 task parent/dependency/project-label relationships
V48 recurring task definitions/occurrences + project task templates
V49 tenant project templates + bounded starter-task snapshots
V50 visual workflow definitions/nodes/edges/executions
```

Project Simulation adds no persistence migration. Never rewrite an applied migration. New persistence starts at **V51+**.

## Non-negotiable architecture rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Current reference implementations include:

- Search coordinator + contributor contracts
- Personal Workspace resolver adapters
- My Work task source
- Saved Views validator SPI
- Calendar deadline-source SPI
- Task Relationships gateway/change sink
- `TaskCreationPort`
- `ProjectCreationPort`
- workflow task-domain events + `TaskAutomationMutationPort`
- Project Simulation task/dependency source ports

Do not move workflow execution into `ProjectTaskService`, recurrence into Calendar, simulation into legacy project/task services, or template orchestration into legacy project/task god-services.

## Established application foundations

Major capabilities now include authentication/tenant isolation, invitations/password recovery, organization hierarchy, scoped authorization/delegation/Explain Access, projects/tasks/collaboration, Task Planning, recurring work/templates, visual workflows, project simulation, search/command palette/personal workspace/My Work/Saved Views/Dashboard/Calendar, R2-compatible attachments, durable notifications/email, API keys/quotas/usage, Stripe/Razorpay billing abstractions, outbound webhooks, enterprise OIDC SSO, auditability, PostgreSQL/Flyway, CI/security/container validation.

## Provider status

### Stripe

Working and validated in deployed Test Mode. Hosted checkout, signed lifecycle synchronization, provider-side cancellation, reconciliation and managed Product/Price provisioning are implemented.

### Razorpay

Application integration and managed Plan provisioning remain implemented. Recurring Test Mode authorization is provider-sandbox blocked; keep the integration available while treating live readiness separately.

## Engineering-health checkpoint

The codebase remains feasible for continued development without a rewrite if new work preserves explicit domains and narrow boundaries.

Priority debt remains:

1. older inconsistent backend package/domain boundaries
2. large legacy application services with accumulated orchestration dependencies
3. older tenant-isolation paths relying partly on repository/query discipline
4. duplicated backend/frontend API contracts
5. growing route/navigation aggregation points
6. comprehensive scale/load characterization still missing
7. deferred operational maturity: backup/restore drills, recovery/load validation and production R2 verification

Canonical assessment: `guides/ENGINEERING_STANDARDS.md`.

## Next committed product sequence

With features #1 and #2 complete, continue the committed differentiated sequence:

1. **Collaborative Whiteboard**
2. Project Health / Risk Radar
3. Forms -> Workflow Engine
4. Approval Workflows
5. Client / Guest Portal
6. Team Workload Engine
7. Workspace Knowledge Graph
8. AI / Agent Teammates
9. resume parked backlog such as bulk actions/CSV, custom fields, knowledge/documents and broader analytics unless priorities are explicitly changed

For Collaborative Whiteboard, keep canvas/document ownership explicit and convert whiteboard objects into real project/task records only through narrow project/task creation contracts. Do not turn the whiteboard module into another project/task god-service.

## Deferred platform work

Production Operations & Disaster Recovery remains important but intentionally deferred from the immediate product sequence:

- PostgreSQL backup/restore drills
- health/readiness/metrics review and alerting
- incident/recovery runbooks
- broader failure-recovery/load validation
- production R2 verification

Optional SAML/SCIM, MFA/passkeys/device management and richer notification channels remain demand-driven.

## Documentation ownership

- `CHECKPOINT.md` — current status
- `HANDOFF.md` — current resume/next action
- `AGENTS.md` — persistent engineering contract
- `guides/README.md` — documentation ownership/index
- `guides/current_architecture.md` — canonical architecture
- `guides/ENGINEERING_STANDARDS.md` — technical-health assessment and quality rules
- `guides/task_relationships.md` — task relationship contract
- `guides/recurring_work_and_templates.md` — recurrence/template contract
- `guides/visual_workflow_builder.md` — workflow definition/runtime/canvas contract
- `guides/project_simulation.md` — advisory What-If simulation contract
- `guides/Wild_Thoughts.md` — product idea vault and committed sequence section
- `wiki/*.md` — canonical reader-facing Wiki source
- `wiki/Roadmap.md` — product direction

Do not recreate duplicate checkpoint/progress/manifests.

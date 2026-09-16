# MultiTenantSAAS — Current Checkpoint

Updated: 2026-09-16
Repository: `Cyfer-ap/MultiTenantSAAS`
Branch target: `main` after PR #147 merge

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
- Collaborative Whiteboard backend/domain foundation — #146
- Collaborative Whiteboard persisted visual workspace — #147

**Project Health / Risk Radar is the active next feature.** Collaborative Whiteboard is complete at the persisted application-workspace level through #146/#147. Live presence/cursors remain a later optional collaboration enhancement and do not block the committed sequence.

## Collaborative Whiteboard checkpoint — #146/#147

### Ownership and boundaries

The explicit `whiteboards` domain owns project-scoped board documents, visual nodes, connectors and node-to-task links. Frontend ownership is localized under `features/whiteboards`.

Cross-domain calls remain narrow:

```text
whiteboards
    -> ProjectAccessPort -> project-owned adapter -> project existence/lifecycle
    -> TaskCreationPort  -> task-owned adapter    -> real task creation
```

Whiteboards do not inject `ProjectService`, `ProjectTaskService`, `ProjectRepository` or `ProjectTaskRepository`.

### V51 document model

V51 is merged and creates:

- `whiteboards`
- `whiteboard_nodes`
- `whiteboard_edges`

Rules:

- boards are tenant/project scoped
- board names are normalized and unique within a project
- board rows use optimistic `version` control
- initial node types: `STICKY`, `TEXT`, `SHAPE`
- node keys are stable identifiers
- node geometry/z-index are bounded
- at most 300 nodes and 600 connectors per submitted board document
- connectors must reference existing nodes, cannot self-reference and cannot duplicate a directed edge
- visual connector cycles are valid; whiteboards are not task dependency DAGs
- sticky/text nodes may persist a `linked_task_id`

### API and concurrency

```text
GET    /api/tenants/{tenantId}/projects/{projectId}/whiteboards
POST   /api/tenants/{tenantId}/projects/{projectId}/whiteboards
GET    /api/tenants/{tenantId}/projects/{projectId}/whiteboards/{boardId}
PUT    /api/tenants/{tenantId}/projects/{projectId}/whiteboards/{boardId}
DELETE /api/tenants/{tenantId}/projects/{projectId}/whiteboards/{boardId}?expectedVersion={version}
POST   /api/tenants/{tenantId}/projects/{projectId}/whiteboards/{boardId}/nodes/{nodeKey}/convert-to-task
```

Reads reuse project task-read authorization. Mutations reuse project task-manage authorization; exact project-lead membership remains a valid resource relationship through the existing authorization model.

Update/delete/task-conversion requests carry the version the client edited. A stale version returns structured HTTP `409 Conflict`; duplicate board names also resolve to `409 RESOURCE_ALREADY_EXISTS`, including database-race conflicts.

Archived projects remain readable but reject board mutation and task conversion.

### Visual workspace — #147

Private route:

```text
/projects/:projectId/whiteboards
```

Project details expose an **Open whiteboard** entry control through a whiteboard-owned wrapper rather than adding more responsibilities directly to the legacy project-details page.

The workspace provides:

- board selector/create/delete
- draggable/resizable sticky, text and shape nodes
- directed connectors
- zoom/pan
- Ctrl/Cmd multi-select
- local undo/redo
- board/node inspector
- committed-edit autosave using the V51 expected version
- explicit reload after stale/conflicting saves
- read-only behavior for archived projects/users without edit authority
- direct project-lead relationship lookup when no explicit task-manage permission exists

The editor does not send a request for every pointer movement. Position/size changes are persisted when the interaction commits. It also avoids server-to-local `useEffect` synchronization; deliberate board switches/reloads remount from the authoritative snapshot.

### Node -> task conversion

Sticky/text conversion uses task-owned `TaskCreationPort` rather than writing task persistence directly. Normal task lifecycle behavior therefore remains authoritative for project status, creator/assignee membership, activity, audit, notifications, outbound webhooks and task-domain events.

The conversion UX supports title, description, priority, active project-member assignee and due date. A converted node stores the returned task ID, rejects repeated conversion and retains the link across document replacement when its stable node key remains. Frontend task-cache invalidation uses the task domain's exported query-key contract.

### Validation

Focused frontend coverage includes:

- board creation for explicit project-task managers
- project-lead management fallback without an explicit task-manage grant
- archived-project read-only behavior
- autosave with the current expected version
- node -> task conversion and returned task/version state

The #147 implementation head cleared all 273 frontend tests, formatting, lint and production build together with Backend, PostgreSQL/Flyway, Security, Container CI and Qodana before the final documentation-only head was created.

### Transport guardrail

V51/#147 contain **no WebSocket/STOMP/presence/cursor persistence**. The stored board model is transport-independent. Presence, cursors and real-time resynchronization can be added later without changing the persisted document contract.

Detailed contract: `guides/collaborative_whiteboard.md`.

## Project Simulation checkpoint

The explicit `projectsimulation` domain owns advisory scenario orchestration and never mutates live project/task/dependency state.

```text
GET  /api/tenants/{tenantId}/projects/{projectId}/simulation/baseline
POST /api/tenants/{tenantId}/projects/{projectId}/simulation
```

Both require project-level `project.task.manage` authority. Authoritative task/dependency state crosses through `ProjectSimulationTaskSource` and `ProjectSimulationDependencySource`, not legacy task/graph services.

Current scenarios support due-date/assignee overrides and dependency add/remove operations, with direct/downstream exposure, baseline-vs-simulated deadline conflicts and open-task workload deltas. The engine is bounded to 500 tasks, 1,000 dependency edges, 100 task overrides and 100 dependency changes per request. It remains advisory with no hidden apply path and does not invent completion dates without duration/effort data.

Frontend route: `/projects/:projectId/simulation`.

Detailed contract: `guides/project_simulation.md`.

## Visual workflow checkpoint

Visual Workflow Builder remains complete through #144 with V50 workflow definitions/nodes/edges/executions, bounded typed graphs, after-commit task-domain events, idempotent execution history and task mutations through `TaskAutomationMutationPort`.

The `/work-automation` Workflow builder tab provides the draggable persisted canvas, branch editing, lifecycle controls and recent execution history. Active definitions remain read-only until paused.

Detailed contract: `guides/visual_workflow_builder.md`.

## Existing work-generation checkpoint

Recurring work/templates remain unchanged from #143:

- V48 recurring definitions/occurrences + project task templates
- V49 tenant project templates + bounded starter-task snapshots
- recurring/task-template generation goes through `TaskCreationPort`
- project-template creation goes through `ProjectCreationPort`
- project-template starter tasks go through `TaskCreationPort`
- ordinary project/task quota, actor, authorization, audit and lifecycle rules remain authoritative

Detailed contract: `guides/recurring_work_and_templates.md`.

## Database checkpoint

Portable PostgreSQL Flyway migrations extend through **V51**.

```text
V45 personal workspace favorites/recent items
V46 saved views
V47 task parent/dependency/project-label relationships
V48 recurring task definitions/occurrences + project task templates
V49 tenant project templates + bounded starter-task snapshots
V50 visual workflow definitions/nodes/edges/executions
V51 project whiteboards/nodes/connectors
```

Project Simulation and #147 add no migration. Never rewrite an applied migration. New persistence starts at **V52+**.

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
- Whiteboard `ProjectAccessPort` + `TaskCreationPort` boundaries

Do not move workflow execution into `ProjectTaskService`, recurrence into Calendar, simulation/whiteboards into legacy project/task services, or template orchestration into legacy project/task god-services.

## Established application foundations

Major capabilities include authentication/tenant isolation, invitations/password recovery, organization hierarchy, scoped authorization/delegation/Explain Access, projects/tasks/collaboration, Task Planning, recurring work/templates, visual workflows, project simulation, project whiteboard workspace, search/command palette/personal workspace/My Work/Saved Views/Dashboard/Calendar, R2-compatible attachments, durable notifications/email, API keys/quotas/usage, Stripe/Razorpay billing abstractions, outbound webhooks, enterprise OIDC SSO, auditability, PostgreSQL/Flyway and CI/security/container validation.

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

**Project Health / Risk Radar is active next.** It must remain explainable/advisory, use explicit project/task/dependency/workload projections through narrow ports, and must not become opaque employee scoring.

Continue in this order:

1. **Project Health / Risk Radar**
2. Forms -> Workflow Engine
3. Approval Workflows
4. Client / Guest Portal
5. Team Workload Engine
6. Workspace Knowledge Graph
7. AI / Agent Teammates
8. resume parked backlog such as bulk actions/CSV, custom fields, knowledge/documents and broader analytics unless priorities are explicitly changed

Live whiteboard presence/cursors remain a later optional enhancement, not an item that blocks the sequence above.

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
- `guides/collaborative_whiteboard.md` — V51 whiteboard persistence/domain/visual-workspace/task-conversion contract
- `guides/Wild_Thoughts.md` — product idea vault and committed sequence section
- `wiki/*.md` — canonical reader-facing Wiki source
- `wiki/Roadmap.md` — product direction

Do not recreate duplicate checkpoint/progress/manifests.

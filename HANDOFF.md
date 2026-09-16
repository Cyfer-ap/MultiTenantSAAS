# MultiTenantSAAS — Development Handoff

Updated: 2026-09-16

This is the **single repository-side resume document**. Current status lives in `CHECKPOINT.md`; architecture/quality rules live in `AGENTS.md` and `guides/ENGINEERING_STANDARDS.md`.

## Read first

1. `AGENTS.md`
2. `CHECKPOINT.md`
3. `guides/current_architecture.md`
4. `guides/ENGINEERING_STANDARDS.md`
5. `guides/task_relationships.md`
6. `guides/recurring_work_and_templates.md`
7. `guides/visual_workflow_builder.md`
8. `guides/project_simulation.md`
9. `guides/collaborative_whiteboard.md`
10. `guides/Wild_Thoughts.md`
11. the focused guide for the domain being changed
12. `wiki/Roadmap.md` when planning product direction

## Current state

Major milestones are complete through the persisted Collaborative Whiteboard workspace:

- billing/catalog — #106
- tenant outbound webhooks — #112
- enterprise OIDC SSO — #119
- authorization delegation + Explain Access — #125/#126
- product vision/documentation governance — #127/#128
- Global Search — #129
- Command Palette — #130
- Favorites + Recently Viewed — #131/#132
- My Work — #133
- Saved Views — #134
- Dashboard Refresh — #136
- Calendar / Deadline View — #137/#138
- Task Relationships + Task Planning — #139/#140
- recurring-task + project-scoped task-template foundation — #141
- documentation checkpoint — #142
- tenant project templates + Work Automation & Templates — #143
- Visual Workflow Builder — #144
- Project Simulation / What-If Engine — #145
- Collaborative Whiteboard backend/persistence foundation — #146
- Collaborative Whiteboard project-facing visual workspace — #147

Portable PostgreSQL Flyway migrations extend through **V51**. Never modify V51 or earlier after merge/application; later persistence starts at **V52+**.

Stripe remains the validated deployed Test Mode billing path. Razorpay integration/catalog provisioning remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

## Architecture rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Backend features should use explicit domain packages. Frontend features should preserve locality under `features/<domain>/...`.

Do not expand `ProjectTaskService` or `ProjectService` merely because a new feature reads or changes projects/tasks.

## Collaborative Whiteboard checkpoint

V51 owns:

- `whiteboards`
- `whiteboard_nodes`
- `whiteboard_edges`

Boundary:

```text
whiteboards
    -> ProjectAccessPort -> project-owned adapter -> project existence/lifecycle
    -> TaskCreationPort  -> task-owned adapter    -> real task creation
```

Preserve these rules:

- project-scoped + tenant-scoped board access
- board names unique within a project after normalization
- node types `STICKY`, `TEXT`, `SHAPE`
- max 300 nodes / 600 connectors per submitted document
- stable node keys and bounded position/size/z-index
- connectors reference existing nodes, reject self/duplicates and may form visual cycles
- board-level optimistic versioning guards update/delete/task-conversion
- stale versions return HTTP 409 with expected/current version details
- duplicate board names return 409, including database-race conflicts
- archived projects remain readable but reject whiteboard mutation
- whiteboard domain does not inject project/task services or repositories
- sticky/text -> task conversion crosses only through `TaskCreationPort`
- converted nodes persist `linked_task_id` and cannot convert twice
- linked task IDs survive document replacement when the stable node key remains
- no WebSocket/STOMP/presence/cursor state is persisted in V51

Project-facing workspace:

```text
/projects/:projectId/whiteboards
```

The #147 UI provides board list/create/delete, drag/resize, connectors, pan/zoom, multi-select, local undo/redo, optimistic autosave/reload recovery, project-lead management fallback and sticky/text -> task conversion. It keeps pointer movement local and persists only committed edits.

Live cursors/presence remain a later optional collaboration slice; the persisted document contract is transport-independent.

Detailed rules: `guides/collaborative_whiteboard.md`.

## Project Simulation checkpoint

```text
projectsimulation
    -> ProjectSimulationTaskSource -> task-owned adapter
    -> ProjectSimulationDependencySource -> Task Relationships-owned adapter
```

API:

```text
GET  /api/tenants/{tenantId}/projects/{projectId}/simulation/baseline
POST /api/tenants/{tenantId}/projects/{projectId}/simulation
```

Simulation remains advisory/read-only: due-date/assignee/dependency what-if changes, downstream exposure, dependency conflicts and workload deltas, with bounded inputs and no hidden apply path.

Frontend route: `/projects/:projectId/simulation`.

Detailed contract: `guides/project_simulation.md`.

## Visual Workflow Builder checkpoint

V50 owns workflow definitions/nodes/edges/executions. Task lifecycle reaches workflows through task-domain events; automated mutations cross through task-owned `TaskAutomationMutationPort`. Execution remains idempotent and auditable, and workflow-generated task mutations remain non-recursive in v1.

Detailed rules: `guides/visual_workflow_builder.md`.

## Existing work-generation boundaries to preserve

```text
recurringwork ───────────┐
                         ├─> TaskCreationPort -> task-owned adapter -> normal task lifecycle
project task templates ──┘

projecttemplates -> ProjectCreationPort -> project-owned adapter -> normal project lifecycle
       │
       └────────> TaskCreationPort -> task-owned adapter -> starter tasks
```

V48/V49 recurrence/template semantics remain documented in `guides/recurring_work_and_templates.md`. Calendar remains deadline projection. Task Relationships remains hierarchy/dependency/label ownership.

## Resume here — Project Health / Risk Radar

The next committed feature is **Project Health / Risk Radar**.

Initial scope should be an explicit risk/health domain, not another set of methods added to project/task god-services.

Recommended first slice:

1. define a project-risk domain and stable risk signal/result DTOs
2. read authorized project/task/dependency state through narrow projection/source ports
3. calculate explainable signals for overdue work, blockers, stale open work and dependency criticality
4. add bounded workload-pressure context only from explicit assignments/capacity data; no employee scoring
5. return signal contributions/reasons rather than an opaque magic score
6. keep the feature advisory/read-only initially
7. expose a project-facing Risk Radar workspace/card with drill-down to contributing work
8. add focused deterministic tests for signal calculation, authorization and bounded traversal

Guardrails:

- do not infer employee productivity or rank people
- do not mutate project/task state from risk calculation
- do not fetch broad tenant data and filter after scoring
- risk explanations must identify concrete contributing signals/entities
- graph traversal remains bounded/cycle-safe
- use current authoritative authorization before exposure

After Risk Radar, continue:

1. Forms -> Workflow Engine
2. Approval Workflows
3. Client / Guest Portal
4. Team Workload Engine
5. Workspace Knowledge Graph
6. AI / Agent Teammates
7. resume parked backlog such as bulk/CSV, custom fields, knowledge/documents and broader analytics unless reprioritized

## Validation before merge

For every slice require applicable green gates on the **final current head**:

- Repository Hygiene
- Backend build/test/verify
- PostgreSQL/Flyway
- Frontend format/tests/coverage/lint/build when frontend changes
- Security
- Container CI
- Qodana
- Wiki Sync when Wiki source changes

Do not merge around failed gates. If Auto Format creates a bot-authored head, verify the formatting and follow it with a human commit so normal PR workflows run on the final feature state.

## Preserve these system invariants

- tenant isolation precedes resource access
- backend authorization is authoritative
- automated workflows never grant authority
- stored personal/workflow/whiteboard/risk definitions do not bypass resource authorization
- Calendar/Task Planning/Automation/Simulation/Whiteboard/Risk data is authorized before exposure
- simulation never mutates live state implicitly
- risk analysis remains advisory until a separately authorized human action exists
- whiteboard optimistic concurrency never silently overwrites newer state
- generated tasks go through task-owned creation behavior
- graph traversal and batch/document work remain bounded
- Explain Access and enforcement share authorization semantics
- delegated authority never exceeds current direct source authority
- public APIs expose DTOs rather than persistence entities
- provider secrets remain server-side

## Deferred platform work

Production Operations & Disaster Recovery remains deliberately deferred behind the committed product sequence. It still includes restore drills, alert/runbook work, broader load/failure-recovery validation and production R2 verification.

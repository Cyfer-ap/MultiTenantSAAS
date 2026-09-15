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

Major milestones are complete through Project Simulation / What-If Engine, and Collaborative Whiteboard foundation is being established in #146:

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
- Collaborative Whiteboard backend/persistence foundation — #146 once merged green

After #146 merges, portable PostgreSQL Flyway migrations extend through **V51**. Never modify V51 or earlier after it is merged/applied; later persistence starts at **V52+**.

Stripe remains the validated deployed Test Mode billing path. Razorpay integration/catalog provisioning remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

## Architecture rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Backend features should use explicit domain packages. Frontend features should preserve locality under `features/<domain>/...`.

Do not expand `ProjectTaskService` or `ProjectService` merely because a new feature reads or changes projects/tasks.

## Collaborative Whiteboard foundation checkpoint

V51 owns:

- `whiteboards`
- `whiteboard_nodes`
- `whiteboard_edges`

Backend ownership:

```text
whiteboards
    -> ProjectAccessPort -> project-owned adapter -> project existence/lifecycle
    -> TaskCreationPort  -> task-owned adapter    -> real task creation
```

Preserve these rules:

- project-scoped + tenant-scoped board access
- board names unique within a project after normalization
- initial nodes: `STICKY`, `TEXT`, `SHAPE`
- max 300 nodes / 600 connectors per submitted document
- stable node keys
- bounded position/size/z-index
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

Foundation API:

```text
GET    /api/tenants/{tenantId}/projects/{projectId}/whiteboards
POST   /api/tenants/{tenantId}/projects/{projectId}/whiteboards
GET    /api/tenants/{tenantId}/projects/{projectId}/whiteboards/{boardId}
PUT    /api/tenants/{tenantId}/projects/{projectId}/whiteboards/{boardId}
DELETE /api/tenants/{tenantId}/projects/{projectId}/whiteboards/{boardId}?expectedVersion={version}
POST   /api/tenants/{tenantId}/projects/{projectId}/whiteboards/{boardId}/nodes/{nodeKey}/convert-to-task
```

Detailed rules: `guides/collaborative_whiteboard.md`.

## Project Simulation checkpoint

Backend boundary:

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

## Resume here

After #146 merges green, continue **Collaborative Whiteboard** with the project-facing visual workspace as the next PR.

Target #147 scope:

1. frontend domain under `features/whiteboards/`
2. project route/entry point for a whiteboard workspace
3. board list/create/select/delete
4. draggable/resizable sticky, text and shape nodes
5. visual connectors between nodes
6. pan + zoom
7. autosave full bounded document using `expectedVersion`
8. explicit 409 stale-version recovery/refetch UX; never silently overwrite a newer board
9. multi-select and local undo/redo
10. node -> task conversion UI using the existing backend endpoint
11. active task links visible on converted nodes
12. no WebSocket/live cursor requirement yet

After the persisted visual workspace is stable, add the live-collaboration slice with transport/reconnect/resync, presence and cursors while keeping the persisted document model transport-independent.

Then continue the committed sequence:

1. Project Health / Risk Radar
2. Forms -> Workflow Engine
3. Approval Workflows
4. Client / Guest Portal
5. Team Workload Engine
6. Workspace Knowledge Graph
7. AI / Agent Teammates
8. resume parked backlog such as bulk/CSV, custom fields, knowledge/documents and broader analytics unless reprioritized

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

Do not merge around failed gates. If Auto Format creates a bot-authored head, make a subsequent human commit only after verifying the formatting so normal PR workflows are retriggered on the final feature state.

## Preserve these system invariants

- tenant isolation precedes resource access
- backend authorization is authoritative
- automated workflows never grant authority
- stored personal/workflow/whiteboard definitions do not bypass resource authorization
- Calendar/Task Planning/Automation/Simulation/Whiteboard data is authorized before exposure
- simulation never mutates live state implicitly
- whiteboard optimistic concurrency never silently overwrites newer state
- generated tasks go through task-owned creation behavior
- graph traversal and batch/document work remain bounded
- Explain Access and enforcement share authorization semantics
- delegated authority never exceeds current direct source authority
- public APIs expose DTOs rather than persistence entities
- provider secrets remain server-side

## Deferred platform work

Production Operations & Disaster Recovery remains deliberately deferred behind the committed product sequence. It still includes restore drills, alert/runbook work, broader load/failure-recovery validation and production R2 verification.

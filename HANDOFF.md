# MultiTenantSAAS — Development Handoff

Updated: 2026-09-15

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
9. `guides/Wild_Thoughts.md`
10. the focused guide for the domain being changed
11. `wiki/Roadmap.md` when planning product direction

## Current state

Major milestones are complete through the Project Simulation / What-If Engine program:

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

Portable PostgreSQL Flyway migrations remain through **V50** because #145 adds no persistence. New persistence must be **V51+**; never modify V50 or earlier after it is merged/applied.

Stripe remains the validated deployed Test Mode billing path. Razorpay integration/catalog provisioning remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

## Architecture rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Backend features should use explicit domain packages. Frontend features should preserve locality under `features/<domain>/...`.

Do not expand `ProjectTaskService` or `ProjectService` merely because a new feature eventually reads or changes projects/tasks.

## Project Simulation checkpoint

Backend ownership:

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

Preserve these rules:

- project-level `project.task.manage` authority is required
- simulation is advisory/read-only
- no hidden apply/mutation path
- maximum 500 tasks and 1,000 dependency edges
- maximum 100 task overrides and 100 dependency changes per request
- unknown tasks, invalid assignees, self-dependencies, duplicates and cycles are rejected
- due-date changes propagate only as dependency exposure/conflict analysis; no fake duration/finish prediction
- assignment changes report open-task workload deltas
- task and dependency state cross domain boundaries through narrow simulation source ports

Frontend ownership:

```text
features/project-simulation/
```

Private route:

```text
/projects/:projectId/simulation
```

The first UI slice supports one task due-date/assignee override plus one dependency add/remove operation, then shows direct/downstream impact, conflicts and workload deltas. There is intentionally no apply action.

Detailed contract: `guides/project_simulation.md`.

## Visual Workflow Builder checkpoint

V50 owns:

- `workflow_definitions`
- `workflow_nodes`
- `workflow_edges`
- `workflow_executions`

Definition/runtime boundaries:

```text
task lifecycle
    -> tasks/events/TaskDomainEvent
    -> after-commit workflow listener
    -> workflows runtime/graph traversal
    -> tasks/automation/TaskAutomationMutationPort
    -> task-owned authorization + mutation rules
```

Rules to preserve:

- tenant-scoped workflow catalog
- `DRAFT` / `ACTIVE` / `PAUSED`
- 2–50 nodes, 1–100 edges
- exactly one trigger
- all nodes reachable from the trigger
- acyclic graph
- trigger/action `DEFAULT`; condition `TRUE`/`FALSE`
- strict typed configuration; no arbitrary code
- active workflow must be paused before editing
- workflow definition version increments on edit
- task events are handled after the task transaction commits
- runtime execution is idempotent per `(tenant, workflow, event)`
- task action authorization is re-checked by the task-owned mutation adapter
- workflow-driven mutations do not recursively emit workflow-triggering events in v1
- execution outcomes are auditable/explainable: `RUNNING`, `SUCCEEDED`, `FAILED`, `SKIPPED`

Initial operations are intentionally narrow: task created/status changed triggers, task priority/status equality conditions, and task priority/status mutations.

Detailed contract: `guides/visual_workflow_builder.md`.

## Existing work-generation boundaries to preserve

```text
recurringwork ───────────┐
                         ├─> TaskCreationPort -> task-owned adapter -> normal task lifecycle
project task templates ──┘

projecttemplates -> ProjectCreationPort -> project-owned adapter -> normal project lifecycle
       │
       └────────> TaskCreationPort -> task-owned adapter -> starter tasks
```

V48/V49 recurrence/template semantics remain documented in `guides/recurring_work_and_templates.md`. Calendar remains a deadline projection. Task Relationships remains hierarchy/dependency/label ownership.

## Resume here

After #145 is merged green, start committed differentiated feature #3: **Collaborative Whiteboard**.

First design constraints:

1. create explicit whiteboard/canvas ownership instead of storing arbitrary canvas state on projects/tasks
2. define a bounded board/document model with nodes, geometry and connections
3. keep initial collaboration transport replaceable; do not couple the domain model directly to a WebSocket implementation
4. convert a sticky/node to a real task only through the task-owned creation contract and current authorization/quota rules
5. keep board access project-scoped and tenant-isolated
6. add real-time presence/cursors only after the persisted single-user/multi-user board model is stable

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
- stored personal/workflow definitions do not bypass resource authorization
- Calendar/Task Planning/Automation/Simulation data is authorized before exposure
- simulation never mutates live state implicitly
- generation/automation retries are idempotent where required
- graph traversal and batch work remain bounded
- Explain Access and enforcement share authorization semantics
- delegated authority never exceeds current direct source authority
- public APIs expose DTOs rather than persistence entities
- provider secrets remain server-side

## Deferred platform work

Production Operations & Disaster Recovery remains deliberately deferred behind the committed product sequence. It still includes restore drills, alert/runbook work, broader load/failure-recovery validation and production R2 verification.

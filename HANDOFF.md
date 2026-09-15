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
8. `guides/Wild_Thoughts.md`
9. the focused guide for the domain being changed
10. `wiki/Roadmap.md` when planning product direction

## Current state

Major milestones are complete through the Visual Workflow Builder program:

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

Portable PostgreSQL Flyway migrations now extend through **V50**. New persistence must be **V51+** after #144; never modify V50 or earlier after it is merged/applied.

Stripe remains the validated deployed Test Mode billing path. Razorpay integration/catalog provisioning remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

## Architecture rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Backend features should use explicit domain packages. Frontend features should preserve locality under `features/<domain>/...`.

Do not expand `ProjectTaskService` or `ProjectService` merely because a new feature eventually reads or changes projects/tasks.

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

Frontend ownership:

```text
features/workflow-builder/
features/work-automation/
```

The `/work-automation` Workflow builder tab provides a draggable canvas, persisted positions, branch-target inspector, save/activate/pause lifecycle and recent tenant execution history. Active definitions are read-only until paused.

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

After #144 is merged green, start committed differentiated feature #2: **Project Simulation / What-If Engine**.

First design decisions:

1. create an explicit simulation/scenario owning domain rather than adding preview flags to live project/task services
2. read authorized project/task/dependency state through narrow read contracts
3. store or calculate private scenario overrides separately from live state
4. compute downstream schedule/workload/blast-radius effects without mutating authoritative records
5. require an explicit human apply step for any live change; application must re-check current authorization and invariants

Then continue the committed sequence:

1. Collaborative Whiteboard
2. Project Health / Risk Radar
3. Forms -> Workflow Engine
4. Approval Workflows
5. Client / Guest Portal
6. Team Workload Engine
7. Workspace Knowledge Graph
8. AI / Agent Teammates
9. resume parked backlog such as bulk/CSV, custom fields, knowledge/documents and broader analytics unless reprioritized

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
- Calendar/Task Planning/Automation data is authorized before exposure
- generation/automation retries are idempotent where required
- graph traversal and batch work remain bounded
- Explain Access and enforcement share authorization semantics
- delegated authority never exceeds current direct source authority
- public APIs expose DTOs rather than persistence entities
- provider secrets remain server-side

## Deferred platform work

Production Operations & Disaster Recovery remains deliberately deferred behind the committed product sequence. It still includes restore drills, alert/runbook work, broader load/failure-recovery validation and production R2 verification.

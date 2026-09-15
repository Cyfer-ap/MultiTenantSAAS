# Developer Handoff

Use this page as the reader-facing Wiki pointer for resuming development. Repository-internal current status lives in `CHECKPOINT.md`; resume instructions live in `HANDOFF.md`.

## Current phase

**Differentiated Work Platform Sequence**

Search, Command Palette, Favorites/Recently Viewed, My Work, Saved Views, Dashboard, Calendar/Deadline View, Task Relationships/Task Planning, recurring work, project/task templates, Work Automation, Visual Workflow Builder and **Project Simulation / What-If Engine** are established through PR #145 once its final green head is merged.

## Read first

Inside the Wiki:

1. [[Architecture]]
2. [[Authorization]]
3. [[Security-and-Authentication]]
4. [[Roadmap]]
5. [[Testing-and-CI]]

Inside the repository:

1. `AGENTS.md`
2. `CHECKPOINT.md`
3. `HANDOFF.md`
4. `guides/current_architecture.md`
5. `guides/ENGINEERING_STANDARDS.md`
6. `guides/task_relationships.md`
7. `guides/recurring_work_and_templates.md`
8. `guides/visual_workflow_builder.md`
9. `guides/project_simulation.md`
10. the focused guide for the domain being changed

## Engineering rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Reference implementations include Search contributor contracts, Personal Workspace resolver adapters, `MyWorkTaskSource`, the Saved Views validator SPI, Calendar's deadline-source contract, Task Relationships' task gateway/change sink, `TaskCreationPort`, `ProjectCreationPort`, task-domain workflow events/`TaskAutomationMutationPort`, and Project Simulation's task/dependency source ports.

## Project Simulation checkpoint

The simulation layer is private/advisory and does not mutate live work.

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

The private `/projects/:projectId/simulation` workspace supports hypothetical due-date, assignee and dependency changes and displays direct/downstream impact, conflict changes and workload deltas. There is no apply action in v1.

Detailed rules live in `guides/project_simulation.md`.

## Workflow checkpoint

V50 establishes workflow definitions/nodes/edges/executions, bounded validated graphs, after-commit task-event execution, task-owned mutation authorization and the `/work-automation` visual canvas. Active workflows must be paused before editing.

Detailed rules live in `guides/visual_workflow_builder.md`.

## Work-generation checkpoint

V48/V49 remain responsible for recurrence/task-template/project-template generation through `TaskCreationPort` and `ProjectCreationPort`. Calendar remains deadline projection and Task Relationships remains hierarchy/dependency/label ownership.

Detailed rules live in `guides/recurring_work_and_templates.md`.

## Resume here

Start from current `main`. Portable PostgreSQL migrations remain through **V50** because #145 adds no persistence; new persistence begins at **V51+**.

The next committed product feature is **Collaborative Whiteboard**.

Before implementation:

1. choose an explicit whiteboard/canvas owning domain
2. keep project-scoped board persistence separate from project/task rows
3. use bounded nodes/geometry/connections with backend-authoritative validation
4. keep real-time transport behind a replaceable boundary rather than embedding WebSocket concerns in the domain model
5. convert a board node/sticky into a task only through the task-owned creation contract and current authorization/quota rules
6. add presence/cursors only after the persisted board model is stable

After that continue with Project Health / Risk Radar, Forms -> Workflow Engine, Approval Workflows, Client / Guest Portal, Team Workload Engine, Workspace Knowledge Graph and AI / Agent Teammates.

## Preserve these system invariants

- tenant isolation precedes resource access
- backend authorization is authoritative
- stored favorites/recents/saved-view/workflow definitions never grant resource access
- Calendar, Task Planning, Work Automation and Simulation data are authorized before exposure
- simulation never implicitly mutates live state
- workflow administration permission does not imply permission to mutate a target task
- workflow/task generation retries remain idempotent
- graph traversal, reads and generation batches remain bounded
- automated workflow mutations do not recursively trigger workflows in v1
- project-template creation enforces ordinary quota/actor/lead/lifecycle rules
- Explain Access and enforcement use the same evaluator
- delegated authority never exceeds its current direct parent authority
- public APIs expose DTOs rather than persistence entities
- provider secrets remain server-side

## Deferred work

Production Operations & Disaster Recovery remains deliberately deferred behind the committed product sequence. It still includes restore drills, alert/runbook work, broader load/failure-recovery validation and production R2 verification.

See [[Roadmap]] for product direction and repository guide `guides/DEFERRED_PLATFORM_WORK.md` for deferred platform work.

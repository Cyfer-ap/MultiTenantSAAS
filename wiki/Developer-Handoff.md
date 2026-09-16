# Developer Handoff

Use this page as the reader-facing Wiki pointer for resuming development. Repository-internal current status lives in `CHECKPOINT.md`; resume instructions live in `HANDOFF.md`.

## Current phase

**Differentiated Work Platform Sequence**

Search, Command Palette, Favorites/Recently Viewed, My Work, Saved Views, Dashboard, Calendar/Deadline View, Task Relationships/Task Planning, recurring work, project/task templates, Work Automation, Visual Workflow Builder, Project Simulation / What-If Engine and the persisted Collaborative Whiteboard workspace are established through PR #147.

**Project Health / Risk Radar is active next.** Live whiteboard presence/cursors remain a later optional enhancement rather than a prerequisite for the next committed feature.

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
10. `guides/collaborative_whiteboard.md`
11. the focused guide for the domain being changed

## Engineering rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Reference implementations include Search contributor contracts, Personal Workspace resolver adapters, `MyWorkTaskSource`, the Saved Views validator SPI, Calendar's deadline-source contract, Task Relationships' task gateway/change sink, `TaskCreationPort`, `ProjectCreationPort`, task-domain workflow events/`TaskAutomationMutationPort`, Project Simulation's task/dependency source ports, and Whiteboard's project/task narrow ports.

## Collaborative Whiteboard checkpoint

V51 owns:

- `whiteboards`
- `whiteboard_nodes`
- `whiteboard_edges`

Boundary:

```text
whiteboards
    -> ProjectAccessPort -> project-owned adapter
    -> TaskCreationPort  -> task-owned adapter
```

Preserve:

- tenant/project-scoped boards
- normalized project-local board names
- `STICKY`, `TEXT`, `SHAPE` nodes
- stable bounded node geometry and max 300 nodes / 600 connectors
- visual cycles allowed; no task-DAG rule on connectors
- optimistic board versions for update/delete/conversion
- structured 409 conflict on stale versions
- 409 duplicate-name behavior, including database race
- archived projects readable but not mutable
- sticky/text conversion only through `TaskCreationPort`
- durable `linked_task_id`
- no WebSocket/STOMP/presence/cursor state in the persistence model

The project-facing `/projects/:projectId/whiteboards` workspace adds board selection/create/delete, drag/resize, connectors, pan/zoom, multi-select, local undo/redo, committed-edit autosave, explicit stale reload recovery and node -> task conversion UX. Edit authority mirrors backend project-task-manage/project-lead semantics.

Detailed rules live in `guides/collaborative_whiteboard.md`.

## Project Simulation checkpoint

```text
projectsimulation
    -> ProjectSimulationTaskSource -> task-owned adapter
    -> ProjectSimulationDependencySource -> Task Relationships-owned adapter
```

The private `/projects/:projectId/simulation` workspace supports hypothetical due-date, assignee and dependency changes and displays direct/downstream impact, conflict changes and workload deltas. There is no apply action in v1.

Detailed rules live in `guides/project_simulation.md`.

## Workflow checkpoint

V50 establishes workflow definitions/nodes/edges/executions, bounded validated graphs, after-commit task-event execution, task-owned mutation authorization and the `/work-automation` visual canvas. Active workflows must be paused before editing.

Detailed rules live in `guides/visual_workflow_builder.md`.

## Resume here — Project Health / Risk Radar

Build the next feature as an explicit risk/health domain with narrow authorized projections rather than expanding project/task legacy services.

Initial goals:

1. explainable project-level signals for overdue work, blockers, stale work and dependency criticality
2. bounded workload-pressure context only where explicit assignment/capacity data supports it
3. advisory/read-only analysis first; no hidden project/task mutations
4. concrete contributing entities/reasons instead of an opaque score
5. project-facing Risk Radar UX with drill-down to contributing work
6. deterministic tests for signal calculation, authorization and bounded traversal

Do not rank employees, infer productivity, or score people. Risk signals must describe project/work conditions and remain permission-aware.

After Risk Radar continue with Forms -> Workflow Engine, Approval Workflows, Client / Guest Portal, Team Workload Engine, Workspace Knowledge Graph and AI / Agent Teammates.

## Preserve these system invariants

- tenant isolation precedes resource access
- backend authorization is authoritative
- stored favorites/recents/saved-view/workflow/whiteboard/risk definitions never grant resource access
- Calendar, Task Planning, Work Automation, Simulation, Whiteboard and Risk data are authorized before exposure
- simulation never implicitly mutates live state
- risk analysis remains advisory until a separately authorized action exists
- whiteboard stale writes never silently overwrite newer documents
- whiteboard-to-task conversion uses task-owned creation behavior
- workflow administration permission does not imply permission to mutate a target task
- graph/document reads and generation batches remain bounded
- automated workflow mutations do not recursively trigger workflows in v1
- project-template creation enforces ordinary quota/actor/lead/lifecycle rules
- Explain Access and enforcement use the same evaluator
- delegated authority never exceeds its current direct parent authority
- public APIs expose DTOs rather than persistence entities
- provider secrets remain server-side

## Deferred work

Production Operations & Disaster Recovery remains deliberately deferred behind the committed product sequence. It still includes restore drills, alert/runbook work, broader load/failure-recovery validation and production R2 verification.

See [[Roadmap]] for product direction and repository guide `guides/DEFERRED_PLATFORM_WORK.md` for deferred platform work.

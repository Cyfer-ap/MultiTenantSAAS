# Developer Handoff

Use this page as the reader-facing Wiki pointer for resuming development. Repository-internal current status lives in `CHECKPOINT.md`; resume instructions live in `HANDOFF.md`.

## Current phase

**Differentiated Work Platform Sequence**

Search, Command Palette, Favorites/Recently Viewed, My Work, Saved Views, Dashboard, Calendar/Deadline View, Task Relationships/Task Planning, recurring work, project/task templates, Work Automation, Visual Workflow Builder and **Project Simulation / What-If Engine** are established through PR #145.

**Collaborative Whiteboard is active.** PR #146 establishes its backend/V51 persistence foundation; the project-facing visual workspace is the next slice, followed later by live presence/cursors.

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

## Collaborative Whiteboard foundation

After #146 merges, V51 owns:

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

Detailed rules live in `guides/collaborative_whiteboard.md`.

## Project Simulation checkpoint

The simulation layer is private/advisory and does not mutate live work.

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

## Resume here

After #146 merges green, continue the **Collaborative Whiteboard visual workspace**.

Build next:

1. frontend ownership under `features/whiteboards/`
2. project-scoped whiteboard route and discoverable project entry point
3. board list/create/select/delete
4. draggable/resizable sticky/text/shape nodes
5. connectors
6. pan/zoom
7. bounded autosave using the backend `expectedVersion`
8. explicit stale-version recovery/refetch UX; never silent overwrite
9. multi-select and local undo/redo
10. node -> task conversion UX and linked-task indicators
11. no live cursor/presence requirement yet

After the persisted workspace is stable, add a separate live collaboration slice with replaceable transport, reconnect/resync, presence and cursors.

After Collaborative Whiteboard is complete, continue with Project Health / Risk Radar, Forms -> Workflow Engine, Approval Workflows, Client / Guest Portal, Team Workload Engine, Workspace Knowledge Graph and AI / Agent Teammates.

## Preserve these system invariants

- tenant isolation precedes resource access
- backend authorization is authoritative
- stored favorites/recents/saved-view/workflow/whiteboard definitions never grant resource access
- Calendar, Task Planning, Work Automation, Simulation and Whiteboard data are authorized before exposure
- simulation never implicitly mutates live state
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

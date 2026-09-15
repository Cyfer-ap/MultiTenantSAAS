# Project Simulation / What-If Engine

The project simulator is a read-only planning workspace for evaluating hypothetical task and dependency changes before touching real project state.

## Access

The frontend route is:

```text
/projects/{projectId}/simulation
```

The workspace requires `project.task.manage` for the selected project. The backend applies the same authorization boundary to both baseline reads and simulations.

## Backend API

### Load the scenario baseline

```http
GET /api/tenants/{tenantId}/projects/{projectId}/simulation/baseline
```

The baseline contains the project's bounded task snapshot and dependency edges. It is intended for building a private client-side scenario editor and does not persist anything.

### Run a scenario

```http
POST /api/tenants/{tenantId}/projects/{projectId}/simulation
```

The request can contain up to 100 task overrides and 100 dependency changes. Task overrides may hypothetically change a due date or assignee. Dependency changes may add or remove an edge.

The simulation rejects unknown project tasks, invalid project assignees, self-dependencies, duplicate changes and cyclic simulated dependency graphs.

## Current impact model

The response separates:

- direct task changes;
- downstream tasks exposed through dependency edges;
- baseline, new and resolved dependency deadline conflicts;
- assignee open-task workload deltas; and
- reassignment counts.

The implementation deliberately does **not** invent task durations or a predicted project completion date because the current task model does not contain an effort/duration field. Schedule impact is therefore based on due-date consistency and dependency exposure rather than fabricated CPM durations.

## Frontend workflow

The What-If Simulator can combine one task override and one dependency change in the current first UI slice. A user can:

1. choose a project task;
2. change its simulated due date and/or assignee;
3. optionally add or remove a dependency edge;
4. run the scenario; and
5. inspect direct changes, downstream exposure, dependency conflicts and workload deltas.

The UI has no apply/commit action. Resetting or leaving the page discards the scenario.

## Architecture boundary

`projectsimulation` owns orchestration and calculations only. It reads task state through `ProjectSimulationTaskSource` and dependency state through `ProjectSimulationDependencySource`. Task and Task Relationships modules own the concrete adapters.

Do not replace those narrow ports by injecting the full project-task or task-graph services into the simulation coordinator. Future scenario features should keep the same domain boundary.

## Safety and limits

- advisory only; no project/task/dependency mutation path exists;
- maximum 500 project tasks per calculation;
- maximum 1,000 dependency edges per calculation;
- maximum 100 task overrides and 100 dependency changes per request;
- simulated graph must remain acyclic;
- no scenario persistence in the current version;
- no Flyway migration is required for this slice.

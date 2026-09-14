# Task Relationships and Labels

Updated: 2026-09-14

This guide documents the product and architecture contract for subtasks, directed task dependencies, project-scoped labels, and the Task Planning workspace.

## Scope

The first task-relationships milestone is delivered by backend PR #139 and frontend PR #140.

Backend ownership lives under `taskrelationships`. Frontend ownership lives under `features/task-relationships`.

This capability is deliberately **not** a generic graph platform. It extends project tasks with bounded planning relationships while preserving the existing task authorization boundary.

## Persistence baseline

Portable common Flyway migrations extend through **V47**.

V47 adds:

- optional `project_tasks.parent_task_id`
- `task_dependencies`
- `project_task_labels`
- `project_task_label_assignments`
- tenant-qualified project/task/label constraints and indexes

Applied migrations remain append-only. Never rewrite V47 after it has been applied.

## Subtask semantics

A task may have at most one direct parent.

Rules:

- parent and child must belong to the same tenant and project
- a task cannot parent itself
- assigning a parent cannot create an ancestry cycle
- ancestry validation is bounded to 64 levels
- direct-child reads are bounded to 200 items and expose truncation
- completing or cancelling a parent does not automatically change child status

The optional parent pointer belongs to the task entity because it is intrinsic task structure. Hierarchy orchestration does not belong in `ProjectTaskService`.

## Dependency semantics

Dependencies are directed:

```text
blocking task  ──blocks──>  dependent task
```

Rules:

- both tasks must belong to the same tenant and project
- a task cannot depend on itself
- duplicate edges are idempotent and database-unique
- a new edge cannot create a directed cycle
- project dependency validation is capped at 1,000 edges
- blocker/dependent reads are bounded to 200 items and expose truncation
- dependencies do not automatically change task status in v1

This keeps dependencies explicit planning metadata. A later workflow/automation feature may consume them, but must do so through a narrow contract rather than embedding automation inside graph persistence.

## Label semantics

Labels are project-scoped reusable task metadata.

Rules:

- normalized label names are unique per project
- a project exposes at most 200 labels through the bounded catalog
- a task may have at most 20 labels
- assignments cannot cross tenant/project boundaries
- optional display color is a six-digit hex value
- deleting a project label removes its task assignments deterministically

Labels are not custom fields. Future custom-field work requires its own schema and lifecycle.

## Backend architecture

```text
TaskRelationshipController
        │
        ├── TaskRelationshipQueryService
        │       └── bounded relationship projection
        │
        ├── TaskGraphService
        │       └── parent/dependency mutations + cycle checks
        │
        └── TaskLabelService
                └── project-label lifecycle + assignment

Task relationship services
        ↓
TaskRelationshipTaskGateway
        ↓
existing project/task persistence

relationship changes
        ↓
TaskRelationshipChangeSink
        ↓
existing audit + task activity systems
```

The relationship domain reuses existing `canReadProjectTasks` and `canManageProjectTasks` authorization. It does not introduce a parallel permission model.

## API surface

Under `/api/tenants/{tenantId}/projects/{projectId}`:

```text
GET    /tasks/{taskId}/relationships
PUT    /tasks/{taskId}/parent
POST   /tasks/{taskId}/dependencies
DELETE /tasks/{taskId}/dependencies/{blockingTaskId}

GET    /task-labels
POST   /task-labels
PUT    /task-labels/{labelId}
DELETE /task-labels/{labelId}

PUT    /tasks/{taskId}/labels/{labelId}
DELETE /tasks/{taskId}/labels/{labelId}
```

Persistence entities are never returned directly; relationship and label DTOs define the public contract.

## Frontend architecture

The user-facing surface is `/task-planning`.

```text
TaskPlanningPage
        ↓
authorization-safe Global Search task selection
        ↓
TaskRelationshipsPanel
        ├── hierarchy
        ├── blockers/dependents
        └── labels
                ↓
        TaskLabelManagerDialog
```

The workspace is intentionally separate from the already-large `ProjectTasksSection` and task collaboration drawer.

Task selection uses the existing Global Search contract so the shell does not guess which projects/tasks the actor may read. Mutation authority uses the existing project task-management permission with the established project-lead membership fallback. Backend authorization remains authoritative.

Because Task Planning is registered in the shared workspace navigation contract, it is also available to Command Palette and Dashboard quick-action composition without duplicating navigation logic.

## Activity compatibility

Relationship mutations are recorded in task activity with dedicated backend activity types. The older collaboration drawer predates those presentation labels. Its frontend API adapter maps relationship activity types to the existing `TASK_UPDATED` display category while preserving the relationship-specific summary text.

This is a compatibility bridge, not a new relationship dependency on the collaboration UI. A later activity-UI cleanup can add first-class presentation labels in the collaboration feature itself.

## What does not belong here

Do not add these concerns to task-relationship services merely because they mention tasks:

- recurring work schedules
- project/task templates
- workflow/approval engines
- custom fields/forms
- calendar/meeting scheduling
- generic automation rules
- cross-project graph traversal without a concrete product requirement

The next product slice is **recurring work + project/task templates**. It should get an explicit owning domain and consume task/project capabilities through narrow contracts rather than expanding `TaskGraphService` or `TaskLabelService`.

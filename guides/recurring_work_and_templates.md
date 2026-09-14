# Recurring Work and Templates

This guide owns the recurring-work and template-generation contract. Current milestone status belongs in `../CHECKPOINT.md`; resume instructions belong in `../HANDOFF.md`.

## Implementation status

Backend foundation is merged through PR #141 (`3460785aa9a1644768f10c696ccaef27422535f8`). Portable common migrations are at **V48**.

The broader milestone is still open. The next implementation slice is **V49+ tenant-scoped project templates plus feature-local frontend management for recurring work, task templates, and project templates**.

## Ownership

Recurring work is an explicit `recurringwork` backend domain. Project-scoped task templates are an explicit `tasktemplates` backend domain. Both create ordinary tasks through the task-owned `tasks/creation/TaskCreationPort` rather than depending on `ProjectTaskService`.

```text
recurringwork ───────┐
                     ├─> TaskCreationPort -> task-owned adapter -> ProjectTask persistence
project task template┘                                  ├─ activity
                                                        ├─ audit
                                                        ├─ assignment notification
                                                        └─ TASK_CREATED webhook
```

Calendar remains a deadline projection. Task relationships remain hierarchy/dependency/label ownership. Neither domain owns scheduling or template lifecycle.

## V48 persistence

V48 adds:

- `recurring_task_definitions`
- `recurring_task_occurrences`
- `project_task_templates`

Tenant/project/user scope is enforced with qualified foreign keys. Recurring occurrence idempotency is enforced by a unique `(definition_id, scheduled_for)` key.

## Recurring-task contract

V1 generates **tasks only**.

Supported cadence values:

- `DAILY`
- `WEEKLY`
- `MONTHLY`

Each definition owns:

- project and tenant scope
- creator identity
- optional assignee
- title/description/priority snapshot used for future generated tasks
- cadence + interval from 1 through 52
- explicit IANA timezone
- next occurrence instant
- optional due offset
- optional end instant
- optional maximum occurrence count
- generated count and lifecycle status

Lifecycle statuses are `ACTIVE`, `PAUSED`, and `ENDED`.

### Time behavior

Schedule advancement uses the configured timezone and calendar arithmetic, not fixed-duration server-time arithmetic. For example, a 09:00 daily rule remains 09:00 local time through a DST boundary. Monthly rules use calendar-month semantics such as January 31 -> February 28 when appropriate.

A generated task due date is:

```text
scheduled occurrence instant + optional due offset
```

No due offset means no generated task deadline.

### Materialization and concurrency

The scheduler discovers at most 50 due definitions per pass. A definition materializes at most five catch-up occurrences in one pass.

Before generation, the definition is loaded with a pessimistic write lock. The occurrence table also protects retries with its database uniqueness constraint. A retry or competing application instance must not create a second logical occurrence.

Materialization is transactional: the task, occurrence linkage and recurrence cursor advance succeed together or roll back together.

A generation failure pauses the definition and stores a bounded diagnostic message. It is not retried indefinitely while still marked active.

### Pause, resume and editing

- pausing stops future generation
- editing changes future generation only
- already generated tasks remain immutable snapshots with respect to the rule
- resume advances past times missed while paused and restarts from the first future occurrence
- resume does **not** intentionally burst-create every paused-period occurrence
- ending/max-count rules stop future generation
- completion of the previous task does not gate the next occurrence in v1

Recurring definitions are retained rather than hard-deleted so occurrence history remains attributable.

## Project-scoped task-template contract

A project may own at most 100 task templates in v1. Template names are normalized and unique per project.

A template snapshots:

- display name
- task title
- optional task description
- priority
- optional assignee
- optional due offset

Instantiating a template creates an ordinary task through `TaskCreationPort`. The current actor becomes the task creator; an optional template assignee must still be an active member of the project when the task is created.

Editing or deleting a template does not mutate tasks previously created from it.

Task templates do not contain subtasks, dependency edges, labels, custom fields, workflow state, or recurring rules in this first version.

## Authorization

Recurring-work reads and task-template reads reuse the established project-task read authorization. Mutations reuse project-task management authority, including the existing project-lead fallback encoded by `AuthorizationSecurityService`.

No shell-role guessing or parallel permission system is introduced.

## Bounded behavior

- recurring-work list pages: maximum 100
- occurrence list pages: maximum 100
- scheduler discovery batch: 50 definitions
- scheduler catch-up: maximum 5 occurrences per definition/pass
- recurrence interval: 1–52
- recurrence max occurrence count: at most 10,000
- recurrence/task-template due offset: at most 525,600 minutes
- task templates: maximum 100/project

## APIs

Recurring work:

```text
GET  /api/tenants/{tenantId}/projects/{projectId}/recurring-work
POST /api/tenants/{tenantId}/projects/{projectId}/recurring-work
GET  /api/tenants/{tenantId}/projects/{projectId}/recurring-work/{definitionId}
PUT  /api/tenants/{tenantId}/projects/{projectId}/recurring-work/{definitionId}
POST /api/tenants/{tenantId}/projects/{projectId}/recurring-work/{definitionId}/pause
POST /api/tenants/{tenantId}/projects/{projectId}/recurring-work/{definitionId}/resume
GET  /api/tenants/{tenantId}/projects/{projectId}/recurring-work/{definitionId}/occurrences
```

Task templates:

```text
GET    /api/tenants/{tenantId}/projects/{projectId}/task-templates
POST   /api/tenants/{tenantId}/projects/{projectId}/task-templates
GET    /api/tenants/{tenantId}/projects/{projectId}/task-templates/{templateId}
PUT    /api/tenants/{tenantId}/projects/{projectId}/task-templates/{templateId}
DELETE /api/tenants/{tenantId}/projects/{projectId}/task-templates/{templateId}
POST   /api/tenants/{tenantId}/projects/{projectId}/task-templates/{templateId}/instantiate
```

## Next slice — project templates + frontend completion

The next slice owns:

1. tenant-scoped project templates using a new V49+ append-only migration
2. a project-owned narrow `ProjectCreationPort` preserving quota, actor, owner-membership, audit, and lifecycle behavior
3. bounded project-template task snapshots and deterministic instantiation failure semantics
4. feature-local frontend management for recurring work and task templates
5. project-template catalog/instantiate frontend flows
6. final milestone documentation/UX closure

Project templates must not inject `ProjectService` into a generic template god-service. Their instantiation should cross the project boundary through a narrow project-owned creation contract, just as task generation crosses through `TaskCreationPort`.
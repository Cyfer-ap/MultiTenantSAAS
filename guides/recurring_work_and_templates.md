# Recurring Work and Templates

This guide owns the recurring-work and template-generation contract. Current milestone status belongs in `../CHECKPOINT.md`; resume instructions belong in `../HANDOFF.md`.

## Implementation status

The Recurring Work + Project/Task Templates milestone is implemented through PR #143.

- V48: recurring-task definitions/occurrences + project-scoped task templates
- V49: tenant-scoped project templates + bounded embedded task snapshots
- frontend: `Work Automation & Templates` workspace for recurring work, task templates, and project templates

Applied migrations are append-only. Never rewrite V49 or earlier migrations after merge.

## Ownership and cross-domain boundaries

The owning backend domains are:

- `recurringwork`
- `tasktemplates`
- `projecttemplates`
- `tasks/creation` for the task-owned creation contract
- `projects/creation` for the project-owned creation contract

Generation crosses domain boundaries only through narrow creation ports:

```text
recurringwork ───────────┐
                         ├─> TaskCreationPort -> task-owned adapter -> normal task lifecycle
project task templates ──┘

projecttemplates ──> ProjectCreationPort -> project-owned adapter -> normal project lifecycle
       │
       └────────────> TaskCreationPort    -> task-owned adapter    -> starter tasks
```

`projecttemplates` does not inject `ProjectService`; recurrence/task templates do not inject `ProjectTaskService`. Calendar remains a deadline projection, and task relationships remain hierarchy/dependency/label ownership.

## V48 recurring-task persistence and contract

V48 adds:

- `recurring_task_definitions`
- `recurring_task_occurrences`
- `project_task_templates`

Tenant/project/user scope is enforced with qualified foreign keys. Recurring occurrence idempotency is enforced by unique `(definition_id, scheduled_for)`.

Recurring work generates **tasks only**. Supported cadence values are `DAILY`, `WEEKLY`, and `MONTHLY` with interval 1–52 and an explicit IANA timezone.

A rule snapshots title, description, priority and optional assignee; it also stores next occurrence, optional due offset/end/max occurrences, generated count, status, and bounded failure diagnostics. Statuses are `ACTIVE`, `PAUSED`, and `ENDED`.

Schedule advancement uses timezone/calendar arithmetic rather than fixed server-time durations. Daily rules retain local wall time across DST, and monthly rules use calendar-month semantics. A generated task deadline is the scheduled occurrence plus the optional due offset.

Materialization is bounded and transactional:

- discover at most 50 due definitions/pass
- catch up at most 5 occurrences/definition/pass
- pessimistic per-definition materialization lock
- database uniqueness protects logical occurrence idempotency
- task creation, occurrence linkage and cursor advance succeed or roll back together
- generation failure pauses the definition with a bounded diagnostic

Pause stops generation. Resume skips paused-period schedules and resumes at the first future occurrence. Edits affect future generated work only; previous generated tasks remain snapshots. Previous task completion does not gate later occurrences in v1.

## Project-scoped task templates

A project may own at most 100 task templates. Template names are normalized and unique per project.

A task template snapshots:

- name
- task title and optional description
- priority
- optional assignee
- optional due offset

Instantiation crosses `TaskCreationPort`, so current actor validation, project membership/assignee rules, task persistence, activity, audit, notification and `TASK_CREATED` webhook behavior remain task-owned. Editing/deleting a template never mutates existing tasks.

Task templates intentionally exclude subtasks, dependencies, labels, custom fields, workflows, and recurrence definitions in v1.

## V49 tenant-scoped project templates

V49 adds:

- `project_templates`
- `project_template_tasks`

Project templates are tenant-scoped and use normalized unique names per tenant. A template snapshots:

- template name
- project name seed
- optional project description
- initial project status except `ARCHIVED`
- zero to 50 ordered starter-task snapshots

Each starter-task snapshot contains title, optional description, priority, and optional due offset. The first version deliberately excludes assignee, subtasks, dependencies, labels, custom fields and workflow state.

Instantiation is snapshot/copy semantics. Later template edits never mutate an instantiated project or task.

### Project creation invariants

`ProjectCreationPort` is project-owned. The default adapter preserves the same invariants as ordinary project creation:

- tenant existence/active state
- active current actor validation
- subscription project quota
- project persistence
- initial `PROJECT_LEAD` membership for the actor
- project creation audit record
- normal project lifecycle/webhook behavior

Ordinary project creation and template-driven creation converge on this same adapter to avoid lifecycle drift.

### Transaction semantics

Project-template instantiation creates the project first through `ProjectCreationPort`, then creates starter tasks in deterministic snapshot order through `TaskCreationPort`. The orchestration is transactional; a starter-task failure must not leave a partially instantiated project/template result.

Due offsets are computed from the created project's creation instant so a template produces deterministic relative deadlines.

## Authorization

Recurring work and task templates reuse project-task authority:

- reads: project-task read authority
- mutations/instantiate: project-task manage authority, including the established project-lead fallback

Project templates are tenant-scoped:

- read: tenant `project.read`
- create/update/delete/instantiate: tenant `project.create`

No parallel role system or shell-side authorization guessing is introduced.

## Frontend workspace

The standalone `/work-automation` workspace is feature-local:

```text
features/recurring-work/
features/task-templates/
features/project-templates/
features/work-automation/
```

The workspace provides:

- authorization-safe project discovery for tenant-wide and project-scoped grants
- recurring rule create/edit/pause/resume
- visible timezone, next occurrence, status, generated count, and occurrence history
- task-template create/edit/delete/instantiate
- tenant project-template create/edit/delete/instantiate
- explicit project-name override during project-template instantiation
- bounded project-template starter-task editor capped at 50 rows

Template/recurrence business logic does not live in `ProjectTasksSection`, Calendar, or `AppShell`.

## Bounded behavior

- recurring list/occurrence pages: API-bounded pagination
- scheduler discovery: 50 definitions/pass
- scheduler catch-up: 5 occurrences/definition/pass
- recurrence interval: 1–52
- max occurrence count: 10,000
- recurrence/task-template/project-template due offset: 525,600 minutes
- task templates: 100/project
- project-template starter tasks: 50/template

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

Project templates:

```text
GET    /api/tenants/{tenantId}/project-templates
POST   /api/tenants/{tenantId}/project-templates
GET    /api/tenants/{tenantId}/project-templates/{templateId}
PUT    /api/tenants/{tenantId}/project-templates/{templateId}
DELETE /api/tenants/{tenantId}/project-templates/{templateId}
POST   /api/tenants/{tenantId}/project-templates/{templateId}/instantiate
```

## Engineering rule

New work-generation behavior must remain domain-owned. Extend narrow contracts when a cross-domain capability is genuinely required; do not make template domains depend on broad legacy services merely because they ultimately create projects or tasks.

# Forms -> Workflow Engine

## Scope

Forms provide an **internal, project-scoped intake surface** that validates bounded user input, records submission provenance and creates authorized project work without bypassing task/project/workflow domain behavior.

This first slice is intentionally private to authenticated tenant users. Public/external intake is not part of V1 because it requires a separate authentication, rate-limit and abuse-prevention boundary.

## Ownership and boundaries

Backend ownership lives in the explicit `forms` domain:

```text
forms
    -> ProjectAccessPort             -> project-owned access adapter
    -> TaskCreationPort              -> task-owned creation adapter
    -> WorkflowFormSubmissionPort    -> workflow-owned entry adapter/runtime
```

Frontend ownership lives under:

```text
features/forms/...
```

and is exposed inside the existing **Work Automation & Templates** workspace rather than adding another top-level route/navigation aggregation point.

The Forms domain must not inject `ProjectService`, `ProjectTaskService` or workflow repositories/services. It does not write directly to task, project or workflow persistence.

## Persistence

Flyway **V52** owns:

- `form_definitions`
- `form_fields`
- `form_submissions`

V51 and earlier remain immutable.

A form definition is scoped by `tenant_id + project_id`, has an explicit lifecycle (`DRAFT`, `ACTIVE`, `PAUSED`) and carries a monotonically increasing `definition_version` when its definition is edited.

A submission records:

- tenant/project/form identity
- the submitted definition version
- submitting user
- validated normalized payload
- created task ID
- deterministic validation context
- submission timestamp

This is provenance, not an authorization shortcut: the referenced task remains governed by normal task/project authorization.

## Supported field schema

V1 deliberately permits only non-executable field types:

- `TEXT`
- `TEXTAREA`
- `NUMBER`
- `DATE`
- `BOOLEAN`
- `SELECT`

Hard bounds:

- maximum 30 fields per form
- maximum 50 options per select field
- maximum 100 characters per select option
- serialized validated submission payload maximum 12,000 characters
- field keys are unique and restricted to a stable identifier pattern

No arbitrary expressions, scripts, formulas or user-provided executable code are evaluated.

Task mappings are explicit:

- title -> `TEXT` or `SELECT`
- description -> optional `TEXT` or `TEXTAREA`
- due date -> optional `DATE`
- priority -> one configured task priority

The backend revalidates both stored definitions and every submission; the browser is never the authority for schema validation.

## Definition lifecycle

- new definitions start as `DRAFT`
- draft/paused definitions may be edited
- active definitions are immutable until paused
- activation revalidates the stored schema and any configured workflow target
- only active definitions accept submissions

Project mutation checks are applied when definitions are created, edited or have lifecycle state changed, so archived projects cannot acquire new intake behavior.

## Authorization

The API is project-scoped:

```text
/api/tenants/{tenantId}/projects/{projectId}/forms
```

Read operations use authoritative project task-read authorization. Definition mutations and internal submission use authoritative project task-manage authorization.

A form definition never grants authority to its project, task or workflow target. Tenant/project IDs in the URL and persisted scope are always checked server-side.

## Task creation contract

An accepted submission creates a task only through task-owned `TaskCreationPort`:

```text
validated form submission
    -> TaskCreationPort
        -> ordinary task authorization/project validation
        -> task persistence
        -> audit/activity/lifecycle behavior
        -> ordinary task-created domain event
```

Forms do not write to `project_tasks` directly. This preserves normal quota, actor, project-state, audit and lifecycle semantics owned by the task domain.

Because normal task creation emits the existing task-created event, ordinary `TRIGGER_TASK_CREATED` workflows continue to behave exactly as they do for manually or otherwise generated tasks.

## Workflow composition

A form may optionally select a workflow whose trigger is:

```text
TRIGGER_FORM_SUBMITTED
```

The Forms domain crosses into workflow behavior only through `WorkflowFormSubmissionPort`.

After the form/task transaction commits, Forms publishes an internal submission event. The listener calls the workflow-owned port with:

- tenant/project
- selected workflow
- form/submission provenance
- newly created task
- authenticated actor
- initial task priority

The workflow runtime remains the existing runtime. It records form submission provenance as source type `FORM_SUBMISSION`, then evaluates conditions/actions against the created task context. Workflow task mutations continue through task-owned `TaskAutomationMutationPort`.

This is additive to the ordinary task-created event. Therefore a form-created task may legitimately match both a generic `TRIGGER_TASK_CREATED` workflow and its explicitly selected `TRIGGER_FORM_SUBMITTED` workflow. Administrators should avoid defining conflicting mutations across those automations; execution history remains the diagnostic source for what ran.

The form-specific workflow entry is dispatched **after commit**. A workflow-entry failure does not roll back an already accepted form submission and created task. Durable retry/outbox semantics are a separate future reliability enhancement rather than an implicit V1 behavior.

## Internal UI

The Forms tab supports:

- project form discovery
- bounded definition creation/editing
- field/schema configuration
- task-field mapping
- optional compatible workflow selection
- activate/pause lifecycle
- authenticated internal submission
- recent submission provenance/history

The Visual Workflow Builder exposes `Form submitted` as a normal trigger option. Forms does not contain a second workflow editor or runtime.

## Deliberately excluded from V1

- anonymous/public forms
- guest/client intake
- arbitrary expressions or executable field logic
- file upload fields
- assignee/user-reference fields
- permission grants from form definitions
- direct project creation
- cross-project target selection
- unbounded dynamic schemas/options/payloads

Public intake, when prioritized, must define authentication/token semantics, abuse controls, rate limits, replay/idempotency behavior and its authorization boundary before being exposed.

## Validation before merge

The final feature head must pass all applicable repository gates:

- Repository Hygiene
- backend build/test/verify
- PostgreSQL/Flyway V52 validation
- frontend format/tests/coverage/lint/build
- Security
- Container CI
- Qodana
- Wiki Sync if canonical Wiki sources change

Focused regression coverage must include schema bounds/type validation, unknown-field rejection, task creation through the task-owned port, workflow target validation/execution and V52 PostgreSQL schema assertions.

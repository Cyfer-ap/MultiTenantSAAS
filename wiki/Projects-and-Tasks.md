# Projects and Tasks

## Projects

Current project lifecycle includes:

```text
PLANNING
ACTIVE
ON_HOLD
COMPLETED
ARCHIVED
```

Project operations include:

- create
- list/read
- update
- status transition
- archive
- membership management

Project data is tenant scoped.

## Memberships

Project membership roles include:

```text
PROJECT_LEAD
MEMBER
```

Important business protections include duplicate prevention, active-user validation, and last-project-lead protection.

## Tasks

Task states:

```text
TODO
IN_PROGRESS
BLOCKED
COMPLETED
CANCELLED
```

Task priorities:

```text
LOW
MEDIUM
HIGH
URGENT
```

Task logic covers assignment/unassignment, completion timestamps, cancellation, and archived-project history.

## Enforcement

Project/task authorization must combine tenant isolation with role/permission/relationship rules.

Subscription lifecycle and quotas may independently restrict growth mutations.

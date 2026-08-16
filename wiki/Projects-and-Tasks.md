# Projects and Tasks

## Projects

Project lifecycle:

```text
PLANNING
ACTIVE
ON_HOLD
COMPLETED
ARCHIVED
```

Project operations include creation, listing/reading, update, status transition, archive, and membership management.

Every project lookup and mutation remains tenant scoped.

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

## Enforcement layers

Project/task operations may require all of the following:

```text
tenant boundary
permission / project relationship
resource-state invariant
subscription mutation access
resource quota where applicable
transaction/database invariant
```

Frontend capability visibility is not a substitute for these backend checks.

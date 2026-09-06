# Collaboration, attachments and notifications

Reviewed for the current platform snapshot on 2026-09-06. The collaboration/notification implementation itself was largely delivered by PR #65 and earlier collaboration/storage work; the platform has since completed the billing milestone through PR #98.

## Task collaboration

Task collaboration is scoped by tenant, project and task. Implemented capabilities include:

- comments
- project-member mentions
- task activity history
- one-level replies
- pinned comments
- task-level and comment-linked attachments
- precise task/comment/reply deep-link resolution

Authorization and tenant isolation remain backend authoritative for every collaboration read/write path.

## Attachment storage

Attachments use an S3-compatible abstraction backed by AWS SDK v2 and designed for Cloudflare R2.

The upload lifecycle is two-phase:

```text
client requests upload authorization
        ↓
backend validates tenant/project/task/comment scope
        ↓
backend returns presigned object-storage upload
        ↓
client uploads object directly
        ↓
client completes attachment metadata flow
        ↓
backend verifies durable attachment state and object metadata
```

Download/delete operations repeat scope and authorization validation rather than trusting client-supplied object keys.

Attachment lifecycle hardening includes pessimistic completion/deletion locking, idempotent state handling, stale `PENDING` cleanup, retryable deferred object deletion and comment-deletion cleanup.

Storage remains environment configured. Never commit R2 credentials.

## Collaboration schema history

```text
V21__create_task_collaboration.sql
V22__create_task_attachments.sql
V23__harden_task_attachment_cleanup.sql
V24__add_comment_threads_and_pins.sql
```

The one-level reply model is intentional; do not silently turn it into arbitrary-depth recursion without revisiting API, query and UI behavior.

## Notification persistence and delivery

Tenant notifications are recipient scoped and include:

- durable notification records
- recipient list and unread count
- mark-one-read and mark-all-read
- safe internal deep links
- durable external delivery records
- bounded retry/backoff
- processing leases/timeouts
- idempotency-oriented claiming
- email delivery through the provider abstraction
- recipient email preferences
- in-app notification bell and unread badge

The authenticated actor/recipient context is authoritative; clients cannot choose an arbitrary recipient identity.

Schema:

```text
V25__create_notifications.sql
V26__create_notification_deliveries.sql
V27__create_notification_preferences.sql
```

## Current product events

```text
TASK_ASSIGNED
TASK_STATUS_CHANGED
TASK_COMMENT_ADDED
TASK_COMMENT_REPLIED
TASK_COMMENT_MENTIONED
PROJECT_MEMBERSHIP_CHANGED
WORKSPACE_INVITATION
SECURITY_ALERT
```

Current producers cover task assignment/reassignment, task lifecycle changes, top-level comments, replies, mentions and project membership changes. Recipient policy suppresses self-notifications and deduplicates overlapping mention/reply/assignee targets.

`WORKSPACE_INVITATION` remains available in the catalogue/preferences surface for future product-level wiring. Security-alert email remains mandatory/non-configurable.

## Precise deep links

Collaboration notification targets carry internal project/task/comment/reply identifiers. The frontend can open the task collaboration drawer, select the Comments tab, resolve comments outside the first page, expand the correct parent thread, highlight/scroll the exact comment or reply, and clean query parameters when the drawer closes.

Removal-from-project notifications intentionally target `/projects` because the removed user may no longer be authorized for the former project.

## Remaining optional work

The original collaboration/notification expansion is complete. Optional follow-ups include:

1. workspace invitation in-app wiring
2. digest/batching behavior
3. live browser delivery via SSE/WebSocket
4. delivery/admin observability
5. provider bounce/complaint processing if needed

These are **not** the next platform foundation. Billing has now also been completed at application level through PR #98.

The recommended next major product milestone is **tenant-configurable outbound webhooks**, which can reuse lessons from the existing durable notification delivery model: retries, leases, idempotency, recipient/endpoint scoping and operational visibility.

## Invariants

```text
tenant isolation is mandatory
backend authorization is authoritative
notification recipients come from server-side domain context
external delivery must be retry-safe
email preferences never erase mandatory in-app history
object keys are not authorization credentials
all schema changes use new Flyway migrations
```

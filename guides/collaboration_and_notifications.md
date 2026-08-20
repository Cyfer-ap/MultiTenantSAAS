# Collaboration, attachments and notifications

This guide summarizes the current tenant-scoped collaboration stack and the notification/delivery subsystem that supports it. The snapshot is current through PR #65.

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

Attachments use an S3-compatible abstraction backed by the AWS SDK v2 and designed for Cloudflare R2.

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

Download/delete operations repeat scope/authorization validation rather than trusting object keys supplied by the client.

Attachment lifecycle hardening includes pessimistic completion/deletion locking, idempotent state handling, stale `PENDING` cleanup, retryable deferred object deletion and comment-deletion cleanup.

Storage remains environment configured. Never commit R2 credentials.

## Collaboration schema history

```text
V21__create_task_collaboration.sql
V22__create_task_attachments.sql
V23__harden_task_attachment_cleanup.sql
V24__add_comment_threads_and_pins.sql
```

The one-level reply model is intentional; do not silently turn it into arbitrary-depth recursive threads without revisiting API, query and UI behavior.

## Notification persistence

Tenant notifications are recipient scoped. Current capabilities include:

- create/store notification records
- list current recipient notifications
- unread count
- mark one notification read
- mark all notifications read
- safe internal deep-link target

The authenticated actor/recipient context is authoritative; clients must not be allowed to select an arbitrary recipient identity.

Schema:

```text
V25__create_notifications.sql
```

## Durable delivery

Notification delivery records provide a PostgreSQL-backed reliability layer for external side effects.

The delivery design includes:

- durable delivery state
- bounded retry attempts
- retry/backoff scheduling
- processing leases/timeouts
- idempotency-oriented claiming/processing
- provider failure handling

Schema:

```text
V26__create_notification_deliveries.sql
```

This foundation supports email delivery through the existing email-provider abstraction.

## Notification preferences

Recipients can configure optional email delivery per configurable notification event while in-app notification history remains mandatory.

Important policy rules:

- in-app persistence is not disabled by an email opt-out
- optional email defaults to enabled for backward compatibility
- security-alert email is mandatory/non-configurable
- preference lookup remains tenant + recipient + notification-type scoped

Schema:

```text
V27__create_notification_preferences.sql
```

## Current product events

The notification catalogue includes:

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

Current producers cover:

- assignment/reassignment of a task to another project member
- task status/cancellation changes for the assignee when the actor differs
- top-level task comments for the assignee
- replies for the parent comment author
- mentions for explicitly mentioned project members
- project membership add/role-change/remove lifecycle events

Recipient policy suppresses self-notifications and deduplicates overlapping mention/reply/assignee targets.

`WORKSPACE_INVITATION` is present in the type catalogue and preferences surface but remains available for future product-level invitation notification wiring.

## Precise deep links

Collaboration notification targets carry internal project/task/comment/reply identifiers. The frontend can:

- open the task collaboration drawer
- open the Comments tab
- resolve a linked comment directly even when it is outside the normal first comment page
- expand the correct parent thread
- highlight and scroll to the exact top-level comment or reply
- clean comment/reply query parameters when the drawer closes

Removal-from-project notifications intentionally target `/projects` because a removed user may no longer be authorized for the former project.

## In-app notification center

The authenticated application shell includes a notification bell with:

- unread badge
- notification list
- loading/empty/error states
- read/unread state
- mark-all-read
- safe internal deep-link navigation

## Configuration

Notification delivery is environment controlled. Compose/production configuration should preserve delivery enablement, batch sizing, retry limits, processing timeout, retry delays and frontend base URL as environment values.

Object storage is likewise environment controlled through the storage provider/R2 endpoint/bucket/credential configuration.

## Remaining product/operations opportunities

The original collaboration-notification expansion is complete. Further work is optional and should be driven by product or operations value:

1. workspace invitation in-app event wiring
2. optional digest/batching behavior
3. optional live notification delivery via SSE/WebSocket
4. delivery/admin observability when needed
5. provider bounce/complaint processing if email operations require it

External billing is the next major platform milestone; notification foundation work should not block it.

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

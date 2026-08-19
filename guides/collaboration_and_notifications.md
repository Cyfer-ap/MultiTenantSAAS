# Collaboration, attachments and notifications

This guide summarizes the current tenant-scoped collaboration stack and the notification/delivery foundation that supports it.

## Task collaboration

Task collaboration is scoped by tenant, project and task. Implemented capabilities include:

- comments
- mentions
- task activity history
- one-level replies
- pinned comments
- task-level and comment-linked attachments

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
backend verifies/records durable attachment state
```

Download/delete operations repeat scope/authorization validation rather than trusting object keys supplied by the client.

Storage should remain environment-configured. Never commit R2 credentials.

## Attachment schema history

```text
V22__create_task_attachments.sql
V23__harden_task_attachment_cleanup.sql
```

## Comment threads and pins

One-level replies and pinned comments were added after the initial collaboration schema.

```text
V21__create_task_collaboration.sql
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
- optional internal deep-link target

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

This foundation currently supports email delivery through the existing email-provider abstraction.

## In-app notification center

The authenticated application shell includes a notification bell with:

- unread badge
- notification list
- loading/empty/error states
- read/unread state
- mark-all-read
- safe internal deep-link navigation

The first product event wired into the system is task assignment/reassignment to another project member.

## Configuration

Notification delivery is environment controlled. Compose/production configuration should preserve delivery enablement, batch sizing, retry limits, processing timeout, retry delays and frontend base URL as environment values.

Object storage is likewise environment controlled through the storage provider/R2 endpoint/bucket/credential configuration.

## Next product work

High-value follow-up:

1. comment notifications
2. reply notifications
3. mention notifications
4. task status/collaboration-event notifications
5. precise task/comment/thread deep links
6. user notification preferences
7. channel controls and optional digests

## Invariants

```text
tenant isolation is mandatory
backend authorization is authoritative
notification recipients come from server-side domain context
external delivery must be retry-safe
object keys are not authorization credentials
all schema changes use new Flyway migrations
```

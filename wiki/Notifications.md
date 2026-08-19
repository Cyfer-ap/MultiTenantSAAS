# Notifications

The notification subsystem is tenant scoped and separates durable notification records from external delivery attempts.

## Current capabilities

- tenant- and recipient-scoped notification persistence
- recipient notification list
- unread count
- mark-one-read
- mark-all-read
- safe internal deep-link target
- durable delivery records
- retry/backoff/lease/idempotency-oriented processing
- email delivery through the existing email-provider abstraction
- in-app notification bell and unread badge
- task-assignment/reassignment notifications

## Schema

```text
V25__create_notifications.sql
V26__create_notification_deliveries.sql
```

## Recipient security

The backend derives tenant and recipient context from authenticated/domain state. Clients must not be allowed to select an arbitrary recipient identity.

## Delivery reliability

External notification delivery uses durable PostgreSQL state rather than assuming a synchronous provider call is sufficient.

The delivery model includes:

- bounded attempts
- retry scheduling/backoff
- processing leases/timeouts
- idempotency-oriented claiming
- provider failure handling

This is the reliability foundation for email and future delivery channels.

## In-app notification center

The authenticated application shell provides:

- unread badge
- notification popover/list
- loading, empty and error states
- read/unread behavior
- mark-all-read
- internal navigation for safe target URLs

The first wired product event is assignment/reassignment of a task to another project member.

## Configuration

Delivery behavior is environment configurable, including enablement, batch size, maximum attempts, processing timeout, retry delays and frontend base URL.

## Next product work

1. comment notifications
2. reply notifications
3. mention notifications
4. task-status and collaboration-event notifications
5. precise task/comment/thread deep links
6. user notification preferences
7. per-channel controls and optional digests

## Related pages

- [[Collaboration-and-Attachments]]
- [[Projects-and-Tasks]]
- [[Operations-and-Observability]]
- [[Roadmap]]

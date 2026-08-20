# Notifications

The notification subsystem is tenant scoped and separates durable notification records from external delivery attempts. This page reflects the post-PR #65 state.

## Current capabilities

- tenant- and recipient-scoped notification persistence
- recipient notification list
- unread count
- mark-one-read
- mark-all-read
- safe internal deep-link targets
- durable delivery records
- retry/backoff/lease/idempotency-oriented processing
- email delivery through the existing email-provider abstraction
- in-app notification bell and unread badge
- task assignment/reassignment notifications
- task status/cancellation notifications
- top-level task comment notifications
- comment reply notifications
- mention notifications
- project membership add/role-change/remove notifications
- precise task/comment/reply deep links
- recipient-scoped optional email preferences

## Notification type catalogue

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

`WORKSPACE_INVITATION` is available in the catalogue/preferences surface for future product-level wiring. Security-alert email remains mandatory/non-configurable.

## Schema

```text
V25__create_notifications.sql
V26__create_notification_deliveries.sql
V27__create_notification_preferences.sql
```

## Recipient security

The backend derives tenant and recipient context from authenticated/domain state. Clients must not be allowed to select an arbitrary recipient identity.

Recipient policy also suppresses self-notifications and deduplicates overlapping assignee/reply/mention recipients.

## Delivery reliability

External notification delivery uses durable PostgreSQL state rather than assuming a synchronous provider call is sufficient.

The delivery model includes:

- bounded attempts
- retry scheduling/backoff
- processing leases/timeouts
- idempotency-oriented claiming
- stale-lease recovery
- provider failure handling

This is the reliability foundation for email and future delivery channels.

## Preferences

Recipients can configure optional email delivery by supported notification event.

The policy intentionally separates persistence from delivery:

```text
domain event
    ↓
mandatory in-app notification
    ↓
recipient email preference
    ├─ enabled  -> enqueue durable email delivery
    └─ disabled -> keep in-app history, skip optional email
```

Security alerts are not user-configurable.

## In-app notification center

The authenticated application shell provides:

- unread badge
- notification popover/list
- loading, empty and error states
- read/unread behavior
- mark-all-read
- internal navigation for safe target URLs

## Precise collaboration deep links

Task collaboration notifications can target the exact task comment/reply. The project workspace resolves and displays the target even when it falls outside the normal first page of comments.

Supported behavior includes:

- open the task collaboration drawer
- open the Comments tab
- highlight/scroll the exact top-level comment
- expand the correct parent thread
- highlight/scroll the exact reply
- remove comment/reply query parameters when the drawer closes

Project-removal notifications target `/projects` because the removed user may no longer be authorized for the former project.

## Configuration

Delivery behavior is environment configurable, including enablement, batch size, maximum attempts, processing timeout, retry delays and frontend base URL.

## Remaining optional work

The previously planned collaboration-notification expansion is complete. Remaining work should be driven by product/operations value:

1. workspace-invitation in-app event wiring
2. optional digest/batching
3. optional live browser delivery via SSE/WebSocket
4. optional push/mobile channels
5. delivery/admin observability
6. provider bounce/complaint processing if needed

Notification foundation work should not block the next major platform milestone: external billing.

## Related pages

- [[Collaboration-and-Attachments]]
- [[Projects-and-Tasks]]
- [[Operations-and-Observability]]
- [[Roadmap]]

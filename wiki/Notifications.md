# Notifications

Reviewed for the current platform snapshot on 2026-09-06. The notification subsystem is tenant scoped and separates durable notification records from external delivery attempts. Its core product expansion was completed before the billing milestone; Billing & Payments is now also complete at application level through PR #98.

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

This is the reliability foundation for email and future delivery channels. The same architectural ideas are relevant to the recommended tenant outbound-webhook milestone, although outbound webhooks require additional endpoint security, signing and SSRF controls.

## Preferences

Recipients can configure optional email delivery by supported notification event while in-app history remains mandatory.

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

The authenticated application shell provides an unread badge, notification popover/list, loading/empty/error states, read/unread behavior, mark-all-read and safe internal navigation.

## Precise collaboration deep links

Task collaboration notifications can target the exact task comment/reply. The project workspace resolves the target even when it falls outside the normal first page, expands the correct thread and highlights/scrolls to the requested comment or reply.

Project-removal notifications target `/projects` because the removed user may no longer be authorized for the former project.

## Configuration

Delivery behavior is environment configurable, including enablement, batch size, maximum attempts, processing timeout, retry delays and frontend base URL.

## Remaining optional work

The planned collaboration-notification expansion is complete. Remaining optional work is product/operations driven:

1. workspace-invitation in-app event wiring
2. digest/batching
3. live browser delivery via SSE/WebSocket
4. push/mobile channels
5. delivery/admin observability
6. provider bounce/complaint processing if needed

Notification work does not block the current recommended next milestone: **tenant-configurable outbound webhooks**.

## Related pages

- [[Collaboration-and-Attachments]]
- [[Projects-and-Tasks]]
- [[Operations-and-Observability]]
- [[Roadmap]]

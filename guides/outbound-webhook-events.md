# Outbound webhook event contract

Tenant outbound webhook events use a common envelope:

```json
{
  "id": "<event-uuid>",
  "type": "project.created",
  "tenantId": "<tenant-uuid>",
  "occurredAt": "<ISO-8601 instant>",
  "data": {}
}
```

The `id` is stable across retries. Delivery attempts reuse the exact stored JSON body.

## Event sources

- `project.created` — project creation
- `project.updated` — project details or non-archive status changes
- `project.archived` — project archive
- `task.created` — task creation
- `task.updated` — task details, assignee, non-completion status, or cancellation changes
- `task.completed` — transition to completed
- `comment.created` — top-level task comment creation
- `comment.replied` — task comment reply creation
- `member.added` — explicit project-member addition
- `member.removed` — explicit project-member removal
- `subscription.updated` — subscription start, plan/lifecycle changes, expiry, or provider synchronization
- `subscription.cancelled` — cancellation from either direct lifecycle management or provider synchronization

Project and task events reuse the corresponding REST response DTO shape. Comment events expose identifiers, author, body and timestamps without serializing JPA relationships. Subscription events use one provider-neutral payload shape for both application-driven and Stripe/Razorpay-synchronized updates.

Events are persisted only when at least one enabled endpoint subscribes to the event type. This prevents tenants without webhook subscriptions from accumulating unused outbox rows.

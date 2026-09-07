# Outbound webhook delivery history and replay

Tenant administrators can inspect outbound webhook delivery state and manually replay terminal deliveries.

## Delivery history API

Base path:

`/api/tenants/{tenantId}/outbound-webhooks/deliveries`

Supported operations:

- `GET /` — paginated delivery history, optionally filtered by `endpointId` and `status`.
- `GET /{deliveryId}` — delivery detail including the exact stored event payload and ordered attempt history.
- `POST /{deliveryId}/replay` — queues a terminal `SENT` or `FAILED` delivery for a new delivery cycle.

All operations require the existing `tenant.update` permission and remain tenant-scoped.

## Attempt history

Each worker claim creates a durable attempt record containing:

- replay number
- attempt number within the replay cycle
- lease token
- processing/success/failure outcome
- HTTP status when available
- bounded failure text
- start and completion timestamps

Stale processing leases are closed as failed before the delivery is reclaimed. Historical attempts are never erased by a replay.

## Replay semantics

A replay:

- is accepted only for terminal `SENT` or `FAILED` deliveries;
- requires the endpoint to still be active and enabled;
- preserves the immutable event ID and stored payload;
- increments the replay counter;
- resets the per-cycle retry count;
- clears terminal transport state and places the delivery back into `PENDING`;
- is recorded in the tenant audit log.

The worker then handles the replay using the same signing, SSRF validation, timeout, lease and retry rules as any normal delivery.

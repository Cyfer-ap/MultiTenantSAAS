# Tenant outbound webhook administration

Tenant users with the `tenant.update` permission can manage outbound webhooks from **Integrations** in the workspace navigation.

## Endpoint management

The Integrations page supports:

- creating HTTPS webhook endpoints;
- selecting tenant event subscriptions;
- enabling or disabling deliveries;
- editing endpoint name, URL, status and events;
- rotating signing secrets;
- archiving endpoints.

The signing secret returned when an endpoint is created or rotated is intentionally shown only once. Store it in the receiving system before closing the secret dialog. Subsequent endpoint responses expose only the secret hint and version.

## Delivery observability

The same page provides paginated delivery history with endpoint and status filters. A delivery detail view includes:

- event and endpoint metadata;
- current delivery status;
- the immutable stored payload;
- ordered delivery-attempt history;
- HTTP status and bounded error details.

Terminal `SENT` or `FAILED` deliveries can be replayed when their endpoint still exists and is enabled. Replay keeps the original event and payload while starting a new delivery cycle.

## Access control

Frontend navigation and routing use `tenant.update`, matching the backend authorization policy on the outbound-webhook endpoint and delivery-management APIs.

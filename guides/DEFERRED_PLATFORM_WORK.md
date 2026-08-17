# Deferred Platform Work

This file tracks platform work that is intentionally being postponed while product-facing UX features are developed. These items are debt to be paid, not discarded roadmap ideas. Keep this list visible during feature planning and retire entries only when the underlying platform capability is actually delivered.

## Priority platform debt

1. **Durable background jobs and transactional outbox**
   - PostgreSQL-backed durable event/job dispatch.
   - Retry, backoff, idempotency, dead-letter handling, and worker observability.
   - Use as the reliability foundation for external side effects.

2. **Production notification delivery**
   - Move email and future notification delivery onto durable processing.
   - Delivery state, retry policy, provider failure handling, and audit visibility.

3. **External billing integration**
   - Connect the existing subscription/entitlement model to a payment provider.
   - Signed webhooks, replay protection, reconciliation, and customer/subscription mapping.

4. **Usage metering and quota accounting**
   - Durable consumption events and atomic accounting periods.
   - Idempotent usage recording, reset semantics, history, and enforcement visibility.

5. **Webhook platform**
   - Tenant-configurable outbound webhooks with signing, retry history, and delivery logs.
   - SSRF protections and secret handling appropriate for tenant-controlled destinations.

6. **API keys and service accounts**
   - Non-browser access with tenant binding, scoped permissions, rotation, expiration, revocation, and audit history.
   - Store only hashed credentials and reveal secrets once at creation.

7. **Operational recovery and alerting**
   - Database backup/restore procedure and tested recovery path.
   - Actionable service/database alerts and production runbooks.

8. **Enterprise SSO / federated authentication**
   - Tenant-configurable OIDC/SAML-style enterprise sign-in, account linking, domain discovery, and optional enforced SSO.

## Revisit rule

Revisit an item when a product feature starts depending on it, before relevant external integrations are considered production-ready, or when scale/reliability makes the current synchronous path unsafe.

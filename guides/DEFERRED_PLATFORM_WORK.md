# Deferred Platform Work

This file tracks platform capabilities that remain intentionally deferred while product-facing work continues. Retire an entry only when the underlying capability is actually delivered.

## Recently retired from this list

### Durable notification outbox foundation — delivered

PR #57 added PostgreSQL-backed notification delivery records with retry/backoff, lease-token/idempotency-oriented processing and durable delivery state.

### Production notification email delivery foundation — delivered

PR #58 connected notification delivery to the existing email-provider abstraction. PR #59 added recipient-scoped in-app task-assignment notifications and the notification center.

These capabilities are no longer generic deferred platform debt. Future notification work is feature expansion and operational hardening.

## Priority platform debt

1. **External billing integration**
   - connect the existing subscription/entitlement model to a payment provider
   - signed webhooks, replay protection, reconciliation and customer/subscription mapping

2. **Usage metering and quota accounting**
   - durable consumption events and atomic accounting periods
   - idempotent usage recording, reset semantics, history and enforcement visibility

3. **Webhook platform**
   - tenant-configurable outbound webhooks with signing, retry history and delivery logs
   - SSRF protections and secure handling of tenant-controlled destinations/secrets

4. **API keys and service accounts**
   - non-browser access with tenant binding, scoped permissions, rotation, expiration, revocation and audit history
   - store only hashed credentials and reveal secrets once at creation

5. **Operational recovery and alerting**
   - database backup/restore procedure and tested recovery path
   - actionable service/database alerts and production runbooks
   - broader load/failure-recovery verification

6. **Enterprise SSO / federated authentication**
   - tenant-configurable OIDC/SAML-style enterprise sign-in
   - account linking, domain discovery and optional enforced SSO

7. **Advanced authorization ergonomics**
   - temporary/scoped authorization delegation
   - explain-access API/UI showing why a subject can or cannot perform an action

## Notification follow-up backlog

The notification foundation exists. Remaining product work includes:

- comment/reply/mention notifications
- task status and collaboration-event notifications
- precise task/comment/thread deep links
- user notification preferences
- per-channel controls and optional digesting
- delivery/admin observability if operational needs justify it

## Revisit rule

Revisit a deferred item when a product feature starts depending on it, before a relevant external integration is treated as production-ready, or when scale/reliability makes the current approach unsafe.

When an item is implemented, link the implementing PR and move it to the retired section rather than leaving stale debt in the active list.

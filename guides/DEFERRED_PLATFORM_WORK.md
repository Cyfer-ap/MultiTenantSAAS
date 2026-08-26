# Deferred Platform Work

Reviewed through PR #84 on 2026-08-26. Retire an item only when it is implemented and verified at the appropriate boundary.

## Recently delivered

- billing provider abstraction and Stripe/Razorpay adapters — PRs #67-#70
- tenant checkout API, signed webhooks and subscription synchronization — PRs #71-#73
- provider-backed cancellation, operations visibility and reconciliation — PRs #74-#76
- durable usage metering — PR #77
- tenant API-key lifecycle, authentication and metering — PRs #78-#79
- plan-level API request quotas — PR #80
- tenant checkout discovery/UI and recovery fixes — PRs #81-#84

These are no longer deferred foundations.

## Active billing validation debt

### Razorpay Test Mode E2E

The hosted subscription page opens, but every attempted card currently fails inside Razorpay before recurring authorization. Activation webhooks and local subscription activation are therefore not validated against the real provider.

Required closeout:

- dashboard-native subscription test
- structured failed-payment diagnostics
- Razorpay Support escalation if necessary
- deployed activation, renewal/charged, pending/halted, cancellation, replay and reconciliation smoke tests
- credential rotation for any exposed test secret

Live KYC, live keys and live plan mapping remain deferred until Test Mode passes.

### Optional application lifecycle simulator

A feature-flagged, system-admin-only simulator could drive signed-equivalent internal lifecycle scenarios without external money movement. It is useful for application testing but cannot replace provider E2E.

## Remaining platform debt

1. **Operational recovery and alerting**
   - database backup/restore drills
   - actionable service/database alerts and runbooks
   - broader load and failure-recovery verification
   - production R2 operational validation

2. **Tenant outbound webhooks**
   - tenant-configurable endpoints, signing, retries and delivery logs
   - SSRF protections and secret handling

3. **Enterprise SSO**
   - OIDC/SAML-style sign-in, account linking, discovery and enforcement

4. **Advanced authorization**
   - temporary/scoped delegation
   - explain-access API/UI

5. **Optional notification expansion**
   - invitation events, digests, live browser delivery and admin observability

## Revisit rule

Revisit a deferred item when product work depends on it or before an external integration is described as production-ready.

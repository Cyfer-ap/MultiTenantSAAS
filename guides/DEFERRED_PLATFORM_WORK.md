# Deferred / Following Platform Work

Reviewed state: post-PR #119 (`c36de3f`), 2026-09-08.

Billing/catalog, tenant outbound webhooks and enterprise OIDC SSO are complete at application level.

## Remaining platform work

1. **Authorization delegation and explain-access** — recommended next product milestone
   - controlled delegation boundaries, expiry/revocation where justified
   - trace effective access through tenant/scoped roles and assignments
   - explain why a user can/cannot access a resource without exposing sensitive internals
   - admin APIs/UX, audit events and cross-tenant regression tests

2. **Backup/restore, monitoring and operational runbooks**
   - PostgreSQL backup/restore drill
   - alerting and failure-response procedures
   - provider/webhook operational runbooks

3. **Broader load/failure-recovery validation**
   - concurrency/load envelopes
   - worker/provider outage recovery
   - production R2 validation

4. **Optional/demand-driven identity expansion**
   - SAML adapter through the existing provider-neutral federation boundary
   - SCIM/directory provisioning if required by enterprise customers
   - provider metadata/JWKS caching only if scale/performance evidence justifies it

5. **Optional notification expansion**
   - digests
   - live browser delivery
   - additional channel integrations

## Independent provider/live-readiness track

- Stripe remains the validated Test Mode path
- Razorpay remains implemented while recurring sandbox authorization is blocked
- live billing credentials/catalog/compliance readiness is separate from application feature completeness
- production outbound-webhook receiver readiness is also deployment-specific

Do not reopen completed application milestones solely because an external provider sandbox or production readiness task is pending.

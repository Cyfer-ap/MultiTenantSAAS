# Deferred / Following Platform Work

Reviewed state: post-PR #125 (`0694403`), 2026-09-13.

Billing/catalog, tenant outbound webhooks, enterprise OIDC SSO, and authorization delegation/Explain Access are complete at application level.

## Remaining platform work

1. **Production Operations & Disaster Recovery — recommended next milestone**
   - PostgreSQL backup policy and restore drill into an isolated database
   - health/readiness/metrics verification
   - alerting and failure-response procedures
   - deployment, database, SSO, billing and webhook incident runbooks
   - secret/key rotation and recovery procedures

2. **Broader load/failure-recovery validation**
   - concurrency/load envelopes
   - worker/provider outage recovery
   - production R2 validation

3. **Optional/demand-driven identity expansion**
   - SAML adapter through the existing provider-neutral federation boundary
   - SCIM/directory provisioning if required by enterprise customers
   - provider metadata/JWKS caching only if scale/performance evidence justifies it

4. **Optional notification expansion**
   - digests
   - live browser delivery
   - additional channel integrations

## Independent provider/live-readiness track

- Stripe remains the validated Test Mode path
- Razorpay remains implemented while recurring sandbox authorization is blocked
- live billing credentials/catalog/compliance readiness is separate from application feature completeness
- production outbound-webhook receiver readiness is deployment-specific

Do not reopen completed application milestones solely because an external provider sandbox or production readiness task is pending.

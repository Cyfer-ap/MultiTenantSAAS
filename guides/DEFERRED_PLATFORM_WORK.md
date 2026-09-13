# Deferred / Following Platform Work

Reviewed state: post-PR #126 (`5013260`), 2026-09-13.

Billing/catalog, tenant outbound webhooks, enterprise OIDC SSO, and authorization delegation/Explain Access are complete at application level.

The immediate development direction is **Product Experience & Work Management Enrichment**. This file now records platform work intentionally deferred behind that user-facing phase.

## Deferred platform work

1. **Production Operations & Disaster Recovery**
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
   - MFA/passkeys/session-device controls when prioritized
   - provider metadata/JWKS caching only if scale/performance evidence justifies it

4. **Optional notification expansion**
   - digests
   - live browser delivery
   - web/mobile push
   - additional channel integrations

## Current product work lives elsewhere

See:

- `Wild_Thoughts.md` for the audited product idea vault and core product gaps
- `../wiki/Roadmap.md` for the current enrichment sequence
- `../CHECKPOINT.md` for the authoritative current checkpoint

## Independent provider/live-readiness track

- Stripe remains the validated Test Mode path
- Razorpay remains implemented while recurring sandbox authorization is blocked
- live billing credentials/catalog/compliance readiness is separate from application feature completeness
- production outbound-webhook receiver readiness is deployment-specific

Do not reopen completed application milestones solely because an external provider sandbox or production readiness task is pending.

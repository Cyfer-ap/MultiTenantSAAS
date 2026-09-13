# Deferred / Following Platform Work

This file records important platform work intentionally deferred behind the current user-facing product-enrichment phase. Current repository status belongs in `../CHECKPOINT.md`.

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

- `../CHECKPOINT.md` for current repository/application status
- `../HANDOFF.md` for the current resume point
- `Wild_Thoughts.md` for the audited product-idea vault
- `../wiki/Roadmap.md` for product direction
- `ENGINEERING_STANDARDS.md` for architecture-quality rules and the technical-debt register

## Independent provider/live-readiness track

- Stripe remains the validated Test Mode path
- Razorpay remains implemented while recurring sandbox authorization is blocked
- live billing credentials/catalog/compliance readiness is separate from application feature completeness
- production outbound-webhook receiver readiness is deployment-specific

Do not reopen completed application milestones solely because an external provider sandbox or production-readiness task is pending.

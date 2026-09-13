# Roadmap

## Completed application milestones

### Billing/catalog lifecycle

Closed through PR #106.

### Tenant-configurable outbound webhooks

Closed through PR #112.

### Enterprise OIDC SSO / identity federation

Closed through PR #119.

### Authorization delegation and Explain Access

Closed through PR #125.

Completed authorization capabilities include:

- structured access decisions from the enforcement evaluator
- tenant-admin Explain Access with stable decision reasoning
- V44 durable delegation provenance and `authorization.delegate`
- create/list/revoke delegation lifecycle and audit events
- explicit direct parent assignment for each delegated grant
- permission/scope/validity non-escalation enforcement
- one-level delegation only and protected authorization permissions
- runtime source revalidation after parent revocation/expiry/narrowing
- delegation-safe reference data for delegate-only actors
- direct-vs-delegated Explain Access provenance
- manager and delegate-only Authorization workspace UX

## Next major product milestone

### 1. Production Operations & Disaster Recovery

Target capabilities:

- defined PostgreSQL backup/export and retention strategy
- repeatable isolated restore drill with validation
- health/readiness and operational metrics review
- actionable alerts for application, database, integration and provider failures
- deployment/database/SSO/billing/webhook incident runbooks
- secret/key rotation and recovery procedures
- evidence that recovery procedures work rather than documentation-only readiness

Recommended sequence:

1. inventory current Render/PostgreSQL operational capabilities and failure signals
2. implement/document backup and isolated restore drill
3. verify Actuator/readiness/metrics and define alert thresholds
4. write incident/runbook procedures around real failure modes
5. exercise recovery paths and capture expected evidence

## Following platform work

2. broader load/failure-recovery and production R2 verification
3. optional SAML/SCIM where concrete enterprise requirements exist
4. optional notification expansion such as digests/live browser delivery

## Independent provider/live-readiness track

- preserve Stripe as the working/validated Test Mode path
- keep Razorpay integration/catalog provisioning available while recurring sandbox authorization remains blocked
- enable live credentials/catalog only after provider-specific readiness review
- validate provider account configuration, billing webhook endpoints, tax/compliance and production operational runbooks separately

## Engineering rules

Preserve tenant isolation, backend-authoritative authorization, delegation non-escalation, verified provider reconciliation, webhook-authoritative normal billing state, immutable history, Flyway invariants, database-backed concurrency, auditability, SSRF protections and server-only secrets/provider identifiers.

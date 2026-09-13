# Developer Handoff

Current snapshot: post-PR #125 (`0694403`), 2026-09-13.

## Current phase

**Authorization delegation and Explain Access complete at application level; Production Operations & Disaster Recovery next.**

Billing/catalog, outbound-webhook and enterprise OIDC SSO milestones remain closed.

## Resume reading

1. [[Home]]
2. [[Authorization]]
3. [[Enterprise-SSO]]
4. [[Security-and-Authentication]]
5. [[Production-Deployment]]
6. [[Testing-and-CI]]
7. [[Roadmap]]

## Preserve these authorization invariants

- tenant isolation precedes permission evaluation
- Explain Access and enforcement share the same evaluator
- every delegated grant derives from one direct parent authority assignment
- delegated permission/scope/validity never exceed the current parent authority
- delegated assignments cannot be re-delegated
- `authorization.manage` and `authorization.delegate` remain non-delegable
- source authority is revalidated during access evaluation
- frontend filtering never replaces backend validation
- cross-tenant subjects/resources remain invalid
- explanations expose matched provenance only, not unrelated grants

Portable migrations extend through V44.

## Preserve these SSO invariants

Keep tenant-bound identity linking, encrypted write-only provider secrets, SSRF-safe provider validation, state/nonce/PKCE protections, no IdP auto-provisioning, backend-authoritative SSO policy, tenant-admin break-glass and opaque one-time browser handoff.

## Provider status

Stripe is working/validated in deployed Test Mode. Razorpay application/catalog integration is implemented, but recurring Test Mode authorization remains provider-sandbox blocked.

## Next

Build the **Production Operations & Disaster Recovery** milestone: backup/restore drills, monitoring, alerts and operational runbooks, then broaden failure-recovery/load and production R2 verification.

# Testing and CI

Reviewed through PR #106 on 2026-09-07.

## Authoritative gates

GitHub Actions is authoritative where local Docker is unavailable. Required repository coverage includes:

- Repository Hygiene
- Backend tests and Maven verification
- PostgreSQL/Flyway integration
- Frontend formatting, lint, tests and build
- Security/Trivy
- Container CI
- Qodana

## Billing/catalog coverage in CI

Automated tests cover:

- provider registry and checkout validation
- Stripe and Razorpay HTTP contracts with mock servers
- signature verification, replay/duplicate handling and durable events
- lifecycle mapping and out-of-order/terminal-state safety
- cross-provider subscription overwrite protection
- verified provider ownership/history cancellation recovery
- Stripe period-end cancellation request semantics
- idempotent reconciliation when a provider is already terminal
- managed Stripe Product/Price provisioning and replacement
- Stripe retirement Product/Price archival and mapping retention
- managed Razorpay Plan provisioning/replacement
- retry adoption of fingerprinted Razorpay Plans
- DB-first provider mapping resolution and legacy import/fallback
- durable plan-retirement operation behavior and partial-failure retry
- purchased-plan snapshot semantics
- immutable subscription-history persistence/APIs
- tenant/system-admin history privacy and pagination
- retired-plan terminal frontend lifecycle behavior
- Spring bean/startup regression for Stripe retirement provisioner
- usage event idempotency and period aggregation
- tenant API-key lifecycle/authentication/metering
- plan-level API request quotas
- checkout recovery through the read-only interceptor
- HTTP-level webhook authentication/security boundaries

## Database coverage

PostgreSQL/Flyway integration extends through **V36** and validates the managed catalog/history schema:

- V34 provider mappings and purchased-plan snapshots
- V35 durable plan-retirement operations
- V36 immutable tenant subscription history

Never rewrite an applied migration.

## Provider E2E boundary

Stripe has passed deployed Test Mode checkout, signed lifecycle synchronization and provider-side cancellation validation.

The earlier missing `customer.subscription.deleted` endpoint configuration remains an operational lesson: provider contract tests cannot prove external dashboard/event-subscription configuration. The endpoint has since been corrected and stale terminal state is defensively recoverable.

Razorpay sandbox availability cannot be proven by CI. Application integration/catalog provisioning are covered, but attempted recurring Test Mode card authorization currently fails at the provider sandbox.

## Billing/catalog closure verification

Application-level billing/catalog lifecycle is considered complete because:

1. Stripe checkout/webhook/cancellation are validated against the real Test Mode provider
2. cancellation/reconciliation and stale-linkage recovery are covered
3. managed provider catalog create/update/retirement behavior is covered
4. existing paid-period entitlement survives safe plan retirement
5. purchased/history snapshots preserve immutable customer terms
6. invalid/unsigned webhook paths fail closed
7. PostgreSQL/Flyway protects V34-V36 schema behavior
8. frontend lifecycle/history UX is regression tested
9. the required CI matrix protects the sequence
10. Razorpay's remaining recurring-card failure occurs at external sandbox authorization rather than missing application architecture

## Without local Docker

Contributors may rely on GitHub Actions for PostgreSQL and container validation, then perform provider smoke testing only in deployed Test Mode. Live-provider configuration remains outside the application test scope.

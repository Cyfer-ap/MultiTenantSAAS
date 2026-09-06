# Testing and CI

Reviewed through PR #98 on 2026-09-06.

## Authoritative gates

GitHub Actions is authoritative where local Docker is unavailable. Required repository coverage includes:

- repository hygiene
- backend tests and Maven verification
- PostgreSQL/Flyway integration
- frontend formatting, lint, tests and build
- security scanning
- Compose and backend/frontend container validation
- Qodana

The billing hardening sequence through PR #98 was validated through the required CI families.

## Billing coverage in CI

Automated tests cover:

- provider registry and checkout validation
- Stripe and Razorpay HTTP contracts with mock servers
- signature verification, replay/duplicate handling and durable events
- lifecycle mapping and out-of-order/terminal-state safety
- cross-provider subscription overwrite protection
- provider ownership and durable-history cancellation recovery
- Stripe period-end cancellation request semantics
- idempotent reconciliation when a provider is already terminal
- cancellation and reconciliation
- usage event idempotency and period aggregation
- tenant API-key lifecycle/authentication/metering
- plan-level API request quotas
- checkout configuration and duplicate-subscription protection
- checkout recovery through the read-only interceptor
- HTTP-level webhook authentication/security boundaries

## Provider E2E boundary

Stripe has passed deployed Test Mode checkout, signed lifecycle synchronization and provider-side cancellation validation.

A critical deployment lesson from final cancellation testing: CI correctly covered the application mapper for `customer.subscription.deleted`, but the Stripe Dashboard webhook endpoint had not enabled that event. Stripe therefore cancelled successfully while the application missed the terminal event. The Test Mode endpoint now enables created/updated/deleted subscription events, and PR #98 provides defensive stale-state repair.

Razorpay sandbox availability cannot be proven by CI. Hosted Test Mode checkout opens, but attempted sandbox cards currently fail before recurring authorization.

## Billing closure verification

Application-level Billing & Payments is considered complete because:

1. Stripe checkout is validated against the real Test Mode provider
2. signed webhook synchronization is validated
3. provider-side cancellation is validated
4. missed terminal webhook state is recoverable idempotently
5. invalid/unsigned webhook paths fail closed
6. reconciliation and provider-linkage recovery are covered
7. the full CI matrix protects regressions
8. Razorpay's remaining failure occurs at external sandbox authorization rather than missing application architecture

## Without local Docker

Contributors may rely on GitHub Actions for PostgreSQL and container validation, then perform provider smoke testing only in deployed Test Mode. Live-provider configuration is outside the current application test scope.

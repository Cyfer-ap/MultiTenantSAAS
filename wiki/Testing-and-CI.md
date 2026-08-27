# Testing and CI

## Authoritative gates

GitHub Actions is authoritative while local Docker is unavailable. Required repository coverage includes:

- repository hygiene
- backend tests and Maven verification
- PostgreSQL/Flyway integration
- frontend formatting, lint, tests and build
- security scanning
- Compose and backend/frontend container validation
- Qodana

PR #90 passed these gates.

## Billing coverage in CI

Automated tests cover:

- provider registry and checkout validation
- Stripe and Razorpay HTTP contracts with mock servers
- signature verification, replay/duplicate handling and durable events
- lifecycle mapping and out-of-order/terminal-state safety
- cancellation and reconciliation
- usage event idempotency and period aggregation
- tenant API-key lifecycle/authentication/metering
- plan-level API request quotas
- checkout configuration and duplicate-subscription protection
- checkout recovery through the read-only interceptor
- Razorpay Spring constructor injection/startup regression
- Stripe Spring constructor injection/startup regression

## Provider E2E boundary

Stripe has passed a deployed Test Mode checkout and signed-webhook synchronization smoke test. CI still does not prove Razorpay sandbox availability: the deployed application opens Razorpay Test Mode checkout, but all attempted cards currently fail before recurring authorization. A successful Razorpay activation webhook/local activation cycle remains outstanding.

Required deployed smoke sequence:

1. successful hosted authorization
2. verified `subscription.activated`
3. charged/update behavior
4. pending/halted handling
5. cancellation and terminal webhook
6. duplicate/replay resistance
7. provider/local reconciliation

A browser GET to the webhook URL is not a webhook test.

## Without local Docker

Contributors may rely on GitHub Actions for PostgreSQL and container validation, then perform provider smoke testing only in the deployed Test Mode environment. Stripe Test Mode is currently working; Razorpay remains blocked. Live plans are not part of the current test scope.

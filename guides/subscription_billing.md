# Subscription and billing

Reviewed through PR #106 on 2026-09-07.

## Milestone status

**Billing, cancellation hardening and managed provider catalogs are complete at application level.**

Live-provider readiness remains a separate deployment/operations concern.

## Domain boundary

The platform keeps application plans, tenant subscription state, evaluated access, entitlements, quotas and payment-provider objects separate.

Application plans are stored in the platform database. Stripe Products/Prices and Razorpay Plans are provider objects linked through durable `subscription_plan_provider_mappings`. Provider IDs and secrets remain server-side.

## Managed provider catalog

System-admin paid-plan create/update operations synchronize enabled managed providers through a provider-neutral catalog layer.

### Stripe

- creates a Stripe Product + recurring Price for a new paid application plan
- stores TEST/LIVE provider mapping durably
- resolves checkout DB-first with legacy configured Price fallback/import
- economic edits (price/currency/interval) create replacement Prices rather than rewriting historical economics
- old Prices remain available for historical/existing subscription resolution but inactive for new sale
- plan retirement deactivates the active Product/Price for new purchase and retains provider references

### Razorpay

- creates a Razorpay subscription Plan for a new paid application plan
- stores TEST/LIVE provider mapping durably
- resolves checkout DB-first with legacy configured Plan fallback/import
- provider-visible edits create replacement Plans because Razorpay does not expose an equivalent mutable Plan lifecycle
- old local mappings are archived while historical Plan IDs remain resolvable
- deterministic application metadata allows retry adoption of an already-created matching Plan

## Plan lifecycle

- `ACTIVE`: available for purchase and normal entitlement
- `INACTIVE`: administrative hard-disable
- `RETIRED`: terminal catalog state; immediately removed from new checkout, not normally editable/reactivatable

Retirement does not destroy historical provider objects or purchased terms. Existing valid subscriptions retain entitlement through the current paid period. Provider-linked subscriptions are scheduled to cancel at period/cycle end, while normal local subscription state remains webhook-authoritative.

V35 persists durable retirement operations with retryable provider cleanup.

## Immutable purchased terms and history

V34 stores purchased-plan snapshots on tenant subscriptions, including plan code/name/description, billing interval, price/currency and resource limits. Later catalog edits therefore do not rewrite what a tenant bought.

V36 adds immutable `tenant_subscription_history` snapshots for material subscription state. History is available through paginated tenant and system-admin APIs and the corresponding frontend UI. Tenant-facing history hides provider subscription references; system-admin history can expose them for operational troubleshooting.

## Implemented billing flow

- `GET /api/tenants/{tenantId}/billing/checkout/configuration`
- `POST /api/tenants/{tenantId}/billing/checkout`
- professional plan selection → provider selection → hosted checkout UX
- hosted Stripe and Razorpay subscription checkout adapters
- `POST /api/billing/webhooks/stripe`
- `POST /api/billing/webhooks/razorpay`
- exact raw-body signature verification, durable events and duplicate/replay protection
- webhook-driven subscription lifecycle mapping
- cross-provider linkage protection and verified-history recovery
- `POST /api/tenants/{tenantId}/billing/cancel`
- provider-aware period-end cancellation and stale-terminal-state repair
- system-admin billing subscription/event/history views
- tenant subscription history view
- read-only provider reconciliation
- append-only usage events and plan-level `API_REQUESTS` limits
- tenant API keys authenticated only under `/api/external/**`

Local provider-linked subscription lifecycle is webhook-authoritative during normal operation. Verified provider lookup/reconciliation may repair stale terminal state when cancellation already succeeded but its webhook was missed.

## Read-only recovery

Ordinary tenant mutations are blocked when subscription access is read-only. Billing checkout is explicitly allowed as a recovery action, while tenant authorization remains required.

## Stripe configuration

Stripe can be enabled without disabling Razorpay. Legacy environment Price variables remain supported for compatibility, while managed plans use durable DB mappings first.

Example server-side configuration:

```dotenv
STRIPE_BILLING_ENABLED=true
STRIPE_SECRET_KEY=sk_test_...
STRIPE_PRICE_PRO=price_...
STRIPE_PRICE_ENTERPRISE=price_...
STRIPE_BILLING_ENVIRONMENT=TEST
STRIPE_SUCCESS_URL=https://multitenantsaas-frontend.onrender.com/subscription?checkout=success
STRIPE_CANCEL_URL=https://multitenantsaas-frontend.onrender.com/subscription?checkout=cancelled
STRIPE_WEBHOOK_ENABLED=true
STRIPE_WEBHOOK_SECRET=whsec_...
```

Webhook target:

```text
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/stripe
```

Required lifecycle events:

```text
customer.subscription.created
customer.subscription.updated
customer.subscription.deleted
```

The deployed Stripe Test Mode subscription flow is the validated payment path. This is not a live-mode readiness claim.

## Razorpay configuration

Legacy Plan variables remain supported for compatibility/import while managed plans use durable DB mappings first.

Example server-side configuration:

```dotenv
RAZORPAY_BILLING_ENABLED=true
RAZORPAY_KEY_ID=...
RAZORPAY_KEY_SECRET=...
RAZORPAY_PLAN_PRO=plan_...
RAZORPAY_PLAN_ENTERPRISE=plan_...
RAZORPAY_BILLING_ENVIRONMENT=TEST
RAZORPAY_SUBSCRIPTION_TOTAL_COUNT=120
RAZORPAY_WEBHOOK_ENABLED=true
RAZORPAY_WEBHOOK_SECRET=...
```

Webhook target:

```text
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/razorpay
```

Razorpay application integration and managed Plan provisioning are implemented, but real Test Mode recurring authorization remains provider-sandbox blocked. Hosted checkout opens while attempted sandbox cards fail before recurring authorization completes.

## Database checkpoint

Common Flyway migrations extend through **V36**. Never rewrite an applied migration.

## Closure decision

Do not extend billing merely to work around Razorpay sandbox/card behavior. Future payment work should be limited to real regressions, explicit live-readiness work, or deliberately scoped billing enhancements. The active product milestone is now tenant-configurable outbound webhooks.

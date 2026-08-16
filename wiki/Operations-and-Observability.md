# Operations and Observability

## Request correlation

Backend requests use:

```text
X-Request-ID
```

for correlation.

When a request ID is supplied it is validated/normalized according to the backend policy; otherwise the application generates one. Completion logging includes the correlation value so an API failure can be traced without exposing authentication tokens.

The frontend preserves the response request ID on normalized `ApiClientError` objects.

## Actuator

Operational endpoints are exposed conservatively in production and protected according to the application security configuration.

Useful areas include:

```text
/actuator/health
/actuator/metrics
```

Do not expose sensitive health details simply to simplify debugging.

## Application metrics

Current SaaS/security counters include:

```text
saas.subscription.restrictions
saas.security.login.attempts
saas.security.account.locks
saas.security.rate_limit.rejections
```

## Metric-cardinality rule

Metric tags must use bounded categories.

Good examples:

```text
outcome=success|failure|blocked
account_type=tenant_user|system_admin
scope=login|recovery|token|onboarding
```

Do not use unbounded identifiers such as tenant IDs, user IDs, emails, raw URLs with arbitrary IDs, or tokens as metric tags.

## Incident/debugging workflow

For an unexpected API failure:

1. capture timestamp and `X-Request-ID`
2. identify the endpoint/method and HTTP status
3. correlate the backend completion/error log
4. inspect the relevant application metric if applicable
5. avoid copying secrets or complete bearer tokens into issue reports

## Production boundary

Application-level instrumentation exists, but external monitoring, alert routing, long-term metrics/log retention and database recovery drills remain separate production-operational work.

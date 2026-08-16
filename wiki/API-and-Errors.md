# API and Errors

## Error contract

The backend uses a stable structured error response rather than returning raw exception text.

Typical categories include:

```text
400  validation / malformed request
401  unauthenticated or invalid session
403  authenticated but not authorized
409  business conflict / subscription restriction
429  public-auth rate limit
5xx  unexpected server failure
```

Exact endpoint contracts remain authoritative over this general classification.

## Request ID

Backend responses expose:

```text
X-Request-ID
```

The frontend preserves it on normalized `ApiClientError` instances.

When reporting an unexpected API failure, include the request ID rather than copying sensitive tokens or full request payloads.

## Subscription restrictions

Subscription/business restrictions intentionally remain different from authorization denials.

Examples include:

```text
WORKSPACE_READ_ONLY
USER_LIMIT_REACHED
PROJECT_LIMIT_REACHED
SERVICE_UNAVAILABLE
```

A valid permission decision does not suppress these rules.

## Production error exposure

The production Spring profile does not include internal exception messages, binding internals, stack traces, or exception class names in generic HTTP error responses.

Operational detail belongs in backend logs correlated through `X-Request-ID`.

## Frontend behavior

The UI should distinguish:

- authentication/session expiry
- authorization
- validation
- conflict/business rules
- subscription/quota restrictions
- network/backend availability
- unexpected server errors

Do not reduce every non-2xx response to a generic toast when a stable actionable error category exists.

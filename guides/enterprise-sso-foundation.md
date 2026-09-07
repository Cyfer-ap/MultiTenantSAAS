# Enterprise SSO / identity federation

Reviewed state: PR #116 tenant-bound OIDC login/callback runtime.

## Scope

Enterprise identity federation now has three application layers: the tenant-scoped OIDC configuration/security foundation from PR #114, provider verification from PR #115, and the tenant-bound authorization/callback runtime in PR #116. Existing local/password authentication remains available and SSO enforcement is intentionally deferred until tenant policy and recovery controls are implemented.

The runtime boundary prevents an unverified or changed IdP configuration from being used for sign-in and prevents a provider identity from implicitly creating or crossing tenant accounts.

## Configuration model

Each tenant may have one identity-provider configuration. The provider-neutral model currently supports `OIDC` and can later be extended with a SAML adapter without placing protocol-specific credentials on the tenant record.

Configuration states are:

- `DRAFT` — configured or changed, but not runtime-verified
- `VERIFIED` — OIDC discovery, remote-endpoint policy, JWKS and Spring client-model checks passed
- `DISABLED` — administratively unavailable

Changing the issuer/client ID/scopes or rotating the client secret invalidates a previous verification and returns the configuration to `DRAFT`.

Stored fields include tenant/protocol, display name, issuer URI, client ID, encrypted client secret, non-sensitive secret hint/version, OIDC scopes, lifecycle timestamps, creator/updater metadata and optimistic locking. `openid` is mandatory; omitted scopes default to `openid profile email`.

## Secret handling

The IdP client secret is write-only through the API and is never returned by normal reads, rotation responses or verification responses. Opaque client-secret values are preserved exactly rather than trimmed or normalized.

At rest the secret is protected with AES-256-GCM using:

```text
IDENTITY_FEDERATION_ENCRYPTION_KEY
```

The value must be Base64 encoding of exactly 32 bytes. The application can start without the key, but operations that require IdP credential encryption/decryption fail closed with `503 IDENTITY_FEDERATION_UNAVAILABLE`.

The same federation key protects stored PKCE verifiers for short-lived authorization transactions. Keep the key stable while encrypted federation data exists. Key rotation requires a deliberate re-encryption migration.

## OIDC verification and remote-request safety

Configuration-time issuer validation is not treated as sufficient protection for server-side OIDC requests. Verification and login re-resolve and validate provider destinations immediately before discovery, JWKS and token operations to reduce DNS-rebinding/SSRF risk.

The runtime requires:

- HTTPS for issuer and OIDC remote endpoints
- no embedded credentials or fragments; endpoint query strings are accepted only where protocol metadata requires them
- public-routable DNS answers only; mixed public/private answers are rejected
- exact equality between configured issuer and discovery-document `issuer`
- valid authorization, token and JWKS endpoint metadata
- optional UserInfo endpoint safety when present
- a JWKS document containing at least one key
- compatibility with Spring Security's OIDC client model using already-validated metadata

Provider HTTP requests do not follow redirects, use bounded connection/request timeouts and cap provider JSON responses. A provider/discovery/JWKS validation failure returns `502 IDENTITY_PROVIDER_VERIFICATION_FAILED`; missing or invalid application-side federation encryption configuration remains a separate `503 IDENTITY_FEDERATION_UNAVAILABLE` condition.

## Tenant management API

Base path:

```text
/api/tenants/{tenantId}/identity-provider
```

All management operations require `tenant.update`.

- `POST` — create the tenant's draft OIDC configuration
- `GET` — read non-secret configuration metadata
- `PUT` — update display name, issuer, client ID and scopes
- `POST /verify` — perform controlled discovery/JWKS/client-model verification and transition to `VERIFIED`
- `POST /rotate-client-secret` — replace the encrypted client secret and invalidate prior verification
- `DELETE` — disable the configuration without deleting its audit boundary

Verification success is audited. A disabled configuration cannot be verified.

## OIDC sign-in runtime

The public OIDC runtime is tenant-bound. Sign-in begins only for a `VERIFIED` provider and creates a short-lived durable authorization transaction containing no raw `state` or nonce values.

Security controls include:

- high-entropy `state` and OIDC nonce values with only SHA-256 hashes persisted
- PKCE S256 with an encrypted verifier at rest
- transaction binding to tenant, provider and provider optimistic-lock version
- single-use transaction consumption before provider token exchange, preventing callback replay even when the provider request later fails
- fresh provider metadata/JWKS validation around external requests
- authorization-code exchange only through the validated token endpoint
- ID-token signature and signing-algorithm validation
- issuer, audience/authorized-party, expiry, issued-at/not-before and nonce validation

A successful callback does not automatically create an application user. The first identity link requires a cryptographically verified OIDC identity with `email_verified=true` whose normalized email already belongs to an active user in the same tenant. The durable binding then uses the provider issuer and immutable OIDC `sub` for subsequent sign-ins.

Only after that tenant-scoped identity resolution succeeds does the application issue its existing JWT and refresh/browser session credentials.

## Database runtime state

Flyway V41 adds:

- `oidc_authorization_transactions` — short-lived, single-use state/nonce/PKCE transaction records
- `tenant_federated_identities` — durable provider-subject-to-existing-user bindings scoped to the tenant and issuer

The PostgreSQL schema contract test verifies V41 and the security-critical columns for both tables.

## Authentication safety boundary

PR #116 deliberately does **not** enforce SSO. Local/password authentication remains available until policy and recovery controls are implemented.

The next policy layer must ensure that:

- SSO can be `OPTIONAL` or explicitly `REQUIRED`
- `REQUIRED` is allowed only for a verified provider
- changing, rotating or disabling the provider cannot strand the tenant in an enforced-but-invalid SSO state
- a tested tenant-administrator break-glass path remains available

## Next phase

Implement tenant discovery and SSO policy enforcement:

1. expose the appropriate authentication mode during the existing verified-email workspace-discovery flow
2. add tenant `OPTIONAL` versus `REQUIRED` SSO policy
3. allow `REQUIRED` only while the provider is verified and recovery prerequisites are satisfied
4. block normal member/manager password login under `REQUIRED` while preserving a guarded tenant-admin break-glass path
5. automatically return policy to a safe non-enforced state when provider configuration loses verification
6. audit policy changes and failed/enforced federation decisions
7. add the corresponding tenant-admin and login UX in the following frontend slice

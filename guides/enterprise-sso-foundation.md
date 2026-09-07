# Enterprise SSO / identity federation

Reviewed state: PR #115 provider-verification runtime.

## Scope

Enterprise identity federation now has two completed layers: the tenant-scoped OIDC configuration/security foundation from PR #114 and the provider-verification runtime from PR #115. Existing local/password authentication remains unchanged. A verified provider is not yet a user-facing SSO login path and cannot enforce SSO.

The boundary is deliberate: tenant administrators can prove that an IdP configuration is reachable, internally consistent and compatible with the application's OIDC client model before any authorization redirect or callback is exposed.

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

Keep the key stable while encrypted IdP credentials exist. Key rotation requires a deliberate re-encryption migration.

## OIDC verification safety

Configuration-time issuer validation is not treated as sufficient protection for server-side OIDC requests. Verification re-resolves and validates destinations immediately before discovery and JWKS access to reduce DNS-rebinding/SSRF risk.

The verifier requires:

- HTTPS for issuer and OIDC remote endpoints
- no embedded credentials or fragments; endpoint query strings are accepted only where protocol metadata requires them
- public-routable DNS answers only; mixed public/private answers are rejected
- exact equality between configured issuer and discovery-document `issuer`
- valid authorization, token and JWKS endpoint metadata
- optional UserInfo endpoint safety when present
- a JWKS document containing at least one key
- compatibility with Spring Security's `ClientRegistration` model using the already-validated metadata

Provider HTTP requests do not follow redirects, use bounded connection/request timeouts and cap JSON responses at 1 MiB. A provider/discovery/JWKS validation failure returns `502 IDENTITY_PROVIDER_VERIFICATION_FAILED`; missing or invalid application-side federation encryption configuration remains a separate `503 IDENTITY_FEDERATION_UNAVAILABLE` condition.

## Tenant API

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

## Authentication safety boundary

`VERIFIED` currently means only that the provider configuration passed the runtime checks above. It does **not** mean:

- the application exposes an OIDC authorization redirect
- an authorization code can be exchanged
- an ID token is accepted
- a provider identity is linked to a tenant user
- local login is disabled
- SSO is enforced

No SSO-enforcement switch should be added until callback validation, identity-linking rules and a tested break-glass/recovery path exist.

## Next phase

Implement the tenant-bound OIDC authorization/callback path:

1. create an authorization transaction with high-entropy `state`, OIDC `nonce` and PKCE S256 verifier/challenge
2. bind the transaction to the tenant, verified IdP configuration and short expiry, and make it single-use
3. construct the authorization redirect only from freshly revalidated verified-provider metadata
4. exchange the authorization code using the stored PKCE verifier and encrypted client credential
5. verify ID-token signature through the approved JWKS plus issuer, audience, expiry and nonce
6. link only cryptographically verified identity claims to an existing active user in the same tenant
7. issue the platform's existing JWT/refresh browser session only after successful identity linking
8. preserve local/break-glass administration until an explicit SSO policy is implemented and recovery-tested

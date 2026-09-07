# Enterprise SSO / identity federation foundation

Reviewed state: PR #114 foundation.

## Scope

This foundation introduces tenant-scoped identity-provider configuration without changing the existing local login path. Federated authentication, OIDC metadata retrieval, callbacks, account linking and SSO enforcement belong to the next implementation phase.

The deliberate boundary prevents an incomplete or invalid IdP configuration from locking a tenant out.

## Configuration model

Each tenant may have one identity-provider configuration. The provider-neutral model begins with `OIDC` and can be extended with a SAML adapter later without placing protocol-specific credentials in the tenant table.

Configuration state begins as `DRAFT`. `VERIFIED` is reserved for the later OIDC verification flow, and `DISABLED` makes a configuration unavailable without deleting its audit/history boundary.

Stored fields include:

- tenant and protocol
- display name
- issuer URI
- client ID
- encrypted client secret + non-sensitive hint/version
- OIDC scopes
- status and verification/disable timestamps
- creator/updater and optimistic-lock metadata

`openid` is mandatory. When scopes are omitted, the default set is `openid profile email`.

## Secret handling

The IdP client secret is write-only through the API and is never returned by normal reads or rotation responses.

At rest it is protected with AES-256-GCM using:

```text
IDENTITY_FEDERATION_ENCRYPTION_KEY
```

The value must be Base64 encoding of exactly 32 bytes. The application can start without the key, but operations that must encrypt/decrypt IdP credentials fail closed with `503 IDENTITY_FEDERATION_UNAVAILABLE`.

Keep the key stable while encrypted IdP credentials exist. Key rotation requires a deliberate data migration/re-encryption procedure.

## Issuer URL safety

The configured issuer must:

- use HTTPS
- include a host
- contain no embedded credentials, query string or fragment
- resolve only to public-routable addresses

Private, loopback, link-local, reserved, multicast and documentation ranges are rejected.

OIDC discovery in the next phase must repeat DNS/public-address validation immediately before every server-side metadata/JWKS request. Configuration-time validation alone is not sufficient protection against DNS rebinding.

## Tenant API

Base path:

```text
/api/tenants/{tenantId}/identity-provider
```

All operations require `tenant.update`.

- `POST` — create the tenant's draft OIDC configuration
- `GET` — read non-secret configuration metadata
- `PUT` — update display name, issuer, client ID and scopes
- `POST /rotate-client-secret` — replace the encrypted client secret
- `DELETE` — disable the configuration without deleting it

The model intentionally exposes no SSO-enforcement switch yet. Enforcement can only be added after provider verification, callback validation, account-linking rules and a tested recovery/break-glass path exist.

## Next phase

Implement the OIDC runtime boundary:

1. add the OAuth2/OIDC client dependency and provider-neutral federation service
2. perform SSRF-safe discovery/JWKS retrieval
3. verify provider configuration before allowing `VERIFIED`
4. add state, nonce and PKCE protected authorization/callback flow
5. link only cryptographically verified identity claims to tenant users
6. add domain/workspace discovery and optional SSO policy
7. preserve local/break-glass administration until enforced SSO has a tested recovery path

# Enterprise SSO / identity federation

Reviewed state: post-PR #119 (`c36de3f`).  
Status: **OIDC enterprise SSO complete at application level**.

## Scope

The OIDC federation milestone spans PRs #114–#119 and now includes tenant configuration, provider verification, secure authorization/callback runtime, workspace discovery, optional/required policy, browser completion, tenant-admin UX and audit visibility.

The provider boundary remains protocol-oriented so SAML can be added later if a concrete enterprise requirement justifies it. SAML is not required to consider the current OIDC milestone complete.

## Configuration model

Each tenant may have one identity-provider configuration. Current protocol: `OIDC`.

Lifecycle states:

- `DRAFT` — configured/changed/re-enabled but not verified for runtime use
- `VERIFIED` — discovery/JWKS/client validation passed
- `DISABLED` — administratively unavailable

Changing issuer/client ID/scopes or rotating the client secret invalidates verification and returns the provider to `DRAFT`. Re-enabling a disabled provider also returns it to `DRAFT`; verification must be performed again.

`openid` is mandatory. Default scopes are `openid profile email`.

## Secret handling

The IdP client secret is write-only. Reads, verification and rotation responses do not expose it.

At rest it is protected with AES-256-GCM using:

```text
IDENTITY_FEDERATION_ENCRYPTION_KEY
```

The value must be Base64 encoding of exactly 32 bytes. Keep it stable while encrypted federation data exists. The same key protects stored PKCE verifiers. Rotation of this application encryption key requires a deliberate re-encryption migration.

If the key is unavailable/invalid, credential operations fail closed with `503 IDENTITY_FEDERATION_UNAVAILABLE`.

## Provider verification and SSRF safety

Verification/runtime remote requests require:

- HTTPS issuer and provider endpoints
- no embedded credentials/fragments
- public-routable DNS only; mixed public/private answers are rejected
- exact configured/discovered issuer equality
- valid authorization, token and JWKS endpoints
- safe optional UserInfo endpoint when present
- non-empty JWKS key set
- bounded connection/request timeouts and response sizes
- redirects disabled

Provider destinations are re-resolved/revalidated around runtime remote requests rather than trusting configuration-time validation alone.

Verification/provider failures are mapped without leaking sensitive remote data.

## Tenant management API

Base path:

```text
/api/tenants/{tenantId}/identity-provider
```

Management requires `tenant.update`.

- `POST` — create draft provider
- `GET` — read non-secret metadata
- `PUT` — update display name, issuer, client ID and scopes
- `POST /verify` — verify and transition to `VERIFIED`
- `POST /rotate-client-secret` — replace secret and invalidate verification
- `DELETE` — disable provider
- `POST /enable` — re-enable as `DRAFT`; verification is still required

Tenant SSO policy APIs persist `OPTIONAL` or `REQUIRED` and enforce prerequisites server-side.

## OIDC sign-in runtime

The public OIDC runtime is tenant bound and uses:

- high-entropy state and nonce; only SHA-256 hashes persisted
- PKCE S256 with encrypted verifier
- transaction binding to tenant/provider/configuration version
- single-use transaction consumption before provider token exchange
- fresh provider endpoint/JWKS safety validation
- validated authorization-code exchange
- ID-token signature and signing-algorithm validation
- issuer, audience/authorized-party, expiry, issued-at/not-before and nonce validation

Federation never automatically creates an application user. First identity linking requires a cryptographically verified OIDC identity with `email_verified=true` whose normalized email already belongs to an active user in the same tenant. Durable linkage then uses the provider issuer plus immutable `sub`.

## Workspace discovery and policy

Verified workspace discovery can return:

```text
PASSWORD_ONLY
PASSWORD_OR_SSO
SSO_ONLY
SSO_REQUIRED
```

Tenant policy is:

- `OPTIONAL` — password and/or SSO availability depends on verified provider state
- `REQUIRED` — normal member/manager password login is blocked and SSO is required

`REQUIRED` is accepted only when:

1. the IdP is currently `VERIFIED`; and
2. at least one active tenant administrator has a usable password-based break-glass path.

Provider disable, verification invalidation or client-secret rotation cannot leave a tenant stranded in enforced SSO; policy safely falls back to `OPTIONAL`.

Tenant-admin break-glass password authentication remains deliberate and auditable.

## Browser login completion

The IdP redirects to the **backend** callback. After successful provider validation and tenant identity resolution, the backend does not put platform access/refresh tokens in the redirect URL.

Instead it creates a short-lived, opaque, single-use session handoff and redirects the browser to:

```text
OIDC_FRONTEND_COMPLETION_URI?code=<opaque-handoff>
```

The frontend completion route exchanges the code server-side and commits the normal browser session using the same session architecture as password login. The query is removed from browser history after capture.

## Tenant-admin UX and observability

The permission-gated **Authentication** workspace supports:

- provider create/edit
- status display: `DRAFT`, `VERIFIED`, `DISABLED`
- verify/re-verify
- write-only client-secret entry and rotation
- disable and recoverable enable-to-draft
- `OPTIONAL`/`REQUIRED` policy control
- prerequisite/error feedback and break-glass warning

Existing tenant audit infrastructure records provider configuration/verification/lifecycle/policy events plus trusted tenant-attributed OIDC authentication success/failure. Audit messages deliberately omit raw provider errors, authorization codes, state, nonce, PKCE, provider tokens, client secrets and sensitive identity/provider payloads.

Invalid/untrusted callback state is not attributed to a tenant audit log.

## Database state

Portable migrations:

- **V40** — tenant identity-provider configuration
- **V41** — `oidc_authorization_transactions` and `tenant_federated_identities`
- **V42** — tenant SSO policy
- **V43** — one-time `oidc_session_handoffs`

Never rewrite applied migrations.

## Deployment variables

Required/important values:

```dotenv
IDENTITY_FEDERATION_ENCRYPTION_KEY=<Base64 of exactly 32 random bytes>
OIDC_REDIRECT_URI=https://YOUR_BACKEND_DOMAIN/api/auth/oidc/callback
OIDC_FRONTEND_COMPLETION_URI=https://YOUR_FRONTEND_DOMAIN/auth/oidc/complete
OIDC_AUTHORIZATION_TRANSACTION_MINUTES=5
OIDC_SESSION_HANDOFF_MINUTES=2
```

Local defaults are:

```text
OIDC_REDIRECT_URI=http://localhost:8081/api/auth/oidc/callback
OIDC_FRONTEND_COMPLETION_URI=http://localhost:8080/auth/oidc/complete
OIDC_AUTHORIZATION_TRANSACTION_MINUTES=5
OIDC_SESSION_HANDOFF_MINUTES=2
```

In hosted environments, register `OIDC_REDIRECT_URI` exactly as an allowed redirect/callback URI in the external IdP. The frontend completion URI is application navigation and normally is not registered as the IdP callback.

## IdP setup procedure

1. Create a confidential web/OIDC client in the identity provider.
2. Register the exact backend callback URI, for example `https://multitenantsaas-akxn.onrender.com/api/auth/oidc/callback`.
3. Ensure the IdP supports authorization code flow and the scopes/claims needed by the tenant; `openid` is mandatory and verified email is needed for first link.
4. Configure `IDENTITY_FEDERATION_ENCRYPTION_KEY`, hosted callback/completion URIs and normal production CORS/cookie settings on the backend deployment.
5. Sign in as a tenant administrator with `tenant.update` and open **Authentication**.
6. Enter display name, issuer URI, client ID, client secret and scopes; save as `DRAFT`.
7. Run **Verify**. Do not enable `REQUIRED` until status is `VERIFIED` and the break-glass prerequisite is satisfied.
8. Test `PASSWORD_OR_SSO`/SSO sign-in using an IdP identity whose verified email already maps to an active tenant user.
9. Confirm browser completion establishes the normal tenant session and that federation success is visible in tenant audit logs.
10. Only then test `REQUIRED`; separately verify a tenant administrator can still use the guarded break-glass password path.

## Regression/security test checklist

- wrong/missing state fails and is not tenant-attributed when state cannot be trusted
- callback replay fails because the authorization transaction is single use
- handoff replay/expiry fails
- wrong nonce/issuer/audience/azp/signature/algorithm/time claims fail
- unverified email cannot create the first identity link
- unknown email does not auto-provision a tenant user
- existing federated identity cannot cross tenant boundaries
- provider configuration/secret changes invalidate verification
- disable stops SSO; re-enable returns only to `DRAFT`
- `REQUIRED` cannot be enabled without verified provider + password-capable active tenant-admin recovery
- no platform tokens appear in callback/completion URLs
- no secret/provider token/state/nonce/PKCE data appears in logs/audit/frontend responses

## Milestone closure and deferred work

OIDC enterprise SSO is complete at application level. Deferred/demand-driven identity work includes:

- SAML adapter through the existing provider-neutral boundary, only when justified
- optional provider metadata/JWKS caching if performance/scale measurements require it; current runtime favors fresh validated remote data
- broader enterprise directory/provisioning protocols such as SCIM if later required

The next core product milestone is **authorization delegation and explain-access**.

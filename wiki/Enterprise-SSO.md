# Enterprise OIDC SSO

Status: **complete at application level through PR #119**.

## Sequence

```text
#114 tenant IdP config + encrypted secret
→ #115 provider verification
→ #116 state/nonce/PKCE callback + safe linking
→ #117 workspace discovery + OPTIONAL/REQUIRED + break-glass
→ #118 browser UX + opaque one-time handoff
→ #119 admin UX + lifecycle recovery + audit visibility
```

## Tenant provider lifecycle

States:

- `DRAFT`
- `VERIFIED`
- `DISABLED`

Provider configuration edits, client-secret rotation and re-enable invalidate/require verification. Re-enable never silently restores `VERIFIED`.

## Authentication modes

Workspace discovery can return:

```text
PASSWORD_ONLY
PASSWORD_OR_SSO
SSO_ONLY
SSO_REQUIRED
```

Tenant policy is `OPTIONAL` or `REQUIRED`. `REQUIRED` needs both a verified provider and an active tenant administrator with a usable password break-glass path.

## Account linking

OIDC does not auto-provision application users. First link requires `email_verified=true` and an existing active user with the same normalized email in the same tenant. Durable linkage then uses issuer + immutable provider `sub`.

## Security controls

- AES-256-GCM encrypted write-only client secret
- HTTPS/public-routable provider endpoints with SSRF/DNS revalidation
- disabled redirects
- high-entropy state/nonce with hashes at rest
- PKCE S256 with encrypted verifier
- single-use authorization transaction
- strict ID-token validation
- tenant/provider/configuration-version binding
- opaque one-time browser session handoff
- sensitive OIDC values excluded from audit/frontend/log output

## Tenant administration

Users with `tenant.update` can open **Authentication** to create/edit provider configuration, verify/re-verify, rotate the client secret, disable/re-enable and manage SSO policy.

## Deployment

```dotenv
IDENTITY_FEDERATION_ENCRYPTION_KEY=<Base64 of exactly 32 random bytes>
OIDC_REDIRECT_URI=https://YOUR_BACKEND_DOMAIN/api/auth/oidc/callback
OIDC_FRONTEND_COMPLETION_URI=https://YOUR_FRONTEND_DOMAIN/auth/oidc/complete
OIDC_AUTHORIZATION_TRANSACTION_MINUTES=5
OIDC_SESSION_HANDOFF_MINUTES=2
```

Register the backend redirect URI exactly with the IdP.

## Test procedure

1. Create a confidential OIDC web client at the IdP.
2. Register the backend callback.
3. Configure the tenant provider and verify it.
4. Use an IdP account whose verified email maps to an existing active tenant user.
5. Test optional SSO and confirm the normal tenant browser session is established.
6. Confirm federation audit visibility.
7. Test `REQUIRED` only after independently validating tenant-admin break-glass.
8. Verify callback/handoff replay, unknown-user, unverified-email and cross-tenant attempts fail.

## Database

- V40 provider configuration
- V41 authorization transactions + federated identities
- V42 SSO policy
- V43 browser session handoffs

## Deferred identity work

SAML/SCIM are demand-driven additions, not blockers for the completed OIDC milestone. Provider metadata/JWKS caching may be added later if scale/performance evidence warrants it; the current implementation prioritizes fresh validated provider data.

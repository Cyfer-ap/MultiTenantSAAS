# Current Architecture

Reviewed: 2026-09-14

This is the canonical technical architecture overview for MultiTenantSAAS. Detailed domain behavior belongs in focused guides such as `authorization_model.md`, `subscription_billing.md`, `enterprise-sso-foundation.md`, and the outbound-webhook guides. Engineering rules and the technical-debt register live in `ENGINEERING_STANDARDS.md`.

## Architectural shape

MultiTenantSAAS is a **modular monolith** with a separate React web client.

```text
React / TypeScript web client
            ↓ HTTPS/JSON
Spring Boot application
            ↓
PostgreSQL + Flyway
            ↓
external providers / S3-compatible storage
```

The monolith is intentional. The current scale and feature set do not justify microservices, Kafka, Redis, Kubernetes, or database sharding.

## Core stack

### Backend

- Java 21
- Spring Boot 4.x
- Spring Security + JWT/OIDC
- Spring Data JPA/Hibernate
- PostgreSQL 17
- Flyway
- Testcontainers
- Actuator/Micrometer
- AWS SDK v2 for S3-compatible object storage

### Frontend

- React 19
- TypeScript 6
- Vite 8
- Material UI
- React Router
- TanStack React Query
- Axios
- React Hook Form + Zod
- Vitest + Testing Library

## Control planes

Tenant users and system administrators are separate identity/control planes. A system administrator is not implemented as a tenant user with a stronger tenant role.

The tenant plane owns normal workspace activity. The system plane owns platform administration such as tenants, plans, provider catalogs, platform audit, and subscription operations.

## Enforcement pipeline

The intended request boundary is:

```text
authentication / federation
        ↓
tenant identity + tenant isolation
        ↓
scoped authorization
        ↓
subscription lifecycle / entitlement access
        ↓
resource and API quotas
        ↓
domain invariants
        ↓
transaction + database constraints
```

The backend is authoritative at every security and entitlement boundary. Frontend guards exist for UX only.

## Major application domains

Current mature domains include:

- authentication, workspace discovery, password recovery and browser sessions
- users, invitations and organization hierarchy
- scoped authorization, delegation and Explain Access
- projects, members, tasks and task collaboration
- attachments through S3/R2-compatible storage
- in-app/email notifications and preferences
- subscription plans, tenant subscriptions, quotas and usage metering
- provider-neutral billing with Stripe and Razorpay implementations
- durable signed provider webhook processing and reconciliation
- tenant-configurable outbound webhooks with retries/history/replay
- enterprise OIDC SSO and tenant IdP configuration
- tenant API keys and isolated external API authentication
- tenant/platform audit trails
- production hardening and observability

Product-enrichment domains such as Global Search, My Work, saved views, Kanban/calendar, workflows and knowledge are the next development stage.

## Authorization architecture

Authorization is scope-aware rather than role-name-only.

Important scopes include tenant, project, organizational unit, organizational subtree, direct reports and self. Delegated authority is bounded by a current direct parent assignment and is revalidated at access time.

Explain Access uses the same evaluator as enforcement so diagnostic output cannot drift into a second permission model.

See `authorization_model.md` for the complete model.

## Persistence and tenancy

The production database uses shared-schema multi-tenancy with tenant ownership represented explicitly in tenant-owned data.

Repository/query methods for tenant-owned resources should include tenant scope. Cross-tenant resource IDs must never be trusted without ownership validation.

Flyway exclusively owns production schema evolution. Portable common migrations currently extend through V44.

The architecture intentionally uses PostgreSQL locking/constraints where correctness depends on concurrent mutations, including sensitive subscription/delivery flows.

## Billing and provider integration

The internal subscription model is provider-neutral. Stripe and Razorpay are adapters around the application billing model rather than the source of application architecture.

Normal billing lifecycle synchronization is webhook-authoritative. Durable provider/customer/subscription/catalog mappings and immutable subscription history allow reconciliation and recovery without exposing provider internals to tenant clients.

Stripe is the validated deployed Test Mode path. Razorpay application/catalog integration remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

See `subscription_billing.md` for details.

## Integration/event architecture

The application currently uses a mixture of direct synchronous application-service calls and durable event/delivery subsystems.

Synchronous calls are appropriate when an invariant must succeed inside the same transaction. Secondary reactions such as outbound delivery should be isolated through narrow application contracts or durable event mechanisms rather than becoming hidden repository writes inside unrelated domains.

Future domains should follow the modularity rules in `ENGINEERING_STANDARDS.md`.

## Frontend architecture

The frontend is increasingly feature-oriented. Healthy feature modules commonly contain:

```text
features/<domain>/
    api/
    components/
    hooks/
    pages/
    types/
```

Server state is managed with React Query. Route/permission guards improve navigation and UX but do not replace backend enforcement.

The main future risk is growth of central routing/navigation aggregation. New modules should expose narrow integration metadata where appropriate rather than moving domain logic into central application files.

## API boundary

Public APIs use explicit request/response DTOs and validation rather than returning JPA entities.

The current web client maintains TypeScript API contracts manually. That is acceptable for the current single-client web application, but a generated/shared contract strategy should be adopted before a substantial mobile client is added so backend/web/mobile models do not drift independently.

## Scalability model

The Spring application is designed to remain stateless enough for horizontal application replication. PostgreSQL is intentionally the primary coordination/persistence layer.

This architecture should comfortably support significant product growth before distributed infrastructure becomes justified.

The following remain unproven until measured:

- large-tenant search/query latency
- sustained concurrent mutation throughput
- database contention envelopes
- background delivery throughput at scale
- large attachment/integration workloads

The policy is to optimize from evidence. Do not introduce distributed infrastructure simply because a future scale problem is imaginable.

## Production boundary

The application code has strong production-oriented security, database, provider and CI practices, but full enterprise operational maturity is not yet claimed.

Deferred work still includes:

- backup/restore drills
- broader failure-recovery exercises
- load/concurrency characterization
- production R2 verification
- deeper operational alert/runbook coverage

These are deliberately deferred behind the current user-facing product-enrichment phase, not forgotten.

## Architecture direction from this point

All new functionality must follow this rule:

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Global Search will be the first new product domain implemented under this standard and should become the template for subsequent modules.

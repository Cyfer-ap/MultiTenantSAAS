# Multi-Tenant SaaS Platform

A full-stack multi-tenant SaaS platform focused on secure tenant isolation, permission-oriented authorization, project/workforce management, subscription enforcement, PostgreSQL readiness, and production deployment hardening.

> **Documentation snapshot**
>
> Repository: `Cyfer-ap/MultiTenantSAAS`
> Reviewed branch: `main`
> Reviewed commit: `3808c0ddf95d075aed7114bf060518640c19d6c2`
> Current engineering phase: **Step 40 — Transaction & Concurrency Hardening**
> Production configuration foundation: **implemented**

---

## 1. What the platform provides

The application has two deliberately separated administration planes.

### Tenant plane

Tenant-scoped functionality includes:

- tenant onboarding and lifecycle
- tenant authentication and session restoration
- refresh-token rotation and revocation
- password change and password recovery
- account lockout and administrative unlock
- tenant users
- invitations
- organizational/authorization capabilities
- projects
- project memberships
- tasks
- dashboards
- tenant audit logs
- subscription visibility and restrictions

### System plane

System-administrator functionality includes:

- system-admin authentication
- platform dashboard
- tenant listing and management
- tenant onboarding
- tenant-user administration
- system-admin management
- platform audit logs
- subscription plan and lifecycle administration

System administrators are a separate identity/control plane; they are not tenant users with a stronger tenant role.

---

## 2. Repository layout

```text
MultiTenantSAAS/
├── .github/
├── guides/
├── multitenant-saas/                 Spring Boot backend
├── multitenant-saas-frontend/        React/Vite frontend
├── compose.yaml
├── docker-compose.postgres.yml
├── .env.example
├── .env.production.example
├── qodana.yaml
└── readme.md
```

---

## 3. Technology stack

### Backend

- Java 21
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Security
- OAuth2 Resource Server / JWT
- Spring Data JPA / Hibernate
- Flyway
- H2 for the historical local/test path
- PostgreSQL 17 for the production-readiness path
- Testcontainers for PostgreSQL integration verification
- Maven
- JUnit 5 / MockMvc
- Springdoc OpenAPI
- Actuator

### Frontend

- React 19
- TypeScript
- Vite
- Material UI
- React Router
- Redux Toolkit / RTK Query in the current architecture
- Vitest
- Testing Library

---

## 4. Security model

Business requests pass through distinct enforcement layers:

```text
authentication
    ↓
tenant isolation
    ↓
authorization / permission policy
    ↓
subscription lifecycle access
    ↓
resource quota enforcement
```

These layers intentionally remain separate because they have different semantics, error contracts, and recovery paths.

### Tenant isolation

Tenant IDs must be carried through controller, service, repository, and security flows. A privileged role never permits cross-tenant data access.

### Authentication and sessions

The tenant authentication model includes:

- JWT access tokens
- hashed refresh-token storage
- refresh-token rotation
- single-session revocation
- logout-all / multi-device revocation
- password change
- password reset
- account lockout
- session-version based invalidation

System-admin identity remains separate from tenant authentication.

---

## 5. Authorization

The application has evolved beyond a purely coarse fixed-role model.

Authorization concepts include:

- permission catalogue
- authorization roles
- role-to-permission assignments
- subject/user role assignments
- authorization policies/rules
- tenant-aware and organization-aware decisions
- Spring method-security checks where appropriate

Legacy roles may still be retained for compatibility:

```text
TENANT_ADMIN
TENANT_MANAGER
TENANT_USER
```

Project roles include:

```text
PROJECT_LEAD
MEMBER
```

The design rule is that backend authorization is authoritative. Frontend route/action visibility is a usability layer only.

---

## 6. Subscription and quota model

The subscription foundation separates:

- subscription plans
- plan entitlements and limits
- tenant subscription lifecycle
- evaluated access state
- quota usage/enforcement

Lifecycle states represented by the current model include states such as:

```text
TRIALING
ACTIVE
PAST_DUE
CANCELLED
EXPIRED
```

Access evaluation uses its own reason model:

```text
ACTIVE
TRIAL_ACTIVE
PAST_DUE_GRACE
NO_SUBSCRIPTION
PLAN_INACTIVE
CANCELLED
EXPIRED
PERIOD_EXPIRED
TRIAL_EXPIRED
```

Do not use lifecycle status names as substitutes for evaluated access reasons.

### Central read-only enforcement

Ordinary tenant business mutations pass through the central subscription mutation guard.

When workspace mutation is not allowed, the standard restriction is:

```text
HTTP 409
restriction = WORKSPACE_READ_ONLY
resource = workspace
```

Read operations remain available.

Explicit cleanup/recovery operations can be allowed so the tenant can reduce usage or restore compliance.

### Resource quotas

When workspace mutation is otherwise permitted, resource-specific quota checks may still reject growth, for example:

```text
USER_LIMIT_REACHED
PROJECT_LIMIT_REACHED
```

---

## 7. Core business modules

### Tenant users

Implemented concerns include:

- create/read/update lifecycle
- status changes
- tenant-scoped uniqueness
- password validation
- account lock/unlock
- role and permission interactions
- self-protection
- last-active-admin protection

### Invitations

The invitation lifecycle includes:

- create
- list/read
- revoke
- accept
- secure random raw tokens
- hashed persisted tokens
- expiration
- one-time acceptance
- replacement invitation behavior
- audit events

Invitation acceptance/replacement is one of the remaining Step 40 concurrency-hardening areas.

### Projects

Project states include:

```text
PLANNING
ACTIVE
ON_HOLD
COMPLETED
ARCHIVED
```

Capabilities include:

- create/list/read/update
- status transitions
- archive
- tenant isolation
- project membership
- project lead assignment
- audit events

### Tasks

Task states include:

```text
TODO
IN_PROGRESS
BLOCKED
COMPLETED
CANCELLED
```

Priorities include:

```text
LOW
MEDIUM
HIGH
URGENT
```

Capabilities include creation, update, assignment, status changes, completion handling, cancellation, and archived-project history.

---

## 8. Audit logging

The system keeps tenant and platform administration audit surfaces separate.

Tenant audit activity covers areas such as:

- tenants/users
- authentication/session events
- password operations
- invitations
- projects
- memberships
- tasks
- authorization-sensitive actions

Platform audit logs cover system-administration actions.

---

## 9. PostgreSQL and Flyway strategy

The repository preserves two migration histories.

### Historical H2 chain

```text
multitenant-saas/src/main/resources/db/migration
```

This contains the established historical V1-V17 development migrations.

### PostgreSQL current-schema baseline

```text
multitenant-saas/src/main/resources/db/postgresql
```

PostgreSQL starts from:

```text
V17__current_schema_baseline.sql
```

### Future shared migrations

```text
multitenant-saas/src/main/resources/db/common
```

All new portable migrations begin at:

```text
V18+
```

Required rule:

```text
H2        -> db/migration + db/common
PostgreSQL -> db/postgresql + db/common
```

Never rewrite an already-applied migration.

---

## 10. Transaction and concurrency hardening — Step 40

Step 40 addresses write paths that are correct sequentially but can fail under concurrent requests.

### Slice 40.1 — subscription state serialization

Completed protections include:

- pessimistic locking for mutable tenant subscription state
- locked subscription reads before plan/lifecycle mutations
- tenant-row locking before one-and-only subscription creation checks
- database-backed locking rather than process-local mutexes

Locking rule:

```text
Existing subscription invariant
    -> lock tenant_subscription row

One-and-only subscription creation
    -> lock tenant row before check/insert
```

### Remaining Step 40 areas

- invitation acceptance/replacement races
- tenant/system-admin failed-login counter races
- session-version/password/logout-all lost-update protection
- normalization of duplicate/integrity races into stable API errors
- targeted PostgreSQL concurrency integration tests
- lock-order/deadlock review

---

## 11. Production deployment foundation

The latest `main` includes a dedicated production configuration profile and production environment template.

Production activation:

```text
SPRING_PROFILES_ACTIVE=postgres,production
```

Important production defaults include:

- no internal exception details in HTTP responses
- `spring.jpa.open-in-view=false`
- `spring.jpa.hibernate.ddl-auto=validate`
- Flyway clean disabled
- Actuator exposure limited to health/info
- health details/components hidden
- health probes enabled
- restrained Hibernate/security logging
- public endpoint rate limiting configuration
- password-reset token exposure disabled
- system-admin bootstrap disabled by default

The repository includes:

```text
.env.production.example
multitenant-saas/src/main/resources/application-production.properties
```

---

## 12. Local development

### Option A — full Docker Compose

Copy:

```text
.env.example
```

to:

```text
.env
```

then configure local secrets and run the project Compose stack.

### Option B — PostgreSQL only

From the repository root:

```powershell
docker compose -f .\docker-compose.postgres.yml up -d
```

Then run the backend with the PostgreSQL profile.

### Backend

Typical local API port:

```text
8081
```

Useful endpoints include the application health/Actuator/OpenAPI endpoints exposed by the selected profile.

### Frontend

Configure:

```dotenv
VITE_API_BASE_URL=http://localhost:8081
```

Then:

```powershell
cd multitenant-saas-frontend
npm install
npm run dev
```

---

## 13. Verification

### Backend

```powershell
cd multitenant-saas
.\mvnw.cmd test
```

PostgreSQL/Testcontainers integration verification should run when Docker is available.

### Frontend

```powershell
cd multitenant-saas-frontend
npm run lint
npm run test
npm run build
```

### Repository hygiene

```powershell
git diff --check
git status --short
```

CI additionally includes repository/backend/frontend/PostgreSQL/security-quality checks configured in GitHub Actions and Qodana.

---

## 14. Documentation source-of-truth

When documentation and implementation disagree, use this order:

1. current application code and tests
2. current Flyway migrations
3. focused current guides
4. historical workflow/planning guides

Current focused guides:

```text
guides/current_architecture.md
guides/data_model.md
guides/authorization_model.md
guides/subscription_billing.md
guides/postgresql_and_migrations.md
guides/step39_closeout.md
guides/step40_transaction_concurrency.md
```

Additional architectural/history references include:

```text
guides/frontend_architecture.md
guides/frontend_testing.md
guides/authorization_v2_plan.md
guides/postman_tests.md
guides/progress.md
guides/Plan.txt
guides/Details.txt
guides/sb_difficulties.txt
```

---

## 15. Engineering principles

```text
Every schema change uses Flyway.
Never rewrite an already-applied migration.
Every tenant business query remains tenant scoped.
Authorization stays backend authoritative.
Subscription access never grants authorization.
Authorization never bypasses subscription lifecycle constraints.
Collection APIs support pagination.
Sensitive mutations are auditable.
Secrets are environment controlled.
Production schema ownership belongs to Flyway.
Concurrency invariants must be database-backed.
New features require regression tests.
```

---

## 16. Current roadmap

Immediate order:

```text
1. Complete Step 40 transaction/concurrency hardening.
2. Add targeted PostgreSQL concurrency tests.
3. Normalize integrity-race API behavior.
4. Review lock ordering/deadlock behavior.
5. Keep production profile and deployment configuration green.
6. Only then expand payment-provider integration and additional production services.
```

The application has a production-readiness foundation, but it should not be described as fully production-complete until concurrency, deployment, observability, provider integration, backup/restore, and operational verification are finished.

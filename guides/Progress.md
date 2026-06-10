# Multi-Tenant SaaS Backend — Project Notes

## 1. Project Setup

The project was created using **Spring Initializr**.

Basic Spring Boot project structure:

```text
src/main/java
src/main/resources
pom.xml
```

The application starts from the main class:

```java
@SpringBootApplication
public class MultitenantSaasApplication {
    public static void main(String[] args) {
        SpringApplication.run(MultitenantSaasApplication.class, args);
    }
}
```

### What this does

* Starts the Spring Boot application
* Auto-configures required Spring components
* Scans packages for controllers, services, repositories, configs, etc.

---

## 2. Dependencies Used

### Initial Dependencies

* **Spring Web**
  Used to build REST APIs using Spring MVC.

* **Spring Boot DevTools**
  Helps during development with auto-restart and live reload.

* **Validation**
  Used to validate request data using annotations like `@NotBlank`, `@Email`, etc.

* **Lombok**
  Reduces boilerplate code such as getters, setters, constructors, and `toString()`.

* **Spring Boot Actuator**
  Provides monitoring endpoints such as health checks and metrics.

### Database Dependencies

* **Spring Data JPA**
  Allows Java classes to map to database tables.

* **Hibernate**
  JPA implementation used internally by Spring Boot.

* **H2 Database**
  Lightweight in-memory/file-based database used for development.

* **H2 Console**
  Browser-based console to inspect the H2 database.

---

## 3. REST Controller

A REST controller handles HTTP requests and exposes API endpoints.

Example flow:

```text
Client → Controller → Service → Repository → Database
```

This structure keeps the code clean:

* **Controller** handles requests and responses
* **Service** contains business logic
* **Repository** handles database operations
* **Entity** represents database tables
* **DTOs** handle request/response data

---

## 4. ResponseEntity and API Response Format

`ResponseEntity` helps control HTTP responses properly.

Instead of returning random response formats, APIs can return a common structure:

```json
{
  "success": true,
  "message": "...",
  "data": "...",
  "timestamp": "..."
}
```

Example:

```java
ResponseEntity.ok(...)
```

This means:

```text
HTTP 200 OK
```

---

## 5. Java Record and Generic Response

A `record` is a compact Java class used for immutable data objects.

Example:

```java
ApiResponse<T>
```

Here, `T` means the response can hold any type of data.

Examples:

```java
ApiResponse<String>
ApiResponse<Tenant>
ApiResponse<List<AppUser>>
ApiResponse<TenantResponse>
```

This makes the response format reusable.

---

# Tenant Management

## 6. Tenant Entity

We added the `Tenant` entity because in a SaaS system, most business data belongs to a tenant.

Future relationships may look like this:

```text
Tenant → Users
Tenant → Roles
Tenant → Projects
Tenant → Subscriptions
Tenant → Settings
```

### Important annotations

* `@Entity`
  Maps the Java class to a database table.

* `@Id`
  Marks the primary key.

* `@GeneratedValue`
  Automatically generates the primary key value.

* `@PrePersist`
  Runs before the record is first saved.

* `@PreUpdate`
  Runs before the record is updated.

---

## 7. Tenant Repository, Service, Controller, and DTOs

We added:

* `TenantRepository`
* `TenantService`
* `TenantController`
* Tenant request/response DTOs

### Standard flow

```text
Client → TenantController → TenantService → TenantRepository → Database
```

### JpaRepository

```java
JpaRepository<Tenant, UUID>
```

This gives ready-made methods:

```java
save()
findAll()
findById()
deleteById()
existsById()
```

### Custom query methods

```java
findBySlug(String slug)
existsBySlug(String slug)
```

Spring Data JPA automatically creates queries from these method names.

---

## 8. Exception Handling

We added:

* `DuplicateResourceException`
* `ResourceNotFoundException`
* `GlobalExceptionHandler`
* Clean validation error response
* Clean duplicate resource response
* Clean 404 response

### Important annotation

```java
@RestControllerAdvice
```

This is a global exception handler.

It catches exceptions from controllers and returns a consistent error response.

---

## 9. Get Tenant by ID and Slug

We added:

* `GET /api/tenants/{id}`
* `GET /api/tenants/slug/{slug}`

### Why this matters

Future requests may identify tenants using:

```text
acme.yoursaas.com
```

or a header:

```text
X-Tenant-Slug: acme
```

The backend must resolve:

```text
slug → tenant record → tenant context
```

### PathVariable

```java
@GetMapping("/{id}")
public ResponseEntity<?> getTenantById(@PathVariable UUID id)
```

For this URL:

```text
/api/tenants/123
```

Spring automatically puts `123` into the `id` variable.

---

## 10. Update Tenant

We added:

* `TenantUpdateRequest`
* `PUT /api/tenants/{id}`
* Tenant update logic
* Duplicate slug protection during update

### Why this matters

Tenant data should be editable, but the slug must remain unique.

The slug may later be used in:

* URLs
* Headers
* Subdomains

### Duplicate slug protection

The logic allows a tenant to keep its own slug but prevents another tenant from using the same slug.

---

## 11. Update Tenant Status

We added:

* `TenantStatusUpdateRequest`
* `PATCH /api/tenants/{id}/status`
* Tenant activation/deactivation/suspension logic
* Better invalid enum/JSON error handling

### Tenant statuses

Use:

```text
ACTIVE
```

when the tenant can use the platform normally.

Use:

```text
INACTIVE
```

when the tenant is disabled.

Use:

```text
SUSPENDED
```

when the tenant is blocked due to billing, abuse, policy violation, or admin action.

---

## 12. Deactivate Tenant / Soft Delete

We added:

```text
DELETE /api/tenants/{id}
```

But internally, the tenant is not physically deleted.

Instead, we do:

```java
tenant.setStatus(TenantStatus.INACTIVE);
```

### Why soft delete?

In SaaS systems, tenant records should usually be preserved because they may have:

* Users
* Subscriptions
* Billing records
* Audit logs
* Historical activity

This is called **soft delete**.

---

# User Role Management

## 13. User Role and Status

We added:

* `UserRole`
* `UserStatus`
* `AppUser` entity
* `AppUserRepository`

We use `AppUser` instead of `User` because `User` can conflict with Spring Security classes later.

---

## 14. Tenant and User Relationship

A user belongs to a tenant.

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "tenant_id", nullable = false)
private Tenant tenant;
```

This means:

```text
Many users can belong to one tenant.
```

Example:

```text
Tenant → Users
```

This is the first major parent-child relationship in the SaaS system.

---

## 15. Email Uniqueness Inside Tenant

We added a unique constraint:

```java
columnNames = {"tenant_id", "email"}
```

This means the same email cannot be repeated inside the same tenant.

Blocked:

```text
Tenant A → admin@example.com
Tenant A → admin@example.com
```

Allowed by current design:

```text
Tenant A → admin@example.com
Tenant B → admin@example.com
```

This is common in multi-tenant SaaS systems.

---

# User Management APIs

## 16. Create User Inside Tenant

We added:

* `AppUserCreateRequest`
* `AppUserResponse`
* `AppUserService`
* `AppUserController`
* `POST /api/tenants/{tenantId}/users`
* `GET /api/tenants/{tenantId}/users`

### Why this matters

The URL:

```text
/api/tenants/{tenantId}/users
```

means:

```text
Manage users belonging to one specific tenant.
```

This supports tenant isolation.

---

## 17. Email Normalization

Before saving emails, we normalize them:

```java
request.email().trim().toLowerCase()
```

This prevents duplicate-looking emails like:

```text
Admin@Acme.com
admin@acme.com
```

---

## 18. Get, Update Role, and Update Status of User

We added:

* Get user by ID inside tenant
* Update user role
* Update user status
* Tenant-scoped user lookup

### Safer URL design

Better:

```text
/api/tenants/{tenantId}/users/{userId}
```

Less safe:

```text
/api/users/{userId}
```

The first one ensures every user operation happens inside a tenant boundary.

---

## 19. Tenant-Scoped User Lookup

Important repository method:

```java
findByTenantIdAndId(UUID tenantId, UUID userId)
```

This ensures the user belongs to the requested tenant.

This is an early foundation of tenant isolation.

---

## 20. Update User Details

We added:

* `AppUserUpdateRequest`
* `PUT /api/tenants/{tenantId}/users/{userId}`
* Full name update
* Email update
* Duplicate email protection inside tenant

### Why this matters

Users should be editable, but email uniqueness must stay tenant-scoped.

Blocked:

```text
Same tenant → same email twice
```

Allowed:

```text
Tenant A → admin@example.com
Tenant B → admin@example.com
```

Even during update, this method keeps the lookup tenant-safe:

```java
findByTenantIdAndId(tenantId, userId)
```

So a user from one tenant cannot be updated through another tenant’s URL.

---

# Dashboard

## 21. Dashboard Summary Endpoint

We added:

* `DashboardSummaryResponse`
* `DashboardService`
* `DashboardController`
* `GET /api/dashboard/summary`
* Repository count methods

### Why this matters

This gives a basic system-level summary before moving to authentication and authorization.

Example data it can show:

* Total tenants
* Active tenants
* Inactive tenants
* Suspended tenants
* Total users
* Active users
* Inactive users

### Spring Data count method

```java
long countByStatus(UserStatus status);
```

Spring Data JPA automatically converts this into a SQL count query filtered by status.

---

# Current API Summary

## Tenant APIs

```text
POST   /api/tenants
GET    /api/tenants
GET    /api/tenants/{id}
GET    /api/tenants/slug/{slug}
PUT    /api/tenants/{id}
PATCH  /api/tenants/{id}/status
DELETE /api/tenants/{id}
```

## User APIs

```text
POST   /api/tenants/{tenantId}/users
GET    /api/tenants/{tenantId}/users
GET    /api/tenants/{tenantId}/users/{userId}
PUT    /api/tenants/{tenantId}/users/{userId}
PATCH  /api/tenants/{tenantId}/users/{userId}/role
PATCH  /api/tenants/{tenantId}/users/{userId}/status
```

## Dashboard APIs

```text
GET /api/dashboard/summary
```

---

# Important Concepts to Remember

## 1. Multi-Tenant SaaS

A multi-tenant system serves multiple organizations using the same backend.

Each tenant should only access its own data.

---

## 2. Tenant Isolation

Tenant isolation means data from one tenant should not be accessible from another tenant.

Example:

```text
Tenant A users should not be visible to Tenant B.
```

This is why we use tenant-scoped APIs like:

```text
/api/tenants/{tenantId}/users/{userId}
```

---

## 3. DTOs

DTOs keep API request/response data separate from database entities.

This improves:

* Security
* Clean API design
* Maintainability
* Flexibility

---

## 4. Soft Delete

Soft delete means we do not remove the record from the database.

Instead, we update its status.

Example:

```text
Tenant deleted from API → Tenant status becomes INACTIVE
```

---

## 5. H2 Database

H2 is useful for development and testing.

It should not be treated as production storage.

If using file-based H2, data can persist locally, but the database files should not be pushed to GitHub.

Add H2 database files to `.gitignore`.

---

# Current Project Status

The backend currently supports:

* Basic Spring Boot setup
* Common API response format
* Global exception handling
* Tenant creation
* Tenant listing
* Tenant lookup by ID
* Tenant lookup by slug
* Tenant update
* Tenant status update
* Tenant soft delete
* User entity under tenant
* User creation inside tenant
* User listing inside tenant
* User lookup inside tenant
* User detail update
* User role update
* User status update
* Dashboard summary endpoint

---

# Next Logical Steps

The next major modules should be:

1. Authentication
2. Password hashing
3. Login API
4. JWT token generation
5. Role-based authorization
6. Tenant context resolution
7. Tenant-level data isolation
8. Audit logging
9. Subscription and plan management
10. Production database migration



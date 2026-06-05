# Multi-Tenant SaaS Backend

A learning-focused Multi-Tenant SaaS backend built with Spring Boot.

The goal of this project is to understand and implement core SaaS backend concepts step by step, including tenant management, authentication, role-based access control, tenant isolation, subscription handling, and scalable API design.

## Project Blueprint

This backend will gradually include the following modules:

```text
1. Project setup and base architecture
2. Tenant management
3. User authentication and authorization
4. Role-based access control
5. Tenant isolation
6. Organization/user management
7. Subscription and plan management
8. Admin and tenant dashboards API
9. Audit logging
10. Production database migration
11. API documentation and testing
```

## Tech Stack

```text
Backend: Spring Boot
Language: Java 21
Build Tool: Maven
Database: H2 for development, PostgreSQL later
ORM: Spring Data JPA / Hibernate
API Style: REST
Server Port: 8081
```

## Current Package Structure

```text
com.chacha.multitenantsaas
 ├── common
 │   └── ApiResponse.java
 ├── config
 ├── controller
 │   ├── HealthCheckController.java
 │   └── TenantController.java
 ├── dto
 │   ├── TenantCreateRequest.java
 │   └── TenantResponse.java
 ├── entity
 │   ├── Tenant.java
 │   └── TenantStatus.java
 ├── exception
 ├── repository
 │   └── TenantRepository.java
 ├── security
 ├── service
 │   └── TenantService.java
 ├── tenant
 └── MultitenantSaasApplication.java
```

## What Has Been Done So Far

The project has been initialized with Spring Boot and Maven.

A clean base package structure has been created for future SaaS modules. The backend is running on port `8081`.

Currently completed:

```text
Spring Boot project setup
Base package structure
Health check API
Common API response wrapper
H2 database integration
Spring Data JPA setup
Hibernate configuration
H2 browser console setup
Actuator health endpoint
Tenant entity creation
Tenant status enum
Tenant repository
Tenant request and response DTOs
Tenant service layer
Tenant controller
Basic tenant creation API
Basic tenant listing API
```

## Current Endpoints

```text
GET  /api/health
GET  /actuator/health
GET  /h2-console

POST /api/tenants
GET  /api/tenants
```

## API Response Format

All custom APIs currently follow a common response structure:

```json
{
  "success": true,
  "message": "Operation message",
  "data": {},
  "timestamp": "2026-06-05T00:00:00Z"
}
```

This response format is handled through:

```text
common/ApiResponse.java
```

## Health Check API

### Request

```http
GET http://localhost:8081/api/health
```

### Expected Response

```json
{
  "success": true,
  "message": "Health check successful",
  "data": "Multi-Tenant SaaS Backend is running on port 8081",
  "timestamp": "..."
}
```

## Tenant Module

The first SaaS domain module has been started.

A tenant represents an organization, company, or customer using the SaaS platform.

Example:

```text
Tenant 1 → Acme Corporation
Tenant 2 → Demo School
Tenant 3 → BlueSoft Technologies
```

The current Tenant model includes:

```text
id
name
slug
status
createdAt
updatedAt
```

Tenant status values:

```text
ACTIVE
INACTIVE
SUSPENDED
```

## Tenant APIs

### Create Tenant

```http
POST http://localhost:8081/api/tenants
Content-Type: application/json
```

Request body:

```json
{
  "name": "Acme Corporation",
  "slug": "acme"
}
```

Expected response:

```json
{
  "success": true,
  "message": "Tenant created successfully",
  "data": {
    "id": "...",
    "name": "Acme Corporation",
    "slug": "acme",
    "status": "ACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  },
  "timestamp": "..."
}
```

### Get All Tenants

```http
GET http://localhost:8081/api/tenants
```

Expected response:

```json
{
  "success": true,
  "message": "Tenants fetched successfully",
  "data": [
    {
      "id": "...",
      "name": "Acme Corporation",
      "slug": "acme",
      "status": "ACTIVE",
      "createdAt": "...",
      "updatedAt": "..."
    }
  ],
  "timestamp": "..."
}
```

## Local Development Configuration

The project currently uses an in-memory H2 database for development.

```properties
spring.application.name=multitenant-saas

server.port=8081

spring.datasource.url=jdbc:h2:mem:multitenant_saas_db
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

## H2 Console

H2 Console URL:

```text
http://localhost:8081/h2-console
```

Login details:

```text
JDBC URL: jdbc:h2:mem:multitenant_saas_db
Username: sa
Password: empty
```

Useful query:

```sql
SELECT * FROM TENANTS;
```

## Current Maven Dependencies

The project currently uses these major dependencies:

```text
Spring Boot Web MVC
Spring Boot Validation
Spring Boot Actuator
Spring Data JPA
Hibernate
H2 Database
H2 Console
Lombok
Spring Boot DevTools
```

For Spring Boot 4, H2 Console support requires the H2 console dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-h2console</artifactId>
</dependency>
```

## Current Development Flow

The project currently follows this backend request flow:

```text
Client
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
Database
```

For the Tenant module:

```text
TenantController
  ↓
TenantService
  ↓
TenantRepository
  ↓
TENANTS table
```

## Current Limitations

The project is still in early development.

Currently missing:

```text
Global exception handling
Update tenant API
Get tenant by ID API
Get tenant by slug API
Delete or deactivate tenant API
Authentication
Authorization
Tenant isolation
User management
Role and permission system
PostgreSQL integration
API documentation
Automated tests
```

## Next Planned Step

The next development step is to add proper global exception handling.

This will improve API error responses for cases such as:

```text
Duplicate tenant slug
Invalid request body
Validation errors
Resource not found
Unexpected server errors
```

After that, the Tenant module can be expanded with additional APIs such as:

```text
Get tenant by ID
Get tenant by slug
Update tenant
Deactivate tenant
```

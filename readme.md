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
 ├── config
 ├── controller
 ├── dto
 ├── entity
 ├── exception
 ├── repository
 ├── security
 ├── service
 ├── tenant
 └── MultitenantSaasApplication.java
```

## What Has Been Done So Far

The project has been initialized with Spring Boot and Maven.
A clean base package structure has been created for future SaaS modules.
The backend is running on port `8081`.

Currently completed:

```text
Spring Boot project setup
Basic package structure
Health check API
Common API response wrapper
H2 database integration
Spring Data JPA setup
H2 browser console setup
Actuator health endpoint
```

## Current Endpoints

```text
GET /api/health
GET /actuator/health
GET /h2-console
```

## Local Development Configuration

The project currently uses an in-memory H2 database for development.

```properties
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
```

H2 Console login:

```text
URL: http://localhost:8081/h2-console
JDBC URL: jdbc:h2:mem:multitenant_saas_db
Username: sa
Password: empty
```

## Next Planned Step

The next development step is to create the first real SaaS domain module:

```text
Tenant Module
```

This will include the tenant entity, repository, service, controller, and basic CRUD APIs.

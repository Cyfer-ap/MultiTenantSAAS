# Local Development

## Repository

Windows + PowerShell are the primary local workflow.

```text
Repository root: D:\Projects\multitenant-saas
Backend:         multitenant-saas
Frontend:        multitenant-saas-frontend
```

Use short project-relative paths in normal development work.

## Start from current main

```powershell
git switch main
git pull --ff-only origin main
git status --short
```

Create a feature branch before making changes because `main` is protected.

## Full-stack Docker/Compose

From the repository root:

```powershell
docker compose up --build --detach
docker compose ps
```

Useful logs:

```powershell
docker compose logs backend --tail 100
docker compose logs frontend --tail 100
```

Stop the stack:

```powershell
docker compose down
```

A PostgreSQL-only Compose helper is also available through `docker-compose.postgres.yml`.

## Backend

Local backend port:

```text
http://localhost:8081
```

Use the Maven Wrapper:

```powershell
cd multitenant-saas
.\mvnw.cmd test
```

Do not substitute a globally installed `mvn` command for repository instructions.

## Frontend

Local frontend port:

```text
http://localhost:8080
```

Typical verification:

```powershell
cd multitenant-saas-frontend
npm run lint
npm test
npm run build
```

`VITE_API_BASE_URL` should point to the backend rather than being hard-coded in source.

## Local secrets

Keep real local credentials and secrets outside version control. Use the repository environment/property templates only as inventories of required values.

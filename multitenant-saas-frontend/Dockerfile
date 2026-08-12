# syntax=docker/dockerfile:1

# ---------------------------------------------------------
# Stage 1: Build React/Vite application
# ---------------------------------------------------------
FROM node:24-alpine3.24 AS build

WORKDIR /app

# Install dependencies first for better Docker layer caching.
COPY package.json package-lock.json ./

RUN npm ci

# Copy application source.
COPY . .

# Vite environment variables are compiled into the frontend
# during the build.
ARG VITE_API_BASE_URL=http://localhost:8081

ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

RUN npm run build


# ---------------------------------------------------------
# Stage 2: Serve static production files with non-root Nginx
# ---------------------------------------------------------
FROM nginxinc/nginx-unprivileged:1.31.1-alpine3.23 AS runtime

COPY --from=build \
    /app/dist \
    /usr/share/nginx/html

COPY nginx/default.conf \
    /etc/nginx/conf.d/default.conf

EXPOSE 8080

HEALTHCHECK \
    --interval=30s \
    --timeout=3s \
    --start-period=5s \
    --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/healthz || exit 1

# syntax=docker/dockerfile:1

# ---------------------------------------------------------
# Stage 1: Build the Spring Boot application
# ---------------------------------------------------------
FROM maven:3.9.16-eclipse-temurin-21-noble AS build

WORKDIR /workspace

# Copy Maven configuration first so dependency downloads can
# be reused from Docker's build cache when source code changes.
COPY pom.xml .

RUN mvn --batch-mode dependency:go-offline

# Copy application source after dependencies.
COPY src ./src

# Build the application.
#
# Tests are already executed by CI before deployment/image
# promotion, so Docker only needs to produce and verify the JAR.
RUN mvn --batch-mode -DskipTests verify


# ---------------------------------------------------------
# Stage 2: Minimal Java runtime
# ---------------------------------------------------------
FROM eclipse-temurin:21-jre-ubi9-minimal AS runtime

WORKDIR /app

# Copy only the packaged application from the build stage.
COPY --from=build --chown=10001:10001 \
    /workspace/target/*.jar \
    /app/app.jar

# Never run the application as root.
USER 10001:10001

EXPOSE 8081

# Container-friendly JVM defaults.
# JAVA_TOOL_OPTIONS can still be overridden at deployment time.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# syntax=docker/dockerfile:1
# ---------------------------------------------------------------------------
# Multi-stage build:
#   stage 1 (build) — a full JDK + Maven, compiles the app to a bootable jar
#   stage 2 (run)   — just a JRE + the jar; small, no build tools shipped
# Tests are skipped here (they need Docker-in-Docker for Testcontainers); CI /
# `./mvnw test` runs them.
# ---------------------------------------------------------------------------

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Resolve dependencies first so this layer is cached until pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sh ./mvnw -B -q -DskipTests dependency:resolve || true

COPY src/ src/
RUN sh ./mvnw -B -q -DskipTests clean package


FROM eclipse-temurin:21-jre-jammy AS run
WORKDIR /app

# Run as a non-root user.
RUN useradd -r -u 1001 -s /usr/sbin/nologin appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser

# The app also honours $PORT (application.yml: server.port=${PORT:8080}).
ENV JAVA_OPTS="-Duser.timezone=UTC -XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

# syntax=docker/dockerfile:1
# ── Build stage: compile + package the Spring Boot jar (tests skipped here;
#    CI/local runs them against BUILT4U — the build host has no DB wallet). ──
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY mvnw .
COPY .mvn ./.mvn
# Warm the dependency cache (best-effort; ignored if the offline resolve misses).
RUN mvn -q dependency:go-offline -DskipTests || true
COPY src ./src
RUN mvn -q clean package -DskipTests

# ── Runtime stage: slim JRE. ──
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# The Oracle wallet is NOT baked into the image (it's a secret). Mount the wallet
# files at runtime (e.g. Render Secret Files at /etc/secrets) and point TNS_ADMIN
# at that directory via an env var. The app binds to $PORT (Render injects it).
# Unpacks a base64'd wallet when one is provided (hosts whose secret storage is
# text-only can't carry the wallet's binary files), then starts the app.
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

ENV JAVA_OPTS=""
EXPOSE 8083
ENTRYPOINT ["/app/docker-entrypoint.sh"]

# =============================================================================
# Flockr Admin Service Dockerfile
# =============================================================================
# Multi-stage build for optimized image size and build caching
#
# Build: docker build -t flockr-admin -f flockr-admin/Dockerfile .
# Run:   docker run -p 8080:8080 flockr-admin
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build
# -----------------------------------------------------------------------------
FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy POMs first for dependency caching
COPY pom.xml .
COPY flockr-admin/pom.xml ./flockr-admin/
COPY flockr-users/pom.xml ./flockr-users/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B -pl flockr-admin -am

# Copy source and build
COPY flockr-admin/src ./flockr-admin/src
RUN mvn clean package -DskipTests -B -pl flockr-admin -am

# -----------------------------------------------------------------------------
# Stage 2: Runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre

# Install runtime dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r flockr && useradd -r -g flockr flockr

WORKDIR /app

# Copy artifacts
COPY --from=builder /app/flockr-admin/target/flockr/flockr-admin-1.0-fat.jar ./app.jar
COPY --from=builder /app/flockr-admin/target/flockr/resources ./resources

# Set ownership
RUN chown -R flockr:flockr /app
USER flockr

# Configuration
ENV MODULE_NAME=flockr-admin
ENV SERVER_PORT=8080
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV HEALTHCHECK_PATH=/healthcheck

EXPOSE ${SERVER_PORT}

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT}${HEALTHCHECK_PATH} || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dapp.environment=${ENV:-docker} -Dlogback.configurationFile=./resources/logback/logback-docker.xml -jar app.jar"]


# Multi-stage build for Flockr Application
# Stage 1: Build the application
FROM maven:3.9.5-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy parent POM and modules
COPY pom.xml .
COPY flockr-admin/pom.xml ./flockr-admin/
COPY flockr-users/pom.xml ./flockr-users/

# Download dependencies (caching layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY flockr-admin/src ./flockr-admin/src
COPY flockr-users/src ./flockr-users/src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests -B

# Stage 2: Create runtime image
FROM eclipse-temurin:17-jre

# Install necessary runtime dependencies
RUN apt-get update && apt-get install -y \
    bash \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create app user and group (Debian syntax)
RUN groupadd -r flockr && useradd -r -g flockr flockr

# Set working directory
WORKDIR /app

# Copy built artifacts from builder stage
COPY --from=builder /app/flockr-admin/target/flockr/flockr-admin-1.0-fat.jar ./flockr-admin.jar
COPY --from=builder /app/flockr-admin/target/flockr/resources ./resources

# Change ownership
RUN chown -R flockr:flockr /app

# Switch to non-root user
USER flockr

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Set JVM options
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dapp.environment=${ENV:-docker} -Dlogback.configurationFile=./resources/logback/logback.xml -jar flockr-admin.jar"]


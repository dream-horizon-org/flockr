# =========================================
# Stage 1: Build Spark JAR
# =========================================
FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy all POMs (parent + all modules to satisfy Maven reactor)
# Note: We only copy POMs, not source code of other modules
# This is needed because parent pom.xml declares all modules
COPY pom.xml .
COPY flockr-admin/pom.xml flockr-admin/pom.xml
COPY flockr-users/pom.xml flockr-users/pom.xml
COPY flockr-historic-engine/pom.xml flockr-historic-engine/pom.xml

# Cache dependencies for historic-engine only
RUN mvn dependency:go-offline -B -pl flockr-historic-engine -am

# Copy only historic-engine source code (not other modules!)
COPY flockr-historic-engine/src flockr-historic-engine/src

# Build only the historic-engine module
RUN mvn clean package -DskipTests -B -pl flockr-historic-engine -am


# =========================================
# Stage 2: Artifact image (JAR carrier only)
# =========================================
FROM eclipse-temurin:17-jre

LABEL maintainer="flockr-team"
LABEL purpose="flockr-historic-spark-jar-artifact"

WORKDIR /jars

# Copy built jar (fat JAR with all dependencies from flockr subdirectory)
COPY --from=builder /build/flockr-historic-engine/target/flockr/flockr-historic-engine-1.0-fat.jar /jars/flockr-historic-engine.jar

# No ENTRYPOINT - This is an artifact image for copying JAR to Spark clusters
# To extract JAR: docker run --rm flockr-historic-engine:latest cat /jars/flockr-historic-engine.jar > app.jar
# To use in deployment: COPY --from=flockr-historic-engine:latest /jars/*.jar /opt/spark-apps/

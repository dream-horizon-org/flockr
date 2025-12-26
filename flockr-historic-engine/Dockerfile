# =========================================
# Stage 1: Build Spark JAR
# =========================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build jar
COPY src ./src
RUN mvn clean package -DskipTests -B


# =========================================
# Stage 2: Artifact image (JAR carrier only)
# =========================================
FROM eclipse-temurin:21-jre-jammy

LABEL maintainer="flockr-team"
LABEL purpose="flockr-historic-spark-jar-artifact"

WORKDIR /jars

# Copy built jar
COPY --from=builder /build/target/flockr-historic-engine-*.jar /jars/flockr-historic-engine.jar

# Set entrypoint to run the application
# Use sh -c to allow environment variable expansion for JAVA_OPTS
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /jars/flockr-historic-engine.jar \"$@\"", "--"]

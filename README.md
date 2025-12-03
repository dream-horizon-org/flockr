# Flockr

[![Java Version](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Maven](https://img.shields.io/badge/Maven-3.6+-red.svg)](https://maven.apache.org/)
[![Vert.x](https://img.shields.io/badge/Vert.x-4.4.9-purple.svg)](https://vertx.io/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> A high-performance, reactive, multi-tenant audience segmentation and user cohort management platform built on Vert.x

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
  - [System Overview](#system-overview)
  - [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Quick Start with Docker](#quick-start-with-docker)
  - [Local Development Setup](#local-development-setup)
- [Modules](#modules)
  - [Flockr Admin](#flockr-admin)
  - [Flockr Users](#flockr-users)
- [API Documentation](#api-documentation)
  - [Flockr Admin APIs](#flockr-admin-apis)
  - [Flockr Users APIs](#flockr-users-apis)
- [Development](#development)
  - [Project Structure](#project-structure)
  - [Building from Source](#building-from-source)
  - [Running Tests](#running-tests)
  - [Code Quality](#code-quality)
- [Deployment](#deployment)
  - [Docker Deployment](#docker-deployment)
  - [Production Considerations](#production-considerations)
- [Configuration](#configuration)
- [Database](#database)
- [Contributing](#contributing)
- [License](#license)
- [Support](#support)

## Overview

**Flockr** is a modern, enterprise-grade platform for **audience segmentation** and **user cohort management**, designed for organizations that need to:

- **Segment audiences dynamically** using rule-based logic with both batch (SQL) and streaming (event-pattern) processing
- **Manage user cohorts** with high-performance, real-time access using Aerospike
- **Connect multiple data sources and sinks** for unified data ingestion and export
- **Process data at scale** with Apache Flink for stream processing and Apache Spark for distributed computing
- **Ensure multi-tenancy** with project-level isolation and security

Built on the **Vert.x reactive toolkit**, Flockr leverages non-blocking, event-driven architecture to deliver high throughput and low latency, making it ideal for real-time analytics, marketing automation, and personalization platforms.

## Key Features

### 🎯 Core Capabilities

**Audience Segmentation (Flockr Admin)**
- Create and manage audience segments with metadata and custom configurations
- **BATCH Rules**: SQL-based queries executed periodically on data sources (Kafka, PostgreSQL, MySQL, S3)
- **STREAM Rules**: Real-time event pattern matching using Apache Flink CEP ⚠️ *In Development*
- Track audience lifecycle, user counts, ownership, and verification
- Set expiry dates for automatic audience cleanup

**User Cohort Management (Flockr Users)**
- Retrieve user cohorts with sub-millisecond latency using Aerospike
- Bulk CSV upload for assigning thousands of users to cohorts
- Batch cohort mapping for multiple user assignments
- Multi-tenant isolation with project-based segmentation

**Data Connectors**
- Pluggable data sources: Kafka, PostgreSQL, MySQL, S3, Google Cloud Storage
- Pluggable data sinks: S3, GCS, Meta Conversions API, The Trade Desk
- JSON schema validation for connector configurations

**Stream Processing & Analytics**
- Apache Flink for real-time event processing
- Apache Spark for distributed batch computing

### 🛠️ Technical Features

- **Reactive Architecture**: Non-blocking I/O with Vert.x and RxJava3
- **High Performance**: Sub-millisecond cohort lookups, thousands of concurrent requests
- **Resilience**: Circuit breakers, retry logic, health checks
- **Multi-Tenancy**: Project-level isolation with encrypted identifiers
- **Observability**: Structured logging, Dropwizard metrics, DataDog integration
- **Developer-Friendly**: Swagger UI, dependency injection, automated testing, hot reload

## Architecture

### System Overview

Flockr is a **multi-module Maven project** with two microservices and supporting infrastructure:

```
┌─────────────────────────────────────────────────────────────┐
│                      FLOCKR PLATFORM                         │
├───────────────────────────┬─────────────────────────────────┤
│     FLOCKR-ADMIN          │       FLOCKR-USERS              │
│  (Audience Management)    │    (User Cohort Access)         │
│      Port 8080            │        Port 8082                │
│         ↓                 │           ↓                     │
│    PostgreSQL 16          │     Aerospike 6.4               │
└───────────────────────────┴─────────────────────────────────┘
                     ↓
        ┌────────────────────────┐
        │  Processing Engines    │
        │  • Apache Flink 1.17   │
        │  • Apache Spark 3.5    │
        └────────────────────────┘
```

**Data Flow**:
1. Define audiences and rules via Flockr Admin API
2. Execute rules (SQL batch queries or Flink CEP stream patterns)
3. Store user→cohort mappings in Aerospike
4. Query cohorts in real-time via Flockr Users API
5. Export audience data to configured sinks (S3, ad platforms, etc.)

### Technology Stack

#### Core Framework
- **Vert.x 4.4.9** - Reactive, non-blocking event-driven toolkit
- **RxJava 3** - Reactive streams and async composition
- **Java 17+** - Modern Java with records and enhanced performance

#### Data & Processing
- **PostgreSQL 16** - Relational database for audience/rule metadata
- **Aerospike 6.4** - High-performance NoSQL for user cohort lookups (<1ms latency)
- **Apache Flink 1.17** - Stream processing (CEP for real-time event patterns)
- **Apache Spark 3.5** - Distributed batch computing
- **Kafka 3.6** - Event streaming

#### Application Stack
- **Google Guice 7.0** - Dependency injection
- **Typesafe Config 1.4.3** - HOCON configuration
- **RESTEasy 6.2.8** - JAX-RS REST APIs
- **Swagger/OpenAPI 3.0** - API documentation
- **Resilience4j 2.2** - Circuit breakers and resilience patterns

#### Observability
- **Logback 1.4.14** + Logstash Encoder - Structured JSON logging
- **Dropwizard Metrics 4.2.23** - Application metrics
- **DataDog StatsD 4.2** - Metrics reporting

#### Testing & Build
- **JUnit 5** + **Mockito 5.3** - Unit testing
- **REST Assured 5.3** + **Testcontainers 1.19** - Integration testing
- **Maven 3.6+** - Build automation
- **google-java-format** - Code formatting

## Getting Started

### Prerequisites

#### For Docker Deployment (Recommended)
- **Docker** 20.10+ ([Download](https://docs.docker.com/get-docker/))
- **Docker Compose** v2.0+ ([Download](https://docs.docker.com/compose/install/))
- At least **4GB RAM** allocated to Docker
- At least **10GB** disk space

#### For Local Development
- **Java JDK 17 or higher** ([Download](https://www.oracle.com/java/technologies/downloads/))
- **Apache Maven 3.6+** ([Download](https://maven.apache.org/download.cgi))
- **PostgreSQL 12+** (for flockr-admin)
- **Aerospike 6.4+** (for flockr-users)
- **Apache Flink 1.17** (optional, for stream processing)
- **Apache Spark 3.5** (optional, for distributed computing)

Verify installation:
```bash
java -version    # Should show Java 17+
mvn -version     # Should show Maven 3.6+
docker --version # For Docker deployment
```

### Quick Start with Docker

The fastest way to run the entire Flockr platform with all dependencies:

```bash
# Clone the repository
git clone https://github.com/yourusername/flockr.git
cd flockr

# Start all services (databases, processing engines, applications)
./docker-start.sh
```

**That's it!** The script will:
- Create environment configuration
- Build Docker images
- Start all services (PostgreSQL, Aerospike, Flink, Spark, Flockr Admin, Flockr Users)
- Initialize database schema and seed data
- Wait for health checks
- Display service URLs

#### Access Services

| Service | URL | Description |
|---------|-----|-------------|
| **Flockr Admin API** | http://localhost:8080 | Audience & rule management |
| **Flockr Admin Swagger** | http://localhost:8080/swagger-ui/ | Interactive API docs |
| **Flockr Users API** | http://localhost:8082 | User cohort queries |
| **Flockr Users Swagger** | http://localhost:8082/swagger-ui/ | Interactive API docs |
| **Flink Dashboard** | http://localhost:8081 | Stream processing UI |
| **Spark Master UI** | http://localhost:8090 | Spark cluster status |
| **Spark Worker UI** | http://localhost:8091 | Spark worker metrics |
| **PostgreSQL** | localhost:5432 | Database (user: `flockr_user`, pass: `flockr_password`) |
| **Aerospike** | localhost:3000 | NoSQL database |

#### Quick Health Check

```bash
# Check all services
./docker-status.sh

# Or use Make
make health

# Or manually
curl http://localhost:8080/healthcheck  # Flockr Admin
curl http://localhost:8082/health       # Flockr Users
```

#### Docker Management

```bash
# View logs
docker-compose logs -f                  # All services
docker-compose logs -f flockr-admin     # Specific service

# Stop services
./docker-stop.sh

# Clean up everything (removes volumes!)
./docker-clean.sh

# Using Make
make up         # Start all
make down       # Stop all
make logs       # View logs
make status     # Check status
make help       # See all commands
```

📖 **For complete Docker documentation**: [DOCKER.md](DOCKER.md)

### Local Development Setup

For local development without Docker:

#### 1. Clone Repository

```bash
git clone https://github.com/yourusername/flockr.git
cd flockr
```

#### 2. Set Up Databases

**PostgreSQL (for flockr-admin):**
```bash
# Create database
createdb flockr

# Run schema
psql -d flockr -f flockr-admin/src/main/resources/db/postgres/01_schema.sql

# (Optional) Load seed data
psql -d flockr -f flockr-admin/src/main/resources/db/postgres/02_seed.sql
```

**Aerospike (for flockr-users):**
```bash
# Install Aerospike (macOS with Homebrew)
brew install aerospike

# Start Aerospike
brew services start aerospike

# Or using Docker
docker run -d --name aerospike -p 3000:3000 aerospike/aerospike-server:6.4.0.0
```

#### 3. Configure Environment Variables

Create a `.env` file or export variables:

```bash
# Environment
export ENV=local

# PostgreSQL (for flockr-admin)
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_USER=your_postgres_user
export POSTGRES_PASSWORD=your_postgres_password
export POSTGRES_DATABASE=flockr

# Aerospike (for flockr-users)
export AEROSPIKE_HOST=localhost
export AEROSPIKE_PORT=3000
export AEROSPIKE_NAMESPACE=test

# Optional: Flink
export FLINK_HOST=localhost
export FLINK_PORT=8081

# Optional: Spark
export SPARK_MASTER_URL=spark://localhost:7077
```

#### 4. Build the Project

```bash
# Build all modules
mvn clean install

# Or build specific module
mvn clean install -pl flockr-admin -am
mvn clean install -pl flockr-users -am

# Skip tests for faster build
mvn clean install -DskipTests
```

#### 5. Run Applications

**Option A: Using Maven Exec Plugin**

```bash
# Run flockr-admin
cd flockr-admin
mvn compile exec:java \
  -Dexec.mainClass="io.ascend.flockr.admin.MainLauncher" \
  -Dexec.args="run io.ascend.flockr.admin.verticle.MainVerticle" \
  -Dapp.environment=local

# Run flockr-users (in another terminal)
cd flockr-users
mvn compile exec:java \
  -Dexec.mainClass="com.ascend.flockr.users.MainApplication" \
  -Dexec.args="run io.ascend.flockr.users.verticle.MainVerticle" \
  -Dapp.environment=local
```

**Option B: Using JAR**

```bash
# Build fat JARs
mvn clean package

# Run flockr-admin
cd flockr-admin/target/flockr-admin
java \
  -Dapp.environment=local \
  -Dlogback.configurationFile=./resources/logback/logback-local.xml \
  -jar flockr-admin-1.0-fat.jar

# Run flockr-users (in another terminal)
cd flockr-users/target/flockr-users
java \
  -Dapp.environment=local \
  -Dlogback.configurationFile=./resources/logback/logback-local.xml \
  -jar flockr-users-1.0-fat.jar
```

**Option C: IntelliJ IDEA Run Configuration**

**For flockr-admin:**
1. Open project in IntelliJ IDEA
2. Create new **Application** run configuration
3. Configure:
   - **Main class**: `io.ascend.flockr.admin.MainLauncher`
   - **VM options**: `-Dapp.environment=local -Dlogback.configurationFile=logback/logback-local.xml`
   - **Program arguments**: `run io.ascend.flockr.admin.verticle.MainVerticle`
   - **Environment variables**: `ENV=local;POSTGRES_USER=your_user;POSTGRES_PASSWORD=your_password`
   - **Working directory**: `$MODULE_WORKING_DIR$`
   - **Use classpath of module**: `flockr-admin`

**For flockr-users:**
- Follow same steps but use:
  - **Main class**: `com.ascend.flockr.users.MainApplication`
  - **Program arguments**: `run io.ascend.flockr.users.verticle.MainVerticle`
  - **Environment variables**: `ENV=local;AEROSPIKE_HOST=localhost;AEROSPIKE_PORT=3000`
  - **Use classpath of module**: `flockr-users`

4. Click **Run** 🚀

## Modules

### Flockr Admin

**Audience segmentation, rule management, and data connector administration**

- **Port**: 8080  
- **Database**: PostgreSQL  
- **Key Features**: Create audiences, define BATCH/STREAM rules, manage data connectors, track ownership

### Flockr Users

**High-performance user cohort lookup and assignment**

- **Port**: 8082  
- **Database**: Aerospike  
- **Key Features**: Sub-millisecond cohort queries, bulk CSV import, batch mapping operations

## API Documentation

### Flockr Admin APIs

**Base URL**: `http://localhost:8080`  
**Swagger UI**: http://localhost:8080/swagger-ui/

**Headers**: 
- `X-Project-Id` (required) - Encrypted project identifier
- `email` (optional) - Actor email for audit tracking

**Core Endpoints**:
- `GET/POST /v1/audiences` - List and create audiences
- `GET/PUT/DELETE /v1/audiences/{id}` - Manage audience details
- `POST /v1/audiences/{audienceId}/rules` - Create rules (BATCH or STREAM*)
- `GET /v1/connectors/types` - List available connector types
- `POST /v1/datasources/onboard` - Onboard data source
- `POST /v1/datasinks/onboard` - Onboard data sink
- `GET /healthcheck` - Health status

*STREAM rules are in development

**Example: Create Audience with BATCH Rule**

```bash
# 1. Create audience
curl -X POST http://localhost:8080/v1/audiences \
  -H "X-Project-Id: your-project-id" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "High Spenders",
    "description": "Users who spent >$1000",
    "sinkIds": [1],
    "expireDate": 1735689600000
  }'

# 2. Create BATCH SQL rule
curl -X POST http://localhost:8080/v1/audiences/123/rules \
  -H "X-Project-Id: your-project-id" \
  -H "Content-Type: application/json" \
  -d '{
    "rules": [{
      "name": "Spending Threshold",
      "ruleType": "BATCH",
      "ruleAction": "APPEND",
      "startTime": 1700000000000,
      "endTime": 1735689600000,
      "configuration": {
        "query": "SELECT user_id FROM transactions WHERE amount > 1000",
        "sourceId": 1
      }
    }]
  }'
```

### Flockr Users APIs

**Base URL**: `http://localhost:8082`  
**Swagger UI**: http://localhost:8082/swagger-ui/

**Headers**: 
- `x-project-key` (required) - Project identifier
- `userId` (for cohort queries) - User ID

**Core Endpoints**:
- `GET /flockr/users/get-cohorts` - Get user's active cohorts
- `POST /flockr/users/map-cohorts` - Assign/remove user cohorts
- `POST /flockr/users/batch-map-cohorts` - Batch cohort updates
- `POST /flockr/users/assignments/bulk` - Bulk CSV import
- `GET /health` - Health status

**Examples**:

```bash
# Get user cohorts
curl -X GET http://localhost:8082/flockr/users/get-cohorts \
  -H "userId: 12345" \
  -H "x-project-key: tenant1_project1"

# Response: ["high_spenders", "vip_members"]

# Map user to cohort
curl -X POST http://localhost:8082/flockr/users/map-cohorts \
  -H "x-project-key: tenant1_project1" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 12345,
    "cohortMappings": [{
      "cohortKey": "high_spenders",
      "action": "APPEND",
      "expireAt": 1735689600000
    }]
  }'

# Bulk import from CSV
curl -X POST http://localhost:8082/flockr/users/assignments/bulk \
  -H "x-project-key: tenant1_project1" \
  -F "csv_file=@users.csv" \
  -F "cohort_name=seasonal_promotion"
```

For complete API specifications, see the Swagger UI when running the applications.

## Development

### Project Structure

```
flockr/
├── pom.xml                          # Parent POM with shared dependencies
├── docker-compose.yml               # Docker services configuration
├── env.docker                       # Docker environment template
├── Makefile                         # Build and deployment automation
├── DOCKER.md                        # Complete Docker documentation
├── README.md                        # This file
│
├── flockr-admin/                    # Admin service module
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/io/ascend/flockr/admin/
│       │   │   ├── client/          # External clients (Postgres, Flink, WebClient)
│       │   │   ├── config/          # Configuration classes
│       │   │   ├── constants/       # Application constants
│       │   │   ├── domain/          # Domain models (Audience, Rule, DataConnector)
│       │   │   ├── exception/       # Custom exceptions
│       │   │   ├── injection/       # Guice modules and injector
│       │   │   ├── io/              # Request/Response DTOs
│       │   │   ├── provider/        # JAX-RS providers
│       │   │   ├── repository/      # Data access layer (PostgreSQL)
│       │   │   ├── rest/            # REST controllers
│       │   │   ├── service/         # Business logic services
│       │   │   ├── util/            # Utility classes
│       │   │   ├── validation/      # Custom validators
│       │   │   ├── verticle/        # Vert.x verticles
│       │   │   └── MainLauncher.java
│       │   └── resources/
│       │       ├── config/          # HOCON configuration files
│       │       │   ├── application/
│       │       │   ├── postgres/
│       │       │   ├── flink/
│       │       │   ├── kafka-producer/
│       │       │   ├── http-server/
│       │       │   └── swagger/
│       │       ├── db/postgres/     # Database scripts
│       │       │   ├── 01_schema.sql
│       │       │   └── 02_seed.sql
│       │       ├── logback/         # Logging configuration
│       │       └── webroot/swagger/ # Swagger UI and spec
│       └── test/                    # Unit and integration tests
│
├── flockr-users/                    # Users service module
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/io/ascend/flockr/users/
│       │   │   ├── client/          # Aerospike client
│       │   │   ├── config/          # Configuration classes
│       │   │   ├── constants/       # Application constants
│       │   │   ├── controller/      # REST controllers
│       │   │   ├── dto/             # Data transfer objects
│       │   │   ├── guice/           # Guice application context
│       │   │   ├── module/          # Guice modules
│       │   │   ├── service/         # Business logic services
│       │   │   ├── verticle/        # Vert.x verticles
│       │   │   ├── AbstractMainApplication.java
│       │   │   └── MainApplication.java
│       │   └── resources/
│       │       ├── config/          # HOCON configuration
│       │       │   ├── aerospike/
│       │       │   ├── http-server/
│       │       │   └── swagger/
│       │       ├── logback/         # Logging configuration
│       │       └── webroot/swagger/ # Swagger UI and spec
│       └── test/                    # Unit and integration tests
│
└── docker/                          # Docker-related scripts and configs
    ├── docker-start.sh              # Start all services
    ├── docker-stop.sh               # Stop all services
    ├── docker-status.sh             # Check service status
    ├── docker-clean.sh              # Clean up containers and volumes
    └── config/                      # Service configurations
        ├── flink.conf
        └── postgres.conf
```

### Building from Source

```bash
# Build all modules
mvn clean install

# Build without tests (faster)
mvn clean install -DskipTests

# Build specific module
mvn clean install -pl flockr-admin -am
mvn clean install -pl flockr-users -am
```

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl flockr-admin

# Run with coverage
mvn clean verify
```

### Code Quality

**Format Code**:
```bash
mvn com.spotify.fmt:fmt-maven-plugin:format
```

**Check Formatting**:
```bash
mvn fmt:check
```

This project follows [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) with automated formatting.

## Deployment

### Docker Deployment

**Production Mode**:
```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

**Build and Push Images**:
```bash
docker-compose build
docker tag flockr-admin:latest registry.com/flockr-admin:1.0
docker push registry.com/flockr-admin:1.0
```

### Production Considerations

**Performance Tuning**:
- Configure JVM heap size via `JAVA_OPTS` environment variable
- Adjust database connection pool sizes in configuration files
- Scale services horizontally behind a load balancer

**Monitoring**:
- Health endpoints: `/healthcheck` (admin), `/health` (users)
- Metrics available via JMX and DataDog StatsD
- Structured JSON logs for centralized aggregation

**High Availability**:
- Run multiple application instances
- Use PostgreSQL replication for flockr-admin
- Deploy Aerospike as a multi-node cluster for flockr-users

**Security**:
- Use external secrets management (Vault, AWS Secrets Manager)
- Enable TLS/SSL for all services
- Implement authentication/authorization for API endpoints
- Regular dependency vulnerability scanning

For Kubernetes deployment examples and detailed production setup, refer to [DOCKER.md](DOCKER.md).

## Configuration

### Environment Variables

**Flockr Admin**:
- `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DATABASE`
- `FLINK_HOST`, `FLINK_PORT`
- `SPARK_MASTER_URL`
- `SERVER_PORT` (default: 8080)
- `JAVA_OPTS` (JVM configuration)

**Flockr Users**:
- `AEROSPIKE_HOST`, `AEROSPIKE_PORT`, `AEROSPIKE_NAMESPACE`
- `SERVER_PORT` (default: 8082)
- `JAVA_OPTS` (JVM configuration)

### Configuration Files

Configuration uses HOCON format in `src/main/resources/config/`:
- `application/` - Application settings
- `http-server/` - HTTP server configuration
- `postgres/` - Database settings (flockr-admin)
- `aerospike/` - Aerospike settings (flockr-users)
- `flink/` - Flink client configuration
- `circuit-breaker/` - Resilience4j settings

Environment variables override default configuration values.

## Database

### PostgreSQL (Flockr Admin)

**Location**: `flockr-admin/src/main/resources/db/postgres/`

**Schema Files**:
- `01_schema.sql` - Table definitions and indexes
- `02_seed.sql` - Initial data (connector types)

**Core Tables**:
- `audiences` - Audience definitions with metadata, type, sinks, and expiry
- `rules` - Rule configurations (BATCH/STREAM) with time ranges and configuration JSONB
- `data_connector_types` - Supported connector types with JSON schemas
- `data_sources` - Onboarded data source instances  
- `data_sinks` - Onboarded data sink instances
- `audience_owners` - Ownership and access control

**Setup**:

```bash
# Docker (automatic)
docker-compose up -d postgres

# Manual
createdb flockr
psql -d flockr -f flockr-admin/src/main/resources/db/postgres/01_schema.sql
psql -d flockr -f flockr-admin/src/main/resources/db/postgres/02_seed.sql
```

### Aerospike (Flockr Users)

**Namespace**: `test` (configurable)

**Data Model**: Each user is a record with cohort names as bins and expiry timestamps as values. Multi-tenancy is achieved using Aerospike sets (one per project).

**Performance**:
- Read/Write Latency: <1ms
- Scalability: Linear with cluster size
- Durability: Configurable replication

For detailed schema information, refer to the SQL files in the repository.

## Contributing

We welcome contributions! Here's how to get started:

### Quick Start

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Make changes and write tests
4. Format code: `mvn fmt:format`
5. Run tests: `mvn test`
6. Commit: `git commit -m "feat: add your feature"`
7. Push and open a Pull Request

### Commit Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `refactor:` - Code refactoring
- `test:` - Test additions/updates

### Code Style

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use automated formatting: `mvn fmt:format`
- Write tests for new features
- Keep methods small and focused
- Add JavaDoc for public APIs

### Pull Request Guidelines

- Provide clear title and description
- Reference related issues
- Ensure all tests pass
- Update documentation if needed

For detailed guidelines, see the full Contributing section in the repository.

## License

This project is licensed under the **Apache License 2.0** - see the [LICENSE](LICENSE) file for details.

```
Copyright 2024 Ascend

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Support

### Documentation

- **API Documentation**: http://localhost:8080/swagger-ui/ (Admin), http://localhost:8082/swagger-ui/ (Users)
- **Docker Guide**: [DOCKER.md](DOCKER.md)
- **Vert.x Docs**: https://vertx.io/docs/

### Getting Help

**Issues**: Report bugs or request features at [GitHub Issues](https://github.com/yourusername/flockr/issues)

**Security**: Report vulnerabilities privately to security@ascend.io

For enterprise support and consulting, contact: support@ascend.io

---

## Acknowledgments

Flockr is built with excellent open-source technologies:

**Core**: [Vert.x](https://vertx.io/) • [RxJava](https://github.com/ReactiveX/RxJava) • [Google Guice](https://github.com/google/guice)  
**Data**: [PostgreSQL](https://www.postgresql.org/) • [Aerospike](https://aerospike.com/)  
**Processing**: [Apache Flink](https://flink.apache.org/) • [Apache Spark](https://spark.apache.org/)  
**Resilience**: [Resilience4j](https://resilience4j.readme.io/)  
**Testing**: [Testcontainers](https://www.testcontainers.org/)

---

<div align="center">

**Built with ❤️ using Vert.x and Java 17**

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Vert.x](https://img.shields.io/badge/Vert.x-4.4.9-purple.svg)](https://vertx.io/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

[Documentation](DOCKER.md) • [Issues](https://github.com/yourusername/flockr/issues) • [Discussions](https://github.com/yourusername/flockr/discussions)

</div>

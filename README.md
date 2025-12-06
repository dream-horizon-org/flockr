# Flockr

[![Java Version](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Maven](https://img.shields.io/badge/Maven-3.6+-red.svg)](https://maven.apache.org/)
[![Vert.x](https://img.shields.io/badge/Vert.x-4.4.9-purple.svg)](https://vertx.io/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> A high-performance, reactive, multi-tenant audience segmentation and user cohort management platform built on Vert.x

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
  - [Quick Start with Docker](#quick-start-with-docker)
  - [Local Development Setup](#local-development-setup)
- [Modules](#modules)
- [Development](#development)
- [Configuration](#configuration)
- [Contributing](#contributing)
- [License](#license)
- [Documentation](#documentation)

## Overview

**Flockr** is an enterprise-grade platform for **audience segmentation** and **user cohort management**, designed for organizations that need to:

- **Manage static audiences** with bulk CSV imports and lifecycle management
- **Access user cohorts** with high-performance, real-time lookups using Aerospike
- **Ensure multi-tenancy** with project-level isolation and security
- **Segment audiences dynamically** *(Coming Soon)* using rule-based logic with batch and streaming processing

Built on the **Vert.x reactive toolkit**, Flockr leverages non-blocking, event-driven architecture to deliver high throughput and low latency.

### Supported Audience Types

| Type | Description | Use Case | Status |
|------|-------------|----------|--------|
| **STATIC** | Manual membership via CSV uploads | Bulk user imports with direct push to configured data sinks, no rules required | ✅ Available |
| **CONDITIONAL** | Dynamic membership through rules | Batch SQL queries and real-time event patterns for automatic membership updates | 🚧 Coming Soon |

## Key Features

### Core Capabilities

**Audience Segmentation (Flockr Admin)**
- Create and manage audience segments with metadata and custom configurations
- Bulk CSV upload for assigning users to static audiences
- Track audience lifecycle, user counts, ownership, and verification
- Set expiry dates for automatic audience cleanup

**User Cohort Management (Flockr Users)**
- Retrieve user cohorts with sub-millisecond latency using Aerospike
- Batch cohort mapping for multiple user assignments
- Multi-tenant isolation with project-based segmentation

**Coming Soon**
- **Conditional Audiences**: Dynamic membership through rule-based logic
- **BATCH Rules**: SQL-based queries executed periodically on data sources
- **STREAM Rules**: Real-time event pattern matching
- **Data Connectors**: S3, Kafka, Webhooks integration

### Technical Features

- **Reactive Architecture**: Non-blocking I/O with Vert.x and RxJava3
- **High Performance**: Sub-millisecond cohort lookups, thousands of concurrent requests
- **Resilience**: Circuit breakers, retry logic, health checks
- **Multi-Tenancy**: Project-level isolation with encrypted identifiers
- **Observability**: Structured logging, Dropwizard metrics
- **Developer-Friendly**: Swagger UI, dependency injection, automated testing

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      FLOCKR PLATFORM                        │
├───────────────────────────┬─────────────────────────────────┤
│     FLOCKR-ADMIN          │       FLOCKR-USERS              │
│  (Audience Management)    │    (User Cohort Access)         │
│      Port 8080            │        Port 8082                │
│         ↓                 │           ↓                     │
│    PostgreSQL 16          │     Aerospike 6.4               │
└───────────────────────────┴─────────────────────────────────┘
```

### Technology Stack

| Category | Technologies |
|----------|-------------|
| **Core Framework** | Vert.x 4.4.9, RxJava 3, Java 17+ |
| **Data Storage** | PostgreSQL 16, Aerospike 6.4 |
| **Application** | Google Guice 7.0, RESTEasy 6.2, Resilience4j 2.2 |
| **Observability** | Logback, Dropwizard Metrics |
| **Testing** | JUnit 5, Mockito 5, REST Assured, Testcontainers |

## Getting Started

### Prerequisites

**For Docker Deployment (Recommended)**
- Docker 20.10+
- Docker Compose v2.0+
- 4GB RAM allocated to Docker
- 10GB disk space

**For Local Development**
- Java JDK 17+
- Apache Maven 3.6+
- PostgreSQL 12+ (for flockr-admin)
- Aerospike 6.4+ (for flockr-users)

### Quick Start with Docker

```bash
# Clone the repository
git clone https://github.com/AscendTech4H/flockr.git
cd flockr

# Start all services
./docker/docker-start.sh
```

### Service URLs

| Service | URL |
|---------|-----|
| Flockr Admin API | http://localhost:8250 |
| Flockr Admin Swagger | http://localhost:8250/swagger-ui/ |
| Flockr Users API | http://localhost:8260 |
| Flockr Users Swagger | http://localhost:8260/swagger-ui/ |
| PostgreSQL | localhost:8230 |
| Aerospike | localhost:8200 |

### Docker Management

```bash
./docker/docker-status.sh   # Check status
./docker/docker-stop.sh     # Stop services
./docker/docker-clean.sh    # Clean up everything
```

### Local Development Setup

**1. Set Up Databases**

```bash
# PostgreSQL (flockr-admin)
createdb flockr
psql -d flockr -f flockr-admin/src/main/resources/db/postgres/01_schema.sql
psql -d flockr -f flockr-admin/src/main/resources/db/postgres/02_seed.sql  # optional

# Aerospike (flockr-users) - via Docker
docker run -d --name aerospike -p 3000:3000 aerospike/aerospike-server:6.4.0.0
```

**2. Set Environment Variables**

```bash
# PostgreSQL
export POSTGRES_HOST=localhost POSTGRES_PORT=5432 POSTGRES_DATABASE=flockr
export POSTGRES_USER=your_user POSTGRES_PASSWORD=your_password

# Aerospike
export AEROSPIKE_HOST=localhost AEROSPIKE_PORT=3000 AEROSPIKE_NAMESPACE=test
```

**3. Build & Run**

```bash
mvn clean install -DskipTests

# Run flockr-admin (terminal 1)
cd flockr-admin/target/flockr
java -Dapp.environment=local -jar flockr-admin-1.0-fat.jar

# Run flockr-users (terminal 2)
cd flockr-users/target/flockr
java -Dapp.environment=local -jar flockr-users-1.0-fat.jar
```

**IDE Setup (IntelliJ IDEA)**

| Module | Main Class | Program Arguments |
|--------|------------|-------------------|
| flockr-admin | `io.ascend.flockr.admin.MainLauncher` | `run io.ascend.flockr.admin.verticle.MainVerticle` |
| flockr-users | `io.ascend.flockr.users.MainLauncher` | `run io.ascend.flockr.users.verticle.MainVerticle` |

VM options: `-Dapp.environment=local`

## Modules

### Flockr Admin

Audience segmentation and management.

- **Port**: 8080 (8250 via Docker)
- **Database**: PostgreSQL
- **Features**: Create and manage static audiences, bulk CSV imports, audience lifecycle management

### Flockr Users

High-performance user cohort lookup and assignment.

- **Port**: 8082 (8260 via Docker)
- **Database**: Aerospike
- **Features**: Sub-millisecond cohort queries, bulk CSV import, batch mapping

## Development

### Building from Source

```bash
# Build all modules
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Build specific module
mvn clean install -pl flockr-admin -am
```

### Running Tests

```bash
mvn test                    # Run all tests
mvn test -pl flockr-admin   # Run tests for specific module
mvn clean verify            # Run with coverage
```

### Code Formatting

```bash
mvn com.spotify.fmt:fmt-maven-plugin:format   # Format code
mvn fmt:check                                  # Check formatting
```

This project follows [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).

## Configuration

### Environment Variables

**Flockr Admin**
- `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DATABASE`

**Flockr Users**
- `AEROSPIKE_HOST`, `AEROSPIKE_PORT`, `AEROSPIKE_NAMESPACE`

Configuration uses HOCON format in `src/main/resources/config/`.

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Make changes and write tests
4. Format code: `mvn fmt:format`
5. Run tests: `mvn test`
6. Commit: `git commit -m "feat: add your feature"`
7. Push and open a Pull Request

Follow [Conventional Commits](https://www.conventionalcommits.org/) for commit messages.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Documentation

- **Docker Guide**: [DOCKER.md](DOCKER.md)
- **API Documentation**: Available via Swagger UI when services are running

---

**Built with ❤️ using Vert.x and Java 17**

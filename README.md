# Flockr

[![Java Version](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Maven](https://img.shields.io/badge/Maven-3.6+-red.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> A high-performance, event-driven audience and data connector management platform built on Vert.x

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Configuration](#configuration)
  - [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Modules](#modules)
- [Development](#development)
  - [Project Structure](#project-structure)
  - [Building from Source](#building-from-source)
  - [Running Tests](#running-tests)
  - [Code Formatting](#code-formatting)
  - [Code Coverage](#code-coverage)
- [Technology Stack](#technology-stack)
- [Database](#database)
- [Contributing](#contributing)
- [License](#license)
- [Support](#support)

## Overview

Flockr is a modern, scalable platform designed for managing audiences, rules, and data connectors. Built on the reactive Vert.x framework, it provides high-performance, non-blocking APIs for real-time data processing and management.

The platform enables organizations to:
- Create and manage audience segments with complex rule configurations
- Connect various data sources and sinks through a unified interface
- Process streaming data with Apache Flink integration
- Maintain data integrity with PostgreSQL persistence

## Features

### Core Capabilities

- **Audience Management**: Create, update, and manage audience segments with complex rule-based logic
- **Data Connector Integration**: Seamless integration with multiple data sources and sinks
- **Rule Engine**: Flexible rule configuration supporting multiple rule types and conditions
- **Real-time Processing**: Stream processing integration with Apache Flink
- **Multi-tenancy**: Built-in tenant and project isolation
- **Reactive Architecture**: Non-blocking, event-driven design for high performance
- **Circuit Breaker**: Resilience4j-based fault tolerance for external service calls
- **Metrics & Monitoring**: Dropwizard metrics with DataDog integration
- **API Documentation**: Interactive Swagger UI for API exploration

### Technical Features

- Reactive programming with Vert.x and RxJava3
- PostgreSQL for persistent storage
- Kafka for event streaming
- Google Guice for dependency injection
- Comprehensive validation with Jakarta Bean Validation
- RESTful APIs with Swagger/OpenAPI documentation
- Structured logging with Logback and Logstash encoder

## Architecture

Flockr follows a modular, multi-module Maven architecture:

```
flockr/
├── flockr-admin/     # Admin service for audience & data connector management
└── flockr-users/     # User-facing services (future module)
```

The application uses:
- **Vert.x**: Reactive toolkit for building event-driven applications
- **PostgreSQL**: Primary database for persistent storage
- **Flink**: Stream processing framework

## Getting Started

### 🐳 Quick Start with Docker (Recommended)

The fastest way to get started is using Docker:

```bash
# Clone the repository
git clone https://github.com/yourusername/flockr.git
cd flockr

# Start all services (PostgreSQL, Flink, and Flockr)
./docker-start.sh
```

That's it! All services will start automatically. Access the API at http://localhost:8080

📖 **[Read the complete Docker guide →](DOCKER.md)**

### Prerequisites

#### For Docker Deployment (Recommended)
- **Docker** 20.10+ ([Download](https://docs.docker.com/get-docker/))
- **Docker Compose** v2.0+ ([Download](https://docs.docker.com/compose/install/))
- At least **4GB RAM** allocated to Docker

#### For Local Development
- **Java JDK 17 or higher** ([Download](https://www.oracle.com/java/technologies/downloads/))
- **Apache Maven 3.6+** ([Download](https://maven.apache.org/download.cgi))
- **PostgreSQL 12+** (for database)
- **Apache Flink** (optional, for stream processing)

### Installation

#### Option A: Docker Installation (Recommended)

1. **Clone the repository**

```bash
git clone https://github.com/yourusername/flockr.git
cd flockr
```

2. **Start with Docker**

```bash
./docker-start.sh
```

The database schema and seed data are automatically loaded. See [DOCKER.md](DOCKER.md) for details.

#### Option B: Local Installation

1. **Clone the repository**

```bash
git clone https://github.com/yourusername/flockr.git
cd flockr
```

2. **Install dependencies**

```bash
mvn clean install
```

3. **Set up the database**

```bash
# Create PostgreSQL database
createdb flockr

# Run schema migrations
psql -d flockr -f flockr-admin/src/main/resources/db/postgres/schema.sql

# (Optional) Load seed data
psql -d flockr -f flockr-admin/src/main/resources/db/postgres/seed.sql
```

### Configuration

Flockr uses Typesafe Config (HOCON) for configuration management. Configuration files are located in:

```
src/main/resources/config/
├── application/default.conf      # Application settings
├── postgres/default.conf         # Database configuration
├── kafka-producer/default.conf   # Kafka settings
├── flink/default.conf            # Flink settings
└── ...
```

#### Environment Variables

Create a `.env` file or export the following environment variables:

```bash
# Environment
export ENV=local
export TEAM_SUFFIX=-dev

# Database
export POSTGRES_USER=your_postgres_user
export POSTGRES_PASSWORD=your_postgres_password
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_DATABASE=flockr

# Optional: Private hosted zone for service discovery
export PRIVATE_HOSTED_ZONE=your-hosted-zone
```

### Running the Application

#### Option 1: Docker (Recommended)

```bash
# Start all services
./docker-start.sh

# View logs
docker-compose logs -f flockr-admin

# Stop services
./docker-stop.sh

# Check status
./docker-status.sh
```

Access the application at http://localhost:8080

See [DOCKER.md](DOCKER.md) for complete Docker documentation.

#### Option 2: Run from JAR

```bash
# Build the fat JAR
mvn clean package

# Navigate to the module directory
cd flockr-admin/target/flockr-admin

# Run the application
java \
  -Dapp.environment=local \
  -Dlogback.configurationFile=./resources/logback/logback-local.xml \
  -jar flockr-admin-1.0-fat.jar
```

#### Option 2: IntelliJ IDEA Run Configuration

1. Open the project in IntelliJ IDEA
2. Create a new **Application** Run Configuration
3. Configure as follows:
   - **Main class**: `io.ascend.flockr.admin.MainLauncher`
   - **VM options**: 
     ```
     -Dapp.environment=local
     -Dlogback.configurationFile=logback/logback-local.xml
     ```
   - **Program arguments**: 
     ```
     run io.ascend.flockr.admin.verticle.MainVerticle
     ```
   - **Environment variables**:
     ```
     ENV=local;POSTGRES_USER=your_user;POSTGRES_PASSWORD=your_password
     ```
4. Run the configuration

#### Option 4: Maven Exec Plugin

```bash
mvn clean compile exec:java \
  -Dexec.mainClass="io.ascend.flockr.admin.MainLauncher" \
  -Dapp.environment=local
```

The application will start on `http://localhost:8080` (default port).

## 🐳 Docker Deployment

Flockr includes a complete Docker setup with all dependencies:

### Quick Start

```bash
./docker-start.sh
```

### What's Included

The Docker environment includes:
- **PostgreSQL 16** - Primary database
- **Apache Flink 1.17** - Stream processing
- **Apache Spark 3.5** - Distributed computing (Master + Worker)
- **Flockr Admin** - Main application

### Services & Ports

| Service | Port | Description |
|---------|------|-------------|
| Flockr Admin | 8080 | Main REST API |
| PostgreSQL | 5432 | Database |
| Flink Dashboard | 8081 | Stream processing UI |
| Spark Master | 8082 | Spark cluster UI |
| Spark Worker | 8083 | Spark worker UI |

### Management Commands

```bash
# Start services
./docker-start.sh

# Check status
./docker-status.sh

# View logs
docker-compose logs -f

# Stop services
./docker-stop.sh

# Clean up everything
./docker-clean.sh
```

**📖 [Complete Docker Documentation →](DOCKER.md)**

## API Documentation

Once the application is running, access the interactive Swagger UI:

```
http://localhost:8080/swagger-ui/
```

### Key API Endpoints

**All API endpoints require the `X-Project-Id` header** containing an encrypted project identifier (amalgamation of tenantId and projectId).

**Optional `email` header** can be provided for actor tracking (defaults to 'system' if not provided).

#### Audiences API

```bash
# List all audiences
GET /v1/audiences
Headers:
  X-Project-Id: <encrypted-project-id>
  
# Get audience by ID
GET /v1/audiences/{id}
Headers:
  X-Project-Id: <encrypted-project-id>
  
# Create new audience
POST /v1/audiences
Headers:
  X-Project-Id: <encrypted-project-id>
  email: user@example.com  # Optional, defaults to 'system'
Body:
  {
    "name": "High Value Customers",
    "description": "Customers with LTV > $1000",
    "type": "STANDARD",
    "sinkIds": [1, 2],
    "expireDate": 1735689600000
  }

# Create rules for an audience
POST /v1/audiences/{audienceId}/rules
Headers:
  X-Project-Id: <encrypted-project-id>
  email: user@example.com  # Optional, defaults to 'system'
Body:
  {
    "rules": [...]
  }

# Get rule details
GET /v1/audiences/{audienceId}/rules/{ruleId}
Headers:
  X-Project-Id: <encrypted-project-id>

# Get audience owners
GET /v1/audiences/{audienceId}/owners
Headers:
  X-Project-Id: <encrypted-project-id>

# Update audience owner
POST /v1/audiences/{audienceId}/owners
Headers:
  X-Project-Id: <encrypted-project-id>
  email: user@example.com  # Optional, defaults to 'system'
Body:
  {
    "action": "ADD",
    "email": "newowner@example.com"
  }
```

#### Data Connectors API

```bash
# List connector types
GET /v1/connectors/types?kind=SOURCE

# List data sources
GET /v1/datasources?pageSize=10&pageNum=0

# List data sinks
GET /v1/datasinks?pageSize=10&pageNum=0

# Onboard a data source
POST /v1/datasources/onboard
Headers:
  email: user@example.com  # Optional, defaults to 'system'
Body:
  {
    "name": "User Events Kafka",
    "typeId": 1,
    "config": {
      "topic": "user-events",
      "bootstrapServersUrl": "localhost:9092"
    }
  }

# Onboard a data sink
POST /v1/datasinks/onboard
Headers:
  email: user@example.com  # Optional, defaults to 'system'
Body:
  {
    "name": "S3 Export Bucket",
    "typeId": 3,
    "config": {
      "bucket": "my-audience-exports",
      "folderPath": "audiences/",
      "region": "us-east-1"
    }
  }
```

#### Health Check

```bash
GET /health
# No headers required
```

### Header Reference

| Header | Required | Description | Example |
|--------|----------|-------------|---------|
| `X-Project-Id` | Yes (for most endpoints) | Encrypted amalgamation of tenantId and projectId | `eyJ0ZW5hbnRJZCI6InRlbmFudDEiLCJwcm9qZWN0SWQiOiJwcm9qMSJ9` |
| `email` | No | Actor email/username for audit tracking. Defaults to 'system' if not provided | `user@example.com` |

**Full API specification**: [Swagger YAML](./flockr-admin/src/main/resources/webroot/swagger/swagger.yaml)

## Modules

### flockr-admin

The admin service provides APIs for managing:
- **Audiences**: Segment definitions with rule-based logic
- **Rules**: Configurable business rules for audience qualification
- **Data Connectors**: Integration with external data sources and sinks
- **Audience Owners**: User/team ownership and permissions

**Main Class**: `io.ascend.flockr.admin.MainLauncher`

### flockr-users

User-facing services (coming soon)

## Development

### Project Structure

```
flockr-admin/
├── src/
│   ├── main/
│   │   ├── java/io/ascend/flockr/admin/
│   │   │   ├── client/           # External client integrations
│   │   │   ├── config/           # Configuration classes
│   │   │   ├── constants/        # Application constants
│   │   │   ├── domain/           # Domain models
│   │   │   ├── exception/        # Custom exceptions
│   │   │   ├── injection/        # Dependency injection
│   │   │   ├── io/               # Request/Response DTOs
│   │   │   ├── repository/       # Data access layer
│   │   │   ├── rest/             # REST controllers
│   │   │   ├── service/          # Business logic
│   │   │   ├── util/             # Utility classes
│   │   │   ├── validation/       # Custom validators
│   │   │   └── verticle/         # Vert.x verticles
│   │   └── resources/
│   │       ├── config/           # Configuration files
│   │       ├── db/               # Database scripts
│   │       ├── logback/          # Logging configuration
│   │       └── webroot/          # Static resources & Swagger
│   └── test/                     # Test classes
└── pom.xml
```

### Building from Source

```bash
# Clean and build all modules
mvn clean install

# Build specific module
mvn clean install -pl flockr-admin

# Skip tests
mvn clean install -DskipTests

# Build without running integration tests
mvn clean package -DskipITs
```

### Running Tests

```bash
# Run all tests (unit + integration)
mvn test integration-test

# Run unit tests only
mvn test

# Run integration tests only
mvn integration-test

# Run tests for specific module
mvn test -pl flockr-admin

# Run specific test class
mvn test -Dtest=AudienceServiceImplTest

# Run with coverage
mvn clean verify
```

### Code Formatting

This project uses [google-java-format](https://github.com/google/google-java-format) via the Spotify fmt-maven-plugin.

```bash
# Auto-format all code
mvn com.spotify.fmt:fmt-maven-plugin:format

# Check code formatting
mvn com.spotify.fmt:fmt-maven-plugin:check
```

**Note**: Code formatting runs automatically during the build process.

### Code Coverage

Code coverage is measured using JaCoCo:

```bash
# Generate coverage report
mvn clean verify

# View report
open target/site/jacoco/index.html
```

Coverage reports are generated in:
- `target/site/jacoco/` - Unit test coverage
- `target/site/jacoco-it/` - Integration test coverage

## Technology Stack

### Core Framework
- **Vert.x 4.4.9** - Reactive toolkit
- **RxJava 3** - Reactive programming

### Data & Persistence
- **PostgreSQL** - Primary database

### Stream Processing
- **Apache Flink** - Stream processing

### Dependency Injection
- **Google Guice 7.0.0** - DI framework

### Validation & API
- **Jakarta Bean Validation** - Input validation
- **Swagger/OpenAPI 3.0** - API documentation
- **RESTEasy 6.2.8** - JAX-RS implementation

### Resilience
- **Resilience4j 2.2.0** - Circuit breaker & retry

### Logging & Metrics
- **Logback 1.4.14** - Logging framework
- **Logstash Encoder 7.4** - Structured logging
- **Dropwizard Metrics 4.2.23** - Application metrics
- **DataDog StatsD Client 4.2.0** - Metrics reporting

### Testing
- **JUnit 4 & 5** - Testing framework
- **Mockito 5.3.1** - Mocking framework
- **REST Assured 5.3.2** - API testing
- **Testcontainers 1.19.8** - Integration testing
- **WireMock 3.6.0** - HTTP mocking

### Build & Utilities
- **Maven 3.6+** - Build tool
- **Lombok 1.18.30** - Code generation
- **Typesafe Config 1.4.3** - Configuration
- **Apache Commons** - Utility libraries
- **Caffeine 3.1.8** - In-memory cache

## Database

### Schema

The database schema is defined in:
- **Schema**: `flockr-admin/src/main/resources/db/postgres/schema.sql`
- **Seed Data**: `flockr-admin/src/main/resources/db/postgres/seed.sql`

### Key Tables

- `audiences` - Audience definitions
- `audience_owners` - Ownership and permissions
- `rules` - Rule configurations
- `data_connectors` - External data source/sink configurations

### Migrations

Currently using SQL scripts. Future versions may integrate with Flyway or Liquibase.

## Contributing

We welcome contributions! Please follow these guidelines:

### How to Contribute

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. **Make your changes**
   - Write clean, documented code
   - Follow existing code style
   - Add tests for new functionality
4. **Format your code**
   ```bash
   mvn com.spotify.fmt:fmt-maven-plugin:format
   ```
5. **Run tests**
   ```bash
   mvn test integration-test
   ```
6. **Commit your changes**
   ```bash
   git commit -m "Add amazing feature"
   ```
7. **Push to your fork**
   ```bash
   git push origin feature/amazing-feature
   ```
8. **Open a Pull Request**

### Code Style

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use meaningful variable and method names
- Write clear comments for complex logic
- Keep methods small and focused
- Write comprehensive tests

### Commit Messages

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
feat: add new audience filtering capability
fix: resolve null pointer in rule validation
docs: update API documentation
test: add integration tests for data connectors
refactor: simplify audience service logic
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Support

### Documentation

- [API Documentation](http://localhost:8080/swagger-ui/) (when running)
- [Vert.x Documentation](https://vertx.io/docs/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

### Issues

If you encounter any issues or have questions:

1. Check the [Issues](https://github.com/yourusername/flockr/issues) page
2. Create a new issue with detailed information:
   - Description of the problem
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details (OS, Java version, etc.)
   - Relevant logs or error messages

### Community

- **GitHub Discussions**: [Join the conversation](https://github.com/yourusername/flockr/discussions)
- **Stack Overflow**: Tag questions with `flockr`

---

**Built with ❤️ using Vert.x and modern Java**

For more information about the architecture and design decisions, see the [Wiki](https://github.com/yourusername/flockr/wiki).

# Flocker Historic Engine

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spark](https://img.shields.io/badge/Spark-4.0.0-blue.svg)](https://spark.apache.org/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-red.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Proprietary-lightgrey.svg)](LICENSE)

A distributed Spark-based engine for processing large-scale cohort data from AWS Athena, transforming it, and synchronizing it with external systems via REST APIs. The engine executes SQL queries on Athena, processes results using Spark, and writes data to S3 and API endpoints in a standardized format.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [API Reference](#api-reference)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## 🎯 Overview

Flocker Historic Engine is designed to:
- Execute SQL queries on AWS Athena to extract user cohort data
- Process and transform large datasets using Apache Spark
- Write results to S3 in Parquet format with standardized schema
- Synchronize cohort updates with external systems via REST APIs
- Support both append and remove operations for cohort management

### Use Cases

- **Cohort Management**: Manage user cohorts based on historical data queries
- **Data Synchronization**: Keep external systems in sync with Athena query results
- **Batch Processing**: Process large-scale data transformations efficiently
- **API Integration**: Automatically notify external APIs about cohort changes

## ✨ Features

- **🔌 Multiple Data Sources**: Support for AWS Athena and S3 as data sources
- **📤 Multiple Sinks**: Write to S3 (Parquet/CSV), REST APIs, and Kafka (planned)
- **🔄 Flexible Actions**: Support for `append` and `remove` operations
- **⚡ Distributed Processing**: Leverages Apache Spark for scalable data processing
- **🔐 AWS Integration**: Native support for AWS Athena, S3, and IAM credentials
- **📊 Data Transformation**: Automatic schema transformation to standardized format
- **🌐 REST API Integration**: Built-in HTTP client with rate limiting and batching
- **⚙️ Configurable**: Flexible configuration via JSON and config files
- **📝 Comprehensive Logging**: Detailed logging for debugging and monitoring

## 🏗️ Architecture

```
┌─────────────────┐
│   Spark Master  │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
┌───▼───┐ ┌──▼────┐
│Worker │ │Worker │
└───┬───┘ └───┬───┘
    │         │
    └────┬────┘
         │
    ┌────▼─────────────────────────────────────┐
    │     Flocker Historic Engine              │
    │                                           │
    │  ┌──────────┐      ┌──────────┐         │
    │  │  Source  │─────▶│  Process │         │
    │  │ (Athena) │      │  (Spark) │         │
    │  └──────────┘      └─────┬────┘         │
    │                          │               │
    │                    ┌─────▼─────┐        │
    │                    │   Sinks   │        │
    │                    │           │        │
    │              ┌─────┴───┐  ┌───┴────┐   │
    │              │   S3    │  │  API   │   │
    │              │(Parquet)│  │(REST)  │   │
    │              └─────────┘  └────────┘   │
    └─────────────────────────────────────────┘
```

### Components

1. **EngineStart**: Main entry point that parses arguments and initializes Spark
2. **Source Modules**: Read data from Athena or S3
3. **S3Process**: Core processing logic that transforms data
4. **Sink Modules**: Write processed data to S3, APIs, or Kafka
5. **ApiClient**: HTTP client for REST API communication with rate limiting

## 📦 Prerequisites

### Required Software

- **Java 21** or higher
- **Apache Spark 4.0.0** or compatible version
- **Maven 3.6+**
- **AWS Account** with:
  - AWS Athena access
  - S3 bucket for query results
  - IAM credentials with appropriate permissions

### Required AWS Permissions

Your AWS credentials need the following permissions:

**Athena:**
- `athena:StartQueryExecution`
- `athena:GetQueryExecution`
- `athena:GetQueryResults`

**S3:**
- Read access to Athena output location
- Write access to S3 sink location

**IAM:**
- Ability to assume roles (if using temporary credentials)

## 🚀 Installation

### 1. Clone the Repository

```bash
git clone <repository-url>
cd flockr-historic-engine
```

### 2. Build the Project

```bash
# Build the project (skip tests for faster build)
mvn clean package -DskipTests

# The JAR will be created at:
# target/flockr-historic-engine-0.1.0-SNAPSHOT.jar
```

### 3. Verify Installation

```bash
# Check JAR was created
ls -lh target/flockr-historic-engine-0.1.0-SNAPSHOT.jar

# Should be approximately 600-700 MB (includes all dependencies)
```

## ⚙️ Configuration

### Configuration Files

Configuration files are located in `src/main/resources/config/`:

#### API Configuration (`config/sink/api/default.conf`)
```conf
url = "http://localhost:8080/flockr/users/map-cohorts"
rateLimitPerSecond = 100
batchSize = 100
timeoutSeconds = 30
contentType = "application/json"
projectKey = "tenant1_100"
```

#### S3 Configuration (`config/sink/s3/default.conf`)
```conf
# S3 sink configuration
bucket = "your-bucket"
path = "output/path"
format = "parquet"
compression = "snappy"
```

#### Spark Configuration (`config/spark/default.conf`)
```conf
# Spark-specific configurations
appName = "FlockerHistoricEngine"
sqlShufflePartitions = 200
```

### Environment Variables

You can override configuration using environment variables or system properties.

## 📖 Usage

### Basic Usage

The engine accepts a JSON configuration as a command-line argument:

```bash
spark-submit \
  --master spark://localhost:7077 \
  --class com.dream11.flocker.engine.EngineStart \
  --deploy-mode cluster \
  --name "FlockerHistoricEngine" \
  target/flockr-historic-engine-0.1.0-SNAPSHOT.jar \
  '{"sqlQuery":"SELECT DISTINCT user_id FROM database.table","cohortName":"test-cohort","action":"append","expireAt":"2025-12-31 23:59:59","sparkMaster":"spark://localhost:7077","sourceJson":[{"type":"ATHENA","config":{"region":"us-east-1","workgroup":"primary","outputLocation":"s3a://bucket/athena-results/","accessKey":"YOUR_KEY","secretKey":"YOUR_SECRET"}}],"destinationJson":[]}'
```

### Using the Test Script

1. **Edit `test-job.sh`** with your configuration:
   ```bash
   # Update these values
   SQL_QUERY="SELECT DISTINCT user_id FROM your_database.your_table"
   COHORT_NAME="test-cohort-001"
   ACTION="append"
   EXPIRE_AT="2025-12-31 23:59:59"
   AWS_ACCESS_KEY="your-access-key"
   AWS_SECRET_KEY="your-secret-key"
   S3_OUTPUT_LOCATION="s3a://your-bucket/athena-results/"
   ```

2. **Run the script**:
   ```bash
   ./test-job.sh
   ```

### JSON Configuration Format

```json
{
  "sqlQuery": "SELECT DISTINCT user_id FROM database.table WHERE condition",
  "cohortName": "premium-users",
  "action": "append",
  "expireAt": "2025-12-31 23:59:59",
  "sparkMaster": "spark://localhost:7077",
  "sourceJson": [
    {
      "type": "ATHENA",
      "config": {
        "region": "us-east-1",
        "workgroup": "primary",
        "outputLocation": "s3a://bucket/athena-results/",
        "accessKey": "YOUR_ACCESS_KEY",
        "secretKey": "YOUR_SECRET_KEY",
        "sessionToken": "OPTIONAL_SESSION_TOKEN"
      }
    }
  ],
  "destinationJson": []
}
```

### Configuration Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `sqlQuery` | String | Yes | SQL query to execute on Athena |
| `cohortName` | String | Yes | Name of the cohort |
| `action` | String | Yes | Action type: `"append"` or `"remove"` |
| `expireAt` | String | Yes | Expiration date/time: `"YYYY-MM-DD HH:MM:SS"` |
| `sparkMaster` | String | Yes | Spark master URL: `"spark://host:port"` |
| `sourceJson` | Array | Yes | Source configuration (must include ATHENA) |
| `destinationJson` | Array | Optional | Destination configuration (empty array for defaults) |

### Source Configuration (Athena)

```json
{
  "type": "ATHENA",
  "config": {
    "region": "us-east-1",
    "workgroup": "primary",
    "outputLocation": "s3a://bucket/path/",
    "accessKey": "AKIA...",
    "secretKey": "secret...",
    "sessionToken": "optional-token",
    "database": "optional-if-in-query"
  }
}
```

**Note:** `database` is optional if your SQL query uses `database.table` format.

## 🔌 API Reference

### Input Format

The engine expects the first column of the query result to contain user IDs.

### Output Format

#### S3 Parquet Output

The Parquet files written to S3 have the following schema:

```json
{
  "user_id": 12345,
  "cohort_key": "premium-users",
  "action": "append",
  "expire_at": "2025-12-31 23:59:59"
}
```

#### API Payload Format

The engine calls the configured API endpoint with the following payload:

**Endpoint:** `http://localhost:8080/flockr/users/map-cohorts`  
**Method:** `POST`  
**Headers:**
- `Content-Type: application/json`
- `x-project-key: tenant1_100`

**Body:**
```json
[
  {
    "user_id": 12345,
    "cohort_key": "premium-users",
    "action": "append",
    "expire_at": "2025-12-31 23:59:59"
  },
  {
    "user_id": 67890,
    "cohort_key": "premium-users",
    "action": "append",
    "expire_at": "2025-12-31 23:59:59"
  }
]
```

The API client automatically batches requests based on the `batchSize` configuration and applies rate limiting.

## 📁 Project Structure

```
flockr-historic-engine/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/dream11/flocker/engine/
│       │       ├── config/          # Configuration classes
│       │       ├── constants/       # Constants
│       │       ├── enums/           # Enumerations
│       │       ├── injector/        # Dependency injection
│       │       ├── modules/
│       │       │   ├── sink/        # Output modules (S3, API, Kafka)
│       │       │   └── source/      # Input modules (Athena, S3)
│       │       ├── service/         # Business logic
│       │       └── utils/          # Utility classes
│       └── resources/
│           └── config/             # Configuration files
├── target/                          # Build output
├── pom.xml                          # Maven configuration
├── test-job.sh                      # Test script
├── TESTING_GUIDE.md                 # Testing documentation
└── README.md                        # This file
```

### Key Classes

- **EngineStart**: Main entry point
- **S3Process**: Core processing logic
- **AthenaSourceImpl**: Athena data source implementation
- **ApiSinkImpl**: REST API sink implementation
- **S3SinkImpl**: S3 sink implementation
- **ApiClient**: HTTP client with rate limiting

## 🧪 Testing

### Quick Start

1. **Set up Spark standalone cluster** (see [TESTING_GUIDE.md](TESTING_GUIDE.md))
2. **Build the project**: `mvn clean package -DskipTests`
3. **Configure test script**: Edit `test-job.sh` with your values
4. **Run test**: `./test-job.sh`

### Detailed Testing Guide

See [TESTING_GUIDE.md](TESTING_GUIDE.md) for comprehensive testing instructions including:
- Spark cluster setup
- Configuration examples
- Monitoring and debugging
- Troubleshooting common issues

### Verify Results

1. **Check Spark UI**: http://localhost:8080
2. **Verify S3 Output**: Check Parquet files in your S3 bucket
3. **Check API Calls**: Verify API endpoint received the data
4. **Review Logs**: Check Spark application logs for details

## 🔧 Troubleshooting

### Common Issues

#### Invalid AWS Credentials
```
Error: The security token included in the request is invalid
```
**Solution:** Verify your AWS credentials are valid and have proper permissions.

#### Cannot Read CSV Files
```
Error: CANNOT_READ_FILE_FOOTER
```
**Solution:** The engine now tries CSV first, then Parquet. Verify S3 path and permissions.

#### Spark Master Not Running
```
Error: Spark Master is not running on port 7077
```
**Solution:** Start Spark master: `$SPARK_HOME/sbin/start-master.sh`

#### JAR File Not Found
```
Error: JAR file not found
```
**Solution:** Build the project: `mvn clean package -DskipTests`

### Getting Help

1. Check the [TESTING_GUIDE.md](TESTING_GUIDE.md) troubleshooting section
2. Review Spark and application logs
3. Verify all configuration values are correct
4. Ensure AWS credentials and permissions are valid

## 🤝 Contributing

### Development Setup

1. Clone the repository
2. Install dependencies: `mvn install`
3. Make your changes
4. Build: `mvn clean package`
5. Test: Follow testing guide

### Code Style

- Follow Java coding conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Maintain existing code style

### Submitting Changes

1. Create a feature branch
2. Make your changes
3. Test thoroughly
4. Submit a pull request with description

## 📄 License

This project is proprietary software. All rights reserved.

## 👥 Authors

- **Dream11 Engineering Team**

## 🙏 Acknowledgments

- Apache Spark community
- AWS SDK contributors
- All open-source dependencies

---

## 📚 Additional Resources

- [Apache Spark Documentation](https://spark.apache.org/docs/latest/)
- [AWS Athena User Guide](https://docs.aws.amazon.com/athena/)
- [Spark Standalone Mode](https://spark.apache.org/docs/latest/spark-standalone.html)

---

**Version:** 0.1.0-SNAPSHOT  
**Last Updated:** December 2025


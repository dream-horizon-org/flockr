# Flocker Historic Engine - Testing Guide

This directory contains everything needed to test the Flocker Historic Engine with a complete local setup including Spark, Kafka, and a mock API server.

## 📋 Prerequisites

- **Docker** (version 20.10 or higher)
- **Docker Compose** (version 2.0 or higher, or use `docker compose` plugin)
- **Java 21** (for building the JAR locally)
- **Maven 3.6+** (for building the JAR)
- **curl** (for health checks)
- **jq** (optional, for pretty JSON output)

## 🏗️ Architecture

The testing environment consists of:

1. **Zookeeper** - Required for Kafka coordination
2. **Kafka** - Message broker for testing Kafka sink
3. **Mock API Server** - Simulates the audience update API endpoint
4. **Spark Master** - Spark cluster master node
5. **Spark Worker** - Spark cluster worker node

## 🚀 Quick Start

### Unified Test Script

The easiest way to run tests is using the unified `test.sh` script:

**Basic Sanity Test (no config file needed):**
```bash
cd testing
chmod +x test.sh
./test.sh --sanity-test
```

**Full Test with Custom Config:**
```bash
cd testing
chmod +x test.sh
./test.sh
```

This will:
- Set up all services (Zookeeper, Kafka, Mock API Server, Spark Master, and Spark Worker)
- Build the JAR using Maven
- Create a Docker image
- Replace credential placeholders in `test-config.json` with actual values (or use built-in sanity test)
- Run a test job with your configuration
- Optionally tear down services after completion

**Available Options:**
- `--sanity-test` - Run a basic sanity test (no config file needed)
- `--setup-only` - Only setup services, don't run tests
- `--teardown-only` - Only teardown services
- `--skip-setup` - Skip service setup (services already running)
- `--skip-teardown` - Skip service teardown (keep services running)
- `--skip-build` - Skip building JAR and Docker image
- `--help` - Show all options

### Credentials Setup

The test config uses placeholders for credentials. You can provide actual credentials in two ways:

**Option 1: Environment Variables**
```bash
export TEST_ACCESS_KEY="your-aws-access-key"
export TEST_SECRET_KEY="your-aws-secret-key"
export TEST_SESSION_TOKEN="your-aws-session-token"
./test.sh
```

**Option 2: Secrets File**
```bash
cp .test-secrets.example .test-secrets
# Edit .test-secrets with your credentials
./test.sh
```

### Manual Service Management

If you prefer to manage services separately:

**Start services:**
```bash
./setup.sh
# or
./test.sh --setup-only
```

**Stop services:**
```bash
./teardown.sh
# or
./test.sh --teardown-only
```

**Run test without setup/teardown:**
```bash
./test.sh --skip-setup --skip-teardown
```

## 📡 Service Endpoints

Once services are running, you can access:

| Service | URL | Description |
|---------|-----|-------------|
| Spark Master UI | http://localhost:8080 | Spark cluster management UI |
| Spark Worker UI | http://localhost:8081 | Spark worker status UI |
| Mock API Health | http://localhost:8000/health | Mock API health check |
| Mock API Endpoint | http://localhost:8000/api/audience/update | Audience update endpoint |
| Kafka Broker | localhost:9092 | Kafka broker connection |

## 🧪 Testing Scenarios

### Test 1: API Sink with Mock Server

This test verifies that the engine can send audience update requests to the API.

**Configuration:**
```json
{
  "audienceName": "test-audience",
  "action": "append",
  "expireAt": 1735689599,
  "source": {
    "type": "S3",
    "config": {
      "bucket": "test-bucket",
      "key": "test-data.csv",
      "format": "CSV"
    }
  },
  "destinationJson": [
    {
      "type": "API",
      "config": {
        "url": "http://host.docker.internal:8000/api/audience/update",
        "rateLimitPerSecond": 10,
        "batchSize": 100,
        "timeoutSeconds": 30,
        "contentType": "application/json"
      }
    }
  ]
}
```

**Run:**
```bash
docker run --rm \
  --network testing-testing-network \
  --add-host=host.docker.internal:host-gateway \
  flockr-historic-engine:test \
  '{"audienceName":"test-audience","action":"append",...}'
```

**Verify:**
- Check Mock API logs: `./testing/api-logs/api-*.log`
- Check Spark UI: http://localhost:8080

### Test 2: Kafka Sink

This test verifies that the engine can write to Kafka topics.

**Configuration:**
```json
{
  "audienceName": "test-audience",
  "action": "append",
  "expireAt": 1735689599,
  "source": {
    "type": "S3",
    "config": {
      "bucket": "test-bucket",
      "key": "test-data.csv",
      "format": "CSV"
    }
  },
  "destinationJson": [
    {
      "type": "KAFKA",
      "config": {
        "bootstrapServers": "kafka:9093",
        "topic": "audience-updates",
        "acks": "all"
      }
    }
  ]
}
```

**Run:**
```bash
docker run --rm \
  --network testing-testing-network \
  flockr-historic-engine:test \
  '{"audienceName":"test-audience","action":"append",...}'
```

**Verify:**
```bash
# Consume messages from Kafka
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic audience-updates \
  --from-beginning
```

### Test 3: S3 Sink

This test verifies that the engine can write to S3 (requires AWS credentials).

**Configuration:**
```json
{
  "audienceName": "test-audience",
  "action": "append",
  "expireAt": 1735689599,
  "source": {
    "type": "S3",
    "config": {
      "bucket": "test-bucket",
      "key": "test-data.csv",
      "format": "CSV"
    }
  },
  "destinationJson": [
    {
      "type": "S3",
      "config": {
        "bucket": "output-bucket",
        "key": "output/audience-updates.json",
        "format": "JSON"
      }
    }
  ]
}
```

## 📊 Monitoring and Debugging

### View Service Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f kafka
docker-compose logs -f mock-api
docker-compose logs -f spark-master
docker-compose logs -f spark-worker
```

### Check Service Status

```bash
docker-compose ps
```

### Mock API Logs

The mock API server logs all requests to:
```
./testing/api-logs/api-YYYYMMDD-HHMMSS.log
```

### Spark Application Logs

View Spark application logs through the Spark UI:
- http://localhost:8080 - Master UI
- http://localhost:8081 - Worker UI

### Kafka Topics

List all topics:
```bash
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

Describe a topic:
```bash
docker exec kafka kafka-topics --describe \
  --topic audience-updates \
  --bootstrap-server localhost:9092
```

## 🔧 Troubleshooting

### Services Not Starting

1. **Check Docker is running:**
   ```bash
   docker info
   ```

2. **Check port conflicts:**
   - Port 8080: Spark Master UI
   - Port 8081: Spark Worker UI
   - Port 8000: Mock API Server
   - Port 9092: Kafka
   - Port 2181: Zookeeper

3. **View service logs:**
   ```bash
   docker-compose logs [service-name]
   ```

### Mock API Not Responding

1. Check if container is running:
   ```bash
   docker ps | grep mock-api
   ```

2. Check health endpoint:
   ```bash
   curl http://localhost:8000/health
   ```

3. View logs:
   ```bash
   docker-compose logs mock-api
   ```

### Kafka Connection Issues

1. Verify Kafka is healthy:
   ```bash
   docker exec kafka kafka-broker-api-versions \
     --bootstrap-server localhost:9092
   ```

2. Check if Zookeeper is running:
   ```bash
   docker exec zookeeper nc -z localhost 2181
   ```

### Spark Job Fails

1. Check Spark UI for errors: http://localhost:8080
2. View Spark logs:
   ```bash
   docker-compose logs spark-master
   docker-compose logs spark-worker
   ```
3. Ensure sufficient memory is allocated to Spark workers

## 🧹 Cleanup

### Stop Services

```bash
./teardown.sh
```

### Remove All Data (Volumes)

```bash
docker-compose down -v
```

### Remove Docker Image

```bash
docker rmi flockr-historic-engine:test
```

## 📝 Example Test Configurations

### Minimal API Test

```json
{
  "audienceName": "test-cohort",
  "action": "append",
  "expireAt": 1735689599,
  "source": {
    "type": "S3",
    "config": {
      "bucket": "test",
      "key": "data.csv",
      "format": "CSV"
    }
  },
  "destinationJson": [
    {
      "type": "API",
      "config": {
        "url": "http://host.docker.internal:8000/api/audience/update",
        "rateLimitPerSecond": 5,
        "batchSize": 50
      }
    }
  ]
}
```

### Multiple Sinks Test

```json
{
  "audienceName": "test-cohort",
  "action": "append",
  "expireAt": 1735689599,
  "source": {
    "type": "S3",
    "config": {
      "bucket": "test",
      "key": "data.csv",
      "format": "CSV"
    }
  },
  "destinationJson": [
    {
      "type": "API",
      "config": {
        "url": "http://host.docker.internal:8000/api/audience/update",
        "rateLimitPerSecond": 10,
        "batchSize": 100
      }
    },
    {
      "type": "KAFKA",
      "config": {
        "bootstrapServers": "kafka:9093",
        "topic": "audience-updates"
      }
    }
  ]
}
```

## 🔐 Security Notes

⚠️ **Important:** This testing environment is for local development only. Do not use in production:

- No authentication/authorization
- No encryption
- Exposed ports
- Default credentials (if any)

For production deployments, ensure proper security measures are in place.

## 📚 Additional Resources

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Apache Spark Documentation](https://spark.apache.org/docs/latest/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Project README](../README.md)

## 🐛 Reporting Issues

If you encounter issues with the testing setup:

1. Check service logs
2. Verify all prerequisites are met
3. Ensure ports are not in use
4. Check Docker resources (memory, CPU)
5. Review the troubleshooting section above

For project-specific issues, please refer to the main project documentation.


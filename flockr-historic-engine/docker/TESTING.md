# Testing Guide

Basic positive testing steps for Flocker Historic Engine using Docker Compose.

## Prerequisites

- Docker and Docker Compose installed
- Maven 3.6+
- AWS credentials with Athena and S3 access

## Testing Steps

### Step 1: Build the Project

```bash
# From project root
mvn clean package -DskipTests
```

### Step 2: Build and Start Docker Services

```bash
# Navigate to docker directory
cd docker

# Build Docker images (first time takes 5-10 minutes)
docker-compose build

# Start all services
docker-compose up -d

# Verify services are running
docker-compose ps
```

**Expected Services:**
- spark-master (port 7077, UI: 8080)
- spark-worker (UI: 8081)
- mock-api (port 8082)

### Step 3: Verify Services

```bash
# Check all services are up
docker-compose ps

# Access Spark UI
open http://localhost:8080

# Test Mock API
curl http://localhost:8082/health
```

### Step 4: Submit Test Job

**Prepare JSON Payload:**

Update the following values in the command below:
- `SQL_QUERY`: Your Athena SQL query
- `COHORT_NAME`: Your cohort name
- `AWS_ACCESS_KEY`: Your AWS access key
- `AWS_SECRET_KEY`: Your AWS secret key
- `S3_OUTPUT_LOCATION`: Your S3 bucket path

**Submit Job:**

```bash
# Build JSON payload (update values)
APP_ARGS='{"sqlQuery":"SELECT DISTINCT userid FROM a0a6bd02a3194a7a99699bc968575d83.users_salary_table limit 5","cohortName":"test-cohort-001","action":"append","expireAt":"2025-12-31 23:59:59","sparkMaster":"spark://spark-master:7077","sourceJson":[{"type":"ATHENA","config":{"region":"us-east-1","workgroup":"primary","outputLocation":"s3a://hascend/athena-results/","accessKey":"<Key>","secretKey":"<Key>","sessionToken":"<key>"}}],"destinationJson":[]}'

# Submit job
docker exec -i spark-master spark-submit \
  --master spark://spark-master:7077 \
  --class com.dream11.flocker.engine.EngineStart \
  --deploy-mode cluster \
  --name "FlockerHistoricEngine" \
  /opt/spark/jars/flockr-historic-engine-0.1.0-SNAPSHOT.jar \
  "${APP_ARGS}"
```

### Step 5: Verify Results

**Check Spark Application:**
- Open http://localhost:8080
- Find application "FlockerHistoricEngine"
- Verify status is "FINISHED"
- Check logs for any errors

**Check API Calls:**
```bash
# View received API data
curl http://localhost:8082/received-data

# Expected format:
# [{"user_id":12345,"cohort_key":"test-cohort-001","action":"append","expire_at":"2025-12-31 23:59:59"}]
```

**Check Application Logs:**
```bash
docker-compose logs spark-master | grep -i "FlockerHistoricEngine"
```

### Step 6: Stop Services

```bash
docker-compose down
```

## Expected Positive Test Results

✅ **Spark Application:**
- Status: FINISHED
- No errors in logs
- Application appears in Spark UI

✅ **API Calls:**
- Mock API receives data at http://localhost:8082/received-data
- Data format matches: `[{user_id, cohort_key, action, expire_at}]`
- Headers include `x-project-key: tenant1_100`

✅ **S3 Output:**
- Parquet files created in configured S3 location
- Files contain columns: user_id, cohort_key, action, expire_at

## Troubleshooting

**Services not starting:**
```bash
docker-compose logs
docker-compose restart
```

**Job fails:**
- Check AWS credentials are valid
- Verify SQL query syntax
- Check Spark UI logs for errors

**API not receiving calls:**
- Verify API URL in config: `http://mock-api:8080/flockr/users/map-cohorts`
- Check mock-api logs: `docker-compose logs mock-api`

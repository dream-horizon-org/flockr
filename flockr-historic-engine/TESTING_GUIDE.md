# Complete Testing Guide for Flocker Historic Engine

This guide provides step-by-step instructions to set up and test the Flocker Historic Engine with Spark standalone cluster.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Spark Standalone Cluster Setup](#spark-standalone-cluster-setup)
3. [Build the Project](#build-the-project)
4. [Prepare Test Configuration](#prepare-test-configuration)
5. [Submit and Test the Job](#submit-and-test-the-job)
6. [Verify Results](#verify-results)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software
- **Java 8 or higher** (Java 21 recommended)
- **Apache Spark** (4.0.1 or compatible version)
- **Maven** 3.6+
- **AWS Account** with Athena and S3 access
- **curl** (for API testing)

### Required AWS Resources
- AWS Athena database and table
- S3 bucket for Athena query results
- AWS IAM credentials with permissions:
  - Athena: `athena:StartQueryExecution`, `athena:GetQueryExecution`, `athena:GetQueryResults`
  - S3: Read/write access to your S3 bucket

---

## Spark Standalone Cluster Setup

### Step 1: Download and Install Spark

```bash
# Download Spark (if not already installed)
cd ~
wget https://archive.apache.org/dist/spark/spark-4.0.1/spark-4.0.1-bin-hadoop3.tgz
tar -xzf spark-4.0.1-bin-hadoop3.tgz
mv spark-4.0.1-bin-hadoop3 spark
```

### Step 2: Configure Spark

```bash
# Navigate to Spark directory
cd ~/spark

# Create configuration directory
mkdir -p conf

# Set environment variables (add to ~/.zshrc or ~/.bashrc)
export SPARK_HOME=~/spark
export PATH=$PATH:$SPARK_HOME/bin:$SPARK_HOME/sbin
```

### Step 3: Start Spark Master

```bash
# Start Spark Master
$SPARK_HOME/sbin/start-master.sh

# Check if master is running
lsof -i :7077

# Access Spark Master UI
# Open browser: http://localhost:8080
```

**Expected Output:**
```
starting org.apache.spark.deploy.master.Master, logging to ...
```

### Step 4: Start Spark Worker

```bash
# Start Spark Worker (connects to master)
$SPARK_HOME/sbin/start-worker.sh spark://localhost:7077

# Check if worker is running
jps | grep Worker
```

**Expected Output:**
```
starting org.apache.spark.deploy.worker.Worker, logging to ...
```

### Step 5: Verify Cluster Status

1. **Check Master UI**: http://localhost:8080
   - You should see 1 worker registered
   - Worker should show available cores and memory

2. **Check via command line**:
```bash
# Check master process
ps aux | grep Master

# Check worker process
ps aux | grep Worker

# Check ports
lsof -i :7077  # Master
lsof -i :8080  # Master UI
lsof -i :8081  # Worker UI
```

### Step 6: Stop Spark Cluster (when done)

```bash
# Stop worker
$SPARK_HOME/sbin/stop-worker.sh

# Stop master
$SPARK_HOME/sbin/stop-master.sh
```

---

## Build the Project

### Step 1: Navigate to Project Directory

```bash
cd <ProjectDirectory>/flockr-historic-engine
```

### Step 2: Build the JAR

```bash
# Clean and build (skip tests for faster build)
mvn clean package -DskipTests

# Verify JAR was created
ls -lh target/flockr-historic-engine-0.1.0-SNAPSHOT.jar
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXX s
```

The JAR should be approximately 600-700 MB (includes all dependencies).

---

## Prepare Test Configuration

### Step 1: Create Test Script

Create a file `test-job.sh`:

```bash
#!/bin/bash

# =============================================================================
# Test Script for Flocker Historic Engine
# =============================================================================

# Get absolute paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_PATH="${SCRIPT_DIR}/target/flockr-historic-engine-0.1.0-SNAPSHOT.jar"
SPARK_HOME="${SPARK_HOME:-$HOME/spark}"

# Configuration - UPDATE THESE VALUES
SPARK_MASTER="spark://localhost:7077"

# Test data - UPDATE THESE VALUES WITH YOUR ACTUAL DATA
SQL_QUERY="SELECT DISTINCT user_id FROM your_database.your_table"
COHORT_NAME="test-cohort-001"
ACTION="append"
EXPIRE_AT="2025-12-31 23:59:59"
AWS_REGION="us-east-1"
AWS_WORKGROUP="primary"
S3_OUTPUT_LOCATION="s3a://your-bucket/athena-results/"
AWS_ACCESS_KEY="YOUR_AWS_ACCESS_KEY"
AWS_SECRET_KEY="YOUR_AWS_SECRET_KEY"
AWS_SESSION_TOKEN="YOUR_SESSION_TOKEN"  # Optional, only for temporary credentials

# Check if JAR exists
if [ ! -f "$JAR_PATH" ]; then
    echo "ERROR: JAR file not found at: $JAR_PATH"
    echo "Please build the project first: mvn clean package -DskipTests"
    exit 1
fi

# Check if Spark is running
if ! lsof -i :7077 > /dev/null 2>&1; then
    echo "ERROR: Spark Master is not running on port 7077"
    echo "Please start Spark Master: $SPARK_HOME/sbin/start-master.sh"
    exit 1
fi

# Find spark-submit
SPARK_SUBMIT=""
if command -v spark-submit &> /dev/null; then
    SPARK_SUBMIT="spark-submit"
elif [ -f "$SPARK_HOME/bin/spark-submit" ]; then
    SPARK_SUBMIT="$SPARK_HOME/bin/spark-submit"
else
    echo "ERROR: spark-submit not found"
    echo "Please set SPARK_HOME or add Spark bin to PATH"
    exit 1
fi

echo "============================================================================="
echo "Submitting Flocker Historic Engine Job"
echo "============================================================================="
echo "JAR Path: ${JAR_PATH}"
echo "Spark Master: ${SPARK_MASTER}"
echo "Cohort Name: ${COHORT_NAME}"
echo "Action: ${ACTION}"
echo "============================================================================="
echo ""

# Build JSON payload
APP_ARGS=$(cat <<EOF
{
  "sqlQuery": "${SQL_QUERY}",
  "cohortName": "${COHORT_NAME}",
  "action": "${ACTION}",
  "expireAt": "${EXPIRE_AT}",
  "sparkMaster": "${SPARK_MASTER}",
  "sourceJson": [
    {
      "type": "ATHENA",
      "config": {
        "region": "${AWS_REGION}",
        "workgroup": "${AWS_WORKGROUP}",
        "outputLocation": "${S3_OUTPUT_LOCATION}",
        "accessKey": "${AWS_ACCESS_KEY}",
        "secretKey": "${AWS_SECRET_KEY}"
      }
    }
  ],
  "destinationJson": []
}
EOF
)

# Submit the job
"${SPARK_SUBMIT}" \
  --master "${SPARK_MASTER}" \
  --class com.dream11.flocker.engine.EngineStart \
  --deploy-mode cluster \
  --name "FlockerHistoricEngine" \
  --conf "spark.executor.memory=2g" \
  --conf "spark.executor.cores=2" \
  --conf "spark.driver.memory=1g" \
  "${JAR_PATH}" \
  "${APP_ARGS}"

echo ""
echo "============================================================================="
echo "Job submitted. Check Spark UI at http://localhost:8080 for status."
echo "============================================================================="
```

### Step 2: Make Script Executable

```bash
chmod +x test-job.sh
```

### Step 3: Update Configuration Values

Edit `test-job.sh` and update:
- `SQL_QUERY` - Your actual Athena SQL query
- `COHORT_NAME` - Your cohort name
- `ACTION` - "append" or "remove"
- `EXPIRE_AT` - Expiration date/time
- `AWS_REGION` - Your AWS region
- `S3_OUTPUT_LOCATION` - Your S3 bucket path
- `AWS_ACCESS_KEY` - Your AWS access key
- `AWS_SECRET_KEY` - Your AWS secret key
- `AWS_SESSION_TOKEN` - Only if using temporary credentials

---

## Submit and Test the Job

### Step 1: Ensure Spark Cluster is Running

```bash
# Check if master is running
lsof -i :7077

# If not running, start it
$SPARK_HOME/sbin/start-master.sh
$SPARK_HOME/sbin/start-worker.sh spark://localhost:7077
```

### Step 2: Run the Test Script

```bash
./test-job.sh
```

### Step 3: Monitor Job Progress

1. **Spark Master UI**: http://localhost:8080
   - Click on your application
   - View logs and status

2. **Check Logs**:
```bash
# View Spark worker logs
tail -f $SPARK_HOME/work/app-*/driver-*/stdout

# Or check Spark UI logs directly
```

### Step 4: Expected Output

You should see logs indicating:
- ✅ Athena query execution
- ✅ Data reading from S3
- ✅ Data transformation
- ✅ Writing to S3 sink
- ✅ API calls to your endpoint

---

## Verify Results

### Step 1: Check S3 Output

```bash
# List files in S3 output location
aws s3 ls s3://your-bucket/athena-results/ --recursive

# Download and inspect Parquet file
aws s3 cp s3://your-bucket/athena-results/part-00000-*.parquet ./test-output.parquet

# Read Parquet file (if you have parquet-tools)
parquet-tools cat test-output.parquet | head -10
```

**Expected Parquet Structure:**
```
user_id | cohort_key      | action | expire_at
--------|-----------------|--------|------------------
12345   | test-cohort-001 | append | 2025-12-31 23:59:59
67890   | test-cohort-001 | append | 2025-12-31 23:59:59
```

### Step 2: Verify API Calls

The application should call your API endpoint:
- **URL**: `http://localhost:8080/flockr/users/map-cohorts`
- **Method**: POST
- **Headers**: 
  - `Content-Type: application/json`
  - `x-project-key: tenant1_100`
- **Body**: Array of objects

**Test API Endpoint** (if you have a test server):

```bash
# Start a test HTTP server to capture requests
python3 -m http.server 8080

# Or use a tool like ngrok to expose localhost
# Or check your actual API server logs
```

**Expected API Payload:**
```json
[
  {
    "user_id": 12345,
    "cohort_key": "test-cohort-001",
    "action": "append",
    "expire_at": "2025-12-31 23:59:59"
  },
  {
    "user_id": 67890,
    "cohort_key": "test-cohort-001",
    "action": "append",
    "expire_at": "2025-12-31 23:59:59"
  }
]
```

### Step 3: Check Spark Application Logs

```bash
# View application logs from Spark UI
# Navigate to: http://localhost:8080 -> Your Application -> Logs

# Or check worker logs
tail -f $SPARK_HOME/work/app-*/driver-*/stdout
tail -f $SPARK_HOME/work/app-*/driver-*/stderr
```

---

## Troubleshooting

### Issue: Spark Master Not Starting

**Symptoms:**
```
ERROR: Spark Master is not running on port 7077
```

**Solutions:**
1. Check if port 7077 is already in use:
   ```bash
   lsof -i :7077
   ```
2. Kill existing process if needed:
   ```bash
   kill -9 <PID>
   ```
3. Check Spark logs:
   ```bash
   cat $SPARK_HOME/logs/spark-*-master-*.out
   ```

### Issue: Worker Not Connecting to Master

**Symptoms:**
- Worker starts but doesn't appear in Master UI

**Solutions:**
1. Verify master URL is correct: `spark://localhost:7077`
2. Check network connectivity
3. Verify firewall settings
4. Check worker logs:
   ```bash
   cat $SPARK_HOME/logs/spark-*-worker-*.out
   ```

### Issue: JAR File Not Found

**Symptoms:**
```
ERROR: JAR file not found
```

**Solutions:**
1. Build the project:
   ```bash
   mvn clean package -DskipTests
   ```
2. Verify JAR exists:
   ```bash
   ls -lh target/flockr-historic-engine-0.1.0-SNAPSHOT.jar
   ```

### Issue: Invalid AWS Credentials

**Symptoms:**
```
The security token included in the request is invalid
UnrecognizedClientException
```

**Solutions:**
1. Verify credentials are valid:
   ```bash
   aws sts get-caller-identity \
     --aws-access-key-id YOUR_KEY \
     --aws-secret-access-key YOUR_SECRET
   ```
2. Check credentials in test script
3. Verify IAM permissions
4. If using temporary credentials, include `sessionToken`

### Issue: Cannot Read CSV Files

**Symptoms:**
```
CANNOT_READ_FILE_FOOTER
Could not read footer
```

**Solutions:**
- This should be fixed in the latest code (tries CSV first)
- Verify Athena output location is correct
- Check S3 bucket permissions
- Ensure files exist at the specified path

### Issue: Job Hangs or Times Out

**Symptoms:**
- Job submitted but no progress
- Application shows as "RUNNING" but no logs

**Solutions:**
1. Check worker resources:
   - Ensure worker has enough memory
   - Check available cores
2. Increase timeout:
   ```bash
   --conf "spark.network.timeout=800s"
   ```
3. Check for network issues
4. Verify S3 connectivity

### Issue: API Not Being Called

**Symptoms:**
- Job completes but no API calls

**Solutions:**
1. Check API endpoint configuration in `default.conf`
2. Verify API endpoint is accessible from Spark workers
3. Check application logs for API errors
4. Verify `projectKey` is configured

---

## Quick Reference Commands

### Start Spark Cluster
```bash
$SPARK_HOME/sbin/start-master.sh
$SPARK_HOME/sbin/start-worker.sh spark://localhost:7077
```

### Stop Spark Cluster
```bash
$SPARK_HOME/sbin/stop-worker.sh
$SPARK_HOME/sbin/stop-master.sh
```

### Check Cluster Status
```bash
# Master UI
open http://localhost:8080

# Check processes
jps | grep -E "Master|Worker"

# Check ports
lsof -i :7077  # Master
lsof -i :8080  # Master UI
```

### Build and Test
```bash
# Build
mvn clean package -DskipTests

# Test
./test-job.sh

# View logs
tail -f $SPARK_HOME/work/app-*/driver-*/stdout
```

---

## Configuration Files

### API Configuration
Location: `src/main/resources/config/sink/api/default.conf`
```conf
url = "http://localhost:8080/flockr/users/map-cohorts"
rateLimitPerSecond = 100
batchSize = 100
timeoutSeconds = 30
contentType = "application/json"
projectKey = "tenant1_100"
```

### Spark Configuration
Location: `src/main/resources/config/spark/default.conf`
- Customize Spark settings here if needed

---

## Next Steps

1. ✅ Set up Spark standalone cluster
2. ✅ Build the project
3. ✅ Configure test script with your AWS credentials
4. ✅ Submit test job
5. ✅ Verify results in S3 and API calls
6. ✅ Monitor Spark UI for job status

For production deployment, consider:
- Using Spark on Kubernetes or EMR
- Setting up proper monitoring and alerting
- Configuring retry logic for API calls
- Implementing proper error handling and logging

---

## Support

If you encounter issues:
1. Check the troubleshooting section above
2. Review Spark and application logs
3. Verify all configuration values are correct
4. Ensure AWS credentials and permissions are valid


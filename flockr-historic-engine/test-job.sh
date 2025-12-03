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
SQL_QUERY="SELECT DISTINCT user_id FROM your_database.your_table WHERE date = CURRENT_DATE"
COHORT_NAME="test-cohort-001"
ACTION="append"
EXPIRE_AT="2025-12-31 23:59:59"
AWS_REGION="us-east-1"
AWS_WORKGROUP="primary"
S3_OUTPUT_LOCATION="s3a://your-bucket/athena-results/"
AWS_ACCESS_KEY="YOUR_AWS_ACCESS_KEY"
AWS_SECRET_KEY="YOUR_AWS_SECRET_KEY"
# AWS_SESSION_TOKEN="YOUR_SESSION_TOKEN"  # Optional, only for temporary credentials

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
if [ -n "$AWS_SESSION_TOKEN" ]; then
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
        "secretKey": "${AWS_SECRET_KEY}",
        "sessionToken": "${AWS_SESSION_TOKEN}"
      }
    }
  ],
  "destinationJson": []
}
EOF
)
else
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
fi

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


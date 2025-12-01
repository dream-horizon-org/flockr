package com.dream11.flocker.engine.modules.source.impl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.AmazonAthenaClientBuilder;
import com.amazonaws.services.athena.model.*;
import com.amazonaws.services.athena.model.QueryExecutionState;
import com.dream11.flocker.engine.config.AthenaConfig;
import com.dream11.flocker.engine.modules.source.Source;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.concurrent.TimeUnit;

@Slf4j
public class AthenaSourceImpl implements Source<Dataset<Row>> {
    
    private final AthenaConfig athenaConfig;
    private final SparkSession sparkSession;

    public AthenaSourceImpl(AthenaConfig athenaConfig, SparkSession sparkSession) {
        if (athenaConfig == null || sparkSession == null) {
            throw new IllegalArgumentException("AthenaConfig or SparkSession cannot be null");
        }
        this.athenaConfig = athenaConfig;
        this.sparkSession = sparkSession;
        log.info("Athena source configured: database={}, region={}", 
                athenaConfig.getDatabase(), athenaConfig.getRegion());
    }

    @Override
    public Dataset<Row> read() throws Exception {
        log.info("Reading data from Athena: database={}, region={}", 
                athenaConfig.getDatabase(), athenaConfig.getRegion());
        
        // Execute Athena query first if SQL query is provided
        String queryExecutionId = null;
        String actualResultPath = null;
        if (athenaConfig.getSqlQuery() != null && !athenaConfig.getSqlQuery().trim().isEmpty()) {
            log.info("Executing Athena query: {}", athenaConfig.getSqlQuery());
            QueryExecutionResult queryResult = executeAthenaQuery(athenaConfig.getSqlQuery());
            queryExecutionId = queryResult.getQueryExecutionId();
            actualResultPath = queryResult.getResultPath();
            log.info("Athena query executed successfully. Query execution ID: {}, Result path: {}", 
                    queryExecutionId, actualResultPath);
        }
        
        if (athenaConfig.getOutputLocation() != null && !athenaConfig.getOutputLocation().isEmpty()) {
            // Read from S3 output location (Athena query results are written to S3)
            String s3Path;
            
            // If we have the actual result path from query execution, use it directly
            if (actualResultPath != null && !actualResultPath.isEmpty()) {
                // Convert s3:// to s3a:// for Spark
                s3Path = actualResultPath.replace("s3://", "s3a://");
                log.info("Reading Athena query results from S3: {}", s3Path);
            } else if (queryExecutionId != null) {
                // Fallback: construct path from output location and query execution ID
                String basePath = athenaConfig.getOutputLocation();
                basePath = basePath.endsWith("/") ? basePath : basePath + "/";
                s3Path = basePath + queryExecutionId + "/";
                // Convert s3:// to s3a:// for Spark
                s3Path = s3Path.replace("s3://", "s3a://");
                log.info("Reading Athena query results from S3: {}", s3Path);
            } else {
                s3Path = athenaConfig.getOutputLocation();
                // Convert s3:// to s3a:// for Spark
                s3Path = s3Path.replace("s3://", "s3a://");
                log.info("Reading Athena query results from S3: {}", s3Path);
            }
            
            // Configure S3 access if credentials are provided
            if (athenaConfig.getAccessKey() != null && athenaConfig.getSecretKey() != null) {
                sparkSession.sparkContext().hadoopConfiguration().set("fs.s3a.access.key", athenaConfig.getAccessKey());
                sparkSession.sparkContext().hadoopConfiguration().set("fs.s3a.secret.key", athenaConfig.getSecretKey());
                
                // Set session token if provided (for temporary credentials)
                if (athenaConfig.getSessionToken() != null && !athenaConfig.getSessionToken().isEmpty()) {
                    sparkSession.sparkContext().hadoopConfiguration().set("fs.s3a.session.token", athenaConfig.getSessionToken());
                    // Use TemporaryAWSCredentialsProvider for temporary credentials with session token
                    sparkSession.sparkContext().hadoopConfiguration().set("fs.s3a.aws.credentials.provider", 
                            "org.apache.hadoop.fs.s3a.TemporaryAWSCredentialsProvider");
                    log.debug("Using TemporaryAWSCredentialsProvider for session token");
                } else {
                    sparkSession.sparkContext().hadoopConfiguration().set("fs.s3a.aws.credentials.provider", 
                            "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider");
                }
            }
            
            // Try to read as Parquet (default Athena output format)
            try {
                Dataset<Row> data = sparkSession.read().parquet(s3Path);
                log.info("Successfully read Athena results from S3 path: {}", s3Path);
                return data;
            } catch (Exception e) {
                log.warn("Failed to read as Parquet, trying CSV format", e);
                // Fallback to CSV
                Dataset<Row> data = sparkSession.read()
                        .option("header", "true")
                        .option("inferSchema", "true")
                        .csv(s3Path);
                log.info("Successfully read Athena results as CSV from S3 path: {}", s3Path);
                return data;
            }
        } else {
            log.error("Athena output location is required but not specified");
            throw new IllegalStateException("Athena outputLocation must be specified in configuration");
        }
    }

    private static class QueryExecutionResult {
        private final String queryExecutionId;
        private final String resultPath;
        
        public QueryExecutionResult(String queryExecutionId, String resultPath) {
            this.queryExecutionId = queryExecutionId;
            this.resultPath = resultPath;
        }
        
        public String getQueryExecutionId() {
            return queryExecutionId;
        }
        
        public String getResultPath() {
            return resultPath;
        }
    }
    
    private QueryExecutionResult executeAthenaQuery(String sqlQuery) throws Exception {
        // Create AWS credentials
        AWSCredentials credentials;
        if (athenaConfig.getSessionToken() != null && !athenaConfig.getSessionToken().isEmpty()) {
            credentials = new BasicSessionCredentials(
                    athenaConfig.getAccessKey(),
                    athenaConfig.getSecretKey(),
                    athenaConfig.getSessionToken()
            );
        } else {
            credentials = new BasicAWSCredentials(
                    athenaConfig.getAccessKey(),
                    athenaConfig.getSecretKey()
            );
        }

        // Create Athena client
        AmazonAthena athenaClient = AmazonAthenaClientBuilder.standard()
                .withCredentials(new AWSCredentialsProvider() {
                    @Override
                    public AWSCredentials getCredentials() {
                        return credentials;
                    }

                    @Override
                    public void refresh() {
                    }
                })
                .withRegion(athenaConfig.getRegion())
                .build();

        // Create query execution request
        QueryExecutionContext queryExecutionContext = new QueryExecutionContext()
                .withDatabase(athenaConfig.getDatabase());

        // Convert s3a:// to s3:// for Athena API (Athena doesn't support s3a://)
        String outputLocation = athenaConfig.getOutputLocation();
        if (outputLocation != null && outputLocation.startsWith("s3a://")) {
            outputLocation = outputLocation.replace("s3a://", "s3://");
        }
        
        ResultConfiguration resultConfiguration = new ResultConfiguration()
                .withOutputLocation(outputLocation);

        StartQueryExecutionRequest startQueryExecutionRequest = new StartQueryExecutionRequest()
                .withQueryString(sqlQuery)
                .withQueryExecutionContext(queryExecutionContext)
                .withResultConfiguration(resultConfiguration);

        if (athenaConfig.getWorkgroup() != null && !athenaConfig.getWorkgroup().isEmpty()) {
            startQueryExecutionRequest.setWorkGroup(athenaConfig.getWorkgroup());
        }

        // Start query execution
        StartQueryExecutionResult startQueryExecutionResult = athenaClient.startQueryExecution(startQueryExecutionRequest);
        String queryExecutionId = startQueryExecutionResult.getQueryExecutionId();
        log.info("Started Athena query execution. QueryExecutionId: {}", queryExecutionId);

        // Wait for query to complete
        GetQueryExecutionRequest getQueryExecutionRequest = new GetQueryExecutionRequest()
                .withQueryExecutionId(queryExecutionId);

        GetQueryExecutionResult getQueryExecutionResult;
        QueryExecutionState queryState;
        int maxWaitTime = 300; // 5 minutes max wait
        int waitTime = 0;
        int pollInterval = 2; // Poll every 2 seconds

        do {
            Thread.sleep(TimeUnit.SECONDS.toMillis(pollInterval));
            waitTime += pollInterval;
            getQueryExecutionResult = athenaClient.getQueryExecution(getQueryExecutionRequest);
            queryState = QueryExecutionState.fromValue(getQueryExecutionResult.getQueryExecution().getStatus().getState());
            log.debug("Query state: {}, waited {} seconds", queryState, waitTime);

            if (waitTime >= maxWaitTime) {
                throw new RuntimeException("Athena query execution timed out after " + maxWaitTime + " seconds");
            }
        } while (queryState == QueryExecutionState.RUNNING || queryState == QueryExecutionState.QUEUED);

        if (queryState == QueryExecutionState.FAILED) {
            String reason = getQueryExecutionResult.getQueryExecution().getStatus().getStateChangeReason();
            throw new RuntimeException("Athena query execution failed: " + reason);
        } else if (queryState == QueryExecutionState.CANCELLED) {
            throw new RuntimeException("Athena query execution was cancelled");
        }

        log.info("Athena query completed successfully. State: {}", queryState);
        
        // Get the actual output location from the query execution result
        String actualOutputLocation = getQueryExecutionResult.getQueryExecution().getResultConfiguration().getOutputLocation();
        String resultPath = null;
        
        if (actualOutputLocation != null && !actualOutputLocation.isEmpty()) {
            // Athena returns the base output location, results are in: outputLocation/queryExecutionId/
            // But the actualOutputLocation might already include the query execution ID
            // Check if it ends with .csv or contains the query execution ID
            if (actualOutputLocation.contains(queryExecutionId)) {
                // Already contains the query execution ID, use as directory
                resultPath = actualOutputLocation.endsWith("/") ? actualOutputLocation : actualOutputLocation + "/";
            } else {
                // Construct the path: outputLocation/queryExecutionId/
                String basePath = actualOutputLocation.endsWith("/") ? actualOutputLocation : actualOutputLocation + "/";
                resultPath = basePath + queryExecutionId + "/";
            }
            log.info("Athena query results location: {}", resultPath);
        }
        
        // Small delay to ensure files are written
        Thread.sleep(1000);
        
        return new QueryExecutionResult(queryExecutionId, resultPath);
    }
}


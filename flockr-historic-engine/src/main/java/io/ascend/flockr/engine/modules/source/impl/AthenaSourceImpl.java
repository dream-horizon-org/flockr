package io.ascend.flockr.engine.modules.source.impl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.AmazonAthenaClientBuilder;
import com.amazonaws.services.athena.model.*;
import com.amazonaws.services.athena.model.QueryExecutionState;
import io.ascend.flockr.engine.config.AthenaConfig;
import io.ascend.flockr.engine.dto.UserIdRow;
import io.ascend.flockr.engine.exception.JobException;
import io.ascend.flockr.engine.modules.source.Source;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

/**
 * Implementation of Source interface for AWS Athena data source.
 *
 * <p>This class provides functionality to:
 *
 * <ul>
 *   <li>Execute SQL queries on AWS Athena
 *   <li>Wait for query completion with polling
 *   <li>Read query results from S3 (CSV or Parquet format)
 *   <li>Extract user IDs and convert to type-safe Dataset&lt;UserIdRow&gt;
 *   <li>Handle AWS credentials (permanent and temporary with session tokens)
 *   <li>Extract database name from SQL query if not provided in config
 * </ul>
 *
 * @see Source
 * @see AthenaConfig
 * @see UserIdRow
 * @author Shivam-Raghuwanshi
 */
@Slf4j
public class AthenaSourceImpl implements Source {

  /** Athena configuration containing credentials, region, workgroup, etc. */
  private final AthenaConfig athenaConfig;

  /** Spark session for reading query results from S3. */
  private final SparkSession sparkSession;

  /**
   * Creates a new AthenaSourceImpl instance.
   *
   * @param athenaConfig Athena configuration (must not be null).
   * @param sparkSession Spark session for reading results (must not be null).
   * @throws IllegalArgumentException If athenaConfig or sparkSession is null.
   */
  public AthenaSourceImpl(AthenaConfig athenaConfig, SparkSession sparkSession) {
    if (athenaConfig == null || sparkSession == null) {
      throw new IllegalArgumentException("AthenaConfig or SparkSession cannot be null");
    }
    this.athenaConfig = athenaConfig;
    this.sparkSession = sparkSession;
    log.info(
        "Athena source configured: database={}, region={}",
        athenaConfig.getDatabase() != null
            ? athenaConfig.getDatabase()
            : "will be extracted from query",
        athenaConfig.getRegion());
  }

  @Override
  public Dataset<UserIdRow> read() throws Exception {
    log.info(
        "Reading data from Athena: database={}, region={}",
        athenaConfig.getDatabase() != null
            ? athenaConfig.getDatabase()
            : "will be extracted from query",
        athenaConfig.getRegion());

    // Execute Athena query first if SQL query is provided
    String queryExecutionId = null;
    String actualResultPath = null;
    if (athenaConfig.getSqlQuery() != null && !athenaConfig.getSqlQuery().trim().isEmpty()) {
      log.info("Executing Athena query: {}", athenaConfig.getSqlQuery());
      QueryExecutionResult queryResult = executeAthenaQuery(athenaConfig.getSqlQuery());
      queryExecutionId = queryResult.getQueryExecutionId();
      actualResultPath = queryResult.getResultPath();
      log.info(
          "Athena query executed successfully. Query execution ID: {}, Result path: {}",
          queryExecutionId,
          actualResultPath);
    }

    if (athenaConfig.getOutputLocation() != null && !athenaConfig.getOutputLocation().isEmpty()) {
      String s3Path;

      if (actualResultPath != null && !actualResultPath.isEmpty()) {
        s3Path = actualResultPath.replace("s3://", "s3a://");
      } else if (queryExecutionId != null) {
        String basePath = athenaConfig.getOutputLocation();
        basePath = basePath.endsWith("/") ? basePath : basePath + "/";
        s3Path = basePath + queryExecutionId + "/";
        s3Path = s3Path.replace("s3://", "s3a://");
      } else {
        s3Path = athenaConfig.getOutputLocation();
        s3Path = s3Path.replace("s3://", "s3a://");
      }
      log.info("Reading Athena query results from S3: {}", s3Path);

      // Configure S3 access if credentials are provided
      if (athenaConfig.getAccessKey() != null && athenaConfig.getSecretKey() != null) {
        sparkSession
            .sparkContext()
            .hadoopConfiguration()
            .set("fs.s3a.access.key", athenaConfig.getAccessKey());
        sparkSession
            .sparkContext()
            .hadoopConfiguration()
            .set("fs.s3a.secret.key", athenaConfig.getSecretKey());

        // Set session token if provided (for temporary credentials)
        if (athenaConfig.getSessionToken() != null && !athenaConfig.getSessionToken().isEmpty()) {
          sparkSession
              .sparkContext()
              .hadoopConfiguration()
              .set("fs.s3a.session.token", athenaConfig.getSessionToken());
          // Use TemporaryAWSCredentialsProvider for temporary credentials with session token
          sparkSession
              .sparkContext()
              .hadoopConfiguration()
              .set(
                  "fs.s3a.aws.credentials.provider",
                  "org.apache.hadoop.fs.s3a.TemporaryAWSCredentialsProvider");
          log.debug("Using TemporaryAWSCredentialsProvider for session token");
        } else {
          sparkSession
              .sparkContext()
              .hadoopConfiguration()
              .set(
                  "fs.s3a.aws.credentials.provider",
                  "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider");
        }
      }

      // Try to read as CSV first (Athena default output is CSV), then fallback to Parquet
      Dataset<Row> data = null;
      try {
        // Try CSV first (Athena's default output format)
        log.debug("Attempting to read as CSV from S3 path: {}", s3Path);
        data =
            sparkSession.read().option("header", "true").option("inferSchema", "true").csv(s3Path);
        log.info("Successfully read Athena results as CSV from S3 path: {}", s3Path);
      } catch (Exception csvException) {
        log.debug("Failed to read as CSV, trying Parquet format", csvException);
        // Fallback to Parquet
        try {
          data = sparkSession.read().parquet(s3Path);
          log.info("Successfully read Athena results as Parquet from S3 path: {}", s3Path);
        } catch (Exception parquetException) {
          log.error("Failed to read as both CSV and Parquet from path: {}", s3Path);
          log.error("CSV error: {}", csvException.getMessage());
          log.error("Parquet error: {}", parquetException.getMessage());
          throw new RuntimeException(
              "Failed to read Athena results from S3. Tried both CSV and Parquet formats.",
              parquetException);
        }
      }

      // Validate user_id column exists
      if (!java.util.Arrays.asList(data.columns()).contains("user_id")) {
        throw new JobException(
            "Athena query must return a 'user_id' column. Found columns: "
                + String.join(", ", data.columns()));
      }

      // Convert to type-safe UserIdRow
      log.debug("Converting Athena results to type-safe UserIdRow dataset");
      Dataset<UserIdRow> userIds = data.select("user_id").as(Encoders.bean(UserIdRow.class));

      long count = userIds.count();
      log.info("Successfully extracted {} user IDs from Athena query results", count);

      return userIds;
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
    // Validate credentials before creating client
    if (athenaConfig.getAccessKey() == null || athenaConfig.getAccessKey().trim().isEmpty()) {
      throw new IllegalArgumentException("AWS Access Key is required but not provided");
    }
    if (athenaConfig.getSecretKey() == null || athenaConfig.getSecretKey().trim().isEmpty()) {
      throw new IllegalArgumentException("AWS Secret Key is required but not provided");
    }

    log.debug(
        "Creating AWS credentials for Athena - AccessKey: {}..., Region: {}",
        athenaConfig.getAccessKey().substring(0, Math.min(8, athenaConfig.getAccessKey().length())),
        athenaConfig.getRegion());

    // Create AWS credentials
    AWSCredentials credentials;
    if (athenaConfig.getSessionToken() != null && !athenaConfig.getSessionToken().isEmpty()) {
      credentials =
          new BasicSessionCredentials(
              athenaConfig.getAccessKey(),
              athenaConfig.getSecretKey(),
              athenaConfig.getSessionToken());
      log.debug("Using temporary credentials with session token");
    } else {
      credentials =
          new BasicAWSCredentials(athenaConfig.getAccessKey(), athenaConfig.getSecretKey());
      log.debug("Using permanent credentials");
    }

    // Create Athena client
    AmazonAthena athenaClient =
        AmazonAthenaClientBuilder.standard()
            .withCredentials(
                new AWSCredentialsProvider() {
                  @Override
                  public AWSCredentials getCredentials() {
                    return credentials;
                  }

                  @Override
                  public void refresh() {}
                })
            .withRegion(athenaConfig.getRegion())
            .build();

    log.debug("Athena client created successfully for region: {}", athenaConfig.getRegion());

    String database = athenaConfig.getDatabase();
    if ((database == null || database.trim().isEmpty()) && athenaConfig.getSqlQuery() != null) {
      String query = athenaConfig.getSqlQuery().trim();
      Pattern pattern =
          java.util.regex.Pattern.compile(
              "(?i)(?:FROM|JOIN|UPDATE|INTO)\\s+[\"']?([a-zA-Z0-9_-]+)[\"']?\\s*\\.\\s*[\"']?([a-zA-Z0-9_-]+)[\"']?",
              java.util.regex.Pattern.CASE_INSENSITIVE);
      java.util.regex.Matcher matcher = pattern.matcher(query);
      if (matcher.find()) {
        database = matcher.group(1);
        log.info("Extracted database '{}' from SQL query", database);
      } else {
        log.warn("Could not extract database from query. Query: {}", query);
      }
    }

    QueryExecutionContext queryExecutionContext = new QueryExecutionContext();
    if (database != null && !database.trim().isEmpty()) {
      queryExecutionContext.withDatabase(database);
    }

    // Convert s3a:// to s3:// for Athena API (Athena doesn't support s3a://)
    String outputLocation = athenaConfig.getOutputLocation();
    if (outputLocation != null && outputLocation.startsWith("s3a://")) {
      outputLocation = outputLocation.replace("s3a://", "s3://");
    }

    ResultConfiguration resultConfiguration =
        new ResultConfiguration().withOutputLocation(outputLocation);

    StartQueryExecutionRequest startQueryExecutionRequest =
        new StartQueryExecutionRequest()
            .withQueryString(sqlQuery)
            .withQueryExecutionContext(queryExecutionContext)
            .withResultConfiguration(resultConfiguration);

    if (athenaConfig.getWorkgroup() != null && !athenaConfig.getWorkgroup().isEmpty()) {
      startQueryExecutionRequest.setWorkGroup(athenaConfig.getWorkgroup());
    }

    // Start query execution
    StartQueryExecutionResult startQueryExecutionResult =
        athenaClient.startQueryExecution(startQueryExecutionRequest);
    String queryExecutionId = startQueryExecutionResult.getQueryExecutionId();
    log.info("Started Athena query execution. QueryExecutionId: {}", queryExecutionId);

    // Wait for query to complete
    GetQueryExecutionRequest getQueryExecutionRequest =
        new GetQueryExecutionRequest().withQueryExecutionId(queryExecutionId);

    GetQueryExecutionResult getQueryExecutionResult;
    QueryExecutionState queryState;
    int maxWaitTime = 300; // 5 minutes max wait
    int waitTime = 0;
    int pollInterval = 2; // Poll every 2 seconds

    do {
      Thread.sleep(TimeUnit.SECONDS.toMillis(pollInterval));
      waitTime += pollInterval;
      getQueryExecutionResult = athenaClient.getQueryExecution(getQueryExecutionRequest);
      queryState =
          QueryExecutionState.fromValue(
              getQueryExecutionResult.getQueryExecution().getStatus().getState());
      log.debug("Query state: {}, waited {} seconds", queryState, waitTime);

      if (waitTime >= maxWaitTime) {
        throw new RuntimeException(
            "Athena query execution timed out after " + maxWaitTime + " seconds");
      }
    } while (queryState == QueryExecutionState.RUNNING || queryState == QueryExecutionState.QUEUED);

    if (queryState == QueryExecutionState.FAILED) {
      String reason =
          getQueryExecutionResult.getQueryExecution().getStatus().getStateChangeReason();
      String errorMessage = "Athena query execution failed: " + reason;
      if (reason != null
          && (reason.contains("UnrecognizedClientException")
              || reason.contains("security token")
              || reason.contains("invalid"))) {
        errorMessage +=
            "Please verify your AWS credentials (accessKey, secretKey, and sessionToken if using temporary credentials) are valid and have proper permissions for Athena.";
      }
      throw new RuntimeException(errorMessage);
    } else if (queryState == QueryExecutionState.CANCELLED) {
      throw new RuntimeException("Athena query execution was cancelled");
    }

    log.info("Athena query completed successfully. State: {}", queryState);

    String actualOutputLocation =
        getQueryExecutionResult.getQueryExecution().getResultConfiguration().getOutputLocation();
    String resultPath = null;

    if (actualOutputLocation != null && !actualOutputLocation.isEmpty()) {
      String basePath =
          actualOutputLocation.endsWith("/") ? actualOutputLocation : actualOutputLocation + "/";
      if (actualOutputLocation.contains(queryExecutionId)) {
        resultPath = basePath;
      } else {
        resultPath = basePath + queryExecutionId + "/";
      }
      log.info("Athena query results location: {}", resultPath);
    }
    Thread.sleep(1000);

    return new QueryExecutionResult(queryExecutionId, resultPath);
  }
}

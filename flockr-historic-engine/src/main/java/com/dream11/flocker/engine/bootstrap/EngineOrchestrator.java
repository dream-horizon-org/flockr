package com.dream11.flocker.engine.bootstrap;

import com.dream11.flocker.engine.config.AthenaConfig;
import com.dream11.flocker.engine.config.ConnectorConfig;
import com.dream11.flocker.engine.config.EngineArguments;
import com.dream11.flocker.engine.config.SourceConfig;
import com.dream11.flocker.engine.constants.Constants;
import com.dream11.flocker.engine.injector.EngineModule;
import com.dream11.flocker.engine.service.s3.S3Process;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;

/**
 * Orchestrates the execution of the Flocker Historic Engine.
 *
 * <p>This class coordinates the overall engine execution flow:
 *
 * <ol>
 *   <li>Validates action type
 *   <li>Configures AWS credentials from source
 *   <li>Configures internal sinks
 *   <li>Creates Spark session
 *   <li>Sets up dependency injection
 *   <li>Executes the data processing pipeline
 * </ol>
 *
 * @author Shivam-Raghuwanshi
 */
@Slf4j
public class EngineOrchestrator {

  private final ArgumentParser argumentParser;
  private final ConfigConverter configConverter;
  private final AwsCredentialsConfigurator awsCredentialsConfigurator;
  private final InternalSinkConfigurator internalSinkConfigurator;
  private final SparkSessionFactory sparkSessionFactory;

  public EngineOrchestrator() {
    this.argumentParser = new ArgumentParser();
    this.configConverter = new ConfigConverter();
    this.awsCredentialsConfigurator = new AwsCredentialsConfigurator();
    this.internalSinkConfigurator = new InternalSinkConfigurator();
    this.sparkSessionFactory = new SparkSessionFactory();
  }

  /**
   * Executes the Flocker Historic Engine with the provided command-line arguments.
   *
   * <p>This method handles the complete lifecycle including SparkSession creation and cleanup.
   *
   * @param args Command-line arguments containing JSON configuration.
   * @throws Exception If any error occurs during execution.
   */
  public void execute(String[] args) throws Exception {
    SparkSession sparkSession = null;
    try {
      // Parse and validate arguments
      EngineArguments engineArgs = argumentParser.parse(args);

      String audienceName = engineArgs.getAudienceName();
      String action = validateAndNormalizeAction(engineArgs.getAction());
      Long expireAt = engineArgs.getExpireAt();
      SourceConfig source = engineArgs.getSource();
      List<ConnectorConfig> sinkConfigs = engineArgs.getDestinationJson();

      String sqlQuery = source.getQuery();
      String sourceType = source.getType().toUpperCase();

      log.info("SQL Query: {}", sqlQuery);
      log.info("Audience Name: {}", audienceName);
      log.info("Action: {}", action);
      log.info("expireAt: {} (epoch timestamp)", expireAt);

      // Convert source config to ConnectorConfig
      ConnectorConfig sourceConnectorConfig =
          configConverter.convertSourceConfigToConnectorConfig(sourceType, source.getConfig());
      log.info("Source Config: {}", sourceConnectorConfig);
      log.info("Destination Configs: {}", sinkConfigs);

      // Set SQL query in Athena config if applicable
      sourceType = sourceConnectorConfig.getType().toUpperCase();
      if ("ATHENA".equalsIgnoreCase(sourceType)) {
        AthenaConfig athenaConfig = (AthenaConfig) sourceConnectorConfig.getConfig();
        athenaConfig.setSqlQuery(sqlQuery);
        log.info(
            "SQL query set in Athena config for execution. Database: {}",
            athenaConfig.getDatabase() != null
                ? athenaConfig.getDatabase()
                : "will be extracted from query");
      }

      // Configure AWS credentials
      AthenaConfig athenaConfig =
          awsCredentialsConfigurator.configureCredentials(sourceConnectorConfig);

      // Configure internal sinks
      sinkConfigs =
          internalSinkConfigurator.configureInternalSinks(
              sinkConfigs, athenaConfig, sourceConnectorConfig);

      // Create Spark session
      sparkSession = sparkSessionFactory.createSparkSession();

      // Set up dependency injection
      Injector injector =
          Guice.createInjector(
              new EngineModule(
                  sparkSession,
                  sourceConnectorConfig,
                  sinkConfigs,
                  audienceName,
                  action,
                  expireAt));
      log.debug("Guice injector created successfully");

      // Execute processing
      S3Process s3Process = injector.getInstance(S3Process.class);
      log.info("Starting S3 processing with query and action: {}", action);
      s3Process.processWithQuery(sqlQuery, audienceName, action, expireAt);
      log.info("Flocker Engine completed successfully");
    } finally {
      // Ensure SparkSession is properly stopped
      if (sparkSession != null) {
        log.debug("Stopping SparkSession...");
        sparkSession.stop();
      }
    }
  }

  /**
   * Validates and normalizes the action string.
   *
   * @param action The action string to validate.
   * @return Normalized action string (lowercase, trimmed).
   * @throws IllegalArgumentException If action is invalid.
   */
  private String validateAndNormalizeAction(String action) {
    String eventAction = action.toLowerCase().trim();
    if (!eventAction.equals(Constants.ACTION_APPEND)
        && !eventAction.equals(Constants.ACTION_REMOVE)) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid action: %s. Valid values: %s, %s",
              action, Constants.ACTION_APPEND, Constants.ACTION_REMOVE));
    }
    return eventAction;
  }
}

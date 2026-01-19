package io.ascend.flockr.engine.bootstrap;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ascend.flockr.engine.config.AthenaConfig;
import io.ascend.flockr.engine.config.ConnectorConfig;
import io.ascend.flockr.engine.config.EngineArguments;
import io.ascend.flockr.engine.config.SourceConfig;
import io.ascend.flockr.engine.constants.Constants;
import io.ascend.flockr.engine.injector.EngineModule;
import io.ascend.flockr.engine.service.BaseProcess;
import java.util.List;
import lombok.Getter;
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
  @Getter private static volatile EngineOrchestrator instance = new EngineOrchestrator();
  private final ArgumentParser argumentParser;
  private final ConfigConverter configConverter;
  private final AwsCredentialsConfigurator awsCredentialsConfigurator;
  private final InternalSinkConfigurator internalSinkConfigurator;
  private final SparkSessionFactory sparkSessionFactory;

  private EngineOrchestrator() {
    this.argumentParser = new ArgumentParser();
    this.configConverter = new ConfigConverter();
    this.awsCredentialsConfigurator = new AwsCredentialsConfigurator();
    this.internalSinkConfigurator = new InternalSinkConfigurator();
    this.sparkSessionFactory = new SparkSessionFactory();
  }

  public void execute(String[] args) {
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
      log.debug("Guice injector created successfully");
      Injector injector =
          Guice.createInjector(
              new EngineModule(
                  sparkSession,
                  sourceConnectorConfig,
                  sinkConfigs,
                  audienceName,
                  action,
                  expireAt));

      log.info("Starting Base processing with query and action: {}", action);
      BaseProcess baseProcess = injector.getInstance(BaseProcess.class);
      baseProcess.processWithQuery(audienceName, action, expireAt);
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

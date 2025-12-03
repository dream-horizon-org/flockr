package com.dream11.flocker.engine;

import com.dream11.flocker.engine.config.*;
import com.dream11.flocker.engine.constants.Constants;
import com.dream11.flocker.engine.injector.EngineModule;
import com.dream11.flocker.engine.service.s3.S3Process;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import org.apache.spark.sql.SparkSession;

@Slf4j
public class EngineStart {
    public static void main(String[] args) {
        SparkSession sparkSession = null;
        Injector injector = null;
        try {
            log.info("Starting Flocker Spark Engine...");

            if (args.length < 1) {
                log.error("Usage: java -jar <jar-file> <ARGUMENTS_JSON>");
                log.error("Arguments JSON should contain:");
                log.error("  - sqlQuery: SQL query to execute");
                log.error("  - cohortName: Cohort name");
                log.error("  - action: Action type (append/remove)");
                log.error("  - sparkMaster: Spark master URL (e.g., spark://host:7077)");
                log.error("  - sourceJson: Source configuration as JSON array (must include ATHENA with accessKey/secretKey)");
                log.error("  - destinationJson: Destination configuration as JSON array (optional, can be empty or S3)");
                log.error("");
                log.error("Example JSON:");
                log.error("{");
                log.error("  \"sqlQuery\": \"SELECT * FROM table\",");
                log.error("  \"cohortName\": \"cohort123\",");
                log.error("  \"action\": \"append\",");
                log.error("  \"sparkMaster\": \"spark://host:7077\",");
                log.error("  \"sourceJson\": [{\"type\":\"ATHENA\",\"config\":{\"database\":\"a0a6bd02a3194a7a99699bc968575d83\",\"region\":\"us-east-1\",\"workgroup\":\"primary\",\"outputLocation\":\"s3a://my-bucket/athena-results/\",\"accessKey\":\"YOUR_ACCESS_KEY\",\"secretKey\":\"YOUR_SECRET_KEY\"}}],");
                log.error("  \"destinationJson\": []");
                log.error("}");
                System.exit(1);
            }

            EngineArguments engineArgs;
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                engineArgs = objectMapper.readValue(args[0], EngineArguments.class);
                log.info("Successfully parsed engine arguments from JSON");
            } catch (Exception e) {
                log.error("Failed to parse arguments JSON: {}", e.getMessage(), e);
                log.error("Please ensure the JSON is valid and contains all required fields");
                System.exit(1);
                return;
            }

            String sqlQuery = engineArgs.getSqlQuery();
            String cohortName = engineArgs.getCohortName();
            String action = engineArgs.getAction();
            String sparkMaster = engineArgs.getSparkMaster();
            List<ConnectorConfig> sourceConfigs = engineArgs.getSourceJson();
            List<ConnectorConfig> sinkConfigs = engineArgs.getDestinationJson();

            if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
                log.error("SQL query cannot be null or empty");
                System.exit(1);
            }
            if (cohortName == null || cohortName.trim().isEmpty()) {
                log.error("Cohort name cannot be null or empty");
                System.exit(1);
            }
            if (action == null || action.trim().isEmpty()) {
                log.error("Action cannot be null or empty");
                System.exit(1);
                return;
            }

            final String nonNullAction = action;
            String eventAction = nonNullAction.toLowerCase().trim();
            if (!eventAction.equals(Constants.ACTION_APPEND) && !eventAction.equals(Constants.ACTION_REMOVE)) {
                log.error("Invalid action: {}. Valid values: {}, {}", action, Constants.ACTION_APPEND,
                    Constants.ACTION_REMOVE);
                System.exit(1);
            }

            log.info("SQL Query: {}", sqlQuery);
            log.info("Cohort Name: {}", cohortName);
            log.info("Action: {}", eventAction);
            log.info("Spark Master: {}", sparkMaster);
            log.info("Source Configs: {}", sourceConfigs);
            log.info("Destination Configs: {}", sinkConfigs);

            if (sourceConfigs == null || sourceConfigs.isEmpty()) {
                log.error("Source configuration cannot be null or empty");
                System.exit(1);
            }


            if (sinkConfigs == null) {
                sinkConfigs = new ArrayList<>();
            }


            AthenaConfig athenaConfig = null;
            try {
                if (sourceConfigs != null) {
                    long athenaCount = sourceConfigs.stream()
                        .filter(config -> "ATHENA".equalsIgnoreCase(config.getType()))
                        .count();
                    long totalCount = sourceConfigs.size();

                    if (athenaCount == 0) {
                        log.error("Athena source is mandatory but not found in source configuration");
                        System.exit(1);
                    }
                    if (athenaCount != totalCount) {
                        log.error("Only Athena source is allowed. Found {} sources, {} are Athena", totalCount, athenaCount);
                        System.exit(1);
                    }

                    log.info("Athena source found in configuration");
                    for (ConnectorConfig config : sourceConfigs) {
                        if ("ATHENA".equalsIgnoreCase(config.getType())) {
                            Object configObj = config.getConfig();
                            athenaConfig = (AthenaConfig) configObj;
                            log.info("Athena config loaded - database: {}, region: {}",
                                    athenaConfig.getDatabase(), athenaConfig.getRegion());
                            break;
                        }
                    }
                }
                if (athenaConfig == null) {
                    throw new IllegalStateException("Athena config not found");
                }

                // Set SQL query in Athena config so it can be executed
                athenaConfig.setSqlQuery(sqlQuery);
                log.info("SQL query set in Athena config for execution. Database: {}", athenaConfig.getDatabase());
            } catch (Exception e) {
                log.error("Failed to process source configuration: {}", e.getMessage(), e);
                System.exit(1);
                return;
            }

            if (athenaConfig.getAccessKey() == null || athenaConfig.getSecretKey() == null) {
                log.error("AWS credentials (accessKey/secretKey) must be provided in Athena source configuration");
                System.exit(1);
            }

            log.info("Athena config - AccessKey: {}, SecretKey: {}, SessionToken: {}",
                    athenaConfig.getAccessKey() != null ? athenaConfig.getAccessKey().substring(0, Math.min(10, athenaConfig.getAccessKey().length())) + "..." : "null",
                    athenaConfig.getSecretKey() != null ? "***" : "null",
                    athenaConfig.getSessionToken() != null ? "present (" + athenaConfig.getSessionToken().length() + " chars)" : "null");

            System.setProperty("fs.s3a.access.key", athenaConfig.getAccessKey());
            System.setProperty("fs.s3a.secret.key", athenaConfig.getSecretKey());
            System.setProperty("AWS_ACCESS_KEY_ID", athenaConfig.getAccessKey());
            System.setProperty("AWS_SECRET_ACCESS_KEY", athenaConfig.getSecretKey());

            // Set session token if provided (for temporary credentials)
            if (athenaConfig.getSessionToken() != null && !athenaConfig.getSessionToken().isEmpty()) {
                System.setProperty("fs.s3a.session.token", athenaConfig.getSessionToken());
                System.setProperty("AWS_SESSION_TOKEN", athenaConfig.getSessionToken());
                // Use TemporaryAWSCredentialsProvider for temporary credentials with session token
                System.setProperty("fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.TemporaryAWSCredentialsProvider");
                log.info("AWS session token configured from Athena source - using TemporaryAWSCredentialsProvider");
            } else {
                System.setProperty("fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider");
                log.info("No session token found - using SimpleAWSCredentialsProvider");
            }
            log.info("AWS credentials configured from Athena source");


            S3Config internalS3Config = S3Config.providerForSink().get();
            internalS3Config.setAccessKey(athenaConfig.getAccessKey());
            internalS3Config.setSecretKey(athenaConfig.getSecretKey());
            if (athenaConfig.getSessionToken() != null && !athenaConfig.getSessionToken().isEmpty()) {
                internalS3Config.setSessionToken(athenaConfig.getSessionToken());
            }
            ConnectorConfig internalS3Sink = new ConnectorConfig("S3", internalS3Config);
            sinkConfigs.add(0, internalS3Sink);
            log.info("Added internal S3 sink: {}", internalS3Config.getS3Path());


            ApiConfig internalApiConfig = ApiConfig.fromConfig(
                com.typesafe.config.ConfigFactory.parseString("{}"));
            ConnectorConfig internalApiSink = new ConnectorConfig("API", internalApiConfig);
            sinkConfigs.add(internalApiSink);
            log.info("Added internal API sink: {}", internalApiConfig.getUrl());

            SparkConfig sparkConfig = SparkConfig.provider().get();
            log.debug("Loaded Spark configuration from config file");

            SparkSession.Builder sessionBuilder = SparkSession.builder();
            sessionBuilder.master(sparkMaster);

            sparkConfig.applyToSessionBuilder(sessionBuilder);

            sparkSession = sessionBuilder.getOrCreate();
            log.debug("SparkSession created successfully with master: {}", sparkMaster);

            injector = Guice
                .createInjector(new EngineModule(sparkSession, sourceConfigs, sinkConfigs, cohortName, eventAction));
            log.debug("Guice injector created successfully");

            S3Process s3Process = injector.getInstance(S3Process.class);
            log.info("Starting S3 processing with query and action: {}", eventAction);

            s3Process.processWithQuery(sqlQuery, cohortName, eventAction);
            log.info("Flocker Engine completed successfully");

        } catch (Exception e) {
            log.error("Error occurred while running Flocker Engine", e);
            System.exit(1);
        } finally {
            if (sparkSession != null) {
                log.debug("Stopping SparkSession...");
                sparkSession.stop();
            }
        }
    }
}

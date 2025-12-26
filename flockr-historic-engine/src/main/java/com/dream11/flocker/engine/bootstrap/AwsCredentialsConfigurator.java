package com.dream11.flocker.engine.bootstrap;

import com.dream11.flocker.engine.config.AthenaConfig;
import com.dream11.flocker.engine.config.ConnectorConfig;
import com.dream11.flocker.engine.config.S3Config;
import lombok.extern.slf4j.Slf4j;

/**
 * Configures AWS credentials from source configurations.
 * 
 * <p>This class is responsible for:
 * <ul>
 *   <li>Extracting AWS credentials from Athena or S3 source configurations</li>
 *   <li>Setting system properties for S3A filesystem access</li>
 *   <li>Configuring appropriate credentials provider based on session token presence</li>
 * </ul>
 * 
 * <p><b>Supported Source Types:</b>
 * <ul>
 *   <li><b>ATHENA</b>: Extracts credentials from AthenaConfig (required)</li>
 *   <li><b>S3</b>: Extracts credentials from S3Config (optional)</li>
 * </ul>
 * 
 * @author Shivam-Raghuwanshi
 */
@Slf4j
public class AwsCredentialsConfigurator {

    /**
     * Configures AWS credentials from the source connector configuration.
     * 
     * <p>This method:
     * <ol>
     *   <li>Extracts the source type and config object</li>
     *   <li>Configures AWS credentials based on source type</li>
     *   <li>Sets system properties for S3A filesystem access</li>
     * </ol>
     * 
     * @param sourceConnectorConfig The source connector configuration.
     * @return The extracted AthenaConfig if source is ATHENA, null otherwise.
     * @throws IllegalStateException If source config is null or invalid.
     * @throws IllegalArgumentException If ATHENA source is missing required credentials.
     */
    public AthenaConfig configureCredentials(ConnectorConfig sourceConnectorConfig) {
        String sourceType = sourceConnectorConfig.getType().toUpperCase();
        Object configObj = sourceConnectorConfig.getConfig();
        
        if (configObj == null) {
            throw new IllegalStateException("Source config object is null");
        }

        AthenaConfig athenaConfig = null;
        S3Config sourceS3Config = null;

        if ("ATHENA".equalsIgnoreCase(sourceType)) {
            athenaConfig = (AthenaConfig) configObj;
            log.info("Athena config loaded - database: {}, region: {}",
                    athenaConfig.getDatabase() != null ? athenaConfig.getDatabase() : "will be extracted from query",
                    athenaConfig.getRegion());

            // Validate required credentials
            if (athenaConfig.getAccessKey() == null || athenaConfig.getSecretKey() == null) {
                throw new IllegalArgumentException(
                    "AWS credentials (accessKey/secretKey) must be provided in Athena source configuration");
            }

            log.info("Athena config - AccessKey: {}, SecretKey: {}, SessionToken: {}",
                    athenaConfig.getAccessKey() != null 
                        ? athenaConfig.getAccessKey().substring(0, Math.min(5, athenaConfig.getAccessKey().length())) + "..." 
                        : "null",
                    athenaConfig.getSecretKey() != null ? "***" : "null",
                    athenaConfig.getSessionToken() != null 
                        ? "present (" + athenaConfig.getSessionToken().length() + " chars)" 
                        : "null");

            configureSystemProperties(athenaConfig.getAccessKey(), athenaConfig.getSecretKey(), 
                    athenaConfig.getSessionToken(), "Athena source");
            
        } else if ("S3".equalsIgnoreCase(sourceType)) {
            sourceS3Config = (S3Config) configObj;
            log.info("S3 source config loaded - bucket: {}, path: {}", 
                    sourceS3Config.getBucket(), sourceS3Config.getPath());

            // Configure credentials if available (optional for S3)
            if (sourceS3Config.getAccessKey() != null && sourceS3Config.getSecretKey() != null) {
                configureSystemProperties(sourceS3Config.getAccessKey(), sourceS3Config.getSecretKey(),
                        sourceS3Config.getSessionToken(), "S3 source");
            }
        } else {
            log.info("Source type: {} - no AWS credentials configuration needed", sourceType);
        }

        return athenaConfig;
    }

    /**
     * Configures system properties for AWS S3A filesystem access.
     * 
     * @param accessKey AWS access key ID.
     * @param secretKey AWS secret access key.
     * @param sessionToken Optional AWS session token for temporary credentials.
     * @param sourceName Name of the source (for logging purposes).
     */
    private void configureSystemProperties(String accessKey, String secretKey, String sessionToken, String sourceName) {
        System.setProperty("fs.s3a.access.key", accessKey);
        System.setProperty("fs.s3a.secret.key", secretKey);
        System.setProperty("AWS_ACCESS_KEY_ID", accessKey);
        System.setProperty("AWS_SECRET_ACCESS_KEY", secretKey);

        if (sessionToken != null && !sessionToken.isEmpty()) {
            System.setProperty("fs.s3a.session.token", sessionToken);
            System.setProperty("AWS_SESSION_TOKEN", sessionToken);
            System.setProperty("fs.s3a.aws.credentials.provider", 
                    "org.apache.hadoop.fs.s3a.TemporaryAWSCredentialsProvider");
            log.info("AWS session token configured from {} - using TemporaryAWSCredentialsProvider", sourceName);
        } else {
            System.setProperty("fs.s3a.aws.credentials.provider", 
                    "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider");
            log.info("No session token found - using SimpleAWSCredentialsProvider");
        }
        
        log.info("AWS credentials configured from {}", sourceName);
    }
}


package io.ascend.flockr.engine.bootstrap;

import com.typesafe.config.ConfigFactory;
import io.ascend.flockr.engine.config.ApiConfig;
import io.ascend.flockr.engine.config.AthenaConfig;
import io.ascend.flockr.engine.config.ConnectorConfig;
import io.ascend.flockr.engine.config.S3Config;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Configures internal sinks (S3 and API) for the Flocker Historic Engine.
 *
 * <p>This class is responsible for:
 *
 * <ul>
 *   <li>Creating internal S3 sink configuration with credentials from source
 *   <li>Creating internal API sink configuration
 *   <li>Adding these sinks to the sink configurations list
 * </ul>
 *
 * <p><b>Internal Sinks:</b>
 *
 * <ul>
 *   <li><b>S3 Sink</b>: Always added at the beginning of the list for data persistence
 *   <li><b>API Sink</b>: Always added at the end for external API notifications
 * </ul>
 *
 * @author Shivam-Raghuwanshi
 */
@Slf4j
public class InternalSinkConfigurator {

  /**
   * Configures and adds internal sinks (S3 and API) to the sink configurations list.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>Creates internal S3 sink with credentials from source (if available)
   *   <li>Adds S3 sink at the beginning of the list
   *   <li>Creates internal API sink with default configuration
   *   <li>Adds API sink at the end of the list
   * </ol>
   *
   * @param sinkConfigs The existing sink configurations list (may be null or empty).
   * @param athenaConfig The Athena configuration (may be null if source is not Athena).
   * @param sourceConnectorConfig The source connector configuration (for S3 source credentials).
   * @return Updated list of sink configurations with internal sinks added.
   */
  public List<ConnectorConfig> configureInternalSinks(
      List<ConnectorConfig> sinkConfigs,
      AthenaConfig athenaConfig,
      ConnectorConfig sourceConnectorConfig) {
    if (sinkConfigs == null) {
      sinkConfigs = new ArrayList<>();
    }

    // Configure internal S3 sink with credentials from source
    S3Config internalS3Config = S3Config.providerForSink().get();
    copyCredentialsToS3Config(internalS3Config, athenaConfig, sourceConnectorConfig);

    ConnectorConfig internalS3Sink = new ConnectorConfig("S3", internalS3Config);
    sinkConfigs.add(0, internalS3Sink);
    log.info("Added internal S3 sink: {}", internalS3Config.getS3Path());

    // Configure internal API sink
    ApiConfig internalApiConfig = ApiConfig.fromConfig(ConfigFactory.parseString("{}"));
    ConnectorConfig internalApiSink = new ConnectorConfig("API", internalApiConfig);
    sinkConfigs.add(internalApiSink);
    log.info("Added internal API sink: {}", internalApiConfig.getUrl());

    return sinkConfigs;
  }

  /**
   * Copies AWS credentials from source configuration to internal S3 sink configuration.
   *
   * @param internalS3Config The internal S3 sink configuration to update.
   * @param athenaConfig The Athena configuration (if source is Athena).
   * @param sourceConnectorConfig The source connector configuration (for S3 source).
   */
  private void copyCredentialsToS3Config(
      S3Config internalS3Config, AthenaConfig athenaConfig, ConnectorConfig sourceConnectorConfig) {
    if (athenaConfig != null) {
      internalS3Config.setAccessKey(athenaConfig.getAccessKey());
      internalS3Config.setSecretKey(athenaConfig.getSecretKey());
      if (athenaConfig.getSessionToken() != null && !athenaConfig.getSessionToken().isEmpty()) {
        internalS3Config.setSessionToken(athenaConfig.getSessionToken());
      }
    } else if (sourceConnectorConfig != null
        && "S3".equalsIgnoreCase(sourceConnectorConfig.getType())) {
      S3Config sourceS3Config = (S3Config) sourceConnectorConfig.getConfig();
      if (sourceS3Config != null
          && sourceS3Config.getAccessKey() != null
          && sourceS3Config.getSecretKey() != null) {
        internalS3Config.setAccessKey(sourceS3Config.getAccessKey());
        internalS3Config.setSecretKey(sourceS3Config.getSecretKey());
        if (sourceS3Config.getSessionToken() != null
            && !sourceS3Config.getSessionToken().isEmpty()) {
          internalS3Config.setSessionToken(sourceS3Config.getSessionToken());
        }
      }
    }
  }
}

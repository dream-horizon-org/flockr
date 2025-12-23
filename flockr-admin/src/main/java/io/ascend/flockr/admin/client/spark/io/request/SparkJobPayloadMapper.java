package io.ascend.flockr.admin.client.spark.io.request;

import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.DataSourceDetails;
import io.ascend.flockr.admin.domain.rule.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Mapper for transforming RuleMetaVerbose into Spark job payload JSON.
 *
 * <p>This mapper converts enriched rule metadata into the format expected by the Spark batch
 * processing engine. The output JSON includes:
 *
 * <ul>
 *   <li>Audience name and action (append/remove)
 *   <li>Expiry timestamp as epoch seconds
 *   <li>Source configuration with query, type, and connection details
 *   <li>Destination configurations for all sinks
 * </ul>
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class SparkJobPayloadMapper {

  /**
   * Transforms RuleMetaVerbose into Spark job payload JSON.
   *
   * <p>Example output:
   *
   * <pre>{@code
   * {
   *   "audienceName": "test-cohort-001",
   *   "action": "append",
   *   "expireAt": 1735689599,
   *   "source": {
   *     "query": "SELECT DISTINCT userid FROM users WHERE ...",
   *     "type": "ATHENA",
   *     "config": { ... }
   *   },
   *   "destinationJson": [{
   *     "type": "S3",
   *     "config": { ... }
   *   }]
   * }
   * }</pre>
   *
   * @param executableRule the enriched rule metadata with source and sink details
   * @return JsonObject containing the Spark job payload
   */
  public static JsonObject buildSparkJobPayload(
      ExecutableRule<SourceInfoEnriched, SinkInfoEnriched> executableRule) {

    log.debug(
        "Building Spark job payload for rule: {} ({})",
        executableRule.getRuleId(),
        executableRule.getName());

    RuleConfiguration<SourceInfoEnriched> configuration = executableRule.getConfiguration();
    if (!(configuration instanceof BatchConfiguration<SourceInfoEnriched> batchConfig)) {
      throw new IllegalArgumentException(
          "Expected BatchConfiguration but got: " + configuration.getClass().getSimpleName());
    }

    JsonObject payload = new JsonObject();

    payload.put("audienceName", executableRule.getAudienceName());

    String action = mapRuleActionToSparkAction(executableRule.getRuleAction());
    payload.put("action", action);

    payload.put("expireAt", executableRule.getExpireAt());

    JsonObject source = buildSource(batchConfig);
    payload.put("source", source);

    JsonArray destinationJson = buildDestinationJson(executableRule.getSinkList());
    payload.put("destinationJson", destinationJson);

    log.debug("Built Spark job payload: {}", payload.encodePrettily());
    return payload;
  }

  /**
   * Maps RuleAction to Spark action format.
   *
   * @param ruleAction the rule action (ADD or REMOVE)
   * @return "append" for ADD, "remove" for REMOVE
   */
  private static String mapRuleActionToSparkAction(RuleAction ruleAction) {
    return switch (ruleAction) {
      case ADD -> "append";
      case REMOVE -> "remove";
    };
  }

  /**
   * Builds the source object from batch configuration.
   *
   * <p>Example output:
   *
   * <pre>{@code
   * {
   *   "query": "SELECT DISTINCT userid FROM users WHERE ...",
   *   "type": "ATHENA",
   *   "config": {
   *     "region": "us-east-1",
   *     "workgroup": "primary",
   *     ...
   *   }
   * }
   * }</pre>
   *
   * @param batchConfig the batch configuration containing source and query
   * @return JsonObject containing source configuration
   */
  private static JsonObject buildSource(BatchConfiguration<SourceInfoEnriched> batchConfig) {
    SourceInfoEnriched source = batchConfig.getSource();
    if (source == null || source.getDetails() == null) {
      log.warn("Source details are missing in batch configuration");
      return new JsonObject();
    }

    DataSourceDetails sourceDetails = source.getDetails();

    return new JsonObject()
        .put("query", batchConfig.getQuery())
        .put("type", sourceDetails.getType())
        .put("config", sourceDetails.getConfig());
  }

  /**
   * Builds the destinationJson array from sink list.
   *
   * <p>Example output:
   *
   * <pre>{@code
   * [{
   *   "type": "S3",
   *   "config": {
   *     "bucket": "my-bucket",
   *     "path": "/audiences",
   *     ...
   *   }
   * }]
   * }</pre>
   *
   * @param sinkList the list of enriched sink information
   * @return JsonArray containing destination configurations
   */
  private static JsonArray buildDestinationJson(java.util.List<SinkInfoEnriched> sinkList) {
    JsonArray destinationArray = new JsonArray();

    if (sinkList == null || sinkList.isEmpty()) {
      log.debug("No sinks configured for this rule");
      return destinationArray;
    }

    for (SinkInfoEnriched sink : sinkList) {
      if (sink == null || sink.getDetails() == null) {
        log.warn("Sink details are missing, skipping sink");
        continue;
      }

      DataSinkDetails sinkDetails = sink.getDetails();

      JsonObject destinationObj = new JsonObject();
      destinationObj.put("type", sinkDetails.getType());
      destinationObj.put("config", sinkDetails.getConfig());

      destinationArray.add(destinationObj);
    }

    return destinationArray;
  }
}

package io.ascend.flockr.admin.client.spark.dto.request;

import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.DataSourceDetails;
import io.ascend.flockr.admin.domain.rule.BatchConfiguration;
import io.ascend.flockr.admin.domain.rule.RuleAction;
import io.ascend.flockr.admin.domain.rule.RuleMeta;
import io.ascend.flockr.admin.domain.rule.SourceInfo;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Mapper for building Spark job submission requests from rule metadata.
 *
 * <p>Converts RuleMeta, DataSourceDetails, and DataSinkDetails into the format expected by the
 * Spark REST Submission API.
 *
 * <p>Program arguments order:
 *
 * <ol>
 *   <li>query - SQL query string
 *   <li>ruleId - Rule ID
 *   <li>ruleAction - "append" or "overwrite"
 *   <li>masterUrl - Spark master URL
 *   <li>sources - JSON array of source configurations
 *   <li>destinations - JSON array of destination configurations
 * </ol>
 *
 * @author Flockr Team
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class SparkJobRequestMapper {

  private static final String MAIN_CLASS = "com.dream11.flocker.engine.EngineStart";
  private static final String CLIENT_SPARK_VERSION = "3.5.0";

  /**
   * Builds a SparkSubmissionRequest from rule metadata and related data.
   *
   * @param rule the rule metadata
   * @param dataSource the enriched data source details
   * @param dataSinks list of data sink details for destinations
   * @param sparkJobConfig configuration containing master URL, JAR path, etc.
   * @return SparkSubmissionRequest ready for submission
   */
  public static SparkSubmissionRequest buildRequest(
      RuleMeta<SourceInfo> rule,
      DataSourceDetails dataSource,
      List<DataSinkDetails> dataSinks,
      SparkJobConfig sparkJobConfig) {

    BatchConfiguration<SourceInfo> batchConfig =
        (BatchConfiguration<SourceInfo>) rule.getConfiguration();

    // Build program arguments
    List<String> appArgs = buildAppArgs(rule, batchConfig, dataSource, dataSinks, sparkJobConfig);

    // Build Spark properties
    Map<String, String> sparkProperties = buildSparkProperties(rule, sparkJobConfig);

    return SparkSubmissionRequest.builder()
        .action("CreateSubmissionRequest")
        .appArgs(appArgs)
        .appResource(sparkJobConfig.getJarPath())
        .clientSparkVersion(CLIENT_SPARK_VERSION)
        .mainClass(MAIN_CLASS)
        .sparkProperties(sparkProperties)
        .build();
  }

  /**
   * Builds the application arguments array.
   *
   * @param rule the rule metadata
   * @param batchConfig the batch configuration
   * @param dataSource the data source details
   * @param dataSinks list of data sink details
   * @param sparkJobConfig spark job configuration
   * @return list of application arguments
   */
  private static List<String> buildAppArgs(
      RuleMeta<SourceInfo> rule,
      BatchConfiguration<SourceInfo> batchConfig,
      DataSourceDetails dataSource,
      List<DataSinkDetails> dataSinks,
      SparkJobConfig sparkJobConfig) {

    // Arg 1: SQL Query
    String query = batchConfig.getQuery();

    // Arg 2: Rule ID
    String ruleId = String.valueOf(rule.getRuleId());

    // Arg 3: Rule Action (append/overwrite)
    String ruleAction = mapRuleAction(rule.getRuleAction());

    // Arg 4: Spark Master URL
    String masterUrl = sparkJobConfig.getMasterUrl();

    // Arg 5: Sources JSON array
    String sourcesJson = buildSourcesJson(dataSource);

    // Arg 6: Destinations JSON array
    String destinationsJson = buildDestinationsJson(dataSinks);

    List<String> args =
        List.of(query, ruleId, ruleAction, masterUrl, sourcesJson, destinationsJson);

    log.debug(
        "Built Spark app args: query={}, ruleId={}, action={}",
        truncate(query, 50),
        ruleId,
        ruleAction);

    return args;
  }

  /**
   * Maps RuleAction enum to Spark action string.
   *
   * @param action the rule action
   * @return the Spark action string
   */
  private static String mapRuleAction(RuleAction action) {
    return switch (action) {
      case ADD -> "append";
      case REMOVE -> "overwrite";
    };
  }

  /**
   * Builds the sources JSON array string.
   *
   * <p>Format: [{"type":"ATHENA","config":{"database":"...","region":"...",...}}]
   *
   * @param dataSource the data source details
   * @return JSON array string of sources
   */
  private static String buildSourcesJson(DataSourceDetails dataSource) {
    JsonArray sources = new JsonArray();

    JsonObject sourceObj = new JsonObject().put("type", dataSource.getType());

    // Config is already a JsonObject, merge it
    if (dataSource.getConfig() != null) {
      sourceObj.put("config", dataSource.getConfig());
    }

    sources.add(sourceObj);

    return sources.encode();
  }

  /**
   * Builds the destinations JSON array string.
   *
   * <p>Format: [{"type":"API","config":{"url":"...","rateLimitPerSecond":10,...}}]
   *
   * @param dataSinks list of data sink details
   * @return JSON array string of destinations
   */
  private static String buildDestinationsJson(List<DataSinkDetails> dataSinks) {
    JsonArray destinations = new JsonArray();

    for (DataSinkDetails sink : dataSinks) {
      JsonObject destObj = new JsonObject().put("type", sink.getType());

      // Config is already a JsonObject, merge it
      if (sink.getConfig() != null) {
        destObj.put("config", sink.getConfig());
      }

      destinations.add(destObj);
    }

    return destinations.encode();
  }

  /**
   * Builds Spark configuration properties.
   *
   * @param rule the rule metadata
   * @param sparkJobConfig spark job configuration
   * @return map of Spark properties
   */
  private static Map<String, String> buildSparkProperties(
      RuleMeta<SourceInfo> rule, SparkJobConfig sparkJobConfig) {

    String appName =
        String.format("flockr-batch-%d-%s", rule.getRuleId(), sanitizeName(rule.getName()));

    return Map.of(
        "spark.master", sparkJobConfig.getMasterUrl(),
        "spark.app.name", appName,
        "spark.submit.deployMode", sparkJobConfig.getDeployMode(),
        "spark.executor.memory", sparkJobConfig.getExecutorMemory(),
        "spark.executor.cores", String.valueOf(sparkJobConfig.getExecutorCores()),
        "spark.executor.instances", String.valueOf(sparkJobConfig.getExecutorInstances()),
        "spark.driver.memory", sparkJobConfig.getDriverMemory());
  }

  /**
   * Converts SparkSubmissionRequest to JsonObject for HTTP submission.
   *
   * @param request the submission request
   * @return JsonObject representation
   */
  public static JsonObject toJsonObject(SparkSubmissionRequest request) {
    return new JsonObject()
        .put("action", request.getAction())
        .put("appArgs", new JsonArray(request.getAppArgs()))
        .put("appResource", request.getAppResource())
        .put("clientSparkVersion", request.getClientSparkVersion())
        .put("mainClass", request.getMainClass())
        .put("environmentVariables", JsonObject.mapFrom(request.getEnvironmentVariables()))
        .put("sparkProperties", JsonObject.mapFrom(request.getSparkProperties()));
  }

  /**
   * Truncates a string to a maximum length for logging.
   *
   * @param str the string to truncate
   * @param maxLen maximum length
   * @return truncated string
   */
  private static String truncate(String str, int maxLen) {
    if (str == null) return null;
    return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
  }

  /**
   * Sanitizes a name for use in Spark app name (removes special characters).
   *
   * @param name the name to sanitize
   * @return sanitized name
   */
  private static String sanitizeName(String name) {
    if (name == null) return "unknown";
    return name.replaceAll("[^a-zA-Z0-9-_]", "_");
  }
}

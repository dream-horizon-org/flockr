package io.ascend.flockr.admin.client.spark.io.request;

import static org.junit.jupiter.api.Assertions.*;

import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.DataSourceDetails;
import io.ascend.flockr.admin.domain.rule.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SparkJobPayloadMapper.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
class SparkJobPayloadMapperTest {

  @Test
  @DisplayName("Should transform RuleMetaVerbose to correct Spark job payload format")
  void testBuildSparkJobPayload() {
    // Given: A rule with enriched source and sink details
    ExecutableRule<SourceInfoEnriched, SinkInfoEnriched> executableRule =
        createTestRuleMetaVerbose();

    // When: Building Spark job payload
    JsonObject payload = SparkJobPayloadMapper.buildSparkJobPayload(executableRule);

    // Then: Payload should have correct structure
    assertNotNull(payload);
    assertEquals("test-cohort-001", payload.getString("audienceName"));
    assertEquals("append", payload.getString("action"));
    assertNotNull(payload.getLong("expireAt"));
    assertEquals(1735689599L, payload.getLong("expireAt"));

    // Verify source object (not array)
    JsonObject source = payload.getJsonObject("source");
    assertNotNull(source);
    assertEquals("SELECT DISTINCT userid FROM users WHERE age > 25", source.getString("query"));
    assertEquals("ATHENA", source.getString("type"));
    assertNotNull(source.getJsonObject("config"));

    // Verify destinationJson array
    JsonArray destinationJson = payload.getJsonArray("destinationJson");
    assertNotNull(destinationJson);
    assertEquals(2, destinationJson.size());

    JsonObject destination1 = destinationJson.getJsonObject(0);
    assertEquals("S3", destination1.getString("type"));
    assertNotNull(destination1.getJsonObject("config"));
  }

  @Test
  @DisplayName("Should map RuleAction.ADD to 'append'")
  void testMapRuleActionAdd() {
    ExecutableRule<SourceInfoEnriched, SinkInfoEnriched> rule = createTestRuleMetaVerbose();
    rule.setRuleAction(RuleAction.ADD);

    JsonObject payload = SparkJobPayloadMapper.buildSparkJobPayload(rule);

    assertEquals("append", payload.getString("action"));
  }

  @Test
  @DisplayName("Should map RuleAction.REMOVE to 'remove'")
  void testMapRuleActionRemove() {
    ExecutableRule<SourceInfoEnriched, SinkInfoEnriched> rule = createTestRuleMetaVerbose();
    rule.setRuleAction(RuleAction.REMOVE);

    JsonObject payload = SparkJobPayloadMapper.buildSparkJobPayload(rule);

    assertEquals("remove", payload.getString("action"));
  }

  @Test
  @DisplayName("Should format timestamp as epoch seconds")
  void testExpireAtAsEpochSeconds() {
    ExecutableRule<SourceInfoEnriched, SinkInfoEnriched> rule = createTestRuleMetaVerbose();
    // Set a known timestamp: 2025-12-31 23:59:59 UTC = 1735689599 epoch seconds
    rule.setExpireAt(1735689599L);

    JsonObject payload = SparkJobPayloadMapper.buildSparkJobPayload(rule);

    Long expireAt = payload.getLong("expireAt");
    assertNotNull(expireAt);
    assertEquals(1735689599L, expireAt);
  }

  @Test
  @DisplayName("Should handle empty sink list")
  void testEmptySinkList() {
    ExecutableRule<SourceInfoEnriched, SinkInfoEnriched> rule = createTestRuleMetaVerbose();
    rule.setSinkList(List.of());

    JsonObject payload = SparkJobPayloadMapper.buildSparkJobPayload(rule);

    JsonArray destinationJson = payload.getJsonArray("destinationJson");
    assertNotNull(destinationJson);
    assertEquals(0, destinationJson.size());
  }

  @Test
  @DisplayName("Should throw exception for non-batch configuration")
  void testNonBatchConfiguration() {
    ExecutableRule<SourceInfoEnriched, SinkInfoEnriched> rule = new ExecutableRule<>();
    rule.setAudienceName("test-audience");
    rule.setRuleAction(RuleAction.ADD);
    rule.setEndTime(1735689599L);

    // Set a StreamConfiguration instead of BatchConfiguration
    StreamConfiguration<SourceInfoEnriched> streamConfig = new StreamConfiguration<>();
    rule.setConfiguration(streamConfig);

    assertThrows(
        IllegalArgumentException.class, () -> SparkJobPayloadMapper.buildSparkJobPayload(rule));
  }

  // Helper method to create test data
  private ExecutableRule<SourceInfoEnriched, SinkInfoEnriched> createTestRuleMetaVerbose() {
    // Create source details
    DataSourceDetails sourceDetails =
        DataSourceDetails.builder()
            .id(1L)
            .name("Test Athena Source")
            .type("ATHENA")
            .config(
                new JsonObject()
                    .put("region", "us-east-1")
                    .put("workgroup", "primary")
                    .put("outputLocation", "s3://test-bucket/athena-output/")
                    .put("accessKey", "TEST_ACCESS_KEY")
                    .put("secretKey", "TEST_SECRET_KEY"))
            .build();

    SourceInfoEnriched source = SourceInfoEnriched.builder().id(1L).details(sourceDetails).build();

    // Create batch configuration
    BatchConfiguration<SourceInfoEnriched> batchConfig =
        BatchConfiguration.<SourceInfoEnriched>builder()
            .source(source)
            .query("SELECT DISTINCT userid FROM users WHERE age > 25")
            .cronExpression("0 0 * * *")
            .build();

    // Create sink details
    DataSinkDetails sink1Details =
        DataSinkDetails.builder()
            .id(10L)
            .name("Test S3 Sink")
            .type("S3")
            .config(
                new JsonObject()
                    .put("bucket", "test-bucket")
                    .put("path", "/audiences")
                    .put("region", "us-east-1"))
            .build();

    DataSinkDetails sink2Details =
        DataSinkDetails.builder()
            .id(11L)
            .name("Test Kafka Sink")
            .type("KAFKA")
            .config(
                new JsonObject()
                    .put("bootstrapServers", "localhost:9092")
                    .put("topic", "audience-updates"))
            .build();

    SinkInfoEnriched sink1 = SinkInfoEnriched.builder().id(10L).details(sink1Details).build();

    SinkInfoEnriched sink2 = SinkInfoEnriched.builder().id(11L).details(sink2Details).build();

    // Create rule metadata
    ExecutableRule<SourceInfoEnriched, SinkInfoEnriched> rule = new ExecutableRule<>();
    rule.setRuleId(123L);
    rule.setAudienceId(456L);
    rule.setAudienceName("test-cohort-001");
    rule.setName("Test Batch Rule");
    rule.setDescription("Test batch rule for unit testing");
    rule.setRuleAction(RuleAction.ADD);
    rule.setRuleType(RuleType.BATCH);
    rule.setConfiguration(batchConfig);
    rule.setSinkList(List.of(sink1, sink2));
    rule.setExpireAt(1735689599L); // 2025-12-31 23:59:59 UTC

    return rule;
  }
}

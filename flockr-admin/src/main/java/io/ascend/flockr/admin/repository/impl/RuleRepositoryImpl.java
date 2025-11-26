package io.ascend.flockr.admin.repository.impl;

import com.google.inject.Inject;
import io.ascend.flockr.admin.client.postgres.PostgresReaderClient;
import io.ascend.flockr.admin.client.postgres.PostgresWriterClient;
import io.ascend.flockr.admin.domain.rule.RuleMeta;
import io.ascend.flockr.admin.domain.rule.SourceInfo;
import io.ascend.flockr.admin.repository.RuleRepository;
import io.ascend.flockr.admin.util.RuleHelpers;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.sqlclient.Tuple;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Implementation of {@link RuleRepository} using PostgreSQL as the data store.
 *
 * <p>This implementation handles:
 *
 * <ul>
 *   <li>Batch creation of rules within transactions
 *   <li>Serialization of rule configurations to JSONB
 *   <li>Retrieval of rules by ID or audience association
 *   <li>Timestamp conversion between epoch and database formats
 * </ul>
 *
 * <p>All write operations are executed within database transactions to ensure atomicity.
 *
 * @author Flockr Team
 * @since 1.0
 */
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class RuleRepositoryImpl implements RuleRepository {
  private final PostgresReaderClient postgresReaderClient;
  private final PostgresWriterClient postgresWriterClient;

  private static final String SQL_CREATE_RULE =
      "INSERT INTO rules (audience_id, tenant_id, project_id, name, description, start_time, end_time, "
          + "rule_action, rule_type, configuration, created_by) "
          + "VALUES ($1, $2, $3, $4, $5, to_timestamp($6), to_timestamp($7), $8, $9, CAST($10 AS JSONB), $11)";

  private static final String SQL_GET_RULE_BY_ID =
      "SELECT id, audience_id, tenant_id, project_id, name, description, "
          + "EXTRACT(EPOCH FROM start_time)::BIGINT as start_time, EXTRACT(EPOCH FROM end_time)::BIGINT as end_time, "
          + "rule_action, rule_type, status, configuration, "
          + "created_by, updated_by, EXTRACT(EPOCH FROM created_at)::BIGINT as created_at, "
          + "EXTRACT(EPOCH FROM updated_at)::BIGINT as updated_at "
          + "FROM rules WHERE id = $1 AND tenant_id = $2 AND project_id = $3";

  private static final String SQL_GET_RULES_BY_AUDIENCE =
      "SELECT id, audience_id, tenant_id, project_id, name, description, "
          + "EXTRACT(EPOCH FROM start_time)::BIGINT as start_time, EXTRACT(EPOCH FROM end_time)::BIGINT as end_time, "
          + "rule_action, rule_type, status, configuration, "
          + "created_by, updated_by, EXTRACT(EPOCH FROM created_at)::BIGINT as created_at, "
          + "EXTRACT(EPOCH FROM updated_at)::BIGINT as updated_at "
          + "FROM rules WHERE audience_id = $1 AND tenant_id = $2 AND project_id = $3 ORDER BY created_at DESC";

  @Override
  public Single<Boolean> createRules(List<RuleMeta<SourceInfo>> ruleMetas) {
    List<Tuple> batchParams = new ArrayList<>(ruleMetas.size());
    for (RuleMeta<SourceInfo> ruleMeta : ruleMetas) {
      String configJson = RuleHelpers.serializeRuleConfiguration(ruleMeta.getConfiguration());
      Tuple params =
          Tuple.tuple()
              .addValue(ruleMeta.getAudienceId())
              .addValue(ruleMeta.getTenantId())
              .addValue(ruleMeta.getProjectId())
              .addValue(ruleMeta.getName())
              .addValue(ruleMeta.getDescription())
              .addValue(ruleMeta.getStartTime())
              .addValue(ruleMeta.getEndTime())
              .addValue(ruleMeta.getRuleAction())
              .addValue(ruleMeta.getRuleType())
              .addValue(configJson)
              .addValue(ruleMeta.getCreatedBy());
      batchParams.add(params);
    }

    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                postgresWriterClient.executeMultiple(conn, SQL_CREATE_RULE, batchParams).toMaybe())
        .switchIfEmpty(
            Maybe.error(new IllegalStateException("Failed to create rules in transaction")))
        .toSingle();
  }

  @Override
  public Single<RuleMeta<SourceInfo>> getRuleById(String tenantId, String projectId, Long ruleId) {
    return postgresReaderClient.fetchOne(
        SQL_GET_RULE_BY_ID, Tuple.of(ruleId, tenantId, projectId), RuleHelpers::mapRuleRow);
  }

  @Override
  public Single<List<RuleMeta<SourceInfo>>> getRulesByAudienceId(
      String tenantId, String projectId, Long audienceId) {
    return postgresReaderClient.fetchAll(
        SQL_GET_RULES_BY_AUDIENCE,
        Tuple.of(audienceId, tenantId, projectId),
        RuleHelpers::mapRuleRow);
  }
}

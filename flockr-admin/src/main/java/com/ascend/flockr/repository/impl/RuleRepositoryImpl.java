package com.ascend.flockr.repository.impl;

import com.ascend.flockr.client.postgres.PostgresReaderClient;
import com.ascend.flockr.client.postgres.PostgresWriterClient;
import com.ascend.flockr.domain.rule.RuleMeta;
import com.ascend.flockr.domain.rule.SourceInfoBasic;
import com.ascend.flockr.repository.RuleRepository;
import com.ascend.flockr.util.RuleHelpers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.sqlclient.Tuple;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class RuleRepositoryImpl implements RuleRepository {
  private final PostgresReaderClient postgresReaderClient;
  private final PostgresWriterClient postgresWriterClient;
  private final ObjectMapper objectMapper;

  private static final String SQL_CREATE_RULE =
      "INSERT INTO rules (audience_id, tenant_id, name, description, start_time, end_time, "
          + "rule_action, rule_type, configuration, created_by) "
          + "VALUES (?, ?, ?, ?, FROM_UNIXTIME(?), FROM_UNIXTIME(?), ?, ?, CAST(? AS JSON), ?)";

  private static final String SQL_GET_RULE_BY_ID =
      "SELECT id, audience_id, tenant_id, name, description, "
          + "UNIX_TIMESTAMP(start_time) as start_time, UNIX_TIMESTAMP(end_time) as end_time, "
          + "rule_action, rule_type, status, configuration, "
          + "created_by, updated_by, UNIX_TIMESTAMP(created_at) as created_at, "
          + "UNIX_TIMESTAMP(updated_at) as updated_at "
          + "FROM rules WHERE id = ?";

  private static final String SQL_GET_RULES_BY_AUDIENCE =
      "SELECT id, audience_id, tenant_id, name, description, "
          + "UNIX_TIMESTAMP(start_time) as start_time, UNIX_TIMESTAMP(end_time) as end_time, "
          + "rule_action, rule_type, status, configuration, "
          + "created_by, updated_by, UNIX_TIMESTAMP(created_at) as created_at, "
          + "UNIX_TIMESTAMP(updated_at) as updated_at "
          + "FROM rules WHERE audience_id = ? ORDER BY created_at DESC";

  @Override
  public Single<Boolean> createRules(List<RuleMeta<SourceInfoBasic>> ruleMetas) {
    List<Tuple> batchParams = new ArrayList<>(ruleMetas.size());
    for (RuleMeta<SourceInfoBasic> ruleMeta : ruleMetas) {
      String configJson = RuleHelpers.serializeRuleConfiguration(ruleMeta.getConfiguration());
      Tuple params =
          Tuple.tuple()
              .addValue(ruleMeta.getAudienceId())
              .addValue(ruleMeta.getTenantId())
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
  public Single<RuleMeta<SourceInfoBasic>> getRuleById(Long ruleId) {
    return postgresReaderClient.fetchOne(
        SQL_GET_RULE_BY_ID, Tuple.of(ruleId), RuleHelpers::mapRuleRow);
  }
}

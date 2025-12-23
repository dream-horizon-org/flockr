package io.ascend.flockr.admin.repository.impl;

import com.google.inject.Inject;
import io.ascend.flockr.admin.client.postgres.PostgresReaderClient;
import io.ascend.flockr.admin.client.postgres.PostgresWriterClient;
import io.ascend.flockr.admin.domain.rule.*;
import io.ascend.flockr.admin.repository.RuleRepository;
import io.ascend.flockr.admin.util.RuleHelpers;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.sqlclient.Tuple;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
 * @author Prithu Sharma
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class RuleRepositoryImpl implements RuleRepository {
  private final PostgresReaderClient postgresReaderClient;
  private final PostgresWriterClient postgresWriterClient;

  private static final String SQL_CREATE_RULE =
      "INSERT INTO rules (audience_id, x_project_id, name, description, start_time, end_time, "
          + "rule_action, rule_type, configuration, created_by) "
          + "VALUES ($1, $2, $3, $4, to_timestamp($5), to_timestamp($6), $7, $8, CAST($9 AS JSONB), $10)";

  private static final String SQL_GET_RULE_BY_ID =
      "SELECT id, audience_id, x_project_id, name, description, "
          + "EXTRACT(EPOCH FROM start_time)::BIGINT as start_time, EXTRACT(EPOCH FROM end_time)::BIGINT as end_time, "
          + "rule_action, rule_type, status, configuration, "
          + "created_by, EXTRACT(EPOCH FROM created_at)::BIGINT as created_at, "
          + "EXTRACT(EPOCH FROM updated_at)::BIGINT as updated_at "
          + "FROM rules WHERE id = $1 AND x_project_id = $2";

  private static final String SQL_GET_RULES_BY_AUDIENCE =
      "SELECT id, audience_id, x_project_id, name, description, "
          + "EXTRACT(EPOCH FROM start_time)::BIGINT as start_time, EXTRACT(EPOCH FROM end_time)::BIGINT as end_time, "
          + "rule_action, rule_type, status, configuration, "
          + "created_by, EXTRACT(EPOCH FROM created_at)::BIGINT as created_at, "
          + "EXTRACT(EPOCH FROM updated_at)::BIGINT as updated_at "
          + "FROM rules WHERE audience_id = $1 AND x_project_id = $2 ORDER BY created_at DESC";

  private static final String SQL_FIND_SCHEDULED_READY_WITH_SINK_IDS =
      "SELECT "
          + "r.id, r.audience_id, r.x_project_id, r.name, r.description, "
          + "EXTRACT(EPOCH FROM r.start_time)::BIGINT as start_time, "
          + "EXTRACT(EPOCH FROM r.end_time)::BIGINT as end_time, "
          + "r.rule_action, r.rule_type, r.status, r.configuration, "
          + "r.created_by, "
          + "EXTRACT(EPOCH FROM r.created_at)::BIGINT as created_at, "
          + "EXTRACT(EPOCH FROM r.updated_at)::BIGINT as updated_at, "
          + "a.name as audience_name, "
          + "a.sinks as sink_ids "
          + "FROM rules r "
          + "INNER JOIN audiences a ON r.audience_id = a.id "
          + "WHERE r.status = 'SCHEDULED' "
          + "AND r.start_time <= NOW() "
          + "AND r.end_time > NOW()";

  private static final String SQL_UPDATE_STATUS_IF_CURRENT =
      "UPDATE rules SET status = $1, updated_at = NOW() WHERE id = $2 AND status = $3";

  @Override
  public Single<Boolean> createRules(List<RuleMeta<SourceInfo>> ruleMetas) {
    List<Tuple> batchParams = new ArrayList<>(ruleMetas.size());
    for (RuleMeta<SourceInfo> ruleMeta : ruleMetas) {
      Tuple params =
          Tuple.tuple()
              .addValue(ruleMeta.getAudienceId())
              .addValue(ruleMeta.getXProjectId())
              .addValue(ruleMeta.getName())
              .addValue(ruleMeta.getDescription())
              .addValue(ruleMeta.getStartTime())
              .addValue(ruleMeta.getEndTime())
              .addValue(ruleMeta.getRuleAction())
              .addValue(ruleMeta.getRuleType())
              .addValue(RuleHelpers.serializeRuleConfiguration(ruleMeta.getConfiguration()))
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
  public Single<RuleMeta<SourceInfo>> getRuleById(String xProjectId, Long ruleId) {
    return postgresReaderClient.fetchOne(
        SQL_GET_RULE_BY_ID, Tuple.of(ruleId, xProjectId), RuleHelpers::mapRuleRow);
  }

  @Override
  public Single<List<RuleMeta<SourceInfo>>> getRulesByAudienceId(
      String xProjectId, Long audienceId) {
    return postgresReaderClient.fetchAll(
        SQL_GET_RULES_BY_AUDIENCE, Tuple.of(audienceId, xProjectId), RuleHelpers::mapRuleRow);
  }

  @Override
  public Single<List<RuleMetaVerbose<SourceInfo, SinkInfo>>> findScheduledRulesReadyWithSinkIds() {
    return postgresReaderClient.fetchAll(
        SQL_FIND_SCHEDULED_READY_WITH_SINK_IDS, Tuple.tuple(), RuleHelpers::mapRuleRowWithSinkIds);
  }

  @Override
  public Single<Boolean> updateRuleStatus(
      Long ruleId, RuleStatus newStatus, RuleStatus currentStatus) {
    return postgresWriterClient
        .getConnection()
        .flatMap(
            conn ->
                postgresWriterClient
                    .execute(
                        conn,
                        SQL_UPDATE_STATUS_IF_CURRENT,
                        Tuple.of(newStatus, ruleId, currentStatus))
                    .doFinally(conn::close));
  }
}

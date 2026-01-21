package io.ascend.flockr.admin.repository.impl;

import com.google.inject.Inject;
import io.ascend.flockr.admin.client.postgres.PostgresReaderClient;
import io.ascend.flockr.admin.client.postgres.PostgresWriterClient;
import io.ascend.flockr.admin.domain.rule.JobStatus;
import io.ascend.flockr.admin.domain.rule.JobType;
import io.ascend.flockr.admin.domain.rule.RuleExecution;
import io.ascend.flockr.admin.domain.rule.RuleStatus;
import io.ascend.flockr.admin.repository.RuleExecutionRepository;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.Tuple;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PostgreSQL implementation of {@link RuleExecutionRepository}.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class RuleExecutionRepositoryImpl implements RuleExecutionRepository {

  private final PostgresWriterClient postgresWriterClient;
  private final PostgresReaderClient postgresReaderClient;

  private static final String SQL_CREATE =
      "INSERT INTO rule_execution (rule_id, type, status, metadata, created_by) "
          + "VALUES ($1, $2, $3, CAST($4 as JSONB), $5) RETURNING id";

  private static final String SQL_UPDATE_STATUS_AND_REF =
      "UPDATE rule_execution SET status = $1, external_job_id = $2, updated_at = NOW() "
          + "WHERE id = $3";

  private static final String SQL_UPDATE_EXECUTION_DETAILS =
      "UPDATE rule_execution SET name = $1, external_job_id = $2, status = $3, "
          + "metadata = $4, updated_at = NOW() "
          + "WHERE id = $5";

  private static final String SQL_UPDATE_RULE_STATUS =
      "UPDATE rules SET status = $1, updated_at = NOW() WHERE id = $2";

  private static final String SQL_UPDATE_RULE_STATUS_IF_CURRENT =
      "UPDATE rules SET status = $1, updated_at = NOW() WHERE id = $2 AND status = $3";

  private static final String SQL_FIND_STALE_FOR_RECONCILIATION =
      "SELECT id, name, rule_id, type, status, metadata, external_job_id, "
          + "created_by, created_at, updated_at "
          + "FROM rule_execution "
          + "WHERE status = ANY($1) "
          + "AND updated_at < NOW() - ($2 * INTERVAL '1 minute') "
          + "ORDER BY updated_at ASC "
          + "LIMIT 100";

  private static final String SQL_BATCH_UPDATE_STATUS =
      "UPDATE rule_execution SET status = $1, updated_at = NOW() WHERE id = $2";

  @Override
  public Completable updateStatusAndExternalJobId(
      Long executionId, JobStatus status, String externalJobId, Instant startedAt) {
    return postgresWriterClient
        .getConnection()
        .flatMapCompletable(
            conn ->
                conn.preparedQuery(SQL_UPDATE_STATUS_AND_REF)
                    .rxExecute(Tuple.of(status.name(), externalJobId, executionId))
                    .ignoreElement()
                    .doFinally(conn::close));
  }

  @Override
  public Completable markFailed(Long executionId, String errorMessage) {
    return postgresWriterClient
        .getConnection()
        .flatMapCompletable(
            conn ->
                conn.preparedQuery(SQL_UPDATE_STATUS_AND_REF)
                    .rxExecute(Tuple.of(JobStatus.FAILED.name(), null, executionId))
                    .ignoreElement()
                    .doFinally(conn::close));
  }

  /**
   * Maps a database row to a RuleExecution entity.
   *
   * @param row the database row
   * @return RuleExecution entity
   */
  private RuleExecution mapRow(Row row) {
    return RuleExecution.builder()
        .executionId(row.getLong("id"))
        .jobName(row.getString("name"))
        .ruleId(row.getLong("rule_id"))
        .executionType(JobType.valueOf(row.getString("type")))
        .status(JobStatus.valueOf(row.getString("status")))
        .metadata(row.getJsonObject("metadata"))
        .externalJobId(row.getString("external_job_id"))
        .createdBy(row.getString("created_by"))
        .createdAt(toInstant(row.getLocalDateTime("created_at")))
        .updatedAt(toInstant(row.getLocalDateTime("updated_at")))
        .build();
  }

  private Instant toInstant(LocalDateTime ldt) {
    return ldt != null ? ldt.toInstant(ZoneOffset.UTC) : null;
  }

  @Override
  public Single<Long> createPendingExecutionAndUpdateRuleStatus(
      RuleExecution ruleExecution, Long ruleId, RuleStatus currentStatus, RuleStatus newStatus) {

    Tuple createParams =
        Tuple.tuple()
            .addLong(ruleExecution.getRuleId())
            .addString(ruleExecution.getExecutionType().name())
            .addString(ruleExecution.getStatus().name())
            .addJsonObject(ruleExecution.getMetadata())
            .addString(ruleExecution.getCreatedBy());

    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                // First: Create execution log
                conn.preparedQuery(SQL_CREATE)
                    .rxExecute(createParams)
                    .map(rows -> rows.iterator().next().getLong("id"))
                    .flatMap(
                        executionId ->
                            // Second: Update rule status (with optimistic lock)
                            conn.preparedQuery(SQL_UPDATE_RULE_STATUS_IF_CURRENT)
                                .rxExecute(Tuple.of(newStatus.name(), ruleId, currentStatus.name()))
                                .map(
                                    updateResult -> {
                                      if (updateResult.rowCount() == 0) {
                                        throw new IllegalStateException(
                                            "Rule "
                                                + ruleId
                                                + " status was not "
                                                + currentStatus
                                                + ", possible concurrent modification");
                                      }
                                      return executionId;
                                    }))
                    .toMaybe())
        .switchIfEmpty(
            Maybe.error(
                new IllegalStateException(
                    "Failed to create execution and update rule status atomically")))
        .toSingle();
  }

  @Override
  public Single<Long> updateExecutionDetailsAndRuleStatus(
      Long executionId,
      String jobName,
      String externalJobId,
      JobStatus jobStatus,
      JsonObject metadata,
      Long ruleId,
      RuleStatus newRuleStatus) {

    Tuple executionParams =
        Tuple.tuple()
            .addString(jobName)
            .addString(externalJobId)
            .addString(jobStatus.name())
            .addJsonObject(metadata)
            .addLong(executionId);

    Tuple ruleParams = Tuple.tuple().addString(newRuleStatus.name()).addLong(ruleId);

    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                conn.preparedQuery(SQL_UPDATE_EXECUTION_DETAILS)
                    .rxExecute(executionParams)
                    .map(
                        executionResult -> {
                          if (executionResult.rowCount() == 0) {
                            log.warn("Execution {} not found for update", executionId);
                            throw new IllegalStateException(
                                "Execution " + executionId + " not found");
                          }
                          return executionResult;
                        })
                    .flatMap(
                        executionResult ->
                            conn.preparedQuery(SQL_UPDATE_RULE_STATUS)
                                .rxExecute(ruleParams)
                                .map(ruleResult -> executionId))
                    .toMaybe())
        .doOnError(
            e ->
                log.error(
                    "Failed to update execution {} and rule {} status in transaction",
                    executionId,
                    ruleId,
                    e))
        .toSingle();
  }

  @Override
  public Single<List<RuleExecution>> findStaleExecutionsForReconciliation(
      int thresholdMinutes, List<JobStatus> statuses) {
    String[] statusArray = statuses.stream().map(JobStatus::name).toArray(String[]::new);
    Tuple sqlParams = Tuple.of(statusArray, thresholdMinutes);

    return postgresWriterClient
        .getConnection()
        .flatMap(
            conn ->
                conn.preparedQuery(SQL_FIND_STALE_FOR_RECONCILIATION)
                    .rxExecute(sqlParams)
                    .map(
                        rows -> {
                          List<RuleExecution> result = new ArrayList<>();
                          rows.forEach(row -> result.add(mapRow(row)));
                          return result;
                        })
                    .doFinally(conn::close));
  }

  @Override
  public Completable batchUpdateStatus(List<Long> executionIds, JobStatus status) {
    List<Tuple> batch = executionIds.stream().map(id -> Tuple.of(status.name(), id)).toList();
    return postgresWriterClient
        .getConnection()
        .flatMapCompletable(
            conn ->
                conn.preparedQuery(SQL_BATCH_UPDATE_STATUS)
                    .rxExecuteBatch(batch)
                    .ignoreElement()
                    .doFinally(conn::close));
  }
}

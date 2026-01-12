package io.ascend.flockr.admin.repository.impl;

import com.google.inject.Inject;
import io.ascend.flockr.admin.client.postgres.PostgresReaderClient;
import io.ascend.flockr.admin.client.postgres.PostgresWriterClient;
import io.ascend.flockr.admin.domain.rule.JobStatus;
import io.ascend.flockr.admin.domain.rule.JobType;
import io.ascend.flockr.admin.domain.rule.ReconciliationMatch;
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

  private static final String SQL_FIND_STALE_SUBMITTING =
      "SELECT id, name, rule_id, type, status, metadata, external_job_id, "
          + "created_by, created_at, updated_at "
          + "FROM rule_execution "
          + "WHERE status = 'SUBMITTING' "
          //          + "AND updated_at < NOW() - INTERVAL '%d minutes' "
          + "ORDER BY updated_at ASC"
          + " LIMIT 100";

  private static final String SQL_CLAIM_STALE_EXECUTIONS =
      "UPDATE rule_execution SET updated_at = NOW() + INTERVAL '1 hour' "
          + "WHERE id = ANY($1) RETURNING id";

  /** SQL for batch update using UNNEST for efficient multi-row update. */
  private static final String SQL_BATCH_UPDATE_STATUS_AND_REF =
      "UPDATE rule_execution AS re SET "
          + "status = $1, "
          + "external_job_id = u.external_job_id, "
          + "started_at = u.started_at, "
          + "updated_at = NOW() "
          + "FROM (SELECT UNNEST($2::bigint[]) AS id, "
          + "             UNNEST($3::text[]) AS external_job_id, "
          + "             UNNEST($4::timestamp[]) AS started_at) AS u "
          + "WHERE re.id = u.id";

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
  public Single<List<RuleExecution>> findStaleSubmittingExecutions(int thresholdMinutes) {
    String sql = String.format(SQL_FIND_STALE_SUBMITTING, thresholdMinutes);
    log.debug(
        "Finding stale SUBMITTING executions older than {} minutes (limit 100)", thresholdMinutes);

    return postgresReaderClient.fetchAll(sql, this::mapRow);
  }

  @Override
  public Single<List<Long>> claimStaleExecutions(List<Long> executionIds) {
    if (executionIds.isEmpty()) {
      return Single.just(List.of());
    }

    Long[] ids = executionIds.toArray(new Long[0]);
    return postgresWriterClient
        .getConnection()
        .flatMap(
            conn ->
                conn.preparedQuery(SQL_CLAIM_STALE_EXECUTIONS)
                    .rxExecute(Tuple.of((Object) ids))
                    .map(
                        rows -> {
                          List<Long> claimedIds = new java.util.ArrayList<>();
                          rows.forEach(row -> claimedIds.add(row.getLong("id")));
                          return claimedIds;
                        })
                    .doFinally(conn::close));
  }

  /** {@inheritDoc} */
  @Override
  public Completable batchUpdateStatusAndExternalJobId(
      List<ReconciliationMatch> matches, JobStatus status) {

    if (matches.isEmpty()) {
      return Completable.complete();
    }

    Long[] ids = matches.stream().map(ReconciliationMatch::getExecutionId).toArray(Long[]::new);

    String[] externalJobIds =
        matches.stream().map(ReconciliationMatch::getExternalJobId).toArray(String[]::new);

    LocalDateTime[] startedAts =
        matches.stream()
            .map(
                m ->
                    m.getStartedAt() != null
                        ? LocalDateTime.ofInstant(m.getStartedAt(), ZoneOffset.UTC)
                        : null)
            .toArray(LocalDateTime[]::new);

    return postgresWriterClient
        .getConnection()
        .flatMapCompletable(
            conn ->
                conn.preparedQuery(SQL_BATCH_UPDATE_STATUS_AND_REF)
                    .rxExecute(Tuple.of(status.name(), ids, externalJobIds, startedAts))
                    .ignoreElement()
                    .doFinally(conn::close));
  }
}

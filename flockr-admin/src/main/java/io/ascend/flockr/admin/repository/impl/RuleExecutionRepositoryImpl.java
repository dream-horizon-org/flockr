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
import java.util.Arrays;
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
      "INSERT INTO rule_execution (rule_id, sink_ids, job_type, job_status, job_metadata, triggered_by, retries) "
          + "VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING job_id";

  private static final String SQL_UPDATE_STATUS_AND_REF =
      "UPDATE rule_jobs SET job_status = $1, job_ref_id = $2, started_at = $3, updated_at = NOW() "
          + "WHERE job_id = $4";

  private static final String SQL_MARK_FAILED =
      "UPDATE rule_jobs SET job_status = 'FAILED', error_message = $1, updated_at = NOW() "
          + "WHERE job_id = $2";

  private static final String SQL_MARK_COMPLETED =
      "UPDATE rule_jobs SET job_status = 'COMPLETED', completed_at = NOW(), updated_at = NOW() "
          + "WHERE job_id = $1";

  private static final String SQL_FIND_BY_ID =
      "SELECT job_id, rule_id, sink_ids, job_type, job_status, job_metadata, job_ref_id, "
          + "retries, error_message, triggered_by, created_at, updated_at, started_at, completed_at "
          + "FROM rule_jobs WHERE job_id = $1";

  private static final String SQL_FIND_LATEST_BY_RULE =
      "SELECT job_id, rule_id, sink_ids, job_type, job_status, job_metadata, job_ref_id, "
          + "retries, error_message, triggered_by, created_at, updated_at, started_at, completed_at "
          + "FROM rule_jobs WHERE rule_id = $1 ORDER BY created_at DESC LIMIT 1";

  private static final String SQL_UPDATE_RULE_STATUS_IF_CURRENT =
      "UPDATE rules SET status = $1, updated_at = NOW() WHERE id = $2 AND status = $3";

  private static final String SQL_UPDATE_EXECUTION_DETAILS =
      "UPDATE rule_jobs SET job_name = $1, job_ref_id = $2, job_status = $3, "
          + "job_metadata = $4, started_at = NOW(), updated_at = NOW() "
          + "WHERE job_id = $5";

  private static final String SQL_UPDATE_RULE_STATUS =
      "UPDATE rules SET status = $1, updated_at = NOW() WHERE id = $2";

  @Override
  public Single<Long> create(RuleExecution ruleExecution) {
    Long[] sinkIdsArray =
        ruleExecution.getSinkIds() != null
            ? ruleExecution.getSinkIds().toArray(new Long[0])
            : new Long[0];

    Tuple params =
        Tuple.tuple()
            .addLong(ruleExecution.getRuleId())
            .addArrayOfLong(sinkIdsArray)
            .addString(ruleExecution.getExecutionType().name())
            .addString(ruleExecution.getStatus().name())
            .addJsonObject(ruleExecution.getMetadata())
            .addString(ruleExecution.getTriggeredBy())
            .addInteger(ruleExecution.getRetries());

    return postgresWriterClient
        .getConnection()
        .flatMap(
            conn ->
                conn.preparedQuery(SQL_CREATE)
                    .rxExecute(params)
                    .map(rows -> rows.iterator().next().getLong("job_id"))
                    .doFinally(conn::close));
  }

  @Override
  public Completable updateStatusAndExternalJobId(
      Long executionId, JobStatus status, String externalJobId, Instant startedAt) {
    LocalDateTime startedAtLocal =
        startedAt != null ? LocalDateTime.ofInstant(startedAt, ZoneOffset.UTC) : null;

    return postgresWriterClient
        .getConnection()
        .flatMapCompletable(
            conn ->
                conn.preparedQuery(SQL_UPDATE_STATUS_AND_REF)
                    .rxExecute(Tuple.of(status.name(), externalJobId, startedAtLocal, executionId))
                    .ignoreElement()
                    .doFinally(conn::close));
  }

  @Override
  public Completable markFailed(Long executionId, String errorMessage) {
    return postgresWriterClient
        .getConnection()
        .flatMapCompletable(
            conn ->
                conn.preparedQuery(SQL_MARK_FAILED)
                    .rxExecute(Tuple.of(errorMessage, executionId))
                    .ignoreElement()
                    .doFinally(conn::close));
  }

  @Override
  public Completable markCompleted(Long executionId) {
    return postgresWriterClient
        .getConnection()
        .flatMapCompletable(
            conn ->
                conn.preparedQuery(SQL_MARK_COMPLETED)
                    .rxExecute(Tuple.of(executionId))
                    .ignoreElement()
                    .doFinally(conn::close));
  }

  @Override
  public Maybe<RuleExecution> findById(Long executionId) {
    return postgresReaderClient
        .fetchOne(SQL_FIND_BY_ID, Tuple.of(executionId), this::mapRow)
        .flatMapMaybe(execution -> execution != null ? Maybe.just(execution) : Maybe.empty());
  }

  @Override
  public Maybe<RuleExecution> findLatestByRuleId(Long ruleId) {
    return postgresReaderClient
        .fetchOne(SQL_FIND_LATEST_BY_RULE, Tuple.of(ruleId), this::mapRow)
        .flatMapMaybe(execution -> execution != null ? Maybe.just(execution) : Maybe.empty());
  }

  /**
   * Maps a database row to a RuleExecution entity.
   *
   * @param row the database row
   * @return RuleExecution entity
   */
  private RuleExecution mapRow(Row row) {
    Long[] sinkIdsArray = row.getArrayOfLongs("sink_ids");
    List<Long> sinkIds = sinkIdsArray != null ? Arrays.asList(sinkIdsArray) : List.of();

    return RuleExecution.builder()
        .jobName(row.getString("job_name"))
        .executionId(row.getLong("job_id"))
        .ruleId(row.getLong("rule_id"))
        .sinkIds(sinkIds)
        .executionType(JobType.valueOf(row.getString("job_type")))
        .status(JobStatus.valueOf(row.getString("job_status")))
        .metadata(row.getJsonObject("job_metadata"))
        .externalJobId(row.getString("job_ref_id"))
        .retries(row.getInteger("retries"))
        .errorMessage(row.getString("error_message"))
        .triggeredBy(row.getString("triggered_by"))
        .createdAt(toInstant(row.getLocalDateTime("created_at")))
        .updatedAt(toInstant(row.getLocalDateTime("updated_at")))
        .build();
  }

  private Instant toInstant(LocalDateTime ldt) {
    return ldt != null ? ldt.toInstant(ZoneOffset.UTC) : null;
  }

  @Override
  public Single<Long> createAndUpdateRuleStatus(
      RuleExecution ruleExecution, Long ruleId, RuleStatus newStatus, RuleStatus currentStatus) {
    Long[] sinkIdsArray =
        ruleExecution.getSinkIds() != null
            ? ruleExecution.getSinkIds().toArray(new Long[0])
            : new Long[0];

    Tuple createParams =
        Tuple.tuple()
            .addLong(ruleExecution.getRuleId())
            .addArrayOfLong(sinkIdsArray)
            .addString(ruleExecution.getExecutionType().name())
            .addString(ruleExecution.getStatus().name())
            .addJsonObject(ruleExecution.getMetadata())
            .addString(ruleExecution.getTriggeredBy())
            .addInteger(ruleExecution.getRetries());

    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                conn.preparedQuery(SQL_CREATE)
                    .rxExecute(createParams)
                    .map(rows -> rows.iterator().next().getLong("job_id"))
                    .flatMap(
                        executionId ->
                            conn.preparedQuery(SQL_UPDATE_RULE_STATUS_IF_CURRENT)
                                .rxExecute(Tuple.of(newStatus, ruleId, currentStatus))
                                .map(updateResult -> executionId))
                    .toMaybe())
        .switchIfEmpty(
            Maybe.error(
                new IllegalStateException(
                    "Failed to create execution and update rule status in transaction")))
        .toSingle();
  }

  @Override
  public Single<Long> createPendingExecutionAndUpdateRuleStatus(
      RuleExecution ruleExecution, Long ruleId, RuleStatus currentStatus, RuleStatus newStatus) {

    Long[] sinkIdsArray =
        ruleExecution.getSinkIds() != null
            ? ruleExecution.getSinkIds().toArray(new Long[0])
            : new Long[0];

    Tuple createParams =
        Tuple.tuple()
            .addLong(ruleExecution.getRuleId())
            .addArrayOfLong(sinkIdsArray)
            .addString(ruleExecution.getExecutionType().name())
            .addString(ruleExecution.getStatus().name())
            .addJsonObject(ruleExecution.getMetadata())
            .addString(ruleExecution.getTriggeredBy())
            .addInteger(ruleExecution.getRetries());

    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                // First: Create execution log
                conn.preparedQuery(SQL_CREATE)
                    .rxExecute(createParams)
                    .map(rows -> rows.iterator().next().getLong("job_id"))
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
}

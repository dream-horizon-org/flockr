package io.ascend.flockr.admin.repository.impl;

import com.google.inject.Inject;
import io.ascend.flockr.admin.client.postgres.PostgresReaderClient;
import io.ascend.flockr.admin.client.postgres.PostgresWriterClient;
import io.ascend.flockr.admin.domain.rule.JobStatus;
import io.ascend.flockr.admin.domain.rule.JobType;
import io.ascend.flockr.admin.domain.rule.RuleExecution;
import io.ascend.flockr.admin.repository.RuleExecutionRepository;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
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
      "INSERT INTO rule_jobs (rule_id, sink_ids, job_type, job_status, job_metadata, triggered_by, retries) "
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
        .startedAt(toInstant(row.getLocalDateTime("started_at")))
        .completedAt(toInstant(row.getLocalDateTime("completed_at")))
        .build();
  }

  private Instant toInstant(LocalDateTime ldt) {
    return ldt != null ? ldt.toInstant(ZoneOffset.UTC) : null;
  }
}

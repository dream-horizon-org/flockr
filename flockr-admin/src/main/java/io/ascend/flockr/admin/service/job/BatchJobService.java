package io.ascend.flockr.admin.service.job;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ascend.flockr.admin.client.spark.SparkClient;
import io.ascend.flockr.admin.client.spark.dto.request.SparkJobConfig;
import io.ascend.flockr.admin.client.spark.dto.request.SparkJobRequestMapper;
import io.ascend.flockr.admin.client.spark.dto.request.SparkSubmissionRequest;
import io.ascend.flockr.admin.config.SparkConfig;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.DataSourceDetails;
import io.ascend.flockr.admin.domain.rule.BatchConfiguration;
import io.ascend.flockr.admin.domain.rule.JobStatus;
import io.ascend.flockr.admin.domain.rule.JobType;
import io.ascend.flockr.admin.domain.rule.RuleExecution;
import io.ascend.flockr.admin.domain.rule.RuleMeta;
import io.ascend.flockr.admin.domain.rule.RuleStatus;
import io.ascend.flockr.admin.domain.rule.SourceInfo;
import io.ascend.flockr.admin.domain.rule.executionmetadata.BatchExecutionMetadata;
import io.ascend.flockr.admin.domain.rule.executionmetadata.ExecutionMetadataCodec;
import io.ascend.flockr.admin.repository.DataConnectorRepository;
import io.ascend.flockr.admin.repository.RuleExecutionRepository;
import io.ascend.flockr.admin.repository.RuleRepository;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AsyncJobService implementation for BATCH rules using Spark.
 *
 * <p>Handles the FULL execution lifecycle for batch SQL jobs:
 *
 * <ul>
 *   <li>Create execution record
 *   <li>Update rule status
 *   <li>Submit to Spark
 *   <li>Handle errors
 * </ul>
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class BatchJobService implements AsyncJobService {

  private final SparkClient sparkClient;
  private final SparkConfig sparkConfig;
  private final DataConnectorRepository dataConnectorRepository;
  private final RuleExecutionRepository ruleExecutionRepository;
  private final RuleRepository ruleRepository;

  @Override
  public Completable execute(RuleMeta<SourceInfo> rule, String triggeredBy) {
    Long ruleId = rule.getRuleId();
    log.info(
        "Executing BATCH rule: ruleId={}, name={}, triggeredBy={}",
        ruleId,
        rule.getName(),
        triggeredBy);

    // Step 1: Create execution record
    RuleExecution execution = buildExecution(rule, triggeredBy);

    return ruleExecutionRepository
        .create(execution)
        .flatMap(
            executionId -> {
              log.info(
                  "Created execution record: executionId={} for ruleId={}", executionId, ruleId);

              // Step 2: Update rule status to RUNNING
              return ruleRepository
                  .updateStatusIfCurrent(ruleId, RuleStatus.SCHEDULED, RuleStatus.RUNNING)
                  .flatMap(
                      updated -> {
                        if (!updated) {
                          log.warn("Rule {} status was not SCHEDULED, skipping execution", ruleId);
                          return ruleExecutionRepository
                              .markFailed(executionId, "Rule was not in SCHEDULED status")
                              .andThen(Single.just(executionId));
                        }

                        // Step 3: Submit to Spark
                        return submitToSpark(rule, executionId)
                            .onErrorResumeNext(
                                error -> handleSubmissionError(executionId, ruleId, error));
                      });
            })
        .ignoreElement()
        .subscribeOn(Schedulers.io());
  }

  @Override
  public Completable cancelJob(String externalJobId) {
    log.info("Cancelling Spark job: {}", externalJobId);
    return sparkClient
        .cancelJob(externalJobId)
        .doOnComplete(() -> log.info("Successfully cancelled Spark job: {}", externalJobId))
        .doOnError(
            err -> log.error("Failed to cancel Spark job {}: {}", externalJobId, err.getMessage()));
  }

  /**
   * Builds an execution record for the rule.
   *
   * @param rule the rule
   * @param triggeredBy who triggered this execution
   * @return RuleExecution ready to be persisted
   */
  private RuleExecution buildExecution(RuleMeta<SourceInfo> rule, String triggeredBy) {
    List<Long> sinkIds = rule.getSinkIds() != null ? rule.getSinkIds() : List.of();

    // Create typed metadata with submit config
    BatchExecutionMetadata metadata =
        ExecutionMetadataCodec.createBatchMetadata(
            sparkConfig.getDriverMemory(),
            sparkConfig.getExecutorMemory(),
            sparkConfig.getExecutorCores(),
            sparkConfig.getExecutorInstances());

    return RuleExecution.builder()
        .ruleId(rule.getRuleId())
        .sinkIds(sinkIds)
        .executionType(JobType.BATCH)
        .status(JobStatus.SUBMITTED)
        .metadata(ExecutionMetadataCodec.toJson(metadata))
        .triggeredBy(triggeredBy)
        .build();
  }

  /**
   * Submits the rule to Spark for execution.
   *
   * @param rule the rule
   * @param executionId the execution ID
   * @return Single emitting the execution ID on success
   */
  private Single<Long> submitToSpark(RuleMeta<SourceInfo> rule, Long executionId) {
    BatchConfiguration<SourceInfo> batchConfig =
        (BatchConfiguration<SourceInfo>) rule.getConfiguration();

    Long sourceId = batchConfig.getSource().getId();
    List<Long> sinkIds = rule.getSinkIds();

    return fetchDataSource(sourceId)
        .flatMap(
            dataSource ->
                fetchDataSinks(sinkIds)
                    .flatMap(
                        dataSinks -> buildAndSubmitJob(rule, executionId, dataSource, dataSinks)));
  }

  /**
   * Fetches data source details by ID.
   *
   * @param sourceId the data source ID
   * @return Single emitting the DataSourceDetails
   */
  private Single<DataSourceDetails> fetchDataSource(Long sourceId) {
    return dataConnectorRepository
        .getDataSourcesByIds(List.of(sourceId))
        .map(
            sources -> {
              if (sources.isEmpty()) {
                throw new IllegalArgumentException("Data source not found: " + sourceId);
              }
              return sources.get(0);
            });
  }

  /**
   * Fetches data sink details by IDs.
   *
   * @param sinkIds list of sink IDs
   * @return Single emitting list of DataSinkDetails
   */
  private Single<List<DataSinkDetails>> fetchDataSinks(List<Long> sinkIds) {
    if (sinkIds == null || sinkIds.isEmpty()) {
      return Single.just(List.of());
    }
    return dataConnectorRepository.getDataSinksByIds(sinkIds);
  }

  /**
   * Builds the Spark submission request and submits the job.
   *
   * @param rule the rule metadata
   * @param executionId our internal execution ID
   * @param dataSource the data source details
   * @param dataSinks list of data sink details
   * @return Single emitting the execution ID on success
   */
  private Single<Long> buildAndSubmitJob(
      RuleMeta<SourceInfo> rule,
      Long executionId,
      DataSourceDetails dataSource,
      List<DataSinkDetails> dataSinks) {

    SparkJobConfig jobConfig = sparkConfig.toJobConfig();
    SparkSubmissionRequest request =
        SparkJobRequestMapper.buildRequest(rule, dataSource, dataSinks, jobConfig);

    log.debug(
        "Built Spark submission request for rule {} with {} sinks",
        rule.getRuleId(),
        dataSinks.size());

    return sparkClient
        .submitHistoricBatchJob(SparkJobRequestMapper.toJsonObject(request))
        .flatMap(
            response -> {
              String submissionId = response.getSubmissionId();
              Boolean success = response.getSuccess();

              if (Boolean.TRUE.equals(success) && submissionId != null) {
                log.info(
                    "Spark job submitted: ruleId={}, executionId={}, submissionId={}",
                    rule.getRuleId(),
                    executionId,
                    submissionId);

                return ruleExecutionRepository
                    .updateStatusAndExternalJobId(
                        executionId, JobStatus.RUNNING, submissionId, Instant.now())
                    .toSingleDefault(executionId);
              } else {
                String errorMessage =
                    response.getMessage() != null
                        ? response.getMessage()
                        : "Unknown error during job submission";
                log.error(
                    "Spark submission failed: ruleId={}, executionId={}, error={}",
                    rule.getRuleId(),
                    executionId,
                    errorMessage);

                return ruleExecutionRepository
                    .markFailed(executionId, errorMessage)
                    .andThen(
                        Single.error(
                            new RuntimeException("Spark job submission failed: " + errorMessage)));
              }
            });
  }

  /**
   * Handles submission errors by marking execution and rule as failed.
   *
   * @param executionId the execution ID
   * @param ruleId the rule ID
   * @param error the error that occurred
   * @return Single that errors after marking as failed
   */
  private Single<Long> handleSubmissionError(Long executionId, Long ruleId, Throwable error) {
    String errorMessage = error.getMessage() != null ? error.getMessage() : "Unknown error";
    log.error(
        "Submission error for ruleId={}, executionId={}: {}", ruleId, executionId, errorMessage);

    return ruleExecutionRepository
        .markFailed(executionId, errorMessage)
        .andThen(ruleRepository.updateStatus(ruleId, RuleStatus.FAILED))
        .andThen(Single.error(error));
  }
}

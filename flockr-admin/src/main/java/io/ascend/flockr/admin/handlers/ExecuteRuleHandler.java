package io.ascend.flockr.admin.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.ascend.flockr.admin.config.ApplicationConfig;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.DataSourceDetails;
import io.ascend.flockr.admin.domain.rule.*;
import io.ascend.flockr.admin.repository.DataConnectorRepository;
import io.ascend.flockr.admin.repository.ExecutionSync;
import io.ascend.flockr.admin.repository.RuleExecutionRepository;
import io.ascend.flockr.admin.repository.RuleRepository;
import io.ascend.flockr.admin.service.JobServiceRegistry;
import io.ascend.flockr.admin.util.RuleHelpers;
import io.reactivex.rxjava3.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.Vertx;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler for executing scheduled rules with distributed lease-based synchronization.
 *
 * <p>This handler periodically executes the following workflow:
 *
 * <ol>
 *   <li>Acquires a distributed lease (ensuring only one instance runs across the cluster)
 *   <li>Queries for rules with status=SCHEDULED and start_time <= now and end_time > now
 *   <li>Enriches rules with their associated data source and sink details
 *   <li>Delegates execution to the appropriate job service via {@link JobServiceRegistry}
 *   <li>Creates execution records and updates rule statuses atomically
 * </ol>
 *
 * <ul>
 *   <li>Thin Handler: Only handles scheduling and coordination
 *   <li>Delegation: All execution logic delegated to job services
 *   <li>Resilience: Continues processing even if individual rules fail
 *   <li>Atomicity: Uses database transactions for status updates
 * </ul>
 *
 * <p><b>Error Handling:</b> Individual rule failures do not stop the entire batch. Failed
 * submissions remain in SUBMITTING state for reconciliation by a separate process.
 *
 * @author Prithu Sharma
 * @since 1.0
 * @see JobServiceRegistry
 * @see AbstractHandler
 */
@Slf4j
public non-sealed class ExecuteRuleHandler extends AbstractHandler {

  /** Identifier for the trigger source in execution records */
  private static final String TRIGGER_SOURCE = "executeRuleHandler";

  private final ObjectMapper objectMapper;
  private final JobServiceRegistry jobServiceRegistry;
  private final RuleRepository ruleRepository;
  private final RuleExecutionRepository ruleExecutionRepository;
  private final DataConnectorRepository dataConnectorRepository;

  /** Maps job submission status to corresponding rule status */
  private final Map<JobStatus, RuleStatus> jobStatusRuleStatusMap =
      Map.of(
          JobStatus.SUBMITTED, RuleStatus.RUNNING,
          JobStatus.RUNNING, RuleStatus.RUNNING,
          JobStatus.COMPLETED, RuleStatus.COMPLETED,
          JobStatus.FAILED, RuleStatus.FAILED,
          JobStatus.RETRYING, RuleStatus.RETRYING);

  /**
   * Constructs the ExecuteRuleHandler with required dependencies.
   *
   * @param vertx the Vert.x instance for async operations
   * @param config the application configuration
   * @param executionSync the distributed synchronization mechanism
   * @param objectMapper the JSON object mapper
   * @param jobServiceRegistry registry for accessing job services by rule type
   * @param ruleRepository repository for rule operations
   * @param ruleExecutionRepository repository for execution records
   * @param dataConnectorRepository repository for data source/sink details
   */
  @Inject
  public ExecuteRuleHandler(
      Vertx vertx,
      ApplicationConfig config,
      ExecutionSync executionSync,
      ObjectMapper objectMapper,
      JobServiceRegistry jobServiceRegistry,
      RuleRepository ruleRepository,
      RuleExecutionRepository ruleExecutionRepository,
      DataConnectorRepository dataConnectorRepository) {
    super(HandlerState.WAITING_TRIGGER, vertx, executionSync, config.getExecuteRuleHandler());
    this.objectMapper = objectMapper;
    this.jobServiceRegistry = jobServiceRegistry;
    this.ruleRepository = ruleRepository;
    this.ruleExecutionRepository = ruleExecutionRepository;
    this.dataConnectorRepository = dataConnectorRepository;
  }

  /**
   * Handles the scheduled execution trigger event.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>Checks if the handler is ready to process (not already running)
   *   <li>Attempts to acquire a distributed lease with TTL
   *   <li>If lease acquired, executes all scheduled rules
   *   <li>Releases the handler state for the next cycle
   * </ol>
   *
   * <p>The lease mechanism ensures only one instance in a cluster processes rules at a time,
   * preventing duplicate executions.
   *
   * @param event the trigger event (typically a timer tick)
   */
  @Override
  public void handle(Long event) {
    if (!checkAndUpdateState()) {
      log.trace("Handler not ready, skipping execution cycle");
      return;
    }

    Duration ttl = Duration.ofSeconds(config.getMinExecutionDelay());

    executionSync
        .acquire(config.getSchedulerKey(), ttl)
        .flatMapSingle(
            acquired -> {
              handlerState = HandlerState.RUNNING;
              log.info(
                  "Acquired lease for key={}, executing scheduled rules...",
                  config.getSchedulerKey());
              return executeScheduledRules();
            })
        .doFinally(
            () -> {
              handlerState = HandlerState.WAITING_TRIGGER;
              log.debug("Handler state reset to WAITING_TRIGGER");
            })
        .subscribe(
            count -> log.info("Scheduled rule execution completed. Executed {} rules", count),
            error -> log.error("Error during scheduled rule execution", error),
            () -> log.debug("No lease acquired, skipping this cycle"));
  }

  /**
   * Executes all scheduled rules that are ready for execution.
   *
   * <p>This method orchestrates the complete rule execution pipeline:
   *
   * <ol>
   *   <li>Fetches scheduled rules from the repository
   *   <li>Extracts source and sink IDs from rules
   *   <li>Fetches source and sink details in parallel (for performance)
   *   <li>Enriches rules with full source/sink details
   *   <li>Executes each enriched rule via job services
   * </ol>
   *
   * <p><b>Performance Optimization:</b> Source and sink details are fetched in parallel to minimize
   * database round trips.
   *
   * <p><b>Error Handling:</b> Individual rule failures don't stop batch processing.
   *
   * @return Single emitting the count of successfully triggered rules
   */
  private Single<Integer> executeScheduledRules() {
    // Fetch all scheduled rules ready for execution
    Single<List<RuleMetaVerbose<SourceInfo, SinkInfo>>> rulesSingle =
        ruleRepository.findScheduledRulesReadyWithSinkIds().cache();

    // Extract all unique source and sink IDs from the fetched rules
    Single<RuleHelpers.SourceAndSinkIds> idsSingle =
        rulesSingle.map(RuleHelpers::extractSourceAndSinkIdsFromRules);

    // Fetch source details in parallel
    Single<List<DataSourceDetails>> sourceDetailsSingle =
        idsSingle
            .map(RuleHelpers.SourceAndSinkIds::sourceIds)
            .flatMap(dataConnectorRepository::getDataSourcesByIds);

    // Fetch sink details in parallel
    Single<List<DataSinkDetails>> sinkDetailsSingle =
        idsSingle
            .map(RuleHelpers.SourceAndSinkIds::sinkIds)
            .flatMap(dataConnectorRepository::getDataSinksByIds);

    // Combine all data streams: enrich rules with fetched details, then execute
    return Single.zip(
            rulesSingle,
            sourceDetailsSingle,
            sinkDetailsSingle,
            this::enrichRulesWithSourcesAndSinks)
        .flatMap(this::executeEnrichedRules);
  }

  /**
   * Executes each enriched rule by delegating to the appropriate job service.
   *
   * <p>This method processes each enriched rule through the following phases:
   *
   * <ol>
   *   <li><b>Prepare:</b> Create pending execution record and update rule status
   *   <li><b>Submit:</b> Submit to external processing engine (Spark/Flink) via {@link
   *       JobServiceRegistry}
   *   <li><b>Record:</b> Log submission details to rule_executions table
   *   <li><b>Update:</b> Update rule status based on job submission result
   * </ol>
   *
   * <p><b>Concurrency:</b> Rules are processed reactively but submissions are serialized to prevent
   * overwhelming external systems.
   *
   * <p><b>Error Handling:</b> Individual failures are logged and counted as 0, allowing the batch
   * to continue processing.
   *
   * @param executableRules the list of enriched rules ready for execution
   * @return Single emitting the count of successfully triggered rules
   */
  private Single<Integer> executeEnrichedRules(
      List<RuleMetaVerbose<SourceInfoEnriched, SinkInfoEnriched>> executableRules) {

    if (executableRules.isEmpty()) {
      log.info("No enriched rules to execute");
      return Single.just(0);
    }

    log.info("Starting execution of {} enriched rules", executableRules.size());

    return Observable.fromIterable(executableRules)
        .concatMapSingle(this::prepareSubmitPhase)
        .reduce(0, Integer::sum)
        .doOnSuccess(
            count ->
                log.info(
                    "Rule execution completed: {}/{} rules successfully triggered",
                    count,
                    executableRules.size()));
  }

  /**
   * Prepares the rule for submission by creating a pending execution record.
   *
   * <p>This phase creates an execution record in SUBMITTING status and atomically updates the rule
   * status from SCHEDULED to SUBMITTING. This ensures:
   *
   * <ul>
   *   <li>Audit trail of execution attempts
   *   <li>Prevention of duplicate submissions
   *   <li>Ability to reconcile stuck submissions
   * </ul>
   *
   * @param ruleMetaVerbose the enriched rule to submit
   * @return Single emitting 1 on success, 0 on failure
   */
  private Single<Integer> prepareSubmitPhase(
      RuleMetaVerbose<SourceInfoEnriched, SinkInfoEnriched> ruleMetaVerbose) {

    RuleExecution pendingExecution = buildPendingExecution(ruleMetaVerbose);

    return ruleExecutionRepository
        .createPendingExecutionAndUpdateRuleStatus(
            pendingExecution,
            ruleMetaVerbose.getRuleId(),
            RuleStatus.SCHEDULED,
            RuleStatus.SUBMITTING)
        .flatMap(executionId -> submitRuleToJobService(ruleMetaVerbose, executionId))
        .onErrorResumeNext(
            error -> {
              log.error(
                  "Failed to create execution for rule {}", ruleMetaVerbose.getRuleId(), error);
              return Single.just(0);
            });
  }

  /**
   * Builds a pending execution record for the given rule.
   *
   * @param ruleMetaVerbose the enriched rule metadata
   * @return a {@link RuleExecution} in SUBMITTING status
   */
  private RuleExecution buildPendingExecution(
      RuleMetaVerbose<SourceInfoEnriched, SinkInfoEnriched> ruleMetaVerbose) {
    return RuleExecution.builder()
        .ruleId(ruleMetaVerbose.getRuleId())
        .sinkIds(ruleMetaVerbose.getSinkList().stream().map(SinkInfoEnriched::getId).toList())
        .executionType(JobType.valueOf(ruleMetaVerbose.getRuleType().name()))
        .status(JobStatus.SUBMITTING)
        .retries(0)
        .triggeredBy(TRIGGER_SOURCE)
        .build();
  }

  /**
   * Submits the rule to the appropriate job service and records the result.
   *
   * <p>This phase:
   *
   * <ol>
   *   <li>Retrieves the appropriate job service from the registry
   *   <li>Submits the rule to the external processing engine
   *   <li>Updates the execution record with job details and status
   *   <li>Maps job status to rule status and updates the rule
   * </ol>
   *
   * <p><b>Error Handling:</b> On failure, the execution remains in SUBMITTING status for
   * reconciliation by a separate process. Timeout handling is delegated to the job service
   * implementation.
   *
   * @param ruleMetaVerbose the enriched rule metadata
   * @param executionId the execution record ID
   * @return Single emitting 1 on success, 0 on failure
   */
  private Single<Integer> submitRuleToJobService(
      RuleMetaVerbose<SourceInfoEnriched, SinkInfoEnriched> ruleMetaVerbose, Long executionId) {

    return jobServiceRegistry
        .get(ruleMetaVerbose.getRuleType())
        .execute(ruleMetaVerbose)
        .flatMap(
            jobSubmissionResult ->
                updateExecutionWithJobDetails(ruleMetaVerbose, executionId, jobSubmissionResult))
        .onErrorResumeNext(
            error -> {
              // Any error from job service or DB update
              // Leave in SUBMITTING for reconciliation
              log.error(
                  "Failed to submit/update job for rule {}, execution {}. Reconciliation will handle.",
                  ruleMetaVerbose.getRuleId(),
                  executionId,
                  error);
              return Single.just(0);
            });
  }

  /**
   * Updates the execution record with job submission details and maps status.
   *
   * @param ruleMetaVerbose the enriched rule metadata
   * @param executionId the execution record ID
   * @param jobSubmissionResult the result from the job service
   * @return Single emitting 1 on success
   */
  private Single<Integer> updateExecutionWithJobDetails(
      RuleMetaVerbose<SourceInfoEnriched, SinkInfoEnriched> ruleMetaVerbose,
      Long executionId,
      JobSubmissionResult jobSubmissionResult) {

    try {
      String metadataJson = objectMapper.writeValueAsString(jobSubmissionResult.getMetadata());

      RuleStatus newRuleStatus =
          jobStatusRuleStatusMap.getOrDefault(
              jobSubmissionResult.getInitialStatus(), RuleStatus.RUNNING);

      return ruleExecutionRepository
          .updateExecutionDetailsAndRuleStatus(
              executionId,
              jobSubmissionResult.getJobName(),
              jobSubmissionResult.getExternalJobId(),
              jobSubmissionResult.getInitialStatus(),
              new JsonObject(metadataJson),
              ruleMetaVerbose.getRuleId(),
              newRuleStatus)
          .map(v -> 1);
    } catch (Exception e) {
      log.error(
          "Failed to serialize metadata for rule {}, execution {}",
          ruleMetaVerbose.getRuleId(),
          executionId,
          e);
      return Single.error(e);
    }
  }

  /**
   * Enriches rules with their associated source and sink details.
   *
   * <p>This method:
   *
   * <ul>
   *   <li>Creates lookup maps for efficient source/sink detail retrieval
   *   <li>Validates that all required sources and sinks exist
   *   <li>Enriches each rule's configuration with full source details
   *   <li>Enriches each rule's sink list with full sink details
   *   <li>Skips rules with missing or invalid data
   * </ul>
   *
   * <p><b>Error Handling:</b> Rules with missing sources or sinks are logged and skipped rather
   * than failing the entire batch.
   *
   * @param rules the list of rules to enrich
   * @param sources the list of all relevant data source details
   * @param sinks the list of all relevant data sink details
   * @return list of successfully enriched rules
   */
  private List<RuleMetaVerbose<SourceInfoEnriched, SinkInfoEnriched>>
      enrichRulesWithSourcesAndSinks(
          List<RuleMetaVerbose<SourceInfo, SinkInfo>> rules,
          List<DataSourceDetails> sources,
          List<DataSinkDetails> sinks) {

    log.debug(
        "Enriching {} rules with {} sources and {} sinks",
        rules.size(),
        sources.size(),
        sinks.size());

    // Build lookup maps for O(1) access
    Map<Long, DataSourceDetails> sourceIdToDetails = buildSourceLookupMap(sources);
    Map<Long, DataSinkDetails> sinkIdToDetails = buildSinkLookupMap(sinks);

    List<RuleMetaVerbose<SourceInfoEnriched, SinkInfoEnriched>> enrichedRules = new ArrayList<>();

    for (RuleMetaVerbose<SourceInfo, SinkInfo> rule : rules) {
      try {
        RuleMetaVerbose<SourceInfoEnriched, SinkInfoEnriched> enrichedRule =
            enrichSingleRule(rule, sourceIdToDetails, sinkIdToDetails);
        enrichedRules.add(enrichedRule);
      } catch (Exception e) {
        log.error(
            "Failed to enrich rule id={}, name={}. Skipping.", rule.getRuleId(), rule.getName(), e);
      }
    }

    log.info("Successfully enriched {} out of {} rules", enrichedRules.size(), rules.size());
    return enrichedRules;
  }

  /**
   * Builds a lookup map from source ID to source details.
   *
   * @param sources the list of data source details
   * @return map of source ID to source details
   */
  private Map<Long, DataSourceDetails> buildSourceLookupMap(List<DataSourceDetails> sources) {
    Map<Long, DataSourceDetails> sourceIdToDetails = new HashMap<>();
    for (DataSourceDetails source : sources) {
      sourceIdToDetails.put(source.getId(), source);
    }
    return sourceIdToDetails;
  }

  /**
   * Builds a lookup map from sink ID to sink details.
   *
   * @param sinks the list of data sink details
   * @return map of sink ID to sink details
   */
  private Map<Long, DataSinkDetails> buildSinkLookupMap(List<DataSinkDetails> sinks) {
    Map<Long, DataSinkDetails> sinkIdToDetails = new HashMap<>();
    for (DataSinkDetails sink : sinks) {
      sinkIdToDetails.put(sink.getId(), sink);
    }
    return sinkIdToDetails;
  }

  /**
   * Enriches a single rule with source and sink details.
   *
   * @param rule the rule to enrich
   * @param sourceIdToDetails lookup map for source details
   * @param sinkIdToDetails lookup map for sink details
   * @return the enriched rule
   * @throws IllegalStateException if required sources or sinks are missing
   */
  private RuleMetaVerbose<SourceInfoEnriched, SinkInfoEnriched> enrichSingleRule(
      RuleMetaVerbose<SourceInfo, SinkInfo> rule,
      Map<Long, DataSourceDetails> sourceIdToDetails,
      Map<Long, DataSinkDetails> sinkIdToDetails) {

    // Validate and enrich sources
    validateRequiredSourcesExist(rule, sourceIdToDetails);
    RuleConfiguration<SourceInfoEnriched> enrichedConfig =
        RuleHelpers.buildEnrichedConfiguration(
            rule.getConfiguration(), sourceIdToDetails, rule.getRuleType());

    // Enrich sinks
    List<SinkInfoEnriched> enrichedSinks = enrichSinks(rule, sinkIdToDetails);

    // Build and return enriched rule
    return buildEnrichedRuleMetaVerbose(rule, enrichedConfig, enrichedSinks);
  }

  /**
   * Validates that all required sources exist in the lookup map.
   *
   * @param rule the rule being validated
   * @param sourceIdToDetails the source lookup map
   * @throws IllegalStateException if any required source is missing
   */
  private void validateRequiredSourcesExist(
      RuleMetaVerbose<SourceInfo, SinkInfo> rule, Map<Long, DataSourceDetails> sourceIdToDetails) {

    List<Long> requiredSourceIds = RuleHelpers.extractSourceIdFromRuleMeta(rule);
    for (Long sourceId : requiredSourceIds) {
      if (!sourceIdToDetails.containsKey(sourceId)) {
        log.error(
            "Missing source details for sourceId={} in rule id={}, name={}. Skipping rule.",
            sourceId,
            rule.getRuleId(),
            rule.getName());
        throw new IllegalStateException("Missing source ID: " + sourceId);
      }
    }
  }

  /**
   * Enriches the sink list for a rule with full sink details.
   *
   * @param rule the rule whose sinks to enrich
   * @param sinkIdToDetails the sink lookup map
   * @return list of enriched sink information
   * @throws IllegalStateException if any required sink is missing
   */
  private List<SinkInfoEnriched> enrichSinks(
      RuleMetaVerbose<SourceInfo, SinkInfo> rule, Map<Long, DataSinkDetails> sinkIdToDetails) {

    List<SinkInfoEnriched> enrichedSinks = new ArrayList<>();
    if (rule.getSinkList() != null) {
      for (SinkInfo sinkInfo : rule.getSinkList()) {
        DataSinkDetails sinkDetails = sinkIdToDetails.get(sinkInfo.getId());
        if (sinkDetails == null) {
          log.error(
              "Missing sink details for sinkId={} in rule id={}, name={}. Skipping rule.",
              sinkInfo.getId(),
              rule.getRuleId(),
              rule.getName());
          throw new IllegalStateException("Missing sink ID: " + sinkInfo.getId());
        }
        SinkInfoEnriched enrichedSink =
            SinkInfoEnriched.builder().id(sinkInfo.getId()).details(sinkDetails).build();
        enrichedSinks.add(enrichedSink);
      }
    }
    return enrichedSinks;
  }

  /**
   * Builds an enriched rule metadata object from the original rule and enriched components.
   *
   * <p>This method performs a field-by-field copy of metadata while replacing the configuration and
   * sink list with their enriched versions. This preserves all original rule metadata (IDs,
   * timestamps, status, etc.) while upgrading the source and sink information to include full
   * details.
   *
   * @param rule the original rule with basic source/sink references
   * @param enrichedConfig the configuration with full source details
   * @param enrichedSinks the list of sinks with full details
   * @return a new rule metadata object with enriched source and sink information
   */
  private static RuleMetaVerbose<SourceInfoEnriched, SinkInfoEnriched> buildEnrichedRuleMetaVerbose(
      RuleMetaVerbose<SourceInfo, SinkInfo> rule,
      RuleConfiguration<SourceInfoEnriched> enrichedConfig,
      List<SinkInfoEnriched> enrichedSinks) {

    RuleMetaVerbose<SourceInfoEnriched, SinkInfoEnriched> enrichedRule = new RuleMetaVerbose<>();

    // Copy identity and metadata fields
    enrichedRule.setXProjectId(rule.getXProjectId());
    enrichedRule.setRuleId(rule.getRuleId());
    enrichedRule.setAudienceId(rule.getAudienceId());
    enrichedRule.setName(rule.getName());
    enrichedRule.setDescription(rule.getDescription());

    // Copy scheduling information
    enrichedRule.setStartTime(rule.getStartTime());
    enrichedRule.setEndTime(rule.getEndTime());

    // Copy rule behavior configuration
    enrichedRule.setRuleAction(rule.getRuleAction());
    enrichedRule.setRuleType(rule.getRuleType());
    enrichedRule.setStatus(rule.getStatus());

    // Set enriched configuration and sinks
    enrichedRule.setConfiguration(enrichedConfig);
    enrichedRule.setSinkList(enrichedSinks);

    // Copy audit fields
    enrichedRule.setCreatedBy(rule.getCreatedBy());
    enrichedRule.setCreatedAt(rule.getCreatedAt());
    enrichedRule.setUpdatedAt(rule.getUpdatedAt());

    // Copy denormalized fields
    enrichedRule.setAudienceName(rule.getAudienceName());

    return enrichedRule;
  }
}

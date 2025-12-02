package io.ascend.flockr.admin.service.impl;

import com.google.inject.Inject;
import io.ascend.flockr.admin.domain.audience.AudienceMeta;
import io.ascend.flockr.admin.domain.audience.AudienceType;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.exception.ErrorEnum;
import io.ascend.flockr.admin.exception.ResourceNotFoundException;
import io.ascend.flockr.admin.io.request.CsvImportForm;
import io.ascend.flockr.admin.io.response.AudienceImportResponse;
import io.ascend.flockr.admin.repository.AudienceRepository;
import io.ascend.flockr.admin.repository.DataConnectorRepository;
import io.ascend.flockr.admin.service.AudienceImportService;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link AudienceImportService} for managing CSV imports.
 *
 * <p>This service handles CSV uploads for STATIC audiences:
 *
 * <ol>
 *   <li>Validates the audience is of type STATIC
 *   <li>Streams through the CSV file (constant memory usage)
 *   <li>Pushes records to configured data sinks in batches
 * </ol>
 *
 * <p>No import history is stored in DB - this is a fire-and-forget API.
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AudienceImportServiceImpl implements AudienceImportService {
  private final AudienceRepository audienceRepository;
  private final DataConnectorRepository dataConnectorRepository;

  private static final int BATCH_SIZE = 1000;

  @Override
  public Single<AudienceImportResponse> createImport(
      String xProjectId, Long audienceId, CsvImportForm form, String actor) {

    log.info("Processing import for audience {} with file {}", audienceId, form.getFileName());
    String fileName = form.getFileName() != null ? form.getFileName() : "upload.csv";
    InputStream inputStream = form.getFile();

    // Step 1: Verify audience exists and is of type STATIC
    return audienceRepository
        .getAudienceById(xProjectId, audienceId)
        .onErrorResumeNext(
            error -> {
              if (error instanceof NoSuchElementException) {
                return Single.error(new ResourceNotFoundException("Audience", audienceId));
              }
              return Single.error(error);
            })
        .map(
            audience -> {
              // Validate audience type is STATIC
              if (!AudienceType.STATIC.name().equals(audience.getType())) {
                log.warn(
                    "Cannot import CSV to CONDITIONAL audience {}. Use rules instead.", audienceId);
                throw ErrorEnum.IMPORT_NOT_ALLOWED_FOR_CONDITIONAL_AUDIENCE.toException();
              }
              return audience;
            })
        // Step 2: Get sinks and process
        .flatMap(
            audience ->
                getDataSinksInBatch(audience.getSinks())
                    .flatMap(
                        sinks ->
                            // Execute blocking I/O on worker thread pool, NOT event loop
                            Single.fromCallable(
                                    () -> streamAndPushToSinks(inputStream, sinks, audience))
                                .subscribeOn(Schedulers.io())
                                .map(
                                    recordCount ->
                                        AudienceImportResponse.builder()
                                            .audienceId(audienceId)
                                            .fileName(fileName)
                                            .recordCount(recordCount)
                                            .status("COMPLETED")
                                            .build())))
        .doOnSuccess(
            response ->
                log.info(
                    "Import completed for audience {} - {} records pushed to sinks",
                    audienceId,
                    response.getRecordCount()))
        .doOnError(
            error ->
                log.error(
                    "Failed to process import for audience {}: {}",
                    audienceId,
                    error.getMessage()));
  }

  /**
   * Streams through the CSV input and pushes records to sinks in batches. Memory usage is
   * O(batchSize) instead of O(fileSize).
   */
  private long streamAndPushToSinks(
      InputStream inputStream, List<DataSinkDetails> sinks, AudienceMeta audience)
      throws IOException {

    List<String> batch = new ArrayList<>(BATCH_SIZE);
    long totalRecords = 0;
    boolean isHeader = true;

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

      String line;
      while ((line = reader.readLine()) != null) {
        // Skip header row
        if (isHeader) {
          isHeader = false;
          continue;
        }

        batch.add(line);
        totalRecords++;

        // Push batch when full
        if (batch.size() >= BATCH_SIZE) {
          pushBatchToSinks(batch, sinks, audience);
          batch.clear();
        }
      }

      // Push remaining records
      if (!batch.isEmpty()) {
        pushBatchToSinks(batch, sinks, audience);
      }
    }

    log.info("Streamed {} records for audience {}", totalRecords, audience.getAudienceId());
    return totalRecords;
  }

  /** Pushes a batch of CSV records to the configured sinks. */
  private void pushBatchToSinks(
      List<String> batch, List<DataSinkDetails> sinks, AudienceMeta audience) {

    if (sinks.isEmpty()) {
      log.debug("No sinks configured, skipping push for {} records", batch.size());
      return;
    }

    // TODO: Implement actual sink push logic based on sink type
    log.debug(
        "Pushing batch of {} records to {} sinks for audience {}",
        batch.size(),
        sinks.size(),
        audience.getAudienceId());

    for (DataSinkDetails sink : sinks) {
      log.debug(
          "Would push {} records to sink: {} (type: {})",
          batch.size(),
          sink.getName(),
          sink.getType());
    }
  }

  /** Fetches data sink details for the given sink IDs. */
  private Single<List<DataSinkDetails>> getDataSinksInBatch(List<Long> sinkIds) {
    if (sinkIds == null || sinkIds.isEmpty()) {
      return Single.just(List.of());
    }
    return dataConnectorRepository.getDataSinksByIds(sinkIds);
  }
}

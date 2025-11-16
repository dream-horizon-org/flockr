package com.ascend.flockr.users.service.impl;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;
import com.ascend.flockr.common.client.Aerospike;
import com.ascend.flockr.common.config.AerospikeConfig;
import com.ascend.flockr.common.constants.Constants;
import com.ascend.flockr.common.exception.errors.DefinedErrors;
import com.ascend.flockr.common.utils.CommonUtils;
import com.ascend.flockr.users.constants.BulkCohortAssignmentConstants;
import com.ascend.flockr.users.dto.BulkOperationResult;
import com.ascend.flockr.users.dto.request.MapUserCohortsRequest;
import com.ascend.flockr.users.service.UserCohortsService;
import com.dream11.rest.exception.RestException;
import com.dream11.rest.util.ExceptionUtil;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.rxjava3.core.Vertx;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;

/**
 * Implementation of {@link UserCohortsService} for managing user cohort assignments.
 *
 * <p>This implementation uses Aerospike as the backend storage and processes bulk operations with
 * streaming and concurrent batch processing for efficiency.
 *
 * @since 1.0
 */
@Slf4j
public class UserCohortServiceImpl implements UserCohortsService {
  private final Aerospike aerospikeClient;

  private final AerospikeConfig aerospikeConfig;
  private final Vertx vertx;

  /**
   * Constructs a new UserCohortServiceImpl.
   *
   * @param aerospikeClient the Aerospike client for database operations
   * @param aerospikeConfig the Aerospike configuration
   * @param vertx the Vert.x instance for async file operations
   */
  public UserCohortServiceImpl(
      Aerospike aerospikeClient, AerospikeConfig aerospikeConfig, Vertx vertx) {
    this.aerospikeClient = aerospikeClient;
    this.aerospikeConfig = aerospikeConfig;
    this.vertx = vertx;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Implementation retrieves cohort data from Aerospike and filters for active cohorts (those
   * with expiry time greater than current time).
   */
  @Override
  public Single<List<String>> getCohorts(Long userId, String guestId, Long projectId) {
    String userKey = CommonUtils.getUserKey(userId, guestId);
    String setName = String.valueOf(projectId);
    return aerospikeClient.getCohortExpiryBin(userKey, setName).map(this::getActiveCohortsFromMap);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Implementation handles both append and remove actions. For append operations, validates
   * expiry time. Returns {@code false} if Aerospike key is not found.
   */
  @Override
  public Single<Boolean> mapUserCohorts(MapUserCohortsRequest request) {
    String userKey = CommonUtils.getUserKey(request.getUserId(), request.getGuestId());
    String source = request.getSource();
    String setName = request.getProjectId() != null ? String.valueOf(request.getProjectId()) : null;

    Single<Boolean> single;
    try {
      if (request.getAction().equals(Constants.ACTION_APPEND)) {
        Long cohortExpiry = request.expiryEpochFromExpireAt();
        single =
            aerospikeClient.appendCohort(
                userKey, request.getCohortKey(), source, cohortExpiry, setName);
      } else {
        single = aerospikeClient.removeCohort(userKey, request.getCohortKey(), source, setName);
      }
    } catch (Exception e) {
      single = Single.error(e);
    }
    return single.onErrorResumeNext(
        throwable -> {
          if (throwable instanceof AerospikeException aerospikeException
              && aerospikeException.getResultCode() == ResultCode.KEY_NOT_FOUND_ERROR) {
            return Single.just(false);
          } else if (throwable instanceof RestException) {
            log.error("Invalid expiryAt: {}", throwable.getMessage());
            return Single.error(
                ExceptionUtil.getException(
                    DefinedErrors.INVALID_EXPIRY_TIME, throwable.getMessage()));
          } else {
            log.error("An internal server error occurred: {}", throwable.getMessage(), throwable);
            return Single.error(
                ExceptionUtil.getException(
                    DefinedErrors.INTERNAL_SERVER_ERROR, throwable.getMessage()));
          }
        });
  }

  /**
   * Filters cohort map to return only active cohorts (those not expired).
   *
   * @param cohortMap map of cohort names to expiry timestamps
   * @return list of active cohort names
   */
  private List<String> getActiveCohortsFromMap(Map<String, Long> cohortMap) {
    Long currentTime = System.currentTimeMillis();

    return cohortMap.entrySet().stream()
        .filter(cohortEntry -> cohortEntry.getValue() >= currentTime)
        .map(Map.Entry::getKey)
        .toList();
  }

  /**
   * {@inheritDoc}
   *
   * <p>Implementation saves the CSV file to disk temporarily, then processes it with streaming to
   * avoid loading entire file into memory. The temp file is automatically cleaned up after
   * processing.
   */
  @Override
  public Single<BulkOperationResult> assignUsersToCohort(String cohortName, InputPart csvFilePart) {
    return Single.fromCallable(() -> persistCsvToTempFile(csvFilePart))
        .flatMap(tempPath -> processCsvAndAssign(tempPath, cohortName));
  }

  /**
   * Saves the uploaded CSV file to a temporary location on disk.
   *
   * @param csvFilePart the multipart file part containing CSV data
   * @return path to the temporary file
   * @throws Exception if file is empty or cannot be written
   */
  private Path persistCsvToTempFile(InputPart csvFilePart) throws Exception {
    byte[] payload = csvFilePart.getBody(byte[].class, null);
    if (payload == null || payload.length == 0) {
      throw ExceptionUtil.getException(
          DefinedErrors.INVALID_REQUEST_PARAMS, "Uploaded CSV is empty");
    }
    Path tempFile = Files.createTempFile("cohort-upload-", ".csv");
    Files.write(tempFile, payload);
    return tempFile;
  }

  /**
   * Processes CSV file and assigns users to cohort with streaming and concurrent batch processing.
   *
   * <p>This method:
   *
   * <ul>
   *   <li>Streams CSV file line by line without loading entire file into memory
   *   <li>Validates UUID format and deduplicates user IDs
   *   <li>Processes users in batches with bounded concurrency
   *   <li>Retries failed operations with exponential backoff
   *   <li>Returns statistics about successful and failed assignments
   * </ul>
   *
   * @param csvFile path to the CSV file on disk
   * @param cohortName name of the cohort to assign users to
   * @return Single emitting bulk operation result
   */
  public Single<BulkOperationResult> processCsvAndAssign(Path csvFile, String cohortName) {
    final AtomicInteger total = new AtomicInteger();
    final AtomicInteger success = new AtomicInteger();
    final AtomicInteger failed = new AtomicInteger();

    return openFileAsFlowable(csvFile)
        .map(String::trim)
        .filter(line -> !line.isEmpty())
        .flatMap(line -> Flowable.fromArray(line.split(",")))
        .map(String::trim)
        .filter(id -> !id.isEmpty())
        .filter(UserCohortServiceImpl::isValidUuid)
        .distinct()
        .doOnNext(id -> total.incrementAndGet())
        .buffer(BulkCohortAssignmentConstants.BATCH_SIZE)
        .flatMap(
            batch ->
                Flowable.fromIterable(batch)
                    .parallel()
                    .runOn(Schedulers.io())
                    .flatMap(
                        userId ->
                            assignSingleUser(userId, cohortName)
                                .retryWhen(errors -> applyRetryPolicy(errors, userId, cohortName))
                                .doOnSuccess(
                                    ok -> {
                                      if (ok) success.incrementAndGet();
                                      else failed.incrementAndGet();
                                    })
                                .toFlowable())
                    .sequential(),
            BulkCohortAssignmentConstants.MAX_CONCURRENCY)
        .ignoreElements()
        .andThen(
            Single.fromCallable(
                () -> {
                  int totalProcessed = total.get();
                  int successes = success.get();
                  int failures = failed.get();
                  log.info(
                      "Bulk cohort assignment completed. cohort={}, processed={}, success={}, failed={}",
                      cohortName,
                      totalProcessed,
                      successes,
                      failures);
                  return new BulkOperationResult(
                      totalProcessed,
                      successes,
                      failures,
                      "Processed %d users for cohort '%s'. Success: %d, Failed: %d"
                          .formatted(totalProcessed, cohortName, successes, failures));
                }))
        .doFinally(() -> deleteQuietly(csvFile));
  }

  /**
   * Opens a file and creates a Flowable that streams lines asynchronously.
   *
   * <p>Uses Vert.x async file operations to read file line by line without blocking the event loop
   * or loading entire file into memory.
   *
   * @param path path to the file to read
   * @return Flowable emitting file lines as strings
   */
  private Flowable<String> openFileAsFlowable(Path path) {
    return Flowable.create(
        emitter ->
            vertx
                .fileSystem()
                .open(path.toString(), new OpenOptions().setRead(true))
                .subscribe(
                    asyncFile -> {
                      // Set buffer size for efficient reading
                      asyncFile.setReadBufferSize(64 * 1024);

                      // Create parser to read lines (newline-delimited)
                      RecordParser parser =
                          RecordParser.newDelimited(
                              "\n",
                              buffer -> {
                                String line = buffer.toString(StandardCharsets.UTF_8);
                                if (!line.isEmpty()) {
                                  emitter.onNext(line);
                                }
                              });

                      // Handle end of file
                      parser.endHandler(
                          v -> {
                            asyncFile.close();
                            emitter.onComplete();
                          });

                      // Handle parsing errors
                      parser.exceptionHandler(
                          err -> {
                            asyncFile.close();
                            emitter.onError(err);
                          });

                      // Connect asyncFile to parser: feed data from file to parser
                      // Use lambda to bridge between AsyncFile's Handler and RecordParser
                      asyncFile.handler(buffer -> parser.handle(buffer.getDelegate()));

                      // Handle file read errors
                      asyncFile.exceptionHandler(
                          err -> {
                            asyncFile.close();
                            emitter.onError(err);
                          });

                      // Start reading the file
                      asyncFile.resume();
                    },
                    error -> {
                      // Handle file open errors
                      emitter.onError(error);
                    }),
        BackpressureStrategy.BUFFER);
  }

  /**
   * Assigns a single user to a cohort using Aerospike append operation.
   *
   * <p>Uses default expiry of 1 year from current time. Returns {@code false} if Aerospike key is
   * not found.
   *
   * @param userUuid the user UUID to assign
   * @param cohortName the cohort name to assign user to
   * @return Single emitting {@code true} if assignment succeeded, {@code false} otherwise
   */
  private Single<Boolean> assignSingleUser(String userUuid, String cohortName) {
    String userKey = CommonUtils.getUserKey(null, userUuid);
    long expiry = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365); // 1-year default expiry
    String setName = aerospikeConfig.getNamespace(); // adjust if set differs per project

    return aerospikeClient
        .appendCohort(userKey, cohortName, Constants.SOURCE_DREAM11, expiry, setName)
        .onErrorResumeNext(
            throwable -> {
              if (throwable instanceof AerospikeException ae
                  && ae.getResultCode() == ResultCode.KEY_NOT_FOUND_ERROR) {
                log.warn("Aerospike key not found for user {}", userUuid);
                return Single.just(false);
              }
              return Single.error(throwable);
            });
  }

  /**
   * Applies retry policy with exponential backoff for failed Aerospike operations.
   *
   * <p>Retries up to MAX_RETRIES times with increasing delay between attempts. After max retries,
   * the error is propagated.
   *
   * @param errors Flowable of errors to retry
   * @param userId the user ID being processed (for logging)
   * @param cohortName the cohort name (for logging)
   * @return Flowable emitting retry attempt numbers
   */
  private Flowable<Long> applyRetryPolicy(
      Flowable<Throwable> errors, String userId, String cohortName) {
    return errors
        .zipWith(
            Flowable.range(1, BulkCohortAssignmentConstants.MAX_RETRIES + 1),
            (error, attempt) -> {
              if (attempt > BulkCohortAssignmentConstants.MAX_RETRIES) {
                throw new RuntimeException(error);
              }
              log.warn(
                  "Retrying assignment. cohort={}, user={}, attempt={}, reason={}",
                  cohortName,
                  userId,
                  attempt,
                  error.getMessage());
              return attempt;
            })
        .flatMap(
            attempt ->
                Flowable.timer(
                    BulkCohortAssignmentConstants.RETRY_BACKOFF.multipliedBy(attempt).toMillis(),
                    TimeUnit.MILLISECONDS));
  }

  /**
   * Deletes a file quietly, logging warnings if deletion fails.
   *
   * @param path path to the file to delete
   */
  private void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (Exception ex) {
      log.warn("Unable to delete temp file {}", path, ex);
    }
  }

  /**
   * Validates if a string matches UUID format pattern.
   *
   * @param value the string to validate
   * @return {@code true} if valid UUID format, {@code false} otherwise
   */
  private static boolean isValidUuid(String value) {
    if (!BulkCohortAssignmentConstants.UUID_PATTERN.matcher(value).matches()) {
      log.warn("Skipping invalid UUID: {}", value);
      return false;
    }
    return true;
  }
}

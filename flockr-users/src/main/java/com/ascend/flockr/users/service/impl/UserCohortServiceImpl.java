package com.ascend.flockr.users.service.impl;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;
import com.ascend.flockr.users.client.Aerospike;
import com.ascend.flockr.users.config.AerospikeConfig;
import com.ascend.flockr.users.constants.Constants;
import com.ascend.flockr.users.exception.errors.DefinedErrors;
import com.ascend.flockr.users.constants.BulkCohortAssignmentConstants;
import com.ascend.flockr.users.dto.BulkOperationResult;
import com.ascend.flockr.users.dto.request.MapUserCohortsRequest;
import com.ascend.flockr.users.service.UserCohortsService;
import com.ascend.flockr.users.util.SetNameUtil;
import com.dream11.rest.exception.RestException;
import com.dream11.rest.util.ExceptionUtil;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.rxjava3.core.Vertx;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
  @Inject
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
   * with expiry time greater than current time). Uses set name generated from tenantId and
   * projectId for multi-tenant isolation.
   */
  @Override
  public Single<List<String>> getCohorts(Long userId, String tenantId, String projectId) {
    String userKey = String.valueOf(userId);
    String setName = SetNameUtil.generateSetName(tenantId, projectId);
    return aerospikeClient.getCohortExpiryBin(userKey, setName).map(this::getActiveCohortsFromMap);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Implementation handles both append and remove actions. For append operations, validates
   * expiry time. Returns {@code false} if Aerospike key is not found. Uses set name generated from
   * tenantId and projectId for multi-tenant isolation. Uses default source since it's removed from
   * API.
   */
  @Override
  public Single<Boolean> mapUserCohorts(
      Long userId, String tenantId, String projectId, MapUserCohortsRequest request) {
    String userKey = String.valueOf(userId);
    // Use default source since it's removed from API
    String source = Constants.SOURCE_DREAM11;
    String setName = SetNameUtil.generateSetName(tenantId, projectId);

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
   * processing. Uses set name generated from tenantId and projectId for multi-tenant isolation.
   */
  @Override
  public Single<BulkOperationResult> assignUsersToCohort(
      String cohortName, String tenantId, String projectId, InputPart csvFilePart) {
    String setName = SetNameUtil.generateSetName(tenantId, projectId);

    // Read InputStream synchronously on request thread (required for JAX-RS context)
    java.io.InputStream inputStream;
    try {
      inputStream = csvFilePart.getBody(java.io.InputStream.class, null);
      if (inputStream == null) {
        log.error("InputStream is null from InputPart");
        return Single.error(ExceptionUtil.getException(DefinedErrors.EMPTY_CSV_FILE));
      }
    } catch (Exception e) {
      log.error("Failed to read InputStream from InputPart", e);
      return Single.error(ExceptionUtil.getException(DefinedErrors.EMPTY_CSV_FILE));
    }

    // Immediately move to background thread for file I/O
    return Single.fromCallable(
            () -> {
              Path tempPath = persistCsvToTempFile(inputStream);
              verifyFileIntegrity(tempPath);
              return tempPath;
            })
        .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
        .doOnError(error -> log.error("Error creating temp file", error))
        .flatMap(
            tempPath ->
                processCsvAndAssign(tempPath, cohortName, setName)
                    .doOnSuccess(
                        result ->
                            log.info(
                                "Bulk assignment completed. cohort={}, total={}, success={}, failed={}",
                                cohortName,
                                result.getTotalProcessed(),
                                result.getSuccessCount(),
                                result.getFailedCount()))
                    .doOnError(
                        error ->
                            log.error(
                                "Error during CSV processing for cohort: {}", cohortName, error)));
  }

  /**
   * Saves the uploaded CSV file to a temporary location on disk using streaming with atomic write.
   *
   * <p>This method is designed for concurrent request handling:
   *
   * <ul>
   *   <li>Uses constant memory (8KB buffer) regardless of file size
   *   <li>Runs on IO scheduler to avoid blocking event loop
   *   <li>Generates unique temp file names (handled by Files.createTempFile)
   *   <li>Uses atomic rename for crash safety
   *   <li>Validates file size during streaming to prevent DoS
   * </ul>
   *
   * <p>Uses a two-phase write pattern:
   *
   * <ol>
   *   <li>Write to a temporary file with .tmp extension
   *   <li>Atomically rename to .csv only after successful complete write
   * </ol>
   *
   * <p>This ensures that if the system crashes during write, we never have a partial .csv file. The
   * .tmp file can be safely ignored or cleaned up.
   *
   * <p><strong>Note:</strong> The InputStream must be obtained from InputPart on the request thread
   * (where JAX-RS context is available) before calling this method.
   *
   * @param inputStream the InputStream from InputPart (must be read on request thread)
   * @return path to the temporary file
   * @throws Exception if file is empty, too large, or cannot be written
   */
  private Path persistCsvToTempFile(java.io.InputStream inputStream) throws Exception {
    if (inputStream == null) {
      throw ExceptionUtil.getException(DefinedErrors.EMPTY_CSV_FILE);
    }

    // Phase 1: Write to temporary file with .tmp extension
    // Files.createTempFile() ensures unique names even with concurrent requests
    Path tempFile = Files.createTempFile("cohort-upload-", ".tmp");

    try (java.io.InputStream is = inputStream;
        java.io.OutputStream os = Files.newOutputStream(tempFile)) {

      byte[] buffer = new byte[8 * 1024]; // 8KB buffer - constant memory
      int bytesRead;
      long totalBytesRead = 0;
      boolean hasData = false;

      // Stream data to temp file
      while ((bytesRead = is.read(buffer)) != -1) {
        if (bytesRead > 0) {
          hasData = true;
          os.write(buffer, 0, bytesRead);
          totalBytesRead += bytesRead;

          // Validate file size during streaming (prevent DoS)
          if (totalBytesRead > BulkCohortAssignmentConstants.MAX_SIZE) {
            log.error(
                "File size {} exceeds maximum {}",
                totalBytesRead,
                BulkCohortAssignmentConstants.MAX_SIZE);
            Files.deleteIfExists(tempFile);
            throw ExceptionUtil.getException(
                DefinedErrors.INVALID_REQUEST,
                "File size exceeds maximum allowed size: "
                    + BulkCohortAssignmentConstants.MAX_SIZE);
          }

          // Flush periodically to ensure data is written to disk
          // This helps with concurrent writes by reducing buffering
          if (totalBytesRead % (64 * 1024) == 0) { // Flush every 64KB
            os.flush();
          }
        }
      }

      // Final flush to ensure all data is written
      os.flush();

      if (!hasData) {
        Files.deleteIfExists(tempFile);
        throw ExceptionUtil.getException(DefinedErrors.EMPTY_CSV_FILE);
      }
    }

    // Phase 2: Atomically rename .tmp to .csv
    // This is an atomic operation on most filesystems (Linux, macOS, Windows)
    // If system crashes before this, we only have a .tmp file (safe to ignore)
    Path finalFile =
        tempFile.resolveSibling(tempFile.getFileName().toString().replace(".tmp", ".csv"));

    try {
      // Atomic move - if this fails, we still have .tmp file
      Files.move(
          tempFile, finalFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      return finalFile;
    } catch (UnsupportedOperationException e) {
      // Fallback for filesystems that don't support ATOMIC_MOVE
      // Regular move is still safe - worst case is partial file (which we verify)
      try {
        Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
        return finalFile;
      } catch (IOException e2) {
        Files.deleteIfExists(tempFile);
        throw new IOException("Failed to persist CSV file", e2);
      }
    }
  }

  /**
   * Verifies that the CSV file is complete and readable before processing.
   *
   * <p>This method checks:
   *
   * <ul>
   *   <li>File exists
   *   <li>File is not empty
   *   <li>File is readable and contains at least one line
   * </ul>
   *
   * @param csvFile path to the CSV file to verify
   * @throws IOException if file verification fails
   */
  private void verifyFileIntegrity(Path csvFile) throws IOException {
    if (!Files.exists(csvFile)) {
      throw new IOException("CSV file does not exist: " + csvFile);
    }

    if (Files.size(csvFile) == 0) {
      throw ExceptionUtil.getException(DefinedErrors.EMPTY_CSV_FILE);
    }

    // Try to read first line to verify file is readable
    try (BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
      String firstLine = reader.readLine();
      if (firstLine == null || firstLine.trim().isEmpty()) {
        throw ExceptionUtil.getException(DefinedErrors.EMPTY_CSV_FILE);
      }
    }
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
   * @param setName the Aerospike set name (generated from tenantId and projectId)
   * @return Single emitting bulk operation result
   */
  public Single<BulkOperationResult> processCsvAndAssign(
      Path csvFile, String cohortName, String setName) {
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
        .doOnNext(
            batch ->
                log.info("Processing batch of {} UUIDs for cohort: {}", batch.size(), cohortName))
        .flatMap(
            batch ->
                Flowable.fromIterable(batch)
                    .parallel()
                    .runOn(Schedulers.io())
                    .flatMap(
                        userId ->
                            assignSingleUser(userId, cohortName, setName)
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
        emitter -> {
          // Handle file open errors
          vertx
              .fileSystem()
              .open(path.toString(), new OpenOptions().setRead(true))
              .subscribe(
                  asyncFile -> {
                    // Set buffer size for efficient reading
                    asyncFile.setReadBufferSize(64 * 1024);

                    // Track if we've already completed to avoid double completion
                    final boolean[] completed = {false};

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

                    // Handle end of file from parser
                    parser.endHandler(
                        v -> {
                          if (!completed[0]) {
                            completed[0] = true;
                            asyncFile.close();
                            emitter.onComplete();
                          }
                        });

                    // Handle parsing errors
                    parser.exceptionHandler(
                        err -> {
                          log.error("Parser error while reading file", err);
                          if (!completed[0]) {
                            completed[0] = true;
                            asyncFile.close();
                            emitter.onError(err);
                          }
                        });

                    // Connect asyncFile to parser: feed data from file to parser
                    asyncFile.handler(buffer -> parser.handle(buffer.getDelegate()));

                    // CRITICAL: Handle file stream end - this is called when file reading completes
                    asyncFile.endHandler(
                        v -> {
                          if (!completed[0]) {
                            completed[0] = true;
                            // Flush any remaining data in parser
                            try {
                              parser.handle(io.vertx.core.buffer.Buffer.buffer());
                            } catch (Exception e) {
                              // Ignore - parser might already be closed
                            }
                            asyncFile.close();
                            emitter.onComplete();
                          }
                        });

                    // Handle file read errors
                    asyncFile.exceptionHandler(
                        err -> {
                          log.error("File read error", err);
                          if (!completed[0]) {
                            completed[0] = true;
                            asyncFile.close();
                            emitter.onError(err);
                          }
                        });

                    // Start reading the file
                    asyncFile.resume();
                  },
                  error -> {
                    log.error("openFileAsFlowable: Failed to open file", error);
                    emitter.onError(error);
                  });
        },
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
   * @param setName the Aerospike set name (generated from tenantId and projectId)
   * @return Single emitting {@code true} if assignment succeeded, {@code false} otherwise
   */
  private Single<Boolean> assignSingleUser(String userUuid, String cohortName, String setName) {
    String userKey = userUuid; // UUID is used directly as userKey
    long expiry = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365); // 1-year default expiry

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
   * Cleans up orphaned temporary files from previous crashes or incomplete uploads.
   *
   * <p>This method should be called on application startup to remove .tmp files that were created
   * but never renamed to .csv (indicating a crash or error during upload).
   *
   * <p>Only deletes files older than 1 hour to avoid interfering with active uploads.
   *
   * @return number of files cleaned up
   */
  public int cleanupOrphanedTempFiles() {
    int cleanedCount = 0;
    try {
      Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
      if (!Files.exists(tempDir) || !Files.isDirectory(tempDir)) {
        return 0;
      }

      long oneHourAgo = System.currentTimeMillis() - 3600000; // 1 hour in milliseconds

      try (var stream = Files.list(tempDir)) {
        cleanedCount =
            (int)
                stream
                    .filter(
                        path ->
                            path.getFileName().toString().startsWith("cohort-upload-")
                                && path.getFileName().toString().endsWith(".tmp"))
                    .filter(
                        path -> {
                          try {
                            long lastModified = Files.getLastModifiedTime(path).toMillis();
                            return lastModified < oneHourAgo;
                          } catch (IOException e) {
                            log.warn("Failed to get last modified time for {}", path, e);
                            return false;
                          }
                        })
                    .mapToInt(
                        path -> {
                          try {
                            Files.deleteIfExists(path);
                            log.debug("Cleaned up orphaned temp file: {}", path);
                            return 1;
                          } catch (Exception e) {
                            log.warn("Failed to cleanup temp file: {}", path, e);
                            return 0;
                          }
                        })
                    .sum();
      }

      if (cleanedCount > 0) {
        log.info("Cleaned up {} orphaned temp file(s)", cleanedCount);
      }
    } catch (Exception e) {
      log.warn("Failed to cleanup orphaned temp files", e);
    }
    return cleanedCount;
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

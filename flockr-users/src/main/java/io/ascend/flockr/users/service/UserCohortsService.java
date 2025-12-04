package io.ascend.flockr.users.service;

import io.ascend.flockr.users.dto.BulkOperationResult;
import io.ascend.flockr.users.dto.request.BatchMapUserCohortsRequest;
import io.ascend.flockr.users.dto.request.MapUserCohortsRequest;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;

/**
 * Service interface for managing user cohort assignments.
 *
 * <p>This service provides operations for:
 *
 * <ul>
 *   <li>Retrieving active cohorts for a user
 *   <li>Mapping individual users to cohorts
 *   <li>Bulk assigning users from CSV files to cohorts
 * </ul>
 *
 * <p>All operations are asynchronous and return RxJava {@link Single} for reactive processing.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
public interface UserCohortsService {

  /**
   * Retrieves the list of active cohorts for a given user.
   *
   * <p>Active cohorts are those with expiry time greater than current time. The projectKey is used
   * directly as the Aerospike set name for multi-tenant isolation.
   *
   * @param userId the user ID, must be positive
   * @param projectKey the project key used as the Aerospike set name, must not be null or blank
   * @return Single emitting a list of active cohort names, empty list if none found
   * @author Sudhanshu Rai
   * @since 1.0
   */
  Single<List<String>> getCohorts(String userId, String projectKey);

  /**
   * Maps a user to a cohort (assigns or removes user from cohort).
   *
   * <p>This operation can either append a user to a cohort with an expiry time, or remove a user
   * from a cohort. The action is determined by the request's {@code action} field.
   *
   * @param userId the user ID from header
   * @param projectKey the project key used as the Aerospike set name, must not be null or blank
   * @param request the mapping request containing cohort and action details
   * @return Single emitting {@code true} if operation succeeded, {@code false} otherwise
   * @author Sudhanshu Rai
   * @since 1.0
   */
  Single<Boolean> mapUserCohorts(String userId, String projectKey, MapUserCohortsRequest request);

  /**
   * Bulk assigns users from a CSV file to a cohort.
   *
   * <p>This method processes a CSV file containing comma-separated user identifiers and assigns
   * each user to the specified cohort. The processing is done asynchronously with streaming to
   * avoid loading the entire file into memory.
   *
   * <p>The projectKey is used directly as the Aerospike set name for multi-tenant isolation.
   *
   * <p>The method returns only after all users in the CSV have been processed. The result includes
   * statistics about successful and failed assignments.
   *
   * @param cohortName the name of the cohort to assign users to
   * @param projectKey the project key used as the Aerospike set name, must not be null or blank
   * @param csvFilePart the multipart file part containing the CSV file
   * @return Single emitting bulk operation result with success/failure statistics
   * @author Sudhanshu Rai
   * @since 1.0
   */
  Single<BulkOperationResult> assignUsersToCohort(
      String cohortName, String projectKey, InputPart csvFilePart);

  /**
   * Batch maps multiple users to cohorts (assigns or removes users from cohorts).
   *
   * <p>This operation processes a list of mapping requests, where each request can either append a
   * user to a cohort with an expiry time, or remove a user from a cohort. The action is determined
   * by each request's {@code action} field.
   *
   * <p>The projectKey is used directly as the Aerospike set name for multi-tenant isolation.
   *
   * @param projectKey the project key used as the Aerospike set name, must not be null or blank
   * @param requests the list of mapping requests, each containing user_id, cohort_key, action, and
   *     expire_at
   * @return Single emitting bulk operation result with success/failure statistics
   * @author Sudhanshu Rai
   * @since 1.0
   */
  Single<BulkOperationResult> batchMapUserCohorts(
      String projectKey, List<BatchMapUserCohortsRequest> requests);
}

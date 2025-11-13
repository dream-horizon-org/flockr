package com.ascend.flockr.users.service;

import com.ascend.flockr.users.dto.BulkOperationResult;
import com.ascend.flockr.users.dto.request.MapUserCohortsRequest;
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
 * @since 1.0
 */
public interface UserCohortsService {

  /**
   * Retrieves the list of active cohorts for a given user.
   *
   * <p>Active cohorts are those with expiry time greater than current time. Either {@code userId}
   * or {@code guestId} must be provided, but not both.
   *
   * @param userId the user ID (optional if guestId is provided)
   * @param guestId the guest ID (optional if userId is provided)
   * @param projectId the project ID, must be positive
   * @return Single emitting a list of active cohort names, empty list if none found
   * @throws IllegalArgumentException if both userId and guestId are null
   * @since 1.0
   */
  Single<List<String>> getCohorts(Long userId, String guestId, Long projectId);

  /**
   * Maps a user to a cohort (assigns or removes user from cohort).
   *
   * <p>This operation can either append a user to a cohort with an expiry time, or remove a user
   * from a cohort. The action is determined by the request's {@code action} field.
   *
   * @param request the mapping request containing user, cohort, and action details
   * @return Single emitting {@code true} if operation succeeded, {@code false} otherwise
   * @throws IllegalArgumentException if request validation fails
   * @since 1.0
   */
  Single<Boolean> mapUserCohorts(MapUserCohortsRequest request);

  /**
   * Bulk assigns users from a CSV file to a cohort.
   *
   * <p>This method processes a CSV file containing comma-separated UUIDs and assigns each user to
   * the specified cohort. The processing is done asynchronously with streaming to avoid loading the
   * entire file into memory.
   *
   * <p>The method returns only after all users in the CSV have been processed. The result includes
   * statistics about successful and failed assignments.
   *
   * @param cohortName the name of the cohort to assign users to
   * @param csvFilePart the multipart file part containing the CSV file
   * @return Single emitting bulk operation result with success/failure statistics
   * @throws IllegalArgumentException if cohortName is blank or CSV file is invalid
   * @since 1.0
   */
  Single<BulkOperationResult> assignUsersToCohort(String cohortName, InputPart csvFilePart);
}

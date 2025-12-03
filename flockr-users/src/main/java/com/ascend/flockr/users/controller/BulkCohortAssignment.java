package com.ascend.flockr.users.controller;

import com.ascend.flockr.users.dto.BulkOperationResult;
import com.ascend.flockr.users.dto.ResponseEntity;
import com.ascend.flockr.users.service.UserCohortsService;
import com.ascend.flockr.users.validator.BulkCohortAssignmentValidator;
import com.ascend.flockr.users.validator.HeaderValidator;
import com.google.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

/**
 * REST controller for bulk cohort assignment operations.
 *
 * <p>Provides endpoint for bulk assigning users from CSV files to cohorts. The processing is done
 * asynchronously with streaming to handle large files efficiently.
 *
 * @since 1.0
 */
@Slf4j
@Path("/flockr/users")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class BulkCohortAssignment {

  private final UserCohortsService userCohortsService;

  /**
   * Bulk assigns users from a CSV file to a cohort.
   *
   * <p>Endpoint: POST /flockr/users/assignments/bulk
   *
   * <p>Headers:
   *
   * <ul>
   *   <li>{@code x-project-key} - Project key used directly as Aerospike set name (required)
   * </ul>
   *
   * <p>Accepts multipart form data with:
   *
   * <ul>
   *   <li>{@code csv_file} - CSV file containing comma-separated user identifiers (snake_case for
   *       API)
   *   <li>{@code cohort_name} - Name of the cohort to assign users to (snake_case for API)
   * </ul>
   *
   * <p>The CSV file is processed line by line, with each line containing comma-separated user
   * identifiers.
   *
   * <p>The x-project-key is used directly as the Aerospike set name for multi-tenant isolation.
   *
   * <p>Response is returned only after all users in the CSV have been processed. The response
   * includes statistics about total processed, successful, and failed assignments.
   *
   * @param projectKey the combined tenant and project identifier from x-project-key header
   * @param input multipart form data containing file and cohort_name
   * @return CompletionStage resolving to HTTP 200 with bulk operation result, or 400/500 if
   *     validation or processing fails
   * @since 1.0
   */
  @POST
  @Path("/assignments/bulk")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<Response> bulkAssignUsers(
      @HeaderParam("x-project-key") String projectKey, MultipartFormDataInput input) {

    // Validate header
    HeaderValidator.validateProjectKeyHeader(projectKey);

    // Validate and extract form data
    String cohortName = BulkCohortAssignmentValidator.validateAndExtractCohortName(input);
    InputPart filePart = BulkCohortAssignmentValidator.validateAndExtractCsvFile(input);

    return userCohortsService
        .assignUsersToCohort(cohortName, projectKey, filePart)
        .map(ResponseEntity.Success<BulkOperationResult>::new)
        .map(res -> Response.ok(res).build())
        .onErrorReturn(
            error -> {
              log.error("Bulk cohort assignment failed", error);
              ResponseEntity.Failure failure =
                  new ResponseEntity.Failure("BULK_ASSIGNMENT_FAILED", error.getMessage(), null);
              return Response.serverError().entity(failure).build();
            })
        .toCompletionStage();
  }
}

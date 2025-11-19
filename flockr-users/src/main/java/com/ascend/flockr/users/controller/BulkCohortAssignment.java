package com.ascend.flockr.users.controller;

import com.ascend.flockr.common.exception.errors.DefinedErrors;
import com.ascend.flockr.users.dto.ResponseEntity;
import com.ascend.flockr.users.service.UserCohortsService;
import com.dream11.rest.util.ExceptionUtil;
import com.google.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
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

  private static final String CSV_FILE = "csvFile";
  private static final String COHORT_NAME = "cohortName";

  private final UserCohortsService userCohortsService;

  private static final String TENANT_ID = "tenantId";
  private static final String PROJECT_ID = "projectId";

  /**
   * Bulk assigns users from a CSV file to a cohort.
   *
   * <p>Endpoint: POST /flockr/users/assignments/bulk
   *
   * <p>Accepts multipart form data with:
   *
   * <ul>
   *   <li>{@code csvFile} - CSV file containing comma-separated user UUIDs
   *   <li>{@code cohortName} - Name of the cohort to assign users to
   *   <li>{@code tenantId} - Tenant ID (required for multi-tenant isolation)
   *   <li>{@code projectId} - Project ID (required for multi-tenant isolation)
   * </ul>
   *
   * <p>The CSV file is processed line by line, with each line containing comma-separated UUIDs.
   * Invalid UUIDs are skipped and counted as failures.
   *
   * <p>The set name used for Aerospike operations is generated as "{tenantId}_{projectId}" to
   * ensure multi-tenant isolation.
   *
   * <p>Response is returned only after all users in the CSV have been processed. The response
   * includes statistics about total processed, successful, and failed assignments.
   *
   * @param input multipart form data containing file, cohortName, tenantId, and projectId
   * @return CompletionStage resolving to HTTP 200 with bulk operation result, or 400/500 if
   *     validation or processing fails
   * @since 1.0
   */
  @POST
  @Path("/assignments/bulk")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<Response> bulkAssignUsers(MultipartFormDataInput input) {
    String cohortName = extractPart(input, COHORT_NAME);
    String tenantId = extractPart(input, TENANT_ID);
    String projectIdStr = extractPart(input, PROJECT_ID);
    InputPart filePart = getPart(input, CSV_FILE);

    // Validate tenantId and projectId
    Long projectId;
    try {
      projectId = Long.parseLong(projectIdStr);
      if (projectId <= 0) {
        throw new IllegalArgumentException("projectId must be positive");
      }
    } catch (IllegalArgumentException e) {
      log.error("Invalid projectId provided: {}", projectIdStr);
      throw ExceptionUtil.getException(DefinedErrors.INVALID_REQUEST_PARAMS);
    }

    if (tenantId == null || tenantId.trim().isEmpty()) {
      log.error("tenantId is required");
      throw ExceptionUtil.getException(DefinedErrors.INVALID_REQUEST_PARAMS);
    }

    return userCohortsService
        .assignUsersToCohort(cohortName, tenantId, projectId, filePart)
        .map(ResponseEntity.Success::new)
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

  /**
   * Extracts a text part from multipart form data.
   *
   * @param input the multipart form data input
   * @param name the name of the form field to extract
   * @return the trimmed string value
   * @throws RuntimeException if extraction fails
   */
  private String extractPart(MultipartFormDataInput input, String name) {
    try {
      return getPart(input, name).getBodyAsString().trim();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets the first input part with the specified name from multipart form data.
   *
   * @param input the multipart form data input
   * @param name the name of the form field
   * @return the first InputPart with the given name
   * @throws IllegalArgumentException if the part is missing
   */
  private InputPart getPart(MultipartFormDataInput input, String name) {
    List<InputPart> parts = input.getFormDataMap().get(name);
    if (parts == null || parts.isEmpty()) {
      throw ExceptionUtil.getException(DefinedErrors.INVALID_REQUEST_PARAMS, "Missing " + name);
    }
    return parts.get(0);
  }
}

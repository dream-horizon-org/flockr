package com.ascend.flockr.users.controller;

import com.ascend.flockr.common.exception.errors.DefinedErrors;
import com.ascend.flockr.users.dto.BulkOperationResult;
import com.ascend.flockr.users.dto.ResponseEntity;
import com.ascend.flockr.users.service.UserCohortsService;
import com.ascend.flockr.users.util.SetNameUtil;
import com.dream11.rest.util.ExceptionUtil;
import com.google.inject.Inject;
import jakarta.ws.rs.*;
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

  private static final String CSV_FILE = "csv_file";
  private static final String COHORT_NAME = "cohort_name";

  private final UserCohortsService userCohortsService;

  /**
   * Bulk assigns users from a CSV file to a cohort.
   *
   * <p>Endpoint: POST /flockr/users/assignments/bulk
   *
   * <p>Headers:
   *
   * <ul>
   *   <li>{@code x-project-key} - Combined tenant and project identifier in format "tenantId_projectId" (required)
   * </ul>
   *
   * <p>Accepts multipart form data with:
   *
   * <ul>
   *   <li>{@code csv_file} - CSV file containing comma-separated user UUIDs (snake_case for API)
   *   <li>{@code cohort_name} - Name of the cohort to assign users to (snake_case for API)
   * </ul>
   *
   * <p>The CSV file is processed line by line, with each line containing comma-separated UUIDs.
   * Invalid UUIDs are skipped and counted as failures.
   *
   * <p>The set name used for Aerospike operations is generated from x-project-key to
   * ensure multi-tenant isolation.
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
      @HeaderParam("x-project-key") String projectKey,
      MultipartFormDataInput input) {

    // Validate x-project-key header is present
    if (projectKey == null || projectKey.trim().isEmpty()) {
      log.error("Missing x-project-key header");
      throw ExceptionUtil.getException(DefinedErrors.MISSING_PROJECT_KEY_HEADER);
    }

    // Parse x-project-key (format: tenantId_projectId)
    String[] projectKeyParts = projectKey.split("_", 2);
    if (projectKeyParts.length != 2) {
      log.error("Invalid x-project-key format: {}", projectKey);
      throw ExceptionUtil.getException(DefinedErrors.INVALID_PROJECT_KEY_FORMAT, projectKey);
    }

    String tenantId = projectKeyParts[0].trim();
    String projectId = projectKeyParts[1].trim();
    
    // Validate tenantId is not empty
    if (tenantId.isEmpty()) {
      log.error("Empty tenantId in x-project-key: {}", projectKey);
      throw ExceptionUtil.getException(DefinedErrors.MISSING_TENANT_ID);
    }

    // Validate projectId is not empty
    if (projectId.isEmpty()) {
      log.error("Empty projectId in x-project-key: {}", projectKey);
      throw ExceptionUtil.getException(DefinedErrors.MISSING_PROJECT_ID);
    }

    // Validate tenantId and projectId (includes UUID validation for tenantId)
    try {
      SetNameUtil.validateTenantAndProject(tenantId, projectId);
    } catch (IllegalArgumentException e) {
      log.error("Invalid tenantId or projectId: tenantId={}, projectId={}, error={}", tenantId, projectId, e.getMessage());
      // Check if it's a UUID validation error
      if (e.getMessage().contains("UUID")) {
        throw ExceptionUtil.getException(DefinedErrors.INVALID_TENANT_ID_FORMAT, tenantId);
      } else {
        throw ExceptionUtil.getException(DefinedErrors.INVALID_PROJECT_KEY_FORMAT, projectKey);
      }
    }

    // Extract form data (using snake_case for API)
    String cohortName;
    InputPart filePart;
    
    try {
      cohortName = extractPart(input, COHORT_NAME);
      if (cohortName == null || cohortName.trim().isEmpty()) {
        log.error("Missing or empty cohort_name in form data");
        throw ExceptionUtil.getException(DefinedErrors.MISSING_COHORT_NAME);
      }
    } catch (Exception e) {
      if (e instanceof RuntimeException && e.getCause() instanceof IllegalArgumentException) {
        // Re-throw if it's already a DefinedErrors exception
        throw e;
      }
      log.error("Missing cohort_name in form data");
      throw ExceptionUtil.getException(DefinedErrors.MISSING_COHORT_NAME);
    }

    try {
      filePart = getPart(input, CSV_FILE);
    } catch (Exception e) {
      if (e instanceof RuntimeException && e.getCause() instanceof IllegalArgumentException) {
        // Re-throw if it's already a DefinedErrors exception
        throw e;
      }
      log.error("Missing csv_file in form data");
      throw ExceptionUtil.getException(DefinedErrors.MISSING_CSV_FILE);
    }

    return userCohortsService
        .assignUsersToCohort(cohortName, tenantId, projectId, filePart)
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
      if (CSV_FILE.equals(name)) {
        throw ExceptionUtil.getException(DefinedErrors.MISSING_CSV_FILE);
      } else if (COHORT_NAME.equals(name)) {
        throw ExceptionUtil.getException(DefinedErrors.MISSING_COHORT_NAME);
      } else {
        // Fallback for unknown form fields - should not happen in normal operation
        throw ExceptionUtil.getException(DefinedErrors.INVALID_REQUEST, "Missing required form field: " + name);
      }
    }
    return parts.get(0);
  }
}

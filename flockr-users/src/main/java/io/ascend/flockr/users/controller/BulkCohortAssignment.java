package io.ascend.flockr.users.controller;

import com.google.inject.Inject;
import io.ascend.flockr.users.dto.BulkOperationResult;
import io.ascend.flockr.users.dto.ResponseEntity;
import io.ascend.flockr.users.service.UserCohortsService;
import io.ascend.flockr.users.util.ErrorHandler;
import io.ascend.flockr.users.validator.BulkCohortAssignmentValidator;
import io.ascend.flockr.users.validator.HeaderValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
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
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Slf4j
@Path("/flockr/users")
@Tag(
    name = "Bulk Cohort Assignment",
    description = "Bulk operations for assigning users to cohorts via CSV upload")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class BulkCohortAssignment {

  private final UserCohortsService userCohortsService;

  /**
   * Bulk assigns users from a CSV file to a cohort.
   *
   * @param projectKey the combined tenant and project identifier from x-project-key header
   * @param input multipart form data containing file and cohort_name
   * @return CompletionStage resolving to HTTP 200 with bulk operation result
   */
  @POST
  @Path("/assignments/bulk")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Bulk assign users to cohort from CSV",
      description =
          "Uploads a CSV file containing user identifiers and assigns all users to the specified cohort. "
              + "The CSV file is processed line by line, with each line containing comma-separated user identifiers. "
              + "Response includes statistics about total processed, successful, and failed assignments.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Bulk assignment completed successfully",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = BulkOperationResult.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request - missing or invalid file/cohort_name",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ResponseEntity.Failure.class))),
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error during processing",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ResponseEntity.Failure.class)))
  })
  public CompletionStage<ResponseEntity.Success<BulkOperationResult>> bulkAssignUsers(
      @NotNull(message = "x-project-key header is required")
          @Parameter(
              description = "Project key used as Aerospike set name for multi-tenant isolation",
              required = true,
              example = "tenant1_project1")
          @HeaderParam("x-project-key")
          String projectKey,
      @Parameter(
              description =
                  "Multipart form data with 'csv_file' (CSV file with user IDs) and 'cohort_name' (target cohort)")
          MultipartFormDataInput input) {

    // Validate header
    HeaderValidator.validateProjectKeyHeader(projectKey);

    // Validate and extract form data
    String cohortName = BulkCohortAssignmentValidator.validateAndExtractCohortName(input);
    InputPart filePart = BulkCohortAssignmentValidator.validateAndExtractCsvFile(input);

    return ErrorHandler.handleAsync(
        userCohortsService.assignUsersToCohort(cohortName, projectKey, filePart),
        "bulkCohortAssignment");
  }
}

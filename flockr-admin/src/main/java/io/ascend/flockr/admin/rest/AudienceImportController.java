package io.ascend.flockr.admin.rest;

import com.google.inject.Inject;
import io.ascend.flockr.admin.io.ResponseEntity;
import io.ascend.flockr.admin.io.request.CsvImportForm;
import io.ascend.flockr.admin.io.response.AudienceImportResponse;
import io.ascend.flockr.admin.service.AudienceImportService;
import io.ascend.flockr.admin.util.ErrorHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

/**
 * REST controller for CSV imports for STATIC audiences.
 *
 * <p>This is a fire-and-forget API - upload CSV, push to sinks, return result. No import history is
 * stored.
 */
@Path("/")
@Tag(name = "Audience Imports", description = "CSV import API for STATIC audiences")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class AudienceImportController {

  private final AudienceImportService audienceImportService;

  @POST
  @Path("/v1/audiences/{audienceId}/imports")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Import CSV data for a STATIC audience",
      description =
          "Uploads a CSV file for a STATIC audience. The data is streamed and pushed to configured sinks. "
              + "This endpoint is only available for audiences with type=STATIC. "
              + "Send the file as multipart/form-data with field name 'file'.")
  @ApiResponse(
      responseCode = "200",
      description = "Import completed successfully",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Success.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request - Invalid CSV or audience type is not STATIC",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Audience not found",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error",
      content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
  public CompletionStage<ResponseEntity.Success<AudienceImportResponse>> createImport(
      @Parameter(description = "Encrypted project identifier", required = true)
          @HeaderParam("X-Project-Key")
          String xProjectId,
      @Parameter(description = "Actor email/username", required = false)
          @HeaderParam("email")
          @DefaultValue("system")
          String actor,
      @Parameter(description = "ID of the STATIC audience", required = true)
          @PathParam("audienceId")
          Long audienceId,
      @MultipartForm CsvImportForm form) {
    return ErrorHandler.handleAsync(
        audienceImportService.createImport(xProjectId, audienceId, form, actor), "createImport");
  }
}

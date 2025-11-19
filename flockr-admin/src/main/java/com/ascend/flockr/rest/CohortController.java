package com.ascend.flockr.rest;

import com.ascend.flockr.io.ResponseEntity;
import com.ascend.flockr.io.request.UpdateCohortOwnerRequest;
import com.ascend.flockr.service.CohortService;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;

@Path("/")
@Tag(name = "Cohorts", description = "Cohort management APIs")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class CohortController {

  private final CohortService cohortService;

    @POST
    @Path("/v1/cohorts/{cohortId}/owner")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Update cohort owner",
            description = "Updates the owner(s) of a specific cohort based on cohort ID.")
    @ApiResponse(
            responseCode = "200",
            description = "Cohort owner updated successfully",
            content = @Content(schema = @Schema(implementation = ResponseEntity.Success.class)))
    @ApiResponse(
            responseCode = "400",
            description = "Bad Request due to invalid/missing parameters",
            content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized: missing or invalid user email header",
            content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
    @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = @Content(schema = @Schema(implementation = ResponseEntity.Failure.class)))
    public CompletionStage<ResponseEntity.Success<String>> updateCohortOwner(
            @Parameter(description = "ID of the cohort whose owner is to be updated", required = true)
            @PathParam("cohortId") Long cohortId,
            @Parameter(description = "Email of the logged-in user (from header 'msd-user-email')", required = true)
            @HeaderParam("msd-user-email") String userEmail,
            @Parameter(description = "Payload indicating action (add/remove) and target email", required = true)
            UpdateCohortOwnerRequest requestBody) {

        return cohortService
                .updateCohortOwner(cohortId, userEmail, requestBody)
                .andThen(io.reactivex.rxjava3.core.Single.just("cohort owner's updated successfully"))
                .map(ResponseEntity.Success::new)
                .toCompletionStage();
    }
}

package com.ascend.flockr.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Response DTO for task trigger operation. */
@Schema(description = "Response containing task trigger operation result")
public record TaskResponse(
    @Schema(description = "The unique identifier of the triggered task", example = "12345")
        Long taskId,
    @Schema(description = "The status of the task after trigger", example = "RUNNING")
        String status,
    @Schema(
            description = "Optional message providing additional information",
            example = "Task triggered successfully")
        String message) {}

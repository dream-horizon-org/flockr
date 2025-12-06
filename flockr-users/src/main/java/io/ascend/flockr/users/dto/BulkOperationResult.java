package io.ascend.flockr.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result DTO for bulk cohort assignment operations.
 *
 * <p>Contains statistics about the bulk operation execution, including total number of users
 * processed, successful assignments, and failures.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Result of a bulk cohort assignment operation")
public class BulkOperationResult {
  /** Total number of users processed from the CSV. */
  @Schema(description = "Total number of users processed", example = "1000")
  private int totalProcessed;

  /** Number of successful cohort assignments. */
  @Schema(description = "Number of successful assignments", example = "995")
  private int successCount;

  /** Number of failed assignments. */
  @Schema(description = "Number of failed assignments", example = "5")
  private int failedCount;

  /** Human-readable message summarizing the operation result. */
  @Schema(
      description = "Summary message of the operation",
      example = "Bulk assignment completed: 995 succeeded, 5 failed out of 1000 total")
  private String message;
}

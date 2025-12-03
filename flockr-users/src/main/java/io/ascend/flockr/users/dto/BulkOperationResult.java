package io.ascend.flockr.users.dto;

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
public class BulkOperationResult {
  /** Total number of users processed from the CSV. */
  private int totalProcessed;

  /** Number of successful cohort assignments. */
  private int successCount;

  /** Number of failed assignments. */
  private int failedCount;

  /** Human-readable message summarizing the operation result. */
  private String message;
}

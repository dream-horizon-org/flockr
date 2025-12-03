package io.ascend.flockr.admin.io.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response object for CSV import result.
 *
 * <p>This is a fire-and-forget API response - contains only the result of the import operation.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudienceImportResponse {
  /** The audience identifier this import was for. */
  private Long audienceId;

  /** Original file name of the uploaded CSV. */
  private String fileName;

  /** Number of records successfully processed from the CSV. */
  private Long recordCount;

  /** Status of the import (COMPLETED or FAILED). */
  private String status;
}

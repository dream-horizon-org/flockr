package com.ascend.flockr.client.spark.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Spark job submission.
 * 
 * <p>Contains the submissionId returned from Spark API after successful job submission.
 * This submissionId is used to query job status and cancel the job.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SparkJobSubmissionResponse {
  /**
   * Spark submission ID returned from job submission.
   * 
   * <p>This ID is used to:
   * <ul>
   *   <li>Query job status via getJobStatus(submissionId)
   *   <li>Cancel job via cancelJob(submissionId)
   * </ul>
   * 
   * <p>Note: submissionId is different from applicationId.
   */
  private String submissionId;
}


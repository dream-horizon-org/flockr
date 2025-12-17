package io.ascend.flockr.admin.client.spark.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Spark job submission.
 *
 * <p>Contains the submissionId returned from Spark API after successful job submission. This
 * submissionId is used to query job status and cancel the job.
 *
 * <p>Follows the Spark REST API response format from /v1/submissions/create endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SparkJobSubmissionResponse {
  /**
   * Whether the submission was successful.
   *
   * <p>True if the job was accepted by Spark, false otherwise.
   */
  private Boolean success;

  /**
   * Spark submission ID returned from job submission.
   *
   * <p>This ID is used to:
   *
   * <ul>
   *   <li>Query job status via getJobStatus(submissionId)
   *   <li>Cancel job via cancelJob(submissionId)
   * </ul>
   *
   * <p>Note: submissionId is different from applicationId. Only populated when success is true.
   */
  private String submissionId;

  /**
   * Response message from Spark API.
   *
   * <p>Contains success message or error details depending on the submission outcome.
   */
  private String message;

  /**
   * The action that was performed (e.g., "CreateSubmissionResponse").
   *
   * <p>Reflects the Spark REST API action type in the response.
   */
  private String action;

  /**
   * Spark version on the server.
   *
   * <p>Returned by Spark to indicate the server version.
   */
  private String serverSparkVersion;
}

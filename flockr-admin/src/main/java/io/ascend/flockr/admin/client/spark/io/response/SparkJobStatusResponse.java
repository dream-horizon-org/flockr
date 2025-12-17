package io.ascend.flockr.admin.client.spark.io.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Spark job status query.
 *
 * <p>Contains job status information including applicationId, status, and other details returned
 * from Spark API when querying job status using submissionId.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SparkJobStatusResponse {
  /**
   * Spark application ID.
   *
   * <p>This ID is different from submissionId and is used to retrieve application logs. Obtained
   * from getJobStatus() response.
   */
  private String applicationId;

  /**
   * Current job status.
   *
   * <p>Possible values:
   *
   * <ul>
   *   <li>SUBMITTED - Job has been submitted but not yet started
   *   <li>RUNNING - Job is currently executing
   *   <li>FINISHED - Job completed successfully
   *   <li>FAILED - Job failed with an error
   *   <li>KILLED - Job was cancelled/killed
   * </ul>
   */
  private String status;

  /**
   * Optional message providing additional information about the job status.
   *
   * <p>May contain error messages if the job failed, or informational messages about job progress.
   */
  private String message;

  /** Optional driver state (if available from Spark API). */
  private String driverState;

  /** Optional worker IDs (if available from Spark API). */
  private String workerId;
}

package com.ascend.flockr.exception;

/** Exception thrown when Flink job submission fails. */
public class FlinkJobSubmissionException extends FlinkClientException {

  public FlinkJobSubmissionException(String message) {
    super("FLINK_JOB_SUBMISSION_FAILED", message, 500);
  }

  public FlinkJobSubmissionException(String message, Throwable cause) {
    super("FLINK_JOB_SUBMISSION_FAILED", message, 500, cause);
  }

  public FlinkJobSubmissionException(String jarId, String entryClass, Throwable cause) {
    super(
        "FLINK_JOB_SUBMISSION_FAILED",
        String.format(
            "Failed to submit Flink job with jarId: %s, entryClass: %s", jarId, entryClass),
        500,
        cause);
  }
}

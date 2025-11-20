package com.ascend.flockr.exception.spark;

/**
 * Exception thrown when Spark job submission fails.
 */
public class SparkJobSubmissionException extends SparkJobException {
    public SparkJobSubmissionException(String message, Throwable cause) {
        super("Failed to submit Spark job: " + message, cause);
    }
}
package io.ascend.flockr.admin.exception.spark;

/** Exception thrown when a Spark job is not found. */
public class SparkJobNotFoundException extends SparkJobException {
  public SparkJobNotFoundException(String applicationId) {
    super(String.format("Spark job not found: %s", applicationId));
  }
}

package io.ascend.flockr.admin.client.flink;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Client interface for interacting with Apache Flink REST API. Provides methods for job management,
 * cluster monitoring, and job lifecycle operations.
 */
public interface FlinkClient {

  /**
   * Close the Flink client and release resources.
   *
   * @return Completable that completes when the client is closed
   */
  Completable close();

  /**
   * Submit a new Flink job to the cluster.
   *
   * @param jarId The ID of the uploaded JAR file
   * @param entryClass The fully qualified name of the entry class
   * @param programArgs Program arguments for the job
   * @param parallelism Parallelism for the job
   * @param savepointPath Optional savepoint path to restore from
   * @return Single containing the job ID
   */
  Single<String> submitJob(
      String jarId,
      String entryClass,
      String programArgs,
      Integer parallelism,
      String savepointPath);

  /**
   * Upload a JAR file to the Flink cluster.
   *
   * @param jarFilePath Path to the JAR file to upload
   * @return Single containing the JAR ID
   */
  Single<String> uploadJar(String jarFilePath);

  /**
   * Get the status of a Flink job.
   *
   * @param jobId The ID of the job
   * @return Single containing job status information as JsonObject
   */
  Single<JsonObject> getJobStatus(String jobId);

  /**
   * Cancel a running Flink job.
   *
   * @param jobId The ID of the job to cancel
   * @return Completable that completes when the job is cancelled
   */
  Completable cancelJob(String jobId);

  /**
   * Cancel a running Flink job with a savepoint.
   *
   * @param jobId The ID of the job to cancel
   * @param savepointDirectory Directory to save the savepoint
   * @return Single containing the savepoint path
   */
  Single<String> cancelJobWithSavepoint(String jobId, String savepointDirectory);

  /**
   * Trigger a savepoint for a running job.
   *
   * @param jobId The ID of the job
   * @param savepointDirectory Directory to save the savepoint
   * @return Single containing the savepoint path
   */
  Single<String> triggerSavepoint(String jobId, String savepointDirectory);

  /**
   * Get list of all jobs in the cluster.
   *
   * @return Single containing list of jobs as JsonArray
   */
  Single<JsonArray> listJobs();

  /**
   * Get detailed information about a specific job.
   *
   * @param jobId The ID of the job
   * @return Single containing job details as JsonObject
   */
  Single<JsonObject> getJobDetails(String jobId);

  /**
   * Get cluster overview information.
   *
   * @return Single containing cluster overview as JsonObject
   */
  Single<JsonObject> getClusterOverview();

  /**
   * Get list of task managers in the cluster.
   *
   * @return Single containing list of task managers as JsonArray
   */
  Single<JsonArray> listTaskManagers();

  /**
   * Get job execution plan.
   *
   * @param jobId The ID of the job
   * @return Single containing job execution plan as JsonObject
   */
  Single<JsonObject> getJobExecutionPlan(String jobId);

  /**
   * Get job exceptions/failures.
   *
   * @param jobId The ID of the job
   * @return Single containing job exceptions as JsonObject
   */
  Single<JsonObject> getJobExceptions(String jobId);

  /**
   * Get job metrics.
   *
   * @param jobId The ID of the job
   * @return Single containing job metrics as JsonObject
   */
  Single<JsonObject> getJobMetrics(String jobId);

  /**
   * Stop a job with a savepoint (for jobs with sources that support it).
   *
   * @param jobId The ID of the job to stop
   * @param savepointDirectory Directory to save the savepoint
   * @param drain Whether to drain the job before stopping
   * @return Single containing the savepoint path
   */
  Single<String> stopJobWithSavepoint(String jobId, String savepointDirectory, boolean drain);

  /**
   * Rescale a running job to a new parallelism.
   *
   * @param jobId The ID of the job
   * @param parallelism The new parallelism
   * @return Completable that completes when the job is rescaled
   */
  Completable rescaleJob(String jobId, int parallelism);

  /**
   * Delete an uploaded JAR file.
   *
   * @param jarId The ID of the JAR to delete
   * @return Completable that completes when the JAR is deleted
   */
  Completable deleteJar(String jarId);

  /**
   * Get list of uploaded JARs.
   *
   * @return Single containing list of JARs as JsonArray
   */
  Single<JsonArray> listJars();

  /**
   * Get the configuration of the Flink cluster.
   *
   * @return Single containing cluster configuration as JsonObject
   */
  Single<JsonObject> getClusterConfiguration();
}

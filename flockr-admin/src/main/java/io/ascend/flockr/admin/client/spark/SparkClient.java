package io.ascend.flockr.admin.client.spark;

import io.ascend.flockr.admin.client.spark.io.response.SparkApplicationInfo;
import io.ascend.flockr.admin.client.spark.io.response.SparkJobStatusResponse;
import io.ascend.flockr.admin.client.spark.io.response.SparkJobSubmissionResponse;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import java.util.List;

/**
 * Client interface for interacting with Apache Spark REST API for historic batch processing.
 *
 * <p>This client provides methods for:
 *
 * <ul>
 *   <li>Submitting jobs to Spark cluster (Master REST API)
 *   <li>Querying job status and logs
 *   <li>Listing applications from History Server (for reconciliation)
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
public interface SparkClient {

  /**
   * Submits a historic batch processing job to Spark cluster.
   *
   * <p>The request payload contains:
   *
   * <ul>
   *   <li>Query string (SQL query for batch processing)
   *   <li>DataSourceDetails (enriched data source configuration)
   *   <li>List of DestinationDetails (sink configurations)
   *   <li>audience name (rule corresponding to the audience)
   * </ul>
   *
   * <p><strong>Response:</strong> Returns SparkJobSubmissionResponse containing submissionId. This
   * submissionId can be used to query job status. Note that submissionId may differ from
   * applicationId.
   *
   * @param requestBody JsonObject containing the job request payload
   * @return Single containing SparkJobSubmissionResponse with submissionId
   */
  Single<SparkJobSubmissionResponse> submit(JsonObject requestBody);

  /**
   * Get the status of a Spark job.
   *
   * <p><strong>Note:</strong> This method accepts submissionId (returned from
   * submitHistoricBatchJob). The response contains applicationId which is different from
   * submissionId.
   *
   * @param submissionId The Spark submission ID (returned from submitHistoricBatchJob)
   * @return Single containing SparkJobStatusResponse with applicationId, status, and other details
   */
  Single<SparkJobStatusResponse> getJobStatus(String submissionId);

  /**
   * Cancel a running Spark job.
   *
   * <p><strong>Note:</strong> This method accepts submissionId. Spark API uses submissionId for
   * cancellation operations.
   *
   * @param submissionId The Spark submission ID (returned from submitHistoricBatchJob)
   * @return Completable that completes when the job is cancelled
   */
  Completable cancelJob(String submissionId);

  /**
   * Get application logs from Spark cluster.
   *
   * <p><strong>Note:</strong> This method accepts applicationId (obtained from getJobStatus
   * response), not submissionId. Spark API uses applicationId for log retrieval.
   *
   * @param applicationId The Spark application ID (obtained from getJobStatus response)
   * @return Single containing application logs as string
   */
  Single<String> getApplicationLogs(String applicationId);

  /**
   * Lists Spark applications from the History Server API.
   *
   * <p>Endpoint: GET
   * /api/v1/applications?status={status}&minDate={date}&maxDate={date}&limit={limit}
   *
   * <p><strong>Note:</strong> The History Server may be on a different host/port than the Spark
   * Master. Configure historyHost and historyPort in SparkConfig.
   *
   * @param status filter by status (running, completed, failed) - optional, pass null to skip
   * @param minDate return apps started after this date - optional, pass null to skip
   * @param maxDate return apps started before this date - optional, pass null to skip
   * @param limit maximum number of results - optional, pass null to skip
   * @return Single containing list of SparkApplicationInfo
   */
  Single<List<SparkApplicationInfo>> listApplications(
      String status, Instant minDate, Instant maxDate, Integer limit);

  /**
   * Finds applications by name pattern from History Server.
   *
   * <p>This method fetches applications from the History Server and filters them by name using a
   * glob pattern. Useful for reconciliation to find jobs by their assigned spark.app.name.
   *
   * <p>Pattern examples:
   *
   * <ul>
   *   <li>"flockr-batch-rule-123-exec-456-*" - matches jobs for rule 123, execution 456
   *   <li>"flockr-batch-*" - matches all flockr batch jobs
   * </ul>
   *
   * @param namePattern glob pattern to match (* and ? wildcards supported)
   * @param minDate search apps started after this date
   * @param maxDate search apps started before this date
   * @return list of matching applications
   */
  Single<List<SparkApplicationInfo>> findApplicationsByNamePattern(
      String namePattern, Instant minDate, Instant maxDate);

  /**
   * Close the Spark client and release resources.
   *
   * @return Completable that completes when the client is closed
   */
  Completable close();
}

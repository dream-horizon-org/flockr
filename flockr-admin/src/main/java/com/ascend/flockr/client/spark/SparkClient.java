package com.ascend.flockr.client.spark;

import com.ascend.flockr.client.spark.dto.response.SparkJobSubmissionResponse;
import com.ascend.flockr.client.spark.dto.response.SparkJobStatusResponse;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;

/**
 * Client interface for interacting with Apache Spark REST API for historic batch processing.
 *
 * <p>This client provides methods for submitting and managing Spark jobs. It follows the same
 * pattern as FlinkClient for consistency.
 *
 * <p><strong>Note:</strong> This client is task-agnostic. The service layer handles task ID lookup
 * and data accumulation before calling this client.
 */
public interface SparkClient {

    /**
     * Submits a historic batch processing job to Spark cluster.
     *
     * <p>The request payload contains:
     * <ul>
     *   <li>Query string (SQL query for batch processing)
     *   <li>DataSourceDetails (enriched data source configuration)
     *   <li>List of DestinationDetails (sink configurations)
     *   <li>Cohort name (for cohort-based filtering)
     * </ul>
     *
     * <p><strong>Response:</strong> Returns SparkJobSubmissionResponse containing submissionId.
     * This submissionId can be used to query job status. Note that submissionId may differ from
     * applicationId.
     *
     * @param requestBody JsonObject containing the job request payload
     * @return Single containing SparkJobSubmissionResponse with submissionId
     */
    Single<SparkJobSubmissionResponse> submitHistoricBatchJob(JsonObject requestBody);

    /**
     * Get the status of a Spark job.
     *
     * <p><strong>Note:</strong> This method accepts submissionId (returned from submitHistoricBatchJob).
     * The response contains applicationId which is different from submissionId.
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
     * <p><strong>Note:</strong> This method accepts applicationId (obtained from getJobStatus response),
     * not submissionId. Spark API uses applicationId for log retrieval.
     *
     * @param applicationId The Spark application ID (obtained from getJobStatus response)
     * @return Single containing application logs as string
     */
    Single<String> getApplicationLogs(String applicationId);

    /**
     * Close the Spark client and release resources.
     *
     * @return Completable that completes when the client is closed
     */
    Completable close();
}
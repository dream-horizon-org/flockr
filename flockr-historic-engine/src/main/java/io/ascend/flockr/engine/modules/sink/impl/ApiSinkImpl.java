package io.ascend.flockr.engine.modules.sink.impl;

import io.ascend.flockr.engine.config.ApiConfig;
import io.ascend.flockr.engine.modules.sink.Sink;
import io.ascend.flockr.engine.utils.ApiClient;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/**
 * Implementation of Sink interface for REST API destinations.
 *
 * <p>This class writes data to a REST API endpoint by:
 *
 * <ul>
 *   <li>Collecting user IDs from the dataset
 *   <li>Sending them to the API via ApiClient with rate limiting and batching
 *   <li>Including audience metadata (audience_key, action, expire_at) in requests
 * </ul>
 *
 * <p><b>Data Format:</b>
 *
 * <p>The sink extracts user IDs from the first column of the dataset and sends them to the API
 * along with:
 *
 * <ul>
 *   <li>audience_key: The audience name (maps to audienceName from application arguments)
 *   <li>action: The action ("append" or "remove")
 *   <li>expire_at: The expiration timestamp
 * </ul>
 *
 * <p><b>Rate Limiting:</b>
 *
 * <p>The ApiClient handles rate limiting and batching automatically based on the ApiConfig
 * settings.
 *
 * <p><b>Limitations:</b>
 *
 * <p>This sink does not support individual write operations (throws UnsupportedOperationException).
 * It only supports dataset-level writes.
 *
 * <p><b>Memory Considerations:</b>
 *
 * <p>The sink collects all user IDs to the driver node using collectAsList(), which may cause
 * memory issues with very large datasets. Consider this when processing extremely large cohorts.
 *
 * @see Sink
 * @see ApiConfig
 * @see io.ascend.flockr.engine.utils.ApiClient
 * @author Shivam-Raghuwanshi
 */
@Slf4j
public class ApiSinkImpl implements Sink<String> {

  /** API client for making HTTP requests with rate limiting. */
  private final ApiClient apiClient;

  /** API configuration (URL, rate limit, batch size, etc.). */
  private final ApiConfig apiConfig;

  /** Audience name (used as audience_key in API requests) - comes from application arguments. */
  private final String audienceName;

  /** Action type ("append" or "remove"). */
  private final String action;

  /** Expiration timestamp in epoch seconds. */
  private final Long expireAt;

  /**
   * Creates a new ApiSinkImpl instance.
   *
   * @param apiConfig API configuration (must not be null, and URL must be set).
   * @param audienceName The audience/cohort name.
   * @param action The action type ("append" or "remove").
   * @param expireAt The expiration timestamp in epoch seconds.
   * @throws IllegalArgumentException If apiConfig is null or URL is empty.
   */
  public ApiSinkImpl(ApiConfig apiConfig, String audienceName, String action, Long expireAt) {
    if (apiConfig == null) {
      throw new IllegalArgumentException("ApiConfig cannot be null");
    }
    if (apiConfig.getUrl() == null || apiConfig.getUrl().isEmpty()) {
      throw new IllegalArgumentException("API URL cannot be null or empty");
    }
    this.apiConfig = apiConfig;
    this.audienceName = audienceName;
    this.action = action;
    this.expireAt = expireAt;
    this.apiClient = new ApiClient(apiConfig);
    log.info(
        "ApiSinkImpl initialized for URL: {} with rate limit: {}/sec",
        apiConfig.getUrl(),
        apiConfig.getRateLimitPerSecond());
  }

  @Override
  public void write(String data) throws Exception {
    throw new UnsupportedOperationException(
        "API sink does not support individual write operations");
  }

  @Override
  public void writeDataset(Dataset<Row> dataset) throws Exception {
    if (dataset == null) {
      throw new IllegalArgumentException("Dataset cannot be null");
    }

    log.info(
        "Writing dataset to API: {} with rate limit: {}/sec",
        apiConfig.getUrl(),
        apiConfig.getRateLimitPerSecond());

    List<String> userIds = new ArrayList<>();
    try {
      List<Row> rows = dataset.select(dataset.columns()[0]).distinct().collectAsList();
      for (Row row : rows) {
        if (row.get(0) != null) {
          userIds.add(row.get(0).toString());
        }
      }
      log.info("Collected {} unique userIds for API call", userIds.size());

      apiClient.notifyAudienceUpdate(audienceName, userIds, action, expireAt);
      log.info("Successfully sent data to API");
    } catch (Exception e) {
      log.error("Failed to write dataset to API", e);
      throw new RuntimeException("Failed to write dataset to API", e);
    }
  }

  @Override
  public void flush() throws Exception {
    log.debug("Flush called on API sink - no operation needed");
  }
}

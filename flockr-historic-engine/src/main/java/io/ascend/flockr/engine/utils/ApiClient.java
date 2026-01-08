package io.ascend.flockr.engine.utils;

import io.ascend.flockr.engine.config.ApiConfig;
import io.ascend.flockr.engine.dto.AudienceUpdateRequest;
import io.ascend.flockr.engine.dto.builder.AudienceUpdateRequestFactory;
import io.ascend.flockr.engine.mapper.JsonMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP client for making REST API calls with rate limiting and batching.
 *
 * <p>This client provides:
 *
 * <ul>
 *   <li><b>Rate Limiting</b>: Enforces requests per second limit via {@link RateLimiter}
 *   <li><b>Batching</b>: Splits large requests into smaller batches
 *   <li><b>JSON Serialization</b>: Uses Jackson for type-safe JSON serialization
 *   <li><b>Custom Headers</b>: Supports project key and content type headers
 * </ul>
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * ApiConfig config = ApiConfig.fromConfig(...);
 * ApiClient client = new ApiClient(config);
 * client.notifyAudienceUpdate("cohort-1", userIds, "append", expireAt);
 * }</pre>
 *
 * <p><b>Rate Limiting:</b>
 *
 * <p>The client uses a {@link RateLimiter} to enforce rate limiting by tracking the time between
 * requests and sleeping if necessary to maintain the configured requests per second limit.
 *
 * <p><b>Batching:</b>
 *
 * <p>Large lists of user IDs are automatically split into batches based on the configured batch
 * size. Each batch is sent as a separate HTTP request.
 *
 * <p><b>Request Format:</b>
 *
 * <p>The client sends POST requests with JSON payloads serialized from {@link
 * AudienceUpdateRequest} POJOs in the following format:
 *
 * <pre>{@code
 * [
 *   {
 *     "user_id": 12345,
 *     "audience_key": "audience-1",
 *     "action": "append",
 *     "expire_at": 1735689599
 *   },
 *   ...
 * ]
 * }</pre>
 *
 * @see ApiConfig
 * @see RateLimiter
 * @see AudienceUpdateRequest
 * @author Shivam-Raghuwanshi
 */
@Slf4j
public class ApiClient {

  /** HTTP client instance for making requests. */
  private final HttpClient httpClient;

  /** API configuration (URL, rate limit, batch size, etc.). */
  private final ApiConfig apiConfig;

  /** Rate limiter for enforcing requests per second limit. */
  private final RateLimiter rateLimiter;

  /**
   * Creates a new ApiClient with default configuration (for testing).
   *
   * <p>This constructor creates a client without API configuration, which will simulate API calls
   * (log only) when {@link #notifyAudienceUpdate} is called.
   *
   * <p><b>Note:</b> This is primarily for testing. Use {@link #ApiClient(ApiConfig)} for production
   * use.
   */
  public ApiClient() {
    this.apiConfig = null;
    this.httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    this.rateLimiter = new RateLimiter(10); // Default 10 requests/sec for testing
  }

  /**
   * Creates a new ApiClient with the specified configuration.
   *
   * @param apiConfig The API configuration (must not be null).
   * @throws IllegalArgumentException If apiConfig is null.
   */
  public ApiClient(ApiConfig apiConfig) {
    if (apiConfig == null) {
      throw new IllegalArgumentException("ApiConfig cannot be null");
    }
    this.apiConfig = apiConfig;
    this.httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(apiConfig.getTimeoutSeconds()))
            .build();
    this.rateLimiter = new RateLimiter(apiConfig.getRateLimitPerSecond());
    log.info(
        "ApiClient initialized with rate limit: {}/sec, batch size: {}, timeout: {}s",
        apiConfig.getRateLimitPerSecond(),
        apiConfig.getBatchSize(),
        apiConfig.getTimeoutSeconds());
  }

  /**
   * Notifies the API about an audience update.
   *
   * <p>This method sends user IDs to the configured API endpoint in batches, with rate limiting
   * applied between batches. The request payload includes user IDs, cohort key (audience name),
   * action, and expiration timestamp.
   *
   * <p><b>Batching:</b>
   *
   * <p>If the number of user IDs exceeds the configured batch size, they are split into multiple
   * batches. Each batch is sent as a separate HTTP request.
   *
   * <p><b>Rate Limiting:</b>
   *
   * <p>Between each batch request, the client waits if necessary to maintain the configured
   * requests per second limit.
   *
   * @param audienceName The name of the audience/cohort.
   * @param userIds List of user IDs to send (will be batched if needed).
   * @param action The action ("append" or "remove").
   * @param expireAt The expiration timestamp in epoch seconds.
   * @throws RuntimeException If the API call fails or if rate limiting is interrupted.
   */
  public void notifyAudienceUpdate(
      String audienceName, List<String> userIds, String action, Long expireAt) {
    if (apiConfig == null) {
      log.warn("ApiConfig not provided, simulating API call");
      log.info("Calling API for audienceName: {} with {} users", audienceName, userIds.size());
      log.info("API call simulated successfully");
      return;
    }

    log.info("Calling API for audienceName: {} with {} users", audienceName, userIds.size());

    try {
      int batchSize = apiConfig.getBatchSize();
      int totalBatches = (userIds.size() + batchSize - 1) / batchSize;

      for (int i = 0; i < totalBatches; i++) {
        int start = i * batchSize;
        int end = Math.min(start + batchSize, userIds.size());
        List<String> batch = userIds.subList(start, end);

        rateLimiter.acquire();

        makeApiCall(audienceName, batch, action, expireAt, i + 1, totalBatches);

        log.debug("Processed batch {}/{} ({} users)", i + 1, totalBatches, batch.size());
      }

      log.info("Successfully sent all {} users to API in {} batches", userIds.size(), totalBatches);
    } catch (Exception e) {
      log.error("Failed to call API for audienceName: {}", audienceName, e);
      throw new RuntimeException("API call failed", e);
    }
  }

  /**
   * Makes a single API call with a batch of user IDs.
   *
   * <p>This method constructs the HTTP request with:
   *
   * <ul>
   *   <li>Content-Type header from configuration
   *   <li>x-project-key header if configured
   *   <li>JSON body serialized from {@link AudienceUpdateRequest} POJOs
   * </ul>
   *
   * @param audienceName The audience name (used as audience_key) - comes from application
   *     arguments.
   * @param userIds The batch of user IDs to send.
   * @param action The action ("append" or "remove").
   * @param expireAt The expiration timestamp.
   * @param batchNumber The current batch number (for logging).
   * @param totalBatches The total number of batches (for logging).
   * @throws Exception If the HTTP request fails or returns a non-2xx status code.
   */
  private void makeApiCall(
      String audienceName,
      List<String> userIds,
      String action,
      Long expireAt,
      int batchNumber,
      int totalBatches)
      throws Exception {
    String requestBody = buildRequestBody(audienceName, userIds, action, expireAt);

    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder()
            .uri(URI.create(apiConfig.getUrl()))
            .timeout(Duration.ofSeconds(apiConfig.getTimeoutSeconds()))
            .header("Content-Type", apiConfig.getContentType());

    if (apiConfig.getProjectKey() != null && !apiConfig.getProjectKey().isEmpty()) {
      requestBuilder.header("x-project-key", apiConfig.getProjectKey());
    }

    HttpRequest request =
        requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() >= 200 && response.statusCode() < 300) {
      log.debug(
          "API call successful for batch {}/{}: status={}",
          batchNumber,
          totalBatches,
          response.statusCode());
    } else {
      log.error(
          "API call failed for batch {}/{}: status={}, body={}",
          batchNumber,
          totalBatches,
          response.statusCode(),
          response.body());
      throw new RuntimeException("API call failed with status: " + response.statusCode());
    }
  }

  /**
   * Builds the JSON request body for the API call using POJOs and Jackson.
   *
   * <p>This method converts user IDs to {@link AudienceUpdateRequest} POJOs and serializes them to
   * JSON using Jackson's ObjectMapper.
   *
   * <p>The request body is a JSON array of objects, each containing:
   *
   * <ul>
   *   <li>user_id: The user ID (number or string, automatically detected)
   *   <li>audience_key: The audience name (maps to audienceName from application arguments)
   *   <li>action: The action ("append" or "remove")
   *   <li>expire_at: The expiration timestamp
   * </ul>
   *
   * @param audienceName The audience name (used as audience_key) - comes from application
   *     arguments.
   * @param userIds The list of user IDs.
   * @param action The action ("append" or "remove").
   * @param expireAt The expiration timestamp.
   * @return A JSON string representing the request body.
   * @throws RuntimeException If JSON serialization fails.
   */
  private String buildRequestBody(
      String audienceName, List<String> userIds, String action, Long expireAt) {
    try {
      List<AudienceUpdateRequest> requests =
          userIds.stream()
              .map(
                  userId ->
                      AudienceUpdateRequestFactory.fromString(
                          userId, audienceName, action, expireAt))
              .collect(Collectors.toList());

      return JsonMapper.getInstance().writeValueAsString(requests);
    } catch (Exception e) {
      log.error("Failed to serialize request body to JSON", e);
      throw new RuntimeException("Failed to build request body", e);
    }
  }
}

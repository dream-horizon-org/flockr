package io.ascend.flockr.engine.modules.sink.impl;

import io.ascend.flockr.engine.config.ApiConfig;
import io.ascend.flockr.engine.dto.AudienceMetadata;
import io.ascend.flockr.engine.dto.AudienceUpdateRequest;
import io.ascend.flockr.engine.dto.UserIdRow;
import io.ascend.flockr.engine.mapper.JsonMapper;
import io.ascend.flockr.engine.modules.sink.Sink;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;

/**
 * Implementation of Sink interface for REST API destinations.
 *
 * <p>This class writes data to a REST API endpoint by:
 *
 * @see Sink
 * @see ApiConfig
 * @author Shivam-Raghuwanshi
 */
@Slf4j
public class ApiSinkImpl implements Sink {

  /** API configuration (URL, rate limit, batch size, etc.). */
  private final ApiConfig apiConfig;

  /**
   * Creates a new ApiSinkImpl instance.
   *
   * @param apiConfig API configuration (must not be null, and URL must be set).
   * @throws IllegalArgumentException If apiConfig is null or URL is empty.
   */
  public ApiSinkImpl(ApiConfig apiConfig) {
    if (apiConfig == null) {
      throw new IllegalArgumentException("ApiConfig cannot be null");
    }
    if (apiConfig.getUrl() == null || apiConfig.getUrl().isEmpty()) {
      throw new IllegalArgumentException("API URL cannot be null or empty");
    }
    this.apiConfig = apiConfig;
    log.info(
        "ApiSinkImpl initialized for URL: {} with rate limit: {}/sec",
        apiConfig.getUrl(),
        apiConfig.getRateLimitPerSecond());
  }

  @Override
  public void write(Dataset<UserIdRow> userIds, AudienceMetadata metadata) {
    log.info(
        "Writing dataset to API: {} with rate limit: {}/sec",
        apiConfig.getUrl(),
        apiConfig.getRateLimitPerSecond());

    final ApiConfig config = this.apiConfig;
    final AudienceMetadata meta = metadata;

    userIds.foreachPartition(
        iterator -> {
          if (!iterator.hasNext()) {
            log.info("No records to process in this partition");
            return;
          }

          HttpClient httpClient =
              HttpClient.newBuilder()
                  .version(HttpClient.Version.HTTP_2)
                  .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                  .build();

          List<AudienceUpdateRequest> batchRequests = new ArrayList<>();
          int processedCount = 0;

          while (iterator.hasNext()) {
            UserIdRow userIdRow = iterator.next();

            AudienceUpdateRequest request =
                AudienceUpdateRequest.builder()
                    .userId(userIdRow.getUser_id())
                    .audienceKey(meta.getAudienceName())
                    .action(meta.getAction())
                    .expireAt(meta.getExpireAt())
                    .build();

            batchRequests.add(request);

            if (batchRequests.size() >= config.getBatchSize()) {
              sendBatchToApi(httpClient, config, batchRequests);
              processedCount += batchRequests.size();
              batchRequests.clear();
            }
          }

          if (!batchRequests.isEmpty()) {
            sendBatchToApi(httpClient, config, batchRequests);
            processedCount += batchRequests.size();
          }

          log.info("Successfully processed {} requests in partition", processedCount);
        });
    log.info("Successfully sent data to API");
  }

  /**
   * Sends a batch of audience update requests to the API.
   *
   * <p>This static helper method is used within the foreachPartition lambda to send batches of
   * requests. It constructs the HTTP request and handles errors.
   *
   * @param httpClient The HTTP client to use for sending requests.
   * @param config The API configuration.
   * @param requests The list of audience update requests to send.
   * @throws Exception If the API call fails.
   */
  private static void sendBatchToApi(
      HttpClient httpClient, ApiConfig config, List<AudienceUpdateRequest> requests)
      throws Exception {
    if (requests == null || requests.isEmpty()) {
      return;
    }

    log.debug("Sending batch of {} requests to API", requests.size());

    String requestBody = JsonMapper.getInstance().writeValueAsString(requests);

    HttpRequest.Builder requestBuilder =
        java.net.http.HttpRequest.newBuilder()
            .uri(URI.create(config.getUrl()))
            .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
            .header("Content-Type", config.getContentType());

    if (config.getProjectKey() != null && !config.getProjectKey().isEmpty()) {
      requestBuilder.header("x-project-key", config.getProjectKey());
    }

    HttpRequest request =
        requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() >= 200 && response.statusCode() < 300) {
      log.debug(
          "API call successful: status={}, batch_size={}", response.statusCode(), requests.size());
    } else {
      log.error(
          "API call failed: status={}, body={}, batch_size={}",
          response.statusCode(),
          response.body(),
          requests.size());
      throw new RuntimeException(
          "API call failed with status: " + response.statusCode() + ", body: " + response.body());
    }
  }
}

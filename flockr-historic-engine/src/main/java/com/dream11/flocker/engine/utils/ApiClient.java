package com.dream11.flocker.engine.utils;

import com.dream11.flocker.engine.config.ApiConfig;
import lombok.extern.slf4j.Slf4j;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class ApiClient {

    private final HttpClient httpClient;
    private final ApiConfig apiConfig;
    private final AtomicLong lastRequestTime = new AtomicLong(0);
    private final long minIntervalMillis;

    public ApiClient() {
        this.apiConfig = null;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.minIntervalMillis = 100; // Default 10 requests per second
    }

    public ApiClient(ApiConfig apiConfig) {
        if (apiConfig == null) {
            throw new IllegalArgumentException("ApiConfig cannot be null");
        }
        this.apiConfig = apiConfig;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(apiConfig.getTimeoutSeconds()))
                .build();
        this.minIntervalMillis = 1000 / apiConfig.getRateLimitPerSecond();
        log.info("ApiClient initialized with rate limit: {}/sec, batch size: {}, timeout: {}s",
            apiConfig.getRateLimitPerSecond(), apiConfig.getBatchSize(), apiConfig.getTimeoutSeconds());
    }

    public void notifyCohortUpdate(String cohortName, List<String> userIds) {
        if (apiConfig == null) {
            log.warn("ApiConfig not provided, simulating API call");
            log.info("Calling API for cohortName: {} with {} users", cohortName, userIds.size());
            log.info("API call simulated successfully");
            return;
        }

        log.info("Calling API for cohortName: {} with {} users", cohortName, userIds.size());

        try {
            int batchSize = apiConfig.getBatchSize();
            int totalBatches = (userIds.size() + batchSize - 1) / batchSize;

            for (int i = 0; i < totalBatches; i++) {
                int start = i * batchSize;
                int end = Math.min(start + batchSize, userIds.size());
                List<String> batch = userIds.subList(start, end);

                waitForRateLimit();

                makeApiCall(cohortName, batch, i + 1, totalBatches);

                log.debug("Processed batch {}/{} ({} users)", i + 1, totalBatches, batch.size());
            }

            log.info("Successfully sent all {} users to API in {} batches", userIds.size(), totalBatches);
        } catch (Exception e) {
            log.error("Failed to call API for cohortName: {}", cohortName, e);
            throw new RuntimeException("API call failed", e);
        }
    }

    private void waitForRateLimit() {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastRequestTime.get();
        long elapsed = currentTime - lastTime;

        if (elapsed < minIntervalMillis) {
            long waitTime = minIntervalMillis - elapsed;
            try {
                Thread.sleep(waitTime);
                log.debug("Rate limiting: waited {}ms", waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Rate limiting wait interrupted", e);
            }
        }

        lastRequestTime.set(System.currentTimeMillis());
    }

    private void makeApiCall(String cohortName, List<String> userIds, int batchNumber, int totalBatches) throws Exception {
        String requestBody = buildRequestBody(cohortName, userIds);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiConfig.getUrl()))
                .timeout(Duration.ofSeconds(apiConfig.getTimeoutSeconds()))
                .header("Content-Type", apiConfig.getContentType())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            log.debug("API call successful for batch {}/{}: status={}", batchNumber, totalBatches, response.statusCode());
        } else {
            log.error("API call failed for batch {}/{}: status={}, body={}",
                batchNumber, totalBatches, response.statusCode(), response.body());
            throw new RuntimeException("API call failed with status: " + response.statusCode());
        }
    }

    private String buildRequestBody(String cohortName, List<String> userIds) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"cohortName\":\"").append(cohortName).append("\",");
        json.append("\"userIds\":[");
        for (int i = 0; i < userIds.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("\"").append(escapeJson(userIds.get(i))).append("\"");
        }
        json.append("]}");
        return json.toString();
    }

    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

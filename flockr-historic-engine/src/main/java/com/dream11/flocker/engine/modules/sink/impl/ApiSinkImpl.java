package com.dream11.flocker.engine.modules.sink.impl;

import com.dream11.flocker.engine.config.ApiConfig;
import com.dream11.flocker.engine.modules.sink.Sink;
import com.dream11.flocker.engine.utils.ApiClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ApiSinkImpl implements Sink<String> {

    private final ApiClient apiClient;
    private final ApiConfig apiConfig;
    private final String cohortName;

    public ApiSinkImpl(ApiConfig apiConfig, String cohortName) {
        if (apiConfig == null) {
            throw new IllegalArgumentException("ApiConfig cannot be null");
        }
        if (apiConfig.getUrl() == null || apiConfig.getUrl().isEmpty()) {
            throw new IllegalArgumentException("API URL cannot be null or empty");
        }
        this.apiConfig = apiConfig;
        this.cohortName = cohortName;
        this.apiClient = new ApiClient(apiConfig);
        log.info("ApiSinkImpl initialized for URL: {} with rate limit: {}/sec",
            apiConfig.getUrl(), apiConfig.getRateLimitPerSecond());
    }

    @Override
    public void write(String data) throws Exception {
        // Not used for API sink - we use writeDataset instead
        throw new UnsupportedOperationException("API sink does not support individual write operations");
    }

    @Override
    public void writeDataset(Dataset<Row> dataset) throws Exception {
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset cannot be null");
        }

        log.info("Writing dataset to API: {} with rate limit: {}/sec",
            apiConfig.getUrl(), apiConfig.getRateLimitPerSecond());

        // Collect userIds from first column
        List<String> userIds = new ArrayList<>();
        try {
            List<Row> rows = dataset.select(dataset.columns()[0]).distinct().collectAsList();
            for (Row row : rows) {
                if (row.get(0) != null) {
                    userIds.add(row.get(0).toString());
                }
            }
            log.info("Collected {} unique userIds for API call", userIds.size());

            apiClient.notifyCohortUpdate(cohortName, userIds);
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


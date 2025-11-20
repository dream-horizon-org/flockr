package com.ascend.flockr.client.spark.dto.request;

import com.ascend.flockr.client.spark.dto.destination.DestinationDetails;
import com.ascend.flockr.domain.dataconnectors.DataSourceDetails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class HistoricBatchSparkJobRequest {
    @NotBlank(message = "query is required")
    private String query;

    @NotNull(message = "dataSourceDetails is required")
    private DataSourceDetails dataSourceDetails;

    @NotEmpty(message = "destinations cannot be empty")
    private List<DestinationDetails> destinations;

    @NotBlank(message = "cohortName is required")
    private String cohortName;
}

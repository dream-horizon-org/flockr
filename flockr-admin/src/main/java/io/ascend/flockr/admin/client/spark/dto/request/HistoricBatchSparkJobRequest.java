package io.ascend.flockr.admin.client.spark.dto.request;

import io.ascend.flockr.admin.client.spark.dto.destination.DestinationDetails;
import io.ascend.flockr.admin.domain.dataconnectors.DataSourceDetails;
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

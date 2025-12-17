package io.ascend.flockr.admin.client.spark.dto.destination;

import com.fasterxml.jackson.annotation.JsonRawValue;

public class DestinationDetails {
  private Long id; // from DataSinkDetails.id
  private String name; // from DataSinkDetails.name
  private String type; // from DataSinkDetails.type
  private String status; // from DataSinkDetails.status

  @JsonRawValue
  private String config; // Serialize DataSinkDetails.config (JsonObject) to JSON string
}

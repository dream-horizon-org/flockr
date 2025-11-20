package com.ascend.flockr.domain.dataconnectors;

import com.ascend.flockr.constants.dataconnectors.DataConnectorTypeConstants;
import com.ascend.flockr.constants.dataconnectors.DataSinkConstants;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSinkDetails {
  private Long id;
  private String name;
  private Long typeId;
  private String type;
  private JsonObject config;
  private String status;
  private String createdBy;

  public static DataSinkDetails mapSinkRow(Row row) {
    JsonObject config = null;
    String configStr = row.getString(DataSinkConstants.CONFIG);
    if (configStr != null && !configStr.isBlank()) {
      config = new JsonObject(configStr);
    }

    return DataSinkDetails.builder()
        .id(row.getLong(DataSinkConstants.ID))
        .name(row.getString(DataSinkConstants.NAME))
        .typeId(row.getLong(DataSinkConstants.TYPE_ID))
        .type(row.getString(DataConnectorTypeConstants.TYPE))
        .config(config)
        .status(row.getString(DataSinkConstants.STATUS))
        .createdBy(row.getString(DataSinkConstants.CREATED_BY))
        .build();
  }
}

package com.ascend.flockr.domain.dataconnectors;

import com.ascend.flockr.constants.dataconnectors.DataConnectorTypeConstants;
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
public class DataConnectorType {
  private Long id;
  private String kind;
  private String type;
  private String displayName;
  private JsonObject configSchema;
  private Boolean active;

  public static DataConnectorType mapTypeRow(Row row) {
    JsonObject configSchema = null;
    String configSchemaStr = row.getString(DataConnectorTypeConstants.CONFIG_SCHEMA);
    if (configSchemaStr != null && !configSchemaStr.isBlank()) {
      configSchema = new JsonObject(configSchemaStr);
    }

    return DataConnectorType.builder()
        .id(row.getLong(DataConnectorTypeConstants.ID))
        .kind(row.getString(DataConnectorTypeConstants.KIND))
        .type(row.getString(DataConnectorTypeConstants.TYPE))
        .displayName(row.getString(DataConnectorTypeConstants.DISPLAY_NAME))
        .configSchema(configSchema)
        .active(row.getBoolean(DataConnectorTypeConstants.IS_ACTIVE))
        .build();
  }
}

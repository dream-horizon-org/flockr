package io.ascend.flockr.admin.domain.dataconnectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.ascend.flockr.admin.constants.dataconnectors.DataConnectorTypeConstants;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataConnectorType {
  private Long id;
  private String kind;
  private String type;
  private String displayName;
  private JsonObject configSchema;
  private Boolean active;

  public static DataConnectorType mapTypeRow(Row row) {
    return DataConnectorType.builder()
        .id(row.getLong(DataConnectorTypeConstants.ID))
        .kind(row.getString(DataConnectorTypeConstants.KIND))
        .type(row.getString(DataConnectorTypeConstants.TYPE))
        .displayName(row.getString(DataConnectorTypeConstants.DISPLAY_NAME))
        .configSchema(row.getJsonObject(DataConnectorTypeConstants.CONFIG_SCHEMA))
        .active(row.getBoolean(DataConnectorTypeConstants.IS_ACTIVE))
        .build();
  }
}

package com.ascend.flockr.domain.dataconnectors;

import com.ascend.flockr.constants.dataconnectors.DataConnectorTypeConstants;
import com.ascend.flockr.constants.dataconnectors.DataSourceConstants;
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
public class DataSourceDetails {
  private Long id;
  private String name;
  private Long typeId;
  private String type;
  private JsonObject config;
  private String status;
  private String createdBy;

  public static DataSourceDetails mapSourceRow(Row row) {
    return DataSourceDetails.builder()
        .id(row.getLong(DataSourceConstants.ID))
        .name(row.getString(DataSourceConstants.NAME))
        .typeId(row.getLong(DataSourceConstants.TYPE_ID))
        .type(row.getString(DataConnectorTypeConstants.TYPE))
        .config(row.getJsonObject(DataSourceConstants.CONFIG))
        .status(row.getString(DataSourceConstants.STATUS))
        .createdBy(row.getString(DataSourceConstants.CREATED_BY))
        .build();
  }
}

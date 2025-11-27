package io.ascend.flockr.admin.domain.dataconnectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.ascend.flockr.admin.constants.dataconnectors.DataConnectorTypeConstants;
import io.ascend.flockr.admin.constants.dataconnectors.DataSourceConstants;
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

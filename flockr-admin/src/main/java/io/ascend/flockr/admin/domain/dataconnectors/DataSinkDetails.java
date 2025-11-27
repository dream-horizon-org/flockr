package io.ascend.flockr.admin.domain.dataconnectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.ascend.flockr.admin.constants.dataconnectors.DataConnectorTypeConstants;
import io.ascend.flockr.admin.constants.dataconnectors.DataSinkConstants;
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
public class DataSinkDetails {
  private Long id;
  private String name;
  private Long typeId;
  private String type;
  private JsonObject config;
  private String status;
  private String createdBy;

  public static DataSinkDetails mapSinkRow(Row row) {
    return DataSinkDetails.builder()
        .id(row.getLong(DataSinkConstants.ID))
        .name(row.getString(DataSinkConstants.NAME))
        .typeId(row.getLong(DataSinkConstants.TYPE_ID))
        .type(row.getString(DataConnectorTypeConstants.TYPE))
        .config(row.getJsonObject(DataSourceConstants.CONFIG))
        .status(row.getString(DataSinkConstants.STATUS))
        .createdBy(row.getString(DataSinkConstants.CREATED_BY))
        .build();
  }
}

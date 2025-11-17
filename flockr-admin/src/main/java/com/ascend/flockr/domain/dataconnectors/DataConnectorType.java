package com.ascend.flockr.domain.dataconnectors;

import com.ascend.flockr.constants.dataconnectors.DataConnectorTypeConstants;
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
  private Boolean active;

  public static DataConnectorType mapTypeRow(Row row) {
    return DataConnectorType.builder()
        .id(row.getLong(DataConnectorTypeConstants.ID))
        .kind(row.getString(DataConnectorTypeConstants.KIND))
        .type(row.getString(DataConnectorTypeConstants.TYPE))
        .displayName(row.getString(DataConnectorTypeConstants.DISPLAY_NAME))
        .active(row.getBoolean(DataConnectorTypeConstants.IS_ACTIVE))
        .build();
  }
}

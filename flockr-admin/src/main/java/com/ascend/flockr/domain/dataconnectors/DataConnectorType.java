package com.ascend.flockr.domain.dataconnectors;

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
        .id(row.getLong("id"))
        .kind(row.getString("kind"))
        .type(row.getString("type"))
        .displayName(row.getString("display_name"))
        .active(row.getBoolean("is_active"))
        .build();
  }
}

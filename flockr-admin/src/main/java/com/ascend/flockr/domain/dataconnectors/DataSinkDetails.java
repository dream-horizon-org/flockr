package com.ascend.flockr.domain.dataconnectors;

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
    return DataSinkDetails.builder()
        .id(row.getLong("id"))
        .name(row.getString("name"))
        .typeId(row.getLong("type_id"))
        .type(row.getString("type"))
        .config(row.getJsonObject("config"))
        .status(row.getString("status"))
        .createdBy(row.getString("created_by"))
        .build();
  }
}

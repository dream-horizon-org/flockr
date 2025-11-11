package com.ascend.flockr.io.request.source.batch;

import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@SuperBuilder
@Jacksonized
@EqualsAndHashCode(callSuper = true)
public class MySqlJdbcSource extends AbstractJdbcDataSource {

  @Builder.Default BatchSourceType type = BatchSourceType.JDBC_MYSQL;

  @Builder.Default String driverClass = "com.mysql.cj.jdbc.Driver";

  // MySQL-specific niceties (all optional)
  String sslMode; // e.g., "REQUIRED", "VERIFY_CA", "DISABLED"
  Map<String, String> sessionVars; // e.g., {"sql_mode":"ANSI","time_zone":"+00:00"}

  @Override
  public BatchSourceType getType() {
    return type;
  }
}

package com.ascend.flockr.io.request.source.batch; // AbstractJdbcDataSource.java

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public abstract sealed class AbstractJdbcDataSource implements BatchDataSource
    permits MySqlJdbcSource, RedshiftJdbcSource {

  /** Implemented by subclasses to return their BatchType (e.g., JDBC_MYSQL) */
  /** Concrete classes define their own driverClass */
  public abstract String getDriverClass();

  public abstract BatchSourceType getType();

  @NotBlank private final String url; // JDBC URL
  @NotBlank private final String dbtable; // table or "(SELECT ...) tmp"

  private final String user;
  private final String password;
}

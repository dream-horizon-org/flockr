package com.ascend.flockr.io.request.sink.batch; // AbstractJdbcSink.java

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public abstract sealed class AbstractJdbcSink implements BatchDataSink
    permits MySqlJdbcSink, RedshiftJdbcSink {

  public abstract BatchSinkType getType();

  @NotBlank private final String url; // JDBC URL
  @NotBlank private final String table; // target table to write into

  private final String user;
  private final String password;
}

package com.ascend.flockr.io.request.sink.batch; // MySqlJdbcSink.java

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@SuperBuilder
@Jacksonized
@EqualsAndHashCode(callSuper = true)
public class MySqlJdbcSink extends AbstractJdbcSink {

  @Builder.Default BatchSinkType type = BatchSinkType.JDBC_MYSQL;

  @Override
  public BatchSinkType getType() {
    return type;
  }
}

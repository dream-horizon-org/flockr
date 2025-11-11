package com.ascend.flockr.io.request.sink.batch; // RedshiftJdbcSink.java

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@SuperBuilder
@Jacksonized
@EqualsAndHashCode(callSuper = true)
public class RedshiftJdbcSink extends AbstractJdbcSink {

  @Builder.Default BatchSinkType type = BatchSinkType.JDBC_REDSHIFT;

  @Override
  public BatchSinkType getType() {
    return type;
  }
}

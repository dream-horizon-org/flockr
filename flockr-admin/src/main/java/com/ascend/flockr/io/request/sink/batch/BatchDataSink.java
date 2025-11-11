package com.ascend.flockr.io.request.sink.batch; // BatchDataSink.java

import com.fasterxml.jackson.annotation.*;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true)
// BatchDataSink.java
@JsonSubTypes({
  @JsonSubTypes.Type(value = S3Sink.class, name = "S3"),
  @JsonSubTypes.Type(value = MySqlJdbcSink.class, name = "JDBC_MYSQL"),
  @JsonSubTypes.Type(value = RedshiftJdbcSink.class, name = "JDBC_REDSHIFT")
})
public sealed interface BatchDataSink permits S3Sink, AbstractJdbcSink {
  BatchSinkType getType();
}

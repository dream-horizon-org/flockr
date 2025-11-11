package com.ascend.flockr.io.request.source.batch; // BatchDataSource.java  (sealed base

// for batch)

import com.fasterxml.jackson.annotation.*;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AthenaGlueDataSource.class, name = "ATHENA_GLUE"),
  @JsonSubTypes.Type(value = MySqlJdbcSource.class, name = "JDBC_MYSQL"),
  @JsonSubTypes.Type(value = RedshiftJdbcSource.class, name = "JDBC_REDSHIFT")
})
public sealed interface BatchDataSource permits AbstractJdbcDataSource, AthenaGlueDataSource {
  BatchSourceType getType();
}

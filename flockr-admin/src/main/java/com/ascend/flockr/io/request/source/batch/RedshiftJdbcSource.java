package com.ascend.flockr.io.request.source.batch;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@SuperBuilder
@Jacksonized
@EqualsAndHashCode(callSuper = true)
public class RedshiftJdbcSource extends AbstractJdbcDataSource {

  @Builder.Default BatchSourceType type = BatchSourceType.JDBC_REDSHIFT;

  @Builder.Default String driverClass = "com.amazon.redshift.jdbc.Driver";

  // Redshift-specific (optional)
  Integer fetchSize; // JDBC fetch size hint
  String iamRoleArn; // if using IAM-based auth flows

  @Override
  public BatchSourceType getType() {
    return type;
  }
}

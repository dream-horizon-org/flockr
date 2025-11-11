package com.ascend.flockr.io.request.sink.batch; // S3Sink.java

import java.util.Map;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class S3Sink implements BatchDataSink {

  @Builder.Default BatchSinkType type = BatchSinkType.S3;

  /** e.g., s3://bucket/prefix/ */
  String path;

  /** append|overwrite|ignore|errorifexists (Spark save modes) */
  @Builder.Default String parquetSaveMode = "overwrite";

  /** Extra Spark options for parquet writer (compression, partitionBy via job, etc.) */
  @Singular("parquetOption")
  Map<String, String> parquetOptions;

  @Override
  public BatchSinkType getType() {
    return type;
  }
}

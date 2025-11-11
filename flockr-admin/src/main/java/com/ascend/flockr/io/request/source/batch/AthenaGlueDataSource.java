package com.ascend.flockr.io.request.source.batch;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class AthenaGlueDataSource implements BatchDataSource {
  @Builder.Default BatchSourceType type = BatchSourceType.ATHENA_GLUE;
  @NotBlank String region;
  @NotBlank String database;
  @NotBlank String table;
  String catalog; // default "AwsDataCatalog" if null at usage time
}

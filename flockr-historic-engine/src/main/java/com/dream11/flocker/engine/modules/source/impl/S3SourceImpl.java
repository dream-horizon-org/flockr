package com.dream11.flocker.engine.modules.source.impl;

import com.dream11.flocker.engine.config.S3Config;
import com.dream11.flocker.engine.enums.FormatTypes;
import com.dream11.flocker.engine.modules.source.Source;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

@Slf4j
public class S3SourceImpl implements Source<Dataset<Row>> {

  private final S3Config sourceConfig;
  private final SparkSession sparkSession;

  public S3SourceImpl(S3Config sourceConfig, SparkSession sparkSession) {
    if (sourceConfig == null || sparkSession == null) {
      throw new IllegalArgumentException("S3Config or SparkSession cannot be null");
    }
    this.sourceConfig = sourceConfig;
    this.sparkSession = sparkSession;
    sourceConfig.configureHadoop(sparkSession.sparkContext().hadoopConfiguration());
    log.info("S3 source configured: path={}", sourceConfig.getS3Path());
  }

  @Override
  public Dataset<Row> read() throws Exception {
    String s3Path = sourceConfig.getS3Path();
    log.info("Reading data from S3 path: {} in format: {}", s3Path, sourceConfig.getFormat());

    var reader = sparkSession.read().format(sourceConfig.getSparkFormat());

    if (sourceConfig.getFormat() == FormatTypes.CSV) {
      if (sourceConfig.getOptions() == null || !sourceConfig.getOptions().containsKey("header")) {
        reader = reader.option("header", "true");
      }
      if (sourceConfig.getOptions() == null
          || !sourceConfig.getOptions().containsKey("inferSchema")) {
        reader = reader.option("inferSchema", "true");
      }
    }

    if (sourceConfig.getOptions() != null) {
      sourceConfig.getOptions().forEach(reader::option);
    }

    Dataset<Row> data = reader.load(s3Path);

    if (sourceConfig.getPartitions() > 0) {
      data = data.repartition(sourceConfig.getPartitions());
    }

    log.info("Successfully read data from S3 path: {}", s3Path);
    return data;
  }
}

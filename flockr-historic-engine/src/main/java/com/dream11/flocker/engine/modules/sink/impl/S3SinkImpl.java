package com.dream11.flocker.engine.modules.sink.impl;

import com.dream11.flocker.engine.config.S3Config;
import com.dream11.flocker.engine.constants.Constants;
import com.dream11.flocker.engine.modules.sink.Sink;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

/**
 * Implementation of Sink interface for Amazon S3 destinations.
 *
 * <p>This class writes Spark datasets to S3 in various formats (Parquet, CSV, JSON) with support
 * for:
 *
 * <ul>
 *   <li>Multiple write modes (append, overwrite, etc.)
 *   <li>Compression (snappy, gzip, etc.)
 *   <li>Custom partitioning
 *   <li>Additional Spark options
 *   <li>AWS credentials configuration
 * </ul>
 *
 * <p><b>Supported Formats:</b>
 *
 * <ul>
 *   <li>Parquet (default)
 *   <li>CSV
 *   <li>JSON
 * </ul>
 *
 * <p><b>Write Modes:</b>
 *
 * <ul>
 *   <li>append: Add data to existing files
 *   <li>overwrite: Replace existing data (default)
 *   <li>error: Fail if data already exists
 *   <li>ignore: Do nothing if data already exists
 * </ul>
 *
 * <p><b>Credentials:</b>
 *
 * <p>The sink configures Hadoop/Spark S3A filesystem settings with AWS credentials from the
 * S3Config. Supports both permanent and temporary credentials (with session tokens).
 *
 * <p><b>Individual Writes:</b>
 *
 * <p>Individual write operations (write(String)) are supported by converting the JSON string to a
 * dataset and writing it.
 *
 * @see Sink
 * @see S3Config
 * @author Shivam-Raghuwanshi
 */
@Slf4j
public class S3SinkImpl implements Sink<String> {

  /** S3 configuration (bucket, path, format, compression, etc.). */
  private final S3Config sinkConfig;

  /** Spark session for writing datasets. */
  private final SparkSession sparkSession;

  /** Write mode (append, overwrite, etc.). */
  private final String writeMode;

  /** Full S3 output path (s3a://bucket/path). */
  private final String outputPath;

  /**
   * Creates a new S3SinkImpl with default write mode and output path.
   *
   * <p>Uses overwrite mode and the S3 path from sinkConfig.
   *
   * @param sinkConfig S3 configuration (must not be null).
   * @param sparkSession Spark session (must not be null).
   */
  public S3SinkImpl(S3Config sinkConfig, SparkSession sparkSession) {
    this(sinkConfig, sparkSession, null, null);
  }

  /**
   * Creates a new S3SinkImpl with specified write mode and output path.
   *
   * <p>If writeMode is null, uses the write mode from sinkConfig, or defaults to overwrite. If
   * outputPath is null, uses the S3 path from sinkConfig.
   *
   * <p>Configures Hadoop S3A filesystem settings with AWS credentials from sinkConfig.
   *
   * @param sinkConfig S3 configuration (must not be null).
   * @param sparkSession Spark session (must not be null).
   * @param writeMode Write mode (append, overwrite, etc.). If null, uses config or defaults to
   *     overwrite.
   * @param outputPath Full S3 output path. If null, uses path from sinkConfig.
   * @throws IllegalArgumentException If sinkConfig or sparkSession is null.
   */
  public S3SinkImpl(
      S3Config sinkConfig, SparkSession sparkSession, String writeMode, String outputPath) {
    if (sinkConfig == null || sparkSession == null) {
      throw new IllegalArgumentException("S3Config and SparkSession cannot be null");
    }
    this.sinkConfig = sinkConfig;
    this.sparkSession = sparkSession;
    this.writeMode =
        writeMode != null
            ? writeMode
            : (sinkConfig.getWriteMode() != null
                ? sinkConfig.getWriteMode()
                : Constants.WRITE_MODE_OVERWRITE);
    this.outputPath = outputPath != null ? outputPath : sinkConfig.getS3Path();
    sinkConfig.configureHadoop(sparkSession.sparkContext().hadoopConfiguration());
    log.info("S3 sink configured: path={}, mode={}", this.outputPath, this.writeMode);
  }

  @Override
  public void write(String data) throws Exception {
    if (data == null || data.trim().isEmpty()) {
      throw new IllegalArgumentException("Data cannot be null or empty");
    }

    Dataset<Row> singleRow =
        sparkSession
            .read()
            .json(
                sparkSession.createDataset(
                    java.util.Arrays.asList(data), org.apache.spark.sql.Encoders.STRING()));
    writeDataset(singleRow);
  }

  @Override
  public void writeDataset(Dataset<Row> dataset) throws Exception {
    if (dataset == null) {
      throw new IllegalArgumentException("Dataset cannot be null");
    }

    log.info("Writing dataset to S3 path: {} with mode: {}", outputPath, writeMode);

    Dataset<Row> datasetToWrite = dataset;
    if (sinkConfig.getPartitions() > 0) {
      log.debug("Repartitioning dataset to {} partitions", sinkConfig.getPartitions());
      datasetToWrite = dataset.coalesce(sinkConfig.getPartitions());
    }

    var writer = datasetToWrite.write().mode(writeMode).format(sinkConfig.getSparkFormat());

    if (sinkConfig.getCompression() != null && !sinkConfig.getCompression().isEmpty()) {
      writer.option("compression", sinkConfig.getCompression());
      log.debug("Compression enabled: {}", sinkConfig.getCompression());
    }

    if (sinkConfig.getOptions() != null && !sinkConfig.getOptions().isEmpty()) {
      sinkConfig.getOptions().forEach(writer::option);
      log.debug("Applied {} custom options", sinkConfig.getOptions().size());
    }

    writer.save(outputPath);
    log.info("Successfully wrote dataset to S3 path: {}", outputPath);
  }
}

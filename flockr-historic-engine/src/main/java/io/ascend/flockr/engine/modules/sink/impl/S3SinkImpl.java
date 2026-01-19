package io.ascend.flockr.engine.modules.sink.impl;

import static org.apache.spark.sql.functions.lit;

import io.ascend.flockr.engine.config.S3Config;
import io.ascend.flockr.engine.constants.Constants;
import io.ascend.flockr.engine.dto.AudienceMetadata;
import io.ascend.flockr.engine.dto.UserIdRow;
import io.ascend.flockr.engine.modules.sink.Sink;
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
public class S3SinkImpl implements Sink {

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
  public void write(Dataset<UserIdRow> userIds, AudienceMetadata metadata) {
    log.info("Writing dataset to S3 path: {} with mode: {}", outputPath, writeMode);

    // Add metadata columns
    Dataset<Row> dataWithMetadata =
        userIds
            .toDF()
            .withColumn(Constants.AUDIENCE_NAME_COLUMN, lit(metadata.getAudienceName()))
            .withColumn(Constants.ACTION_COLUMN, lit(metadata.getAction()))
            .withColumn(Constants.EXPIRE_AT_COLUMN, lit(metadata.getExpireAt()));

    dataWithMetadata.write().mode(writeMode).format(sinkConfig.getSparkFormat()).save(outputPath);
    log.info("Successfully wrote dataset to S3 path: {}", outputPath);
  }
}

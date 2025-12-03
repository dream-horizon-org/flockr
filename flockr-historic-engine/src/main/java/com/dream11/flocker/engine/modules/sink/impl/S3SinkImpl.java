package com.dream11.flocker.engine.modules.sink.impl;

import com.dream11.flocker.engine.config.S3Config;
import com.dream11.flocker.engine.constants.Constants;
import com.dream11.flocker.engine.modules.sink.Sink;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

@Slf4j
public class S3SinkImpl implements Sink<String> {

    private final S3Config sinkConfig;
    private final SparkSession sparkSession;
    private final String writeMode;
    private final String outputPath;

    public S3SinkImpl(S3Config sinkConfig, SparkSession sparkSession) {
        this(sinkConfig, sparkSession, null, null);
    }

    public S3SinkImpl(S3Config sinkConfig, SparkSession sparkSession, String writeMode, String outputPath) {
        if (sinkConfig == null || sparkSession == null) {
            throw new IllegalArgumentException("S3Config and SparkSession cannot be null");
        }
        this.sinkConfig = sinkConfig;
        this.sparkSession = sparkSession;
        this.writeMode = writeMode != null ? writeMode :
            (sinkConfig.getWriteMode() != null ? sinkConfig.getWriteMode() : Constants.WRITE_MODE_OVERWRITE);
        this.outputPath = outputPath != null ? outputPath : sinkConfig.getS3Path();
        sinkConfig.configureHadoop(sparkSession.sparkContext().hadoopConfiguration());
        log.info("S3 sink configured: path={}, mode={}", this.outputPath, this.writeMode);
    }


    @Override
    public void write(String data) throws Exception {
        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        Dataset<Row> singleRow = sparkSession.read().json(sparkSession.createDataset(
            java.util.Arrays.asList(data),
            org.apache.spark.sql.Encoders.STRING()
        ));
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


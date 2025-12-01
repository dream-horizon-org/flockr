package com.dream11.flocker.engine.modules.source.impl;

import com.dream11.flocker.engine.config.AthenaConfig;
import com.dream11.flocker.engine.config.S3Config;
import com.dream11.flocker.engine.enums.SourceTypes;
import com.dream11.flocker.engine.modules.source.Source;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class SourceFactory {
    
    public static Source<Dataset<Row>> createSource(SourceTypes sourceType, Object config, SparkSession sparkSession) {
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null for source type: " + sourceType);
        }
        
        return switch (sourceType) {
            case S3 -> {
                if (!(config instanceof S3Config)) {
                    throw new IllegalArgumentException("S3 source requires S3Config, got: " + config.getClass().getName());
                }
                yield new S3SourceImpl((S3Config) config, sparkSession);
            }
            case ATHENA -> {
                if (!(config instanceof AthenaConfig)) {
                    throw new IllegalArgumentException("Athena source requires AthenaConfig, got: " + config.getClass().getName());
                }
                yield new AthenaSourceImpl((AthenaConfig) config, sparkSession);
            }
            case REDSHIFT -> throw new UnsupportedOperationException("Redshift source implementation not yet available");
            case KAFKA -> throw new UnsupportedOperationException("Kafka source implementation not yet available");
            default -> throw new IllegalArgumentException("Unsupported source type: " + sourceType);
        };
    }
}


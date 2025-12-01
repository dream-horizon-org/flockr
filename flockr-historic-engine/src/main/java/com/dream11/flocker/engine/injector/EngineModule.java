package com.dream11.flocker.engine.injector;

import com.dream11.flocker.engine.config.ApiConfig;
import com.dream11.flocker.engine.config.AthenaConfig;
import com.dream11.flocker.engine.config.ConnectorConfig;
import com.dream11.flocker.engine.config.KafkaConfig;
import com.dream11.flocker.engine.config.S3Config;
import com.dream11.flocker.engine.enums.SourceTypes;
import com.dream11.flocker.engine.modules.sink.Sink;
import com.dream11.flocker.engine.modules.source.Source;
import com.dream11.flocker.engine.modules.source.impl.SourceFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

@Slf4j
public class EngineModule extends AbstractModule {

    private final SparkSession sparkSession;
    private final List<ConnectorConfig> sourceConfigs;
    private final List<ConnectorConfig> sinkConfigs;
    @SuppressWarnings("unused")
    private final String cohortId;
    @SuppressWarnings("unused")
    private final String action;

    public EngineModule(SparkSession sparkSession,
            List<ConnectorConfig> sourceConfigs,
            List<ConnectorConfig> sinkConfigs,
            String cohortId, String action) {
        this.sparkSession = sparkSession;
        this.sourceConfigs = sourceConfigs;
        this.sinkConfigs = sinkConfigs;
        this.cohortId = cohortId;
        this.action = action;
        log.debug("EngineModule initialized with action: {}", action);
    }

    @Override
    protected void configure() {
        bind(SparkSession.class).toInstance(sparkSession);
    }

    @Provides
    @Singleton
    @Named("sources")
    public List<Source<Dataset<Row>>> provideSources() {
        log.debug("Providing Source instances");
        List<Source<Dataset<Row>>> sources = new ArrayList<>();

        for (ConnectorConfig config : sourceConfigs) {
            SourceTypes type = SourceTypes.valueOf(config.getType().toUpperCase());
            Object configObj = config.getConfig();

            if (configObj instanceof com.typesafe.config.Config) {
                com.typesafe.config.Config typesafeConfig = (com.typesafe.config.Config) configObj;
                configObj = switch (type) {
                    case S3 -> S3Config.fromConfig(typesafeConfig);
                    case ATHENA -> AthenaConfig.fromConfig(typesafeConfig);
                    case KAFKA -> KafkaConfig.fromConfig(typesafeConfig);
                    default -> throw new IllegalArgumentException("Unsupported source type: " + type);
                };
            }

            sources.add(SourceFactory.createSource(type, configObj, sparkSession));
        }
        return sources;
    }

    @Provides
    @Singleton
    @Named("sinks")
    public List<Sink<String>> provideSinks() {
        log.debug("Providing Sink instances");
        List<Sink<String>> sinks = new ArrayList<>();

        for (ConnectorConfig config : sinkConfigs) {
            String type = config.getType().toUpperCase();
            if ("S3".equals(type)) {
                S3Config s3Config;
                Object configObj = config.getConfig();

                if (configObj instanceof S3Config) {
                    s3Config = (S3Config) configObj;
                } else if (configObj instanceof com.typesafe.config.Config) {
                    s3Config = S3Config.fromConfig((com.typesafe.config.Config) configObj);
                } else {
                    throw new IllegalArgumentException(
                            "Unknown config type for S3 sink: " + configObj.getClass().getName());
                }

                String writeMode = s3Config.getWriteMode() != null ? s3Config.getWriteMode()
                        : com.dream11.flocker.engine.constants.Constants.WRITE_MODE_APPEND;
                String outputPath = "s3a://" + s3Config.getBucket() + "/" + s3Config.getPath();

                sinks.add(new com.dream11.flocker.engine.modules.sink.impl.S3SinkImpl(
                        s3Config, sparkSession, writeMode, outputPath));
                log.info("Added S3 sink: {}", outputPath);
            } else if ("API".equals(type)) {
                Object configObj = config.getConfig();
                ApiConfig apiConfig;
                
                if (configObj instanceof ApiConfig) {
                    apiConfig = (ApiConfig) configObj;
                } else if (configObj instanceof com.typesafe.config.Config) {
                    apiConfig = ApiConfig.fromConfig((com.typesafe.config.Config) configObj);
                } else {
                    throw new IllegalArgumentException(
                            "Unknown config type for API sink: " + configObj.getClass().getName());
                }
                
                sinks.add(new com.dream11.flocker.engine.modules.sink.impl.ApiSinkImpl(apiConfig, cohortId));
                log.info("Added API sink with rate limit: {}/sec", apiConfig.getRateLimitPerSecond());
            } else if ("KAFKA".equals(type)) {
                log.warn("Kafka sink not yet implemented, skipping");
            }
        }
        
        return sinks;
    }
}

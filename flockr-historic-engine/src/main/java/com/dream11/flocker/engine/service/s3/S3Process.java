package com.dream11.flocker.engine.service.s3;

import com.dream11.flocker.engine.modules.sink.Sink;
import com.dream11.flocker.engine.modules.source.Source;
import com.dream11.flocker.engine.modules.source.impl.AthenaSourceImpl;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.lit;

@Slf4j
@Data
public class S3Process {

    private final SparkSession sparkSession;
    private final List<Source<Dataset<Row>>> sources;
    private final List<Sink<String>> sinks;

    @Inject
    public S3Process(SparkSession sparkSession,
            @Named("sources") List<Source<Dataset<Row>>> sources,
            @Named("sinks") List<Sink<String>> sinks) {
        this.sparkSession = sparkSession;
        this.sources = sources;
        this.sinks = sinks;
    }

    public void processWithQuery(String sqlQuery, String cohortName, String action, String expireAt) {
        try {
            log.info("Starting S3 processing with query: {}", sqlQuery);
            log.info("Cohort Name: {}, Action: {}", cohortName, action);

            if (sources.isEmpty()) {
                throw new IllegalStateException("No sources configured");
            }

            Dataset<Row> unifiedData = null;
            for (Source<Dataset<Row>> source : sources) {
                Dataset<Row> data = source.read();
                if (unifiedData == null) {
                    unifiedData = data;
                } else {
                    unifiedData = unifiedData.union(data);
                }
            }

            if (unifiedData == null) {
                throw new IllegalStateException("Failed to read data from sources");
            }

            unifiedData.cache();
            long rowCount = unifiedData.count();
            log.info("Successfully read data from {} sources. Total row count: {}", sources.size(), rowCount);

            Dataset<Row> queryResult;
            if (sources.size() == 1 && sources.get(0) instanceof AthenaSourceImpl) {
                log.info("Athena source detected - query already executed on Athena, using results directly");
                queryResult = unifiedData;
            } else {
                unifiedData.createOrReplaceTempView("source_data");
                log.debug("Created temporary view 'source_data'");
                queryResult = sparkSession.sql(sqlQuery);
            }

            queryResult.cache();
            long resultCount = queryResult.count();
            log.info("Query result row count: {}", resultCount);

            Dataset<Row> resultWithFields = queryResult;

            String[] columns = queryResult.columns();
            if (columns.length == 0) {
                throw new IllegalStateException("Query result has no columns");
            }

            String firstColumnName = columns[0];
            log.debug("First column name: {}", firstColumnName);

            if (!"user_id".equalsIgnoreCase(firstColumnName)) {
                resultWithFields = resultWithFields.withColumnRenamed(firstColumnName, "user_id");
                log.debug("Renamed column '{}' to 'user_id'", firstColumnName);
            }

            resultWithFields = resultWithFields.select("user_id");

            if (cohortName != null && !cohortName.isEmpty()) {
                resultWithFields = resultWithFields.withColumn("cohort_key",
                        lit(cohortName));
                log.debug("Added cohort_key field: {}", cohortName);
            } else {
                resultWithFields = resultWithFields.withColumn("cohort_key",
                        lit(""));
            }

            if (action != null && !action.isEmpty()) {
                resultWithFields = resultWithFields.withColumn("action",
                        lit(action));
                log.debug("Added action field: {}", action);
            } else {
                resultWithFields = resultWithFields.withColumn("action",
                        lit("append"));
            }

            if (expireAt != null && !expireAt.isEmpty()) {
                resultWithFields = resultWithFields.withColumn("expire_at",
                        lit(expireAt));
                log.debug("Added expire_at field: {}", expireAt);
            } else {
                resultWithFields = resultWithFields.withColumn("expire_at",
                        lit(""));
            }

            resultWithFields = resultWithFields.select("user_id", "cohort_key", "action", "expire_at");

            log.info("Transformed data structure - columns: {}", java.util.Arrays.toString(resultWithFields.columns()));

            log.info("Writing results to {} sinks...", sinks.size());
            for (Sink<String> sink : sinks) {
                try {
                    sink.writeDataset(resultWithFields);
                    log.info("Successfully wrote dataset to sink: {}", sink.getClass().getSimpleName());
                } catch (UnsupportedOperationException e) {
                    log.warn("Sink does not support writeDataset, falling back to individual writes", e);
                    writeRowsToSink(resultWithFields, sink, sink.getClass().getSimpleName());
                } catch (Exception e) {
                    log.error("Failed to write to sink: {}", sink.getClass().getSimpleName(), e);
                    throw new RuntimeException("Failed to write to sink", e);
                }

                try {
                    sink.flush();
                } catch (Exception e) {
                    log.debug("Sink flush failed or not supported", e);
                }
            }

        } catch (Exception e) {
            log.error("Error processing S3 data with query", e);
            throw new RuntimeException("Failed to process S3 data with query", e);
        }
    }

    private void writeRowsToSink(Dataset<Row> dataset, Sink<String> sink, String sinkName) {
        log.warn("Using collectAsList for {} - this may cause memory issues with large datasets", sinkName);
        List<Row> rows = dataset.collectAsList();
        log.info("Collected {} rows to send to {}", rows.size(), sinkName);

        for (Row row : rows) {
            try {
                String json = convertRowToJsonManually(row);
                sink.write(json);
            } catch (Exception e) {
                log.error("Error writing row to sink: {}", sinkName, e);
                throw new RuntimeException("Failed to write row to sink: " + sinkName, e);
            }
        }

        log.info("Successfully sent {} rows to {}", rows.size(), sinkName);
    }

    private String convertRowToJsonManually(Row row) {
        StringBuilder json = new StringBuilder("{");
        String[] fieldNames = row.schema().fieldNames();
        for (int i = 0; i < fieldNames.length; i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("\"").append(fieldNames[i]).append("\":");
            Object value = row.get(i);
            if (value == null) {
                json.append("null");
            } else if (value instanceof String) {
                json.append("\"").append(escapeJson(value.toString())).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else {
                json.append("\"").append(escapeJson(value.toString())).append("\"");
            }
        }
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }
}

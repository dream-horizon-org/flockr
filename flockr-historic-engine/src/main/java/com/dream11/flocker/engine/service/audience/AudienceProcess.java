package com.dream11.flocker.engine.service.audience;

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

/**
 * Core processing service for the Flocker Historic Engine.
 * 
 * <p>This class orchestrates the data processing pipeline:
 * <ol>
 *   <li>Reads data from the configured source (e.g., Athena)</li>
 *   <li>Applies SQL queries if needed</li>
 *   <li>Transforms data to standardized format (user_id, audience_key, action, expire_at)</li>
 *   <li>Writes results to all configured sinks (S3, API, Kafka)</li>
 * </ol>
 * 
 * <p><b>Data Transformation:</b>
 * <p>The service transforms query results into a standardized schema:
 * <ul>
 *   <li><b>user_id</b>: Extracted from the first column of the query result. 
 *       Accepts column names: userId, userid, or user_id (case-insensitive)</li>
 *   <li><b>audience_key</b>: Set to the audience name (maps to audienceName from application arguments)</li>
 *   <li><b>action</b>: Set to "append" or "remove"</li>
 *   <li><b>expire_at</b>: Set to the expiration timestamp (epoch seconds)</li>
 * </ul>
 * 
 * <p><b>Source Handling:</b>
 * <p>For Athena source, the SQL query is already executed by the source,
 * so the results are used directly. For other source types, the SQL query may
 * be applied to the read data.
 * 
 * <p><b>Sink Writing:</b>
 * <p>The service attempts to write using {@link Sink#writeDataset(Dataset)} first
 * (preferred for distributed processing). If not supported, it falls back to
 * collecting rows and writing individually (may cause memory issues with large datasets).
 * 
 * @see Source
 * @see Sink
 * @author Shivam-Raghuwanshi
 */
@Slf4j
@Data
public class AudienceProcess {

    /** Spark session for distributed data processing. */
    private final SparkSession sparkSession;
    
    /** Single configured data source. */
    private final Source<Dataset<Row>> source;
    
    /** List of configured data sinks. */
    private final List<Sink<String>> sinks;

    /**
     * Checks if a column name is a valid user ID column (userId, userid, or user_id, case-insensitive).
     * 
     * @param columnName The column name to check.
     * @return true if the column name matches any of the valid user ID column names.
     */
    private boolean isValidUserIdColumn(String columnName) {
        if (columnName == null) {
            return false;
        }
        String lowerColumnName = columnName.toLowerCase();
        return "user_id".equals(lowerColumnName) 
            || "userid".equals(lowerColumnName) 
            || "userid".equals(lowerColumnName.replace("_", ""));
    }

    /**
     * Creates a new AudienceProcess instance with the specified dependencies.
     * 
     * <p>This constructor is used by Guice dependency injection to provide
     * the Spark session, source, and sinks.
     * 
     * @param sparkSession The Spark session for data processing.
     * @param source Single data source to read from.
     * @param sinks List of data sinks to write to.
     */
    @Inject
    public AudienceProcess(SparkSession sparkSession,
            @Named("source") Source<Dataset<Row>> source,
            @Named("sinks") List<Sink<String>> sinks) {
        this.sparkSession = sparkSession;
        this.source = source;
        this.sinks = sinks;
    }

    /**
     * Processes data with the given query and writes results to all configured sinks.
     * 
     * <p>This method:
     * <ol>
     *   <li>Reads data from the configured source</li>
     *   <li>Applies SQL query if needed (for non-Athena sources)</li>
     *   <li>Transforms data to standardized format</li>
     *   <li>Writes to all configured sinks</li>
     * </ol>
     * 
     * <p><b>Data Format:</b>
     * <p>The output dataset has the following schema:
     * <ul>
     *   <li>user_id: Long or String (from first column of query result)</li>
     *   <li>audience_key: String (set to audienceName from application arguments)</li>
     *   <li>action: String ("append" or "remove")</li>
     *   <li>expire_at: Long (epoch timestamp in seconds)</li>
     * </ul>
     * 
     * @param sqlQuery The SQL query to execute (may be ignored for Athena sources).
     * @param audienceName The name of the audience/cohort.
     * @param action The action to perform ("append" or "remove").
     * @param expireAt The expiration timestamp in epoch seconds.
     * @throws IllegalStateException If source is not configured or data reading fails.
     * @throws RuntimeException If writing to any sink fails.
     */
    public void processWithQuery(String sqlQuery, String audienceName, String action, Long expireAt) {
        try {
            log.info("Starting audience processing with query: {}", sqlQuery);
            log.info("Audience Name: {}, Action: {}", audienceName, action);

            if (source == null) {
                throw new IllegalStateException("Source is not configured");
            }

            Dataset<Row> data = source.read();
            if (data == null) {
                throw new IllegalStateException("Failed to read data from source");
            }

            data.cache();
            long rowCount = data.count();
            log.info("Successfully read data from source. Total row count: {}", rowCount);

            Dataset<Row> queryResult;
            if (source instanceof AthenaSourceImpl) {
                log.info("Athena source detected - query already executed on Athena, using results directly");
                queryResult = data;
            } else {
                data.createOrReplaceTempView("source_data");
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

            // Check if the first column is a valid user ID column (userId, userid, or user_id, case-insensitive)
            if (!isValidUserIdColumn(firstColumnName)) {
                resultWithFields = resultWithFields.withColumnRenamed(firstColumnName, "user_id");
                log.debug("Renamed column '{}' to 'user_id'", firstColumnName);
            } else {
                // Normalize to user_id if it's a valid user ID column but not already user_id
                String lowerColumnName = firstColumnName.toLowerCase();
                if (!"user_id".equals(lowerColumnName)) {
                    resultWithFields = resultWithFields.withColumnRenamed(firstColumnName, "user_id");
                    log.debug("Normalized column '{}' to 'user_id'", firstColumnName);
                }
            }

            resultWithFields = resultWithFields.select("user_id");

            if (audienceName != null && !audienceName.isEmpty()) {
                resultWithFields = resultWithFields.withColumn("audience_key",
                        lit(audienceName));
                log.debug("Added audience_key field: {}", audienceName);
            } else {
                resultWithFields = resultWithFields.withColumn("audience_key",
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

            if (expireAt != null) {
                resultWithFields = resultWithFields.withColumn("expire_at",
                        lit(expireAt));
                log.debug("Added expire_at field: {} (epoch timestamp)", expireAt);
            } else {
                resultWithFields = resultWithFields.withColumn("expire_at",
                        lit(0L));
            }

            resultWithFields = resultWithFields.select("user_id", "audience_key", "action", "expire_at");

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
            log.error("Error processing audience data with query", e);
            throw new RuntimeException("Failed to process audience data with query", e);
        }
    }

    /**
     * Writes dataset rows to a sink that doesn't support writeDataset.
     * 
     * <p>This is a fallback method that collects all rows to the driver and
     * writes them individually. This may cause memory issues with large datasets.
     * 
     * <p><b>Warning:</b> This method uses {@link Dataset#collectAsList()} which
     * brings all data to the driver node. For large datasets, this can cause
     * out-of-memory errors. Sinks should implement {@link Sink#writeDataset(Dataset)}
     * to avoid this issue.
     * 
     * @param dataset The dataset to write.
     * @param sink The sink to write to.
     * @param sinkName The name of the sink (for logging).
     */
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

    /**
     * Converts a Spark Row to a JSON string manually.
     * 
     * <p>This method manually constructs JSON from a Row object, handling
     * different data types (String, Number, Boolean, null).
     * 
     * @param row The Spark Row to convert.
     * @return A JSON string representation of the row.
     */
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

    /**
     * Escapes special characters in a string for JSON encoding.
     * 
     * <p>Escapes the following characters:
     * <ul>
     *   <li>Backslash (\) → \\</li>
     *   <li>Double quote (") → \"</li>
     *   <li>Newline (\n) → \n</li>
     *   <li>Carriage return (\r) → \r</li>
     *   <li>Tab (\t) → \t</li>
     *   <li>Backspace (\b) → \b</li>
     *   <li>Form feed (\f) → \f</li>
     * </ul>
     * 
     * @param str The string to escape.
     * @return The escaped string, or empty string if input is null.
     */
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


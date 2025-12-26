package com.dream11.flocker.engine.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * Configuration for a single data source.
 * 
 * <p>This class represents the configuration for one data source in the engine.
 * The engine supports only a single source per execution, but this configuration
 * can be used for different source types (Athena, S3, Kafka, etc.).
 * 
 * <p><b>Example JSON:</b>
 * <pre>{@code
 * {
 *   "query": "SELECT DISTINCT userid FROM users WHERE age > 25",
 *   "type": "ATHENA",
 *   "config": {
 *     "region": "us-east-1",
 *     "workgroup": "primary",
 *     "outputLocation": "s3://bucket/athena-output/",
 *     "accessKey": "AKIA...",
 *     "secretKey": "..."
 *   }
 * }
 * }</pre>
 * 
 * @see EngineArguments
 * @author Shivam-Raghuwanshi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceConfig {
    
    /**
     * SQL query to execute on the data source.
     * 
     * <p>For Athena sources, this is the SQL query that will be executed.
     * For other source types, this may be used differently or ignored.
     * 
     * <p><b>Note:</b> The first column of the query result should contain user IDs.
     * 
     * @return The SQL query string, or null if not set.
     */
    @JsonProperty("query")
    private String query;
    
    /**
     * Type of the data source.
     * 
     * <p>Supported values:
     * <ul>
     *   <li><b>ATHENA</b>: AWS Athena data source</li>
     *   <li><b>S3</b>: S3 data source</li>
     *   <li><b>KAFKA</b>: Kafka data source</li>
     * </ul>
     * 
     * <p>The type is case-insensitive and will be converted to uppercase.
     * 
     * @return The source type string, or null if not set.
     */
    @JsonProperty("type")
    private String type;
    
    /**
     * Source-specific configuration map.
     * 
     * <p>This map contains configuration parameters specific to the source type.
     * The structure depends on the source type:
     * 
     * <p><b>For ATHENA:</b>
     * <ul>
     *   <li>region: AWS region (e.g., "us-east-1")</li>
     *   <li>workgroup: Athena workgroup name</li>
     *   <li>outputLocation: S3 location for Athena query results</li>
     *   <li>accessKey: AWS access key ID</li>
     *   <li>secretKey: AWS secret access key</li>
     *   <li>sessionToken: Optional AWS session token for temporary credentials</li>
     *   <li>database: Optional database name (can be extracted from query)</li>
     * </ul>
     * 
     * @return The configuration map, or null if not set.
     */
    @JsonProperty("config")
    private Map<String, Object> config;
}


package com.dream11.flocker.engine;

import com.dream11.flocker.engine.bootstrap.EngineOrchestrator;
import lombok.extern.slf4j.Slf4j;

/**
 * Main entry point for the Flocker Historic Engine.
 *
 * <p>This class serves as the entry point and delegates all orchestration
 * responsibilities to the {@link EngineOrchestrator}.
 *
 * <p>The engine supports:
 * <ul>
 *   <li><b>Single Source</b>: One data source (Athena, S3, Kafka, etc.)</li>
 *   <li><b>Multiple Destinations</b>: Multiple sink configurations (S3, API, Kafka)</li>
 *   <li><b>Actions</b>: "append" or "remove" operations for cohort management</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * java -jar flockr-historic-engine.jar '{"audienceName":"cohort-1","action":"append",...}'
 * }</pre>
 *
 * <p><b>Expected command-line argument format:</b>
 * <pre>{@code
 * {
 *   "audienceName": "cohort-name",
 *   "action": "append|remove",
 *   "expireAt": 1735689599,
 *   "source": {
 *     "query": "SELECT DISTINCT userid FROM users",
 *     "type": "ATHENA",
 *     "config": { ... }
 *   },
 *   "destinationJson": [ ... ]
 * }
 * }</pre>
 *
 * @author Shivam-Raghuwanshi
 * @version 0.1.0-SNAPSHOT
 */
@Slf4j
public class EngineStart {

    /**
     * Main entry point for the Flocker Historic Engine.
     *
     * <p>This method:
     * <ol>
     *   <li>Creates an EngineOrchestrator instance</li>
     *   <li>Delegates execution to the orchestrator</li>
     *   <li>Handles errors and ensures proper exit codes</li>
     * </ol>
     *
     * <p>Resource cleanup (SparkSession) is handled internally by the orchestrator.
     *
     * @param args Command-line arguments. args[0] must contain the JSON configuration string.
     */
    public static void main(String[] args) {
        try {
            log.info("Starting Flocker Spark Engine...");

            EngineOrchestrator orchestrator = new EngineOrchestrator();
            orchestrator.execute(args);

        } catch (IllegalArgumentException e) {
            log.error("Invalid arguments: {}", e.getMessage(), e);
            System.exit(1);
        } catch (Exception e) {
            log.error("Error occurred while running Flocker Engine", e);
            System.exit(1);
        }
    }
}

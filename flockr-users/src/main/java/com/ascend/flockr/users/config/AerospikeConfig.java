package com.ascend.flockr.users.config;

import lombok.Data;

/**
 * Configuration class for Aerospike client and database settings.
 *
 * <p>This class holds all configuration properties needed to connect to and interact with an
 * Aerospike cluster. It includes both client connection settings and database-specific
 * configuration (namespace, set names, bin names).
 *
 * <p><strong>Configuration Properties:</strong>
 *
 * <ul>
 *   <li><strong>Connection Settings:</strong> host, event loop size, connection limits, queue
 *       limits
 *   <li><strong>Database Settings:</strong> namespace, set names, bin names for cohort storage
 * </ul>
 *
 * <p>This class is typically populated from configuration files (e.g., Typesafe Config) and
 * injected via Guice.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Data
public class AerospikeConfig {
  /** Aerospike cluster host address (host:port format). */
  private String host;

  /** Number of event loop threads for the Aerospike client. */
  private Integer eventLoopSize;

  /** Maximum number of commands that can be in process simultaneously. */
  private Integer maxCommandsInProcess;

  /** Maximum number of connections per Aerospike node. */
  private Integer maxConnectionsPerNode;

  /** Maximum number of commands that can be queued. */
  private Integer maxCommandsInQueue;

  /** Aerospike namespace where user records are stored. */
  private String namespace;

  /** Name of the Aerospike set for persistent cohort storage. */
  private String persistentCohortsSet;

  /** Name of the bin storing cohort creation timestamps (map: cohort name → timestamp). */
  private String cohortCreatedAtBin;

  /** Name of the bin storing cohort last update timestamps (map: cohort name → timestamp). */
  private String cohortUpdatedAtBin;

  /** Name of the bin storing cohort expiry timestamps (map: cohort name → expiry timestamp). */
  private String cohortExpiryBin;
}

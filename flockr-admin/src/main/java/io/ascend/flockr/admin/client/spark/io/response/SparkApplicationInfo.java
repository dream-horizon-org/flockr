package io.ascend.flockr.admin.client.spark.io.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a Spark application from History Server API.
 *
 * <p>Contains application metadata retrieved from the Spark History Server REST API endpoint: GET
 * /api/v1/applications
 *
 * @author Flockr Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SparkApplicationInfo {

  /** Spark Application ID (e.g., "app-20241220-123456") */
  private String id;

  /** Application name set via spark.app.name property */
  private String name;

  /**
   * Application state.
   *
   * <p>Possible values:
   *
   * <ul>
   *   <li>RUNNING - Application is currently executing
   *   <li>FINISHED - Application completed successfully
   *   <li>FAILED - Application failed with an error
   *   <li>KILLED - Application was terminated
   * </ul>
   */
  private String state;

  /** When the application started */
  private Instant startTime;

  /** When the application ended (null if still running) */
  private Instant endTime;

  /** Duration in milliseconds */
  private Long duration;

  /** User who submitted the application */
  private String user;
}

package io.ascend.flockr.admin.domain.audience;

/**
 * Enumeration of audience types that define how audience membership is determined.
 *
 * <p>The audience type determines what operations are allowed on an audience:
 *
 * <ul>
 *   <li>{@link #CONDITIONAL} - Supports batch and realtime rules for dynamic membership
 *   <li>{@link #STATIC} - Supports CSV uploads for manual membership management
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
public enum AudienceType {
  /**
   * Conditional audiences support dynamic membership determination through rules.
   *
   * <p>Features:
   *
   * <ul>
   *   <li>Batch rules: SQL queries executed on Apache Spark
   *   <li>Stream rules: Real-time event patterns processed by Apache Flink
   *   <li>Automatic membership updates based on rule execution
   * </ul>
   */
  CONDITIONAL,

  /**
   * Static audiences support manual membership management via CSV uploads.
   *
   * <p>Features:
   *
   * <ul>
   *   <li>CSV file upload for bulk user imports
   *   <li>Direct push to configured data sinks
   *   <li>No rules allowed - membership is explicitly defined
   * </ul>
   */
  STATIC
}

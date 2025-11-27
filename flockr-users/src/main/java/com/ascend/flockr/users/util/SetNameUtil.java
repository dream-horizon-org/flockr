package com.ascend.flockr.users.util;

import com.ascend.flockr.users.constants.BulkCohortAssignmentConstants;
import lombok.experimental.UtilityClass;

/**
 * Utility class for generating Aerospike set names from tenant and project identifiers.
 *
 * <p>This utility provides methods to construct set names that combine tenantId (UUID) and projectId (String) for
 * multi-tenant isolation in Aerospike.
 *
 * <p><strong>Set Name Format:</strong>
 *
 * <p>The set name is constructed as: {@code "{tenantId}_{projectId}"}
 *
 * <p><strong>Examples:</strong>
 *
 * <ul>
 *   <li>{@code generateSetName("550e8400-e29b-41d4-a716-446655440000", "project-100")} returns {@code "550e8400-e29b-41d4-a716-446655440000_project-100"}
 *   <li>{@code generateSetName("6ba7b810-9dad-11d1-80b4-00c04fd430c8", "project-200")} returns {@code "6ba7b810-9dad-11d1-80b4-00c04fd430c8_project-200"}
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
@UtilityClass
public class SetNameUtil {

  /**
   * Generates an Aerospike set name from tenant ID (UUID) and project ID (String).
   *
   * <p>The set name is constructed by combining tenantId and projectId with an underscore
   * separator. This ensures multi-tenant isolation in Aerospike.
   *
   * @param tenantId the tenant identifier (UUID format), must not be null or blank
   * @param projectId the project identifier (String), must not be null or blank
   * @return the generated set name in format "{tenantId}_{projectId}"
   * @throws IllegalArgumentException if tenantId is null/blank, invalid UUID, or projectId is null/blank
   */
  public static String generateSetName(String tenantId, String projectId) {
    if (tenantId == null || tenantId.trim().isEmpty()) {
      throw new IllegalArgumentException("tenantId cannot be null or blank");
    }
    if (projectId == null || projectId.trim().isEmpty()) {
      throw new IllegalArgumentException("projectId cannot be null or blank");
    }
    
    // Validate tenantId is a valid UUID
    if (!BulkCohortAssignmentConstants.UUID_PATTERN.matcher(tenantId.trim()).matches()) {
      throw new IllegalArgumentException("tenantId must be a valid UUID format");
    }
    
    return tenantId.trim() + "_" + projectId.trim();
  }

  /**
   * Validates that tenantId (UUID) and projectId (String) are both provided and valid.
   *
   * @param tenantId the tenant identifier (UUID) to validate
   * @param projectId the project identifier (String) to validate
   * @throws IllegalArgumentException if either parameter is invalid
   */
  public static void validateTenantAndProject(String tenantId, String projectId) {
    if (tenantId == null || tenantId.trim().isEmpty()) {
      throw new IllegalArgumentException("tenantId is required and cannot be null or blank");
    }
    
    // Validate tenantId is a valid UUID
    if (!BulkCohortAssignmentConstants.UUID_PATTERN.matcher(tenantId.trim()).matches()) {
      throw new IllegalArgumentException("tenantId must be a valid UUID format");
    }
    
    if (projectId == null || projectId.trim().isEmpty()) {
      throw new IllegalArgumentException("projectId is required and cannot be null or blank");
    }
  }
}

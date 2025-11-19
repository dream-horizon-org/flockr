package com.ascend.flockr.users.util;

import lombok.experimental.UtilityClass;

/**
 * Utility class for generating Aerospike set names from tenant and project identifiers.
 *
 * <p>This utility provides methods to construct set names that combine tenantId and projectId for
 * multi-tenant isolation in Aerospike.
 *
 * <p><strong>Set Name Format:</strong>
 *
 * <p>The set name is constructed as: {@code "{tenantId}_{projectId}"}
 *
 * <p><strong>Examples:</strong>
 *
 * <ul>
 *   <li>{@code generateSetName("tenant1", 100L)} returns {@code "tenant1_100"}
 *   <li>{@code generateSetName("tenant-abc", 200L)} returns {@code "tenant-abc_200"}
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
@UtilityClass
public class SetNameUtil {

  /**
   * Generates an Aerospike set name from tenant ID and project ID.
   *
   * <p>The set name is constructed by combining tenantId and projectId with an underscore
   * separator. This ensures multi-tenant isolation in Aerospike.
   *
   * @param tenantId the tenant identifier, must not be null or blank
   * @param projectId the project identifier, must not be null
   * @return the generated set name in format "{tenantId}_{projectId}"
   * @throws IllegalArgumentException if tenantId is null/blank or projectId is null
   */
  public static String generateSetName(String tenantId, Long projectId) {
    if (tenantId == null || tenantId.trim().isEmpty()) {
      throw new IllegalArgumentException("tenantId cannot be null or blank");
    }
    if (projectId == null) {
      throw new IllegalArgumentException("projectId cannot be null");
    }
    return tenantId.trim() + "_" + projectId;
  }

  /**
   * Validates that tenantId and projectId are both provided and valid.
   *
   * @param tenantId the tenant identifier to validate
   * @param projectId the project identifier to validate
   * @throws IllegalArgumentException if either parameter is invalid
   */
  public static void validateTenantAndProject(String tenantId, Long projectId) {
    if (tenantId == null || tenantId.trim().isEmpty()) {
      throw new IllegalArgumentException("tenantId is required and cannot be null or blank");
    }
    if (projectId == null || projectId <= 0) {
      throw new IllegalArgumentException("projectId is required and must be positive");
    }
  }
}

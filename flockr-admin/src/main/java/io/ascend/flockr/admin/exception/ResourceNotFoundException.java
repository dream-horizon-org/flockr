package io.ascend.flockr.admin.exception;

import lombok.Getter;

/**
 * Exception thrown when a requested resource is not found in the database.
 *
 * <p>This exception provides a standardized way to handle "not found" scenarios across the module,
 * ensuring consistent error responses for API consumers.
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * // Throwing for a specific resource
 * throw new ResourceNotFoundException("Audience", audienceId);
 *
 * // With custom message
 * throw new ResourceNotFoundException("RULE_NOT_FOUND", "Rule with ID 123 not found for audience 456");
 * }</pre>
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

  /** The error code for API responses. */
  private final String errorCode;

  /** The type of resource that was not found. */
  private final String resourceType;

  /** The identifier of the resource that was not found. */
  private final Object resourceId;

  /**
   * Constructs a ResourceNotFoundException with resource type and ID.
   *
   * @param resourceType the type of resource (e.g., "Audience", "Rule")
   * @param resourceId the identifier of the resource
   */
  public ResourceNotFoundException(String resourceType, Object resourceId) {
    super(String.format("%s with ID %s not found", resourceType, resourceId));
    this.errorCode = resourceType.toUpperCase() + "_NOT_FOUND";
    this.resourceType = resourceType;
    this.resourceId = resourceId;
  }

  /**
   * Constructs a ResourceNotFoundException with a custom error code and message.
   *
   * @param errorCode the error code for API responses
   * @param message the error message
   */
  public ResourceNotFoundException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
    this.resourceType = null;
    this.resourceId = null;
  }

  /**
   * Constructs a ResourceNotFoundException with resource details and a cause.
   *
   * @param resourceType the type of resource
   * @param resourceId the identifier of the resource
   * @param cause the underlying cause
   */
  public ResourceNotFoundException(String resourceType, Object resourceId, Throwable cause) {
    super(String.format("%s with ID %s not found", resourceType, resourceId), cause);
    this.errorCode = resourceType.toUpperCase() + "_NOT_FOUND";
    this.resourceType = resourceType;
    this.resourceId = resourceId;
  }
}

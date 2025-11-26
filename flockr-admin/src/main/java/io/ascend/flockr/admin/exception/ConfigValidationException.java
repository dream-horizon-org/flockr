package io.ascend.flockr.admin.exception;

/**
 * Exception thrown when connector configuration validation fails. This exception should be caught
 * and converted to RestException in REST layer.
 */
public class ConfigValidationException extends RuntimeException {

  private final String connectorType;
  private final String connectorKind;
  private final String errorCode;

  public ConfigValidationException(String message) {
    super(message);
    this.connectorType = null;
    this.connectorKind = null;
    this.errorCode = "CONFIG_VALIDATION_FAILED";
  }

  public ConfigValidationException(String message, Throwable cause) {
    super(message, cause);
    this.connectorType = null;
    this.connectorKind = null;
    this.errorCode = "CONFIG_VALIDATION_FAILED";
  }

  public ConfigValidationException(String connectorType, String connectorKind, String message) {
    super(message);
    this.connectorType = connectorType;
    this.connectorKind = connectorKind;
    this.errorCode = "CONFIG_VALIDATION_FAILED";
  }

  public ConfigValidationException(
      String connectorType, String connectorKind, String message, Throwable cause) {
    super(message, cause);
    this.connectorType = connectorType;
    this.connectorKind = connectorKind;
    this.errorCode = "CONFIG_VALIDATION_FAILED";
  }

  public ConfigValidationException(String errorCode, String message) {
    super(message);
    this.connectorType = null;
    this.connectorKind = null;
    this.errorCode = errorCode;
  }

  public String getConnectorType() {
    return connectorType;
  }

  public String getConnectorKind() {
    return connectorKind;
  }

  public String getErrorCode() {
    return errorCode;
  }
}

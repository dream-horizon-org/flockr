package io.ascend.flockr.admin.exception;

/**
 * Exception thrown when connector configuration parsing fails. This exception should be caught and
 * converted to RestException in REST layer.
 */
public class ConfigParsingException extends RuntimeException {

  private final String connectorKind;
  private final String errorCode;

  public ConfigParsingException(String message) {
    super(message);
    this.connectorKind = null;
    this.errorCode = "CONFIG_PARSING_FAILED";
  }

  public ConfigParsingException(String message, Throwable cause) {
    super(message, cause);
    this.connectorKind = null;
    this.errorCode = "CONFIG_PARSING_FAILED";
  }

  public ConfigParsingException(String connectorKind, String message) {
    super(message);
    this.connectorKind = connectorKind;
    this.errorCode = "CONFIG_PARSING_FAILED";
  }

  public ConfigParsingException(String connectorKind, String message, Throwable cause) {
    super(message, cause);
    this.connectorKind = connectorKind;
    this.errorCode = "CONFIG_PARSING_FAILED";
  }

  public String getConnectorKind() {
    return connectorKind;
  }

  public String getErrorCode() {
    return errorCode;
  }
}

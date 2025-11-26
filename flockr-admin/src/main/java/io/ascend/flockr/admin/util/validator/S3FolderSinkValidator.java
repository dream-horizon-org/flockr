package io.ascend.flockr.admin.util.validator;

import com.google.inject.Singleton;
import io.ascend.flockr.admin.exception.ConfigValidationException;
import io.vertx.core.json.JsonObject;

@Deprecated
/** Validator for S3 folder sink configurations. */
@Singleton
public class S3FolderSinkValidator implements ConfigValidator {

  @Override
  public void validate(JsonObject config) {
    if (config == null) {
      throw new ConfigValidationException(
          getConnectorType(), getConnectorKind(), "S3 folder sink config is required");
    }

    // Required: bucket name
    if (isBlank(config.getString("bucket"))) {
      throw new ConfigValidationException(
          getConnectorType(), getConnectorKind(), "S3 folder sink config requires 'bucket' field");
    }

    // Required: folder path
    String folderPath = config.getString("folderPath");
    if (isBlank(folderPath)) {
      throw new ConfigValidationException(
          getConnectorType(),
          getConnectorKind(),
          "S3 folder sink config requires 'folderPath' field");
    }

    // Validate folder path format
    String trimmed = folderPath.trim();
    // S3 paths should not start with '/' and should not end with '/'
    if (trimmed.startsWith("/")) {
      throw new ConfigValidationException(
          getConnectorType(),
          getConnectorKind(),
          "S3 folder sink config 'folderPath' should not start with '/'");
    }
    if (trimmed.endsWith("/")) {
      throw new ConfigValidationException(
          getConnectorType(),
          getConnectorKind(),
          "S3 folder sink config 'folderPath' should not end with '/'");
    }
    // Basic validation: should not contain consecutive slashes
    if (trimmed.contains("//")) {
      throw new ConfigValidationException(
          getConnectorType(),
          getConnectorKind(),
          "S3 folder sink config 'folderPath' should not contain consecutive slashes");
    }

    // Optional: validate region format if present
    String region = config.getString("region");
    if (region != null && !isBlank(region)) {
      // Basic AWS region format validation: e.g., us-east-1, eu-west-1
      if (!region.matches("^[a-z]{2}-[a-z]+-[0-9]+$")) {
        throw new ConfigValidationException(
            getConnectorType(),
            getConnectorKind(),
            "S3 folder sink config 'region' field must be a valid AWS region format (e.g., us-east-1)");
      }
    }

    // Validate AWS credentials if provided
    String accessKey = config.getString("accessKey");
    String secretKey = config.getString("secretKey");

    // If one credential is provided, both must be provided
    if ((accessKey != null && isBlank(accessKey)) || (secretKey != null && isBlank(secretKey))) {
      throw new ConfigValidationException(
          getConnectorType(),
          getConnectorKind(),
          "S3 folder sink config: if AWS credentials are provided, both 'accessKey' and 'secretKey' must be non-empty");
    }

    // Validate access key format if provided
    if (accessKey != null && !isBlank(accessKey)) {
      if (secretKey == null || isBlank(secretKey)) {
        throw new ConfigValidationException(
            getConnectorType(),
            getConnectorKind(),
            "S3 folder sink config: 'secretKey' is required when 'accessKey' is provided");
      }
      // AWS access keys are typically 20 characters, alphanumeric
      if (!accessKey.matches("^[A-Z0-9]{20}$")) {
        throw new ConfigValidationException(
            getConnectorType(),
            getConnectorKind(),
            "S3 folder sink config 'accessKey' must be a valid AWS access key format (20 uppercase alphanumeric characters)");
      }
    }

    // Validate secret key format if provided
    if (secretKey != null && !isBlank(secretKey)) {
      if (accessKey == null || isBlank(accessKey)) {
        throw new ConfigValidationException(
            getConnectorType(),
            getConnectorKind(),
            "S3 folder sink config: 'accessKey' is required when 'secretKey' is provided");
      }
      // AWS secret keys are typically 40 characters, base64-like
      if (secretKey.length() < 40) {
        throw new ConfigValidationException(
            getConnectorType(),
            getConnectorKind(),
            "S3 folder sink config 'secretKey' must be at least 40 characters long");
      }
    }

    // Optional: validate file format if present
    String fileFormat = config.getString("fileFormat");
    if (fileFormat != null && !isBlank(fileFormat)) {
      String normalized = fileFormat.toLowerCase();
      if (!normalized.equals("json")
          && !normalized.equals("csv")
          && !normalized.equals("parquet")) {
        throw new ConfigValidationException(
            getConnectorType(),
            getConnectorKind(),
            "S3 folder sink config 'fileFormat' must be one of: json, csv, parquet");
      }
    }
  }

  @Override
  public String getConnectorType() {
    return "S3_FOLDER";
  }

  @Override
  public String getConnectorKind() {
    return "SINK";
  }

  private boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }
}

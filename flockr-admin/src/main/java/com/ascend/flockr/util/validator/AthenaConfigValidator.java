package com.ascend.flockr.util.validator;

import com.ascend.flockr.exception.ConfigValidationException;
import com.ascend.flockr.util.ConfigValidator;
import com.google.inject.Singleton;
import io.vertx.core.json.JsonObject;

/** Validator for Athena data source configurations. */
@Singleton
public class AthenaConfigValidator implements ConfigValidator {

  @Override
  public void validate(JsonObject config) {
    if (config == null) {
      throw new ConfigValidationException(
          getConnectorType(), getConnectorKind(), "Athena config is required");
    }

    // Minimum requirement: query string
    if (isBlank(config.getString("query"))) {
      throw new ConfigValidationException(
          getConnectorType(),
          getConnectorKind(),
          "Athena config requires 'query' field");
    }

    // Optional: validate query is not empty
    String query = config.getString("query");
    if (query != null && query.trim().isEmpty()) {
      throw new ConfigValidationException(
          getConnectorType(),
          getConnectorKind(),
          "Athena config 'query' field cannot be empty");
    }

    // Optional: validate database field if present
    String database = config.getString("database");
    if (database != null && isBlank(database)) {
      throw new ConfigValidationException(
          getConnectorType(),
          getConnectorKind(),
          "Athena config 'database' field cannot be blank if provided");
    }

    // Optional: validate region format if present
    String region = config.getString("region");
    if (region != null && !isBlank(region)) {
      // Basic AWS region format validation: e.g., us-east-1, eu-west-1
      if (!region.matches("^[a-z]{2}-[a-z]+-[0-9]+$")) {
        throw new ConfigValidationException(
            getConnectorType(),
            getConnectorKind(),
            "Athena config 'region' field must be a valid AWS region format (e.g., us-east-1)");
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
          "Athena config: if AWS credentials are provided, both 'accessKey' and 'secretKey' must be non-empty");
    }

    // Validate access key format if provided (AWS access keys are typically 20 characters)
    if (accessKey != null && !isBlank(accessKey)) {
      if (secretKey == null || isBlank(secretKey)) {
        throw new ConfigValidationException(
            getConnectorType(),
            getConnectorKind(),
            "Athena config: 'secretKey' is required when 'accessKey' is provided");
      }
      // AWS access keys are typically 20 characters, alphanumeric
      if (!accessKey.matches("^[A-Z0-9]{20}$")) {
        throw new ConfigValidationException(
            getConnectorType(),
            getConnectorKind(),
            "Athena config 'accessKey' must be a valid AWS access key format (20 uppercase alphanumeric characters)");
      }
    }

    // Validate secret key format if provided (AWS secret keys are typically 40 characters)
    if (secretKey != null && !isBlank(secretKey)) {
      if (accessKey == null || isBlank(accessKey)) {
        throw new ConfigValidationException(
            getConnectorType(),
            getConnectorKind(),
            "Athena config: 'accessKey' is required when 'secretKey' is provided");
      }
      // AWS secret keys are typically 40 characters, base64-like
      if (secretKey.length() < 40) {
        throw new ConfigValidationException(
            getConnectorType(),
            getConnectorKind(),
            "Athena config 'secretKey' must be at least 40 characters long");
      }
    }
  }

  @Override
  public String getConnectorType() {
    return "ATHENA";
  }

  @Override
  public String getConnectorKind() {
    return "SOURCE"; // Athena is only used as a source
  }

  private boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }
}

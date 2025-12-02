package io.ascend.flockr.admin.util.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import io.ascend.flockr.admin.exception.ConfigValidationException;
import io.vertx.core.json.JsonObject;
import java.util.Set;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for validating JsonObjects against JSON Schema.
 *
 * <p>This utility uses the NetworkNT json-schema-validator library to validate Vert.x JsonObjects
 * against JSON Schema specifications stored in the database.
 *
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class JsonSchemaValidationUtil {

  @Inject private static JsonSchemaFactory schemaFactory;
  @Inject private static ObjectMapper objectMapper;

  /**
   * Validates a JsonObject against a JSON Schema.
   *
   * @param data the JsonObject to validate
   * @param schema the JSON Schema as JsonObject (must be valid JSON Schema format)
   * @throws ConfigValidationException if validation fails or schema is invalid
   */
  public static void validate(JsonObject data, JsonObject schema) {
    if (schema == null || schema.isEmpty()) {
      log.warn("Schema is null or empty, skipping validation");
      return;
    }

    if (data == null) {
      throw new ConfigValidationException(
          "SCHEMA_VALIDATION_FAILED", "Data to validate cannot be null");
    }

    try {
      // Convert Vert.x JsonObject to Jackson JsonNode for NetworkNT validator
      JsonNode schemaNode = objectMapper.readTree(schema.encode());
      JsonNode dataNode = objectMapper.readTree(data.encode());
      // Load schema
      JsonSchema jsonSchema = schemaFactory.getSchema(schemaNode);
      // Validate
      Set<ValidationMessage> errors = jsonSchema.validate(dataNode);

      if (!errors.isEmpty()) {
        String errorMessage = buildValidationErrorMessage(errors);
        log.error("Schema validation failed: {}", errorMessage);
        throw new ConfigValidationException(
            "SCHEMA_VALIDATION_FAILED", "Configuration does not match schema: " + errorMessage);
      }

      log.debug("Successfully validated JsonObject against schema");

    } catch (ConfigValidationException e) {
      throw e; // Re-throw our custom exceptions
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      log.error("Failed to parse JSON for schema validation", e);
      throw new ConfigValidationException(
          "SCHEMA_VALIDATION_ERROR: Invalid JSON format: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Failed to validate configuration against schema", e);
      throw new ConfigValidationException(
          "SCHEMA_VALIDATION_ERROR: Failed to validate configuration against schema: "
              + e.getMessage(),
          e);
    }
  }

  /**
   * Builds a user-friendly error message from validation messages.
   *
   * @param errors the set of validation errors
   * @return formatted error message
   */
  private static String buildValidationErrorMessage(Set<ValidationMessage> errors) {
    if (errors.isEmpty()) {
      return "Unknown validation error";
    }

    if (errors.size() == 1) {
      ValidationMessage error = errors.iterator().next();
      return String.format("%s at path: %s", error.getMessage(), error.getPath());
    }

    StringBuilder sb = new StringBuilder("Multiple validation errors found:");
    errors.forEach(
        error ->
            sb.append(String.format("\n  - %s at path: %s", error.getMessage(), error.getPath())));

    return sb.toString();
  }
}

package io.ascend.flockr.admin.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import io.ascend.flockr.admin.exception.ConfigValidationException;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.util.Set;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Comprehensive JSON utility for the Flockr application.
 *
 * <p>This utility provides:
 *
 * <ul>
 *   <li><b>JSON Schema Validation:</b> Validate JsonObjects against JSON Schema specifications
 *   <li><b>Custom Deserializers:</b> Jackson deserializers for Vert.x JsonObject types
 * </ul>
 *
 * <h2>JSON Schema Validation</h2>
 *
 * <p>Uses the NetworkNT json-schema-validator library to validate Vert.x JsonObjects against JSON
 * Schema specifications stored in the database or configuration.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * JsonObject schema = new JsonObject()
 *     .put("type", "object")
 *     .put("properties", new JsonObject()
 *         .put("name", new JsonObject().put("type", "string"))
 *         .put("age", new JsonObject().put("type", "number")))
 *     .put("required", new JsonArray().add("name"));
 *
 * JsonObject data = new JsonObject()
 *     .put("name", "John")
 *     .put("age", 30);
 *
 * JsonUtil.validateAgainstSchema(data, schema); // Passes
 * }</pre>
 *
 * <h2>Custom Deserializers</h2>
 *
 * <p>Provides Jackson deserializers for seamless conversion between Jackson JsonNode and Vert.x
 * JsonObject types during JSON deserialization.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Slf4j
@UtilityClass
public final class JsonUtil {

  @Inject private static JsonSchemaFactory schemaFactory;
  @Inject private static ObjectMapper objectMapper;

  // ==================== JSON Schema Validation Methods ====================

  /**
   * Validates a JsonObject against a JSON Schema.
   *
   * <p>This method validates the provided data against the schema specification. Both the data and
   * schema should be valid JsonObject instances.
   *
   * @param data the JsonObject to validate
   * @param schema the JSON Schema as JsonObject (must be valid JSON Schema format)
   * @throws ConfigValidationException if validation fails or schema is invalid
   */
  public static void validateAgainstSchema(JsonObject data, JsonObject schema) {
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

  // ==================== Custom Jackson Deserializers ====================

  /**
   * Custom Jackson deserializer for Vert.x JsonObject.
   *
   * <p>This deserializer converts Jackson JsonNode instances to Vert.x JsonObject during JSON
   * deserialization. It's typically registered with an ObjectMapper for automatic use.
   *
   * <p>Usage with Jackson:
   *
   * <pre>{@code
   * ObjectMapper mapper = new ObjectMapper();
   * SimpleModule module = new SimpleModule();
   * module.addDeserializer(JsonObject.class, new JsonUtil.JsonObjectDeserializer());
   * mapper.registerModule(module);
   * }</pre>
   *
   * @author Prithu Sharma
   * @since 1.0
   */
  public static class JsonObjectDeserializer extends JsonDeserializer<JsonObject> {

    @Override
    public JsonObject deserialize(
        JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
      JsonNode node = jsonParser.getCodec().readTree(jsonParser);
      return new JsonObject(node.toString());
    }
  }
}

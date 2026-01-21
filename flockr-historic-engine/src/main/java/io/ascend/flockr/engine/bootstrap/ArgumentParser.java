package io.ascend.flockr.engine.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ascend.flockr.engine.config.EngineArguments;
import io.ascend.flockr.engine.config.SourceConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles parsing and validation of command-line arguments for the Flocker Historic Engine.
 *
 * <p>This class is responsible for:
 *
 * <ul>
 *   <li>Parsing JSON command-line arguments into EngineArguments
 *   <li>Validating all required fields are present and non-empty
 *   <li>Providing helpful error messages when validation fails
 * </ul>
 *
 * @author Shivam-Raghuwanshi
 */
@Slf4j
public class ArgumentParser {

  /**
   * Parses command-line arguments from JSON string.
   *
   * @param args Command-line arguments array. args[0] must contain JSON configuration.
   * @return Parsed EngineArguments object.
   * @throws IllegalArgumentException If arguments are invalid or missing required fields.
   */
  public EngineArguments parse(String[] args) {
    validateArgumentsPresent(args);

    EngineArguments engineArgs = parseJson(args[0]);
    validateEngineArguments(engineArgs);

    return engineArgs;
  }

  /** Validates that command-line arguments are present and shows usage if not. */
  private void validateArgumentsPresent(String[] args) {
    if (args.length < 1) {
      log.error("Usage: java -jar <jar-file> <ARGUMENTS_JSON>");
      log.error("Arguments JSON should contain:");
      log.error("  - audienceName: Audience/cohort name");
      log.error("  - action: Action type (append/remove)");
      log.error("  - expireAt: Expiration timestamp (Unix timestamp)");
      log.error("  - source: Single source configuration object with query, type, and config");
      log.error(
          "  - destinationJson: List of destination configurations as JSON array (optional, multiple destinations supported)");
      log.error("");
      log.error("Example JSON:");
      log.error("{");
      log.error("  \"audienceName\": \"test-cohort-001\",");
      log.error("  \"action\": \"append\",");
      log.error("  \"expireAt\": 1735689599,");
      log.error("  \"source\": {");
      log.error("    \"query\": \"SELECT DISTINCT userid FROM users WHERE age > 25\",");
      log.error("    \"type\": \"ATHENA\",");
      log.error("    \"config\": {");
      log.error("      \"region\": \"us-east-1\",");
      log.error("      \"workgroup\": \"primary\",");
      log.error("      \"outputLocation\": \"s3://test-bucket/athena-output/\",");
      log.error("      \"accessKey\": \"YOUR_ACCESS_KEY\",");
      log.error("      \"secretKey\": \"YOUR_SECRET_KEY\"");
      log.error("    }");
      log.error("  },");
      log.error("  \"destinationJson\": []");
      log.error("}");
      throw new IllegalArgumentException("Missing required command-line arguments");
    }
  }

  /** Parses JSON string into EngineArguments object. */
  private EngineArguments parseJson(String jsonString) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      EngineArguments engineArgs = objectMapper.readValue(jsonString, EngineArguments.class);
      log.info("Successfully parsed engine arguments from JSON");
      return engineArgs;
    } catch (Exception e) {
      log.error("Failed to parse arguments JSON: {}", e.getMessage(), e);
      log.error("Please ensure the JSON is valid and contains all required fields");
      throw new IllegalArgumentException("Invalid JSON arguments: " + e.getMessage(), e);
    }
  }

  /** Validates that all required fields in EngineArguments are present and valid. */
  private void validateEngineArguments(EngineArguments engineArgs) {
    if (engineArgs.getAudienceName() == null || engineArgs.getAudienceName().trim().isEmpty()) {
      throw new IllegalArgumentException("Audience name cannot be null or empty");
    }

    if (engineArgs.getAction() == null || engineArgs.getAction().trim().isEmpty()) {
      throw new IllegalArgumentException("Action cannot be null or empty");
    }

    if (engineArgs.getExpireAt() == null) {
      throw new IllegalArgumentException("expireAt cannot be null");
    }

    SourceConfig source = engineArgs.getSource();
    if (source == null) {
      throw new IllegalArgumentException("Source configuration cannot be null");
    }

    if (source.getQuery() == null || source.getQuery().trim().isEmpty()) {
      throw new IllegalArgumentException("Source query cannot be null or empty");
    }

    if (source.getType() == null || source.getType().trim().isEmpty()) {
      throw new IllegalArgumentException("Source type cannot be null or empty");
    }

    if (source.getConfig() == null || source.getConfig().isEmpty()) {
      throw new IllegalArgumentException("Source config cannot be null or empty");
    }
  }
}

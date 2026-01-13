package io.ascend.flockr.engine.utils;

import lombok.experimental.UtilityClass;
import org.apache.spark.sql.Row;

/**
 * Utility class for converting Spark Row objects to JSON strings.
 *
 * <p>This class provides methods for manually converting Spark Row objects to JSON format, handling
 * different data types (String, Number, Boolean, null) and escaping special characters
 * appropriately.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * String json = RowJsonConverter.convertRowToJson(row);
 * }</pre>
 *
 * <p><b>Note:</b> This is a fallback method for sinks that don't support writeDataset. For better
 * performance with large datasets, sinks should implement writeDataset directly.
 *
 * @author Shivam-Raghuwanshi
 */
@UtilityClass
public final class RowJsonConverter {

  /**
   * Converts a Spark Row to a JSON string manually.
   *
   * <p>This method manually constructs JSON from a Row object, handling different data types
   * (String, Number, Boolean, null).
   *
   * @param row The Spark Row to convert.
   * @return A JSON string representation of the row.
   */
  public static String convertRowToJson(Row row) {
    StringBuilder json = new StringBuilder("{");
    String[] fieldNames = row.schema().fieldNames();
    for (int i = 0; i < fieldNames.length; i++) {
      if (i > 0) {
        json.append(",");
      }
      json.append("\"").append(fieldNames[i]).append("\":");
      Object value = row.get(i);
      if (value == null) {
        json.append("null");
      } else if (value instanceof String) {
        json.append("\"").append(escapeJson(value.toString())).append("\"");
      } else if (value instanceof Number || value instanceof Boolean) {
        json.append(value);
      } else {
        json.append("\"").append(escapeJson(value.toString())).append("\"");
      }
    }
    json.append("}");
    return json.toString();
  }

  /**
   * Escapes special characters in a string for JSON encoding.
   *
   * <p>Escapes the following characters:
   *
   * <ul>
   *   <li>Backslash (\) → \\
   *   <li>Double quote (") → \"
   *   <li>Newline (\n) → \n
   *   <li>Carriage return (\r) → \r
   *   <li>Tab (\t) → \t
   *   <li>Backspace (\b) → \b
   *   <li>Form feed (\f) → \f
   * </ul>
   *
   * @param str The string to escape.
   * @return The escaped string, or empty string if input is null.
   */
  public static String escapeJson(String str) {
    if (str == null) {
      return "";
    }
    return str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace("\b", "\\b")
        .replace("\f", "\\f");
  }
}

package io.ascend.flockr.admin.domain.dataconnectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.ascend.flockr.admin.constants.dataconnectors.DataConnectorTypeConstants;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing a data connector type.
 *
 * <p>A connector type defines the category of data source or sink (e.g., KAFKA, POSTGRES, S3) and
 * includes a JSON schema that validates the configuration for connectors of this type.
 *
 * <p>Connector types are divided into two kinds:
 *
 * <ul>
 *   <li>SOURCE - Connectors that read data for rule processing
 *   <li>SINK - Connectors that export computed audience data
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataConnectorType {
  /** The unique identifier of the connector type. */
  private Long id;

  /** The kind of connector ("SOURCE" or "SINK"). */
  private String kind;

  /** The type identifier (e.g., "KAFKA", "POSTGRES", "S3"). */
  private String type;

  /** The human-readable display name for the connector type. */
  private String displayName;

  /** The JSON schema used to validate connector configurations. */
  private JsonObject configSchema;

  /** Whether this connector type is active and available for use. */
  private Boolean active;

  /**
   * Maps a database row to a DataConnectorType domain object.
   *
   * @param row the database row containing connector type data
   * @return a DataConnectorType instance with data from the row
   */
  public static DataConnectorType mapTypeRow(Row row) {
    return DataConnectorType.builder()
        .id(row.getLong(DataConnectorTypeConstants.ID))
        .kind(row.getString(DataConnectorTypeConstants.KIND))
        .type(row.getString(DataConnectorTypeConstants.TYPE))
        .displayName(row.getString(DataConnectorTypeConstants.DISPLAY_NAME))
        .configSchema(row.getJsonObject(DataConnectorTypeConstants.CONFIG_SCHEMA))
        .active(row.getBoolean(DataConnectorTypeConstants.IS_ACTIVE))
        .build();
  }
}

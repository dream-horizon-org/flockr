package io.ascend.flockr.admin.domain.dataconnectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.ascend.flockr.admin.constants.dataconnectors.DataConnectorTypeConstants;
import io.ascend.flockr.admin.constants.dataconnectors.DataSourceConstants;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing data source details.
 *
 * <p>A data source is an external system from which data can be read for processing by rules. This
 * class contains all metadata associated with a data source, including its configuration, type, and
 * status.
 *
 * <p>Data sources are used in rule configurations to specify where data should be read from for
 * audience computation.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataSourceDetails {
  /** The unique identifier of the data source. */
  private Long id;

  /** The name of the data source. */
  private String name;

  /** The ID of the connector type this source uses. */
  private Long typeId;

  /** The type identifier of the connector (e.g., "KAFKA", "POSTGRES"). */
  private String type;

  /** The configuration JSON containing connection details and settings. */
  private JsonObject config;

  /** The current status of the data source (e.g., "ACTIVE", "INACTIVE"). */
  private String status;

  /** The username of the user who created this data source. */
  private String createdBy;

  /**
   * Maps a database row to a DataSourceDetails domain object.
   *
   * @param row the database row containing source data
   * @return a DataSourceDetails instance with data from the row
   */
  public static DataSourceDetails mapSourceRow(Row row) {
    return DataSourceDetails.builder()
        .id(row.getLong(DataSourceConstants.ID))
        .name(row.getString(DataSourceConstants.NAME))
        .typeId(row.getLong(DataSourceConstants.TYPE_ID))
        .type(row.getString(DataConnectorTypeConstants.TYPE))
        .config(row.getJsonObject(DataSourceConstants.CONFIG))
        .status(row.getString(DataSourceConstants.STATUS))
        .createdBy(row.getString(DataSourceConstants.CREATED_BY))
        .build();
  }
}

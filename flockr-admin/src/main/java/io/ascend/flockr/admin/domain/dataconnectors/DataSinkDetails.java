package io.ascend.flockr.admin.domain.dataconnectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.ascend.flockr.admin.constants.dataconnectors.DataConnectorTypeConstants;
import io.ascend.flockr.admin.constants.dataconnectors.DataSinkConstants;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing data sink details.
 *
 * <p>A data sink is an external destination where computed audience data is sent. This class
 * contains all metadata associated with a data sink, including its configuration, type, and status.
 *
 * <p>Data sinks are associated with audiences to specify where the audience membership data should
 * be exported.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataSinkDetails {
  /** The unique identifier of the data sink. */
  private Long id;

  /** The name of the data sink. */
  private String name;

  /** The ID of the connector type this sink uses. */
  private Long typeId;

  /** The type identifier of the connector (e.g., "S3", "WEBHOOK"). */
  private String type;

  /** The configuration JSON containing connection details and settings. */
  private JsonObject config;

  /** The current status of the data sink (e.g., "ACTIVE", "INACTIVE"). */
  private String status;

  /** The username of the user who created this data sink. */
  private String createdBy;

  /**
   * Maps a database row to a DataSinkDetails domain object.
   *
   * @param row the database row containing sink data
   * @return a DataSinkDetails instance with data from the row
   */
  public static DataSinkDetails mapSinkRow(Row row) {
    return DataSinkDetails.builder()
        .id(row.getLong(DataSinkConstants.ID))
        .name(row.getString(DataSinkConstants.NAME))
        .typeId(row.getLong(DataSinkConstants.TYPE_ID))
        .type(row.getString(DataConnectorTypeConstants.TYPE))
        .config(row.getJsonObject(DataSinkConstants.CONFIG))
        .status(row.getString(DataSinkConstants.STATUS))
        .createdBy(row.getString(DataSinkConstants.CREATED_BY))
        .build();
  }
}

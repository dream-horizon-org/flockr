package io.ascend.flockr.engine.dto;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple POJO representing a user ID row from query results.
 *
 * @see AudienceUpdateRequest
 * @author Shivam-Raghuwanshi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserIdRow implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * The user ID extracted from the query result.
   *
   * <p>This field maps to the "user_id" column in the Dataset. Spark's Bean Encoder automatically
   * handles the snake_case to camelCase conversion.
   */
  private String user_id;
}

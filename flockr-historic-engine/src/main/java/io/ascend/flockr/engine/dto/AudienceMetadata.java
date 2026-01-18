package io.ascend.flockr.engine.dto;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata about an audience operation.
 *
 * <p>This DTO contains contextual information about the audience operation that needs to be passed
 * along with user IDs to sinks.
 *
 * @author Shivam-Raghuwanshi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudienceMetadata implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private String audienceName;

  private String action;

  private Long expireAt;
}

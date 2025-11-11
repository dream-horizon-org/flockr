package com.ascend.flockr.domain.audience;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Immutable POJO representing an Audience. */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class AudienceMeta {
  private String tenantId;
  private String projectId;
  private Long audienceId;
  private String name;
  private String description;
  private String type;
  private JsonNode customAudienceConfig;
  private List<Long> sinks;
  private Integer rulesCount;
  private Boolean verified;
  private Long userCount;
  private Long expireDate;
  private Long lastAudienceUpdatedAt;
  private Long createdAt;
  private Long updatedAt;
}

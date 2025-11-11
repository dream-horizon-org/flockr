package com.ascend.flockr.io.request;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateAudienceRequest {
  private String name;
  private String description;
  private String userCount;
  private JsonNode customAudienceConfig;
  private String type;
  private Long expiryDate;
  private List<Long> sinkIds;
}

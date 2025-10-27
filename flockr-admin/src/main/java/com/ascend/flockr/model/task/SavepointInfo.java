package com.ascend.flockr.model.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavepointInfo {

  private Long id;
  private Long taskId;
  private String jobId;
  private String savepointPath;
  private Long createdAt;
}

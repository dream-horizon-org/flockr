package com.ascend.flockr.model.task;

import lombok.Data;

@Data
public class JarDetail {
  private String jarId;
  private String s3Path;
  private String deploymentId;
  private Long createdAt; /* epoch seconds */
}

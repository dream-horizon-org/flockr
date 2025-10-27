package com.ascend.flockr.model.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Deprecated
@Getter
public class ChronicJobMetadata {

  private Long jobReferenceId;
  private Long taskId;
  /* private Long createdAt; (epoch seconds) */
  private Long nextExecutionTime; /* epoch seconds */
  private String requestId;
}

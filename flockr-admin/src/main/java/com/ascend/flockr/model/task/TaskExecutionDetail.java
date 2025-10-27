package com.ascend.flockr.model.task;

import com.ascend.flockr.util.stacktraceparser.ErrorLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionDetail {

  private String jobId;
  private Long taskId;
  private String jarId;
  private Long startTime;
  private Long endTime;
  private String state;
  private String requestId;
  private Boolean synced;
  private Long watermark;
  private Long createdAt;
  private Long updatedAt;
  private ErrorLog errorLog;
  private Long recordsProcessed;
}

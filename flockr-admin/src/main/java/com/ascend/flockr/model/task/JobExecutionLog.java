package com.ascend.flockr.model.task;

import com.ascend.flockr.util.stacktraceparser.ErrorLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionLog {

  private String id;
  private Long taskId;
  private String jarId;
  /* private Long startTime; (epoch seconds) */
  /* private Long endTime; (epoch seconds) */
  private String state;
  private String requestId;
  private Boolean synced;
  private Long watermark;
  /* private Long createdAt; (epoch seconds) */
  /* private Long updatedAt; (epoch seconds) */
  private ErrorLog errorLog;
  private Long recordsProcessed;

  public JobExecutionLog(Long taskId, String jarId) {
    this.taskId = taskId;
    this.jarId = jarId;
  }
}

package com.ascend.flockr.io.response;

import com.ascend.flockr.model.task.Task;
import com.ascend.flockr.model.task.TaskExecutionDetail;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionDetailResponse<T> {

  private Task task;
  private List<TaskExecutionDetail> jobExecutionLog;

  private T metadata;
}

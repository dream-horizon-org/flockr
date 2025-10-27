package com.ascend.flockr.io.response;

import com.ascend.flockr.model.task.SavepointInfo;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventStreamTaskExecutionDetailResponse {

  private List<SavepointInfo> savepointInfo;
}

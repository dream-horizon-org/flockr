package com.ascend.flockr.model.task;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class JobExecutionLogDetail extends JobExecutionLog {

  private Long startTime; /* epoch seconds */
  private Long endTime; /* epoch seconds */
}

package com.ascend.flockr.dao.taskdefinition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskSchedule {
  private String cronExpression;
  private Long startDate;
  private Long endDate;
}

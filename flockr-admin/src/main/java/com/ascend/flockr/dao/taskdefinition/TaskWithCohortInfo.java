package com.ascend.flockr.dao.taskdefinition;

import com.ascend.flockr.model.task.constant.TaskStatus;
import com.ascend.flockr.model.task.constant.TaskType;
import lombok.Data;

@Data
public class TaskWithCohortInfo {

    private Long id;
    private Long cohortId;
    private TaskType type;
    private Long startTime; /* epoch seconds */
    private Long endTime; /* epoch seconds */
    private String cronExpression; /* unix, UTC */
    private TaskStatus status;
    private Boolean expired;
    private Long cohortExpiry;
}


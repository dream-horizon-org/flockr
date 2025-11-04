package com.ascend.flockr.io.request;

import com.ascend.flockr.annotation.FutureEpoch;
import com.ascend.flockr.exception.DefinedException;
import com.ascend.flockr.exception.ErrorEntity;
import com.ascend.flockr.util.CronUtil;
import com.cronutils.model.CronType;
import com.cronutils.validation.Cron;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;

@Data
public class UpdateTaskRequest {

  @FutureEpoch(unit = FutureEpoch.TimeUnit.SECONDS)
  private Long endDate;

  @FutureEpoch(unit = FutureEpoch.TimeUnit.SECONDS)
  private Long startDate;

  @Cron(type = CronType.UNIX)
  private String cronExpression;

  @JsonSetter("endDate")
  public void setEndDate(Long endDate) {
    if (endDate != null) this.endDate = endDate / 1000;
  }

  @JsonSetter("startDate")
  public void setStartDate(Long startDate) {
    if (startDate != null) this.startDate = startDate / 1000;
  }

  public void validate() {
    if (startDate == null && endDate == null && cronExpression == null) {
      throw new DefinedException(ErrorEntity.INVALID_REQUEST);
    }
    validateCronExpression();
  }

  private void validateCronExpression() {
    if (cronExpression != null && !CronUtil.isMinimumHourlyCronDifference(cronExpression)) {
      throw new DefinedException(ErrorEntity.CRON_EXPRESSION_CONSTRAINT_VIOLATION);
    }
  }
}

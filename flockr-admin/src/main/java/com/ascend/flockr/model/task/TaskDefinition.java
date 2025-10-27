package com.ascend.flockr.model.task;

import com.ascend.flockr.io.request.AbstractSequence;
import com.ascend.flockr.model.task.rule.Rule;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TaskDefinition extends Task {

  private AbstractSequence sequence;
  private Rule rule;
  private Long updatedAt; /* epoch seconds */
  private String updatedBy;
  private Boolean resumeUsingLatestArtifact;
  private Boolean resumeFromSavepoint;
}

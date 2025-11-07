package com.ascend.flockr.domain.rule;

import com.ascend.flockr.domain.rule.enums.RuleType;
import io.reactivex.rxjava3.core.Completable;
import jakarta.validation.constraints.NotNull;

public abstract class Rule {
  private String jobName;

  @NotNull private RuleType ruleType;

  public abstract Completable validate();
}

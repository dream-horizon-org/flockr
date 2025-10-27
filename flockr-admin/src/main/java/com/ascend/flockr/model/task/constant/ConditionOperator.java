package com.ascend.flockr.model.task.constant;

import java.util.Arrays;
import java.util.Optional;

public enum ConditionOperator {
  equalsTo("="),
  notEqualsTo("!="),
  lessThan("<"),
  greaterThan(">"),
  lessThanEqualsTo("<="),
  greaterThanEqualsTo(">="),
  in("in"),
  contains("contains"),
  notContains("not_contains"),
  notIN("not_in"),
  inBetween("><");

  private final String sign;

  ConditionOperator(String sign) {
    this.sign = sign;
  }

  public static ConditionOperator fromSign(String sign) {
    Optional<ConditionOperator> conditionOperator =
        Arrays.stream(ConditionOperator.values()).filter(i -> i.sign().equals(sign)).findAny();
    return conditionOperator.orElse(null);
  }

  public String sign() {
    return sign;
  }
}

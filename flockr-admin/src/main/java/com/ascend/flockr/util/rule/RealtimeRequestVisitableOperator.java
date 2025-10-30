package com.ascend.flockr.util.rule;

public interface RealtimeRequestVisitableOperator {

  void accept(RealtimeRequestOperatorVisitor visitor);
}

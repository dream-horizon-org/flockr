package com.ascend.flockr.util.rule;

import com.ascend.flockr.io.request.RealTimeSequence;

public interface RealtimeRequestOperatorVisitor {

  void visit(RealTimeSequence.PropertyFilter.SingleValueOperator singleValueOperator);

  void visit(RealTimeSequence.PropertyFilter.ListOperator listOperator);

  void visit(RealTimeSequence.PropertyFilter.RangeOperator rangeOperator);
}

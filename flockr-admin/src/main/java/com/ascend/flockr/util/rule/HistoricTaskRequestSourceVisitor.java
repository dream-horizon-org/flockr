package com.ascend.flockr.util.rule;

import com.ascend.flockr.io.request.HistoricSequence;

public interface HistoricTaskRequestSourceVisitor {

  void visit(HistoricSequence.AthenaDataSource visitable);

  void visit(HistoricSequence.RedShiftDataSource visitable);
}

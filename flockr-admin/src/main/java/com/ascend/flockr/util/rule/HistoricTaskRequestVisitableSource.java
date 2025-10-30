package com.ascend.flockr.util.rule;

public interface HistoricTaskRequestVisitableSource {

  void accept(HistoricTaskRequestSourceVisitor visitor);
}

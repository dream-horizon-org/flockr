package com.ascend.flockr.util.rule;

import com.ascend.flockr.io.request.AbstractDestination;

public interface CohortRequestDestinationVisitor {

  String UC_D11_SOURCE = "Dream11";
  String UC_FANCODE_SOURCE = "FanCode";
  String UC_ACTION_APPEND = "append";
  String UC_ACTION_REMOVE = "remove";
  Boolean ICEBERG_VALUE_TRUE = Boolean.TRUE;
  Boolean ICEBERG_VALUE_FALSE = Boolean.FALSE;
  String COMA_ACTION_ADD = "ADD";
  String COMA_ACTION_REMOVE = "REMOVE";
  String COMA_CREATION_SOURCE_AUDIENCE_ENGINE = "audience_engine";

  void visit(AbstractDestination.CleverTapDestinationStruct cleverTapDestinationStruct);

  void visit(AbstractDestination.UserCohortDestinationStruct userCohortDestinationStruct);

  void visit(AbstractDestination.DataFeastDestinationStruct dataFeastDestinationStruct);

  void visit(AbstractDestination.IcebergDestinationStruct icebergDestinationStruct);

  void visit(AbstractDestination.ComaServiceDestinationStruct comaServiceDestinationStruct);

  void visit(
      AbstractDestination.UserCohortFanCodeDestinationStruct
          userCohortServiceFanCodeDestinationStruct);
}

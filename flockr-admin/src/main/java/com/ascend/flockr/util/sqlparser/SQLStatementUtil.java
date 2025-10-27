package com.ascend.flockr.util.sqlparser;

import io.reactivex.Completable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SQLStatementUtil {

  public static Completable validate(String query) {
    return SqlParser.parseSelectQuery(query).ignoreElement();
  }
}
